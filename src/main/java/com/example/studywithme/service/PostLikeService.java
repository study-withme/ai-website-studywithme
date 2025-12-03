package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.PostLike;
import com.example.studywithme.repository.PostLikeRepository;
import com.example.studywithme.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;

    // 좋아요 토글 (좋아요가 있으면 취소, 없으면 추가)
    @Transactional
    public boolean toggleLike(Integer userId, Long postId) {
        if (postId == null) {
            throw new RuntimeException("게시글 ID가 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        boolean isLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);

        if (isLiked) {
            // 좋아요 취소
            postLikeRepository.deleteByUserIdAndPostId(userId, postId);
            int currentLikes = post.getLikeCount() != null ? post.getLikeCount() : 0;
            post.setLikeCount(Math.max(0, currentLikes - 1));
            postRepository.save(post);
            return false;
        } else {
            // 좋아요 추가
            PostLike postLike = new PostLike();
            postLike.setUserId(userId);
            postLike.setPostId(postId);
            postLikeRepository.save(postLike);
            int currentLikes = post.getLikeCount() != null ? post.getLikeCount() : 0;
            post.setLikeCount(currentLikes + 1);
            postRepository.save(post);

            // 알림: 게시글 작성자에게 좋아요 알림 (자기 자신 제외)
            try {
                if (post.getUser() != null && !post.getUser().getId().equals(userId)) {
                    notificationService.notify(
                            post.getUser().getId(),
                            "POST_LIKE",
                            "내 게시글에 좋아요가 눌렸습니다",
                            "'" + post.getTitle() + "' 게시글에 좋아요가 추가되었습니다.",
                            "/posts/" + postId
                    );
                }
            } catch (Exception ignored) {}
            return true;
        }
    }

    // 좋아요 여부 확인
    public boolean isLiked(Integer userId, Long postId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    // 게시글의 좋아요 수
    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }
}

