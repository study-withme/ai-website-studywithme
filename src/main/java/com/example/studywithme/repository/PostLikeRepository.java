package com.example.studywithme.repository;

import com.example.studywithme.entity.PostLike;
import com.example.studywithme.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    // 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    boolean existsByUserIdAndPostId(Integer userId, Long postId);

    // 사용자와 게시글로 좋아요 찾기
    Optional<PostLike> findByUserIdAndPostId(Integer userId, Long postId);

    // 게시글의 좋아요 수
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.postId = :postId")
    long countByPostId(@Param("postId") Long postId);

    // 특정 사용자가 누른 좋아요 수
    long countByUserId(Integer userId);

    // 사용자가 좋아요한 게시글 삭제
    void deleteByUserIdAndPostId(Integer userId, Long postId);
}

