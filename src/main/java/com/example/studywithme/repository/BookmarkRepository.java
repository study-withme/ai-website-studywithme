package com.example.studywithme.repository;

import com.example.studywithme.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 사용자가 특정 게시글을 북마크했는지 확인
    boolean existsByUser_IdAndPost_Id(Integer userId, Long postId);

    // 사용자의 북마크 목록
    Page<Bookmark> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 사용자의 북마크 개수
    long countByUser_Id(Integer userId);

    // 사용자의 북마크 삭제
    void deleteByUser_IdAndPost_Id(Integer userId, Long postId);
}

