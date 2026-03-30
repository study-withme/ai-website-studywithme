package com.example.studywithme.comment.repository;

import com.example.studywithme.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost_IdAndDeletedFalseOrderByCreatedAtAsc(Long postId);
}


