package com.example.studywithme.service;

import com.example.studywithme.entity.Bookmark;
import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.BookmarkRepository;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 북마크 토글 (북마크가 있으면 취소, 없으면 추가)
    @Transactional
    public boolean toggleBookmark(Integer userId, Long postId) {
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        if (postId == null) {
            throw new RuntimeException("게시글 ID가 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        boolean isBookmarked = bookmarkRepository.existsByUser_IdAndPost_Id(userId, postId);

        if (isBookmarked) {
            // 북마크 취소
            bookmarkRepository.deleteByUser_IdAndPost_Id(userId, postId);
            return false;
        } else {
            // 북마크 추가
            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setPost(post);
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    // 북마크 여부 확인
    public boolean isBookmarked(Integer userId, Long postId) {
        return bookmarkRepository.existsByUser_IdAndPost_Id(userId, postId);
    }

    // 사용자의 북마크 목록
    public Page<Bookmark> getBookmarks(Integer userId, Pageable pageable) {
        return bookmarkRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }
}

