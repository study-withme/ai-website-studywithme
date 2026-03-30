package com.example.studywithme.comment.repository;

import com.example.studywithme.comment.entity.CommentLike;
import com.example.studywithme.comment.entity.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    boolean existsByUserIdAndCommentId(Integer userId, Long commentId);

    void deleteByUserIdAndCommentId(Integer userId, Long commentId);

    long countByCommentId(Long commentId);
}


