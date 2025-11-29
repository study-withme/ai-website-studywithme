package com.example.studywithme.repository;

import com.example.studywithme.entity.PostApplication;
import com.example.studywithme.entity.PostApplication.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostApplicationRepository extends JpaRepository<PostApplication, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Integer userId);
    Optional<PostApplication> findByPost_IdAndUser_Id(Long postId, Integer userId);
    int countByPost_IdAndStatus(Long postId, PostApplication.ApplicationStatus status);

    // 사용자의 총 지원 횟수
    long countByUser_Id(Integer userId);
    
    // 게시글의 지원 목록 조회
    List<PostApplication> findByPost_IdOrderByCreatedAtDesc(Long postId);
    List<PostApplication> findByPost_IdAndStatusOrderByCreatedAtDesc(Long postId, ApplicationStatus status);
    
    // 사용자의 지원 목록 조회
    List<PostApplication> findByUser_IdOrderByCreatedAtDesc(Integer userId);
    List<PostApplication> findByUser_IdAndStatusOrderByCreatedAtDesc(Integer userId, ApplicationStatus status);
}

