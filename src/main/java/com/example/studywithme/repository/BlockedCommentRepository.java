package com.example.studywithme.repository;

import com.example.studywithme.entity.BlockedComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockedCommentRepository extends JpaRepository<BlockedComment, Long> {
    Optional<BlockedComment> findByCommentId(Long commentId);
    
    Page<BlockedComment> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<BlockedComment> findByStatusOrderByCreatedAtDesc(BlockedComment.BlockStatus status, Pageable pageable);
    
    @Query(value = "SELECT COUNT(*) FROM blocked_comments WHERE status = 'BLOCKED'", nativeQuery = true)
    long countBlocked();
}

