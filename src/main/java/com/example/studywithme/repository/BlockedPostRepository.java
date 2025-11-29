package com.example.studywithme.repository;

import com.example.studywithme.entity.BlockedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedPostRepository extends JpaRepository<BlockedPost, Long> {
    Optional<BlockedPost> findByPostId(Long postId);
    
    Page<BlockedPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<BlockedPost> findByStatusOrderByCreatedAtDesc(BlockedPost.BlockStatus status, Pageable pageable);
    
    @Query(value = "SELECT COUNT(*) FROM blocked_posts WHERE status = 'BLOCKED'", nativeQuery = true)
    long countBlocked();
}

