package com.example.studywithme.repository;

import com.example.studywithme.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 사용자의 최근 대화 내역 조회 (최신순)
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE (cm.user.id = :userId OR (cm.user IS NULL AND :userId IS NULL)) " +
           "ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);

    /**
     * 사용자의 최근 N개 메시지 조회 (대화 맥락용)
     */
    @Query(value = "SELECT * FROM chat_messages WHERE (user_id = :userId OR (user_id IS NULL AND :userId IS NULL)) " +
           "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ChatMessage> findRecentMessages(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * 30일 이전 메시지 삭제 (스케줄러용)
     */
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.createdAt < :cutoffDate")
    int deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 사용자별 메시지 개수
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE (cm.user.id = :userId OR (cm.user IS NULL AND :userId IS NULL))")
    long countByUserId(@Param("userId") Integer userId);
}
