package com.example.studywithme.repository;

import com.example.studywithme.entity.PostApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostApplicationRepository extends JpaRepository<PostApplication, Long> {
    boolean existsByPost_IdAndUser_Id(Long postId, Integer userId);
    Optional<PostApplication> findByPost_IdAndUser_Id(Long postId, Integer userId);
    int countByPost_IdAndStatus(Long postId, PostApplication.ApplicationStatus status);

    // 사용자의 총 지원 횟수
    long countByUser_Id(Integer userId);
}

