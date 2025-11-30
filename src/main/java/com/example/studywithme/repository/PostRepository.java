package com.example.studywithme.repository;

import com.example.studywithme.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 사용자별 게시글 조회
    Page<Post> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    // 카테고리별 게시글 조회
    Page<Post> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    // 전체 게시글 최신순 조회
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 전체 게시글 인기순 조회 (좋아요 + 조회수 기준)
    @Query("SELECT p FROM Post p ORDER BY (COALESCE(p.likeCount, 0) * 2 + COALESCE(p.viewCount, 0) * 0.1) DESC, p.createdAt DESC")
    Page<Post> findAllByOrderByPopularityDesc(Pageable pageable);

    // 카테고리별 게시글 인기순 조회
    @Query("SELECT p FROM Post p WHERE p.category = :category ORDER BY (COALESCE(p.likeCount, 0) * 2 + COALESCE(p.viewCount, 0) * 0.1) DESC, p.createdAt DESC")
    Page<Post> findByCategoryOrderByPopularityDesc(@Param("category") String category, Pageable pageable);

    // 게시글 상세 조회 (작성자까지 로딩)
    @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Post> findByIdWithUser(@Param("id") Long id);

    // 제목 또는 내용으로 검색
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 사용자별 게시글 수
    long countByUser_Id(Integer userId);

    // 사용자가 작성한 게시글이 받은 총 좋아요 수
    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.user.id = :userId")
    long sumLikeCountByUserId(@Param("userId") Integer userId);

    // 작성자의 다른 게시글 상위 5개
    java.util.List<Post> findTop5ByUser_IdAndIdNotOrderByCreatedAtDesc(Integer userId, Long excludeId);
}

