package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 게시글 작성
    @Transactional
    public Post createPost(Integer userId, String title, String content, String category, String tags) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setTags(tags);
        post.setViewCount(0);
        post.setLikeCount(0);

        return postRepository.save(post);
    }

    // 게시글 수정
    @Transactional
    public Post updatePost(Long postId, Integer userId, String title, String content, String category, String tags) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("게시글을 수정할 권한이 없습니다.");
        }

        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setTags(tags);

        return postRepository.save(post);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, Integer userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    // 게시글 상세 조회 (조회수 증가, 좋아요 수 동기화)
    @Transactional
    public Post getPost(Long postId) {
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        Integer currentViews = post.getViewCount() != null ? post.getViewCount() : 0;
        post.setViewCount(currentViews + 1);
        
        // 좋아요 수 동기화 (실제 DB의 좋아요 수와 동기화)
        // PostLikeService를 주입받아 사용하거나, 여기서 직접 카운트할 수 있음
        // 일단은 기존 likeCount를 유지
        
        postRepository.save(post);

        return post;
    }

    // 게시글 목록 조회 (최신순)
    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 카테고리별 게시글 조회
    @Transactional(readOnly = true)
    public Page<Post> getPostsByCategory(String category, Pageable pageable) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    // 사용자별 게시글 조회
    @Transactional(readOnly = true)
    public Page<Post> getPostsByUserId(Integer userId, Pageable pageable) {
        return postRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(keyword, pageable);
    }

    // 게시글 존재 여부 확인
    @Transactional(readOnly = true)
    public boolean existsById(Long postId) {
        return postRepository.existsById(postId);
    }

    // 작성자의 다른 게시글 상위 5개
    @Transactional(readOnly = true)
    public java.util.List<Post> getOtherPostsByAuthor(Integer userId, Long excludePostId) {
        return postRepository.findTop5ByUser_IdAndIdNotOrderByCreatedAtDesc(userId, excludePostId);
    }
}

