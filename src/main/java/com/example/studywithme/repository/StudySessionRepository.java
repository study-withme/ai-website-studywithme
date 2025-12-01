package com.example.studywithme.repository;

import com.example.studywithme.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    
    // 사용자의 활성 세션 조회
    Optional<StudySession> findByUser_IdAndStatus(Integer userId, StudySession.SessionStatus status);
    
    // 스터디 그룹의 활성 세션 목록
    List<StudySession> findByStudyGroup_IdAndStatus(Long studyGroupId, StudySession.SessionStatus status);
    
    // 사용자의 특정 날짜 세션 목록
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId " +
           "AND DATE(s.startTime) = :date")
    List<StudySession> findByUserAndDate(@Param("userId") Integer userId, @Param("date") LocalDate date);
    
    // 사용자의 오늘 세션 목록
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId " +
           "AND DATE(s.startTime) = CURRENT_DATE")
    List<StudySession> findTodaySessionsByUser(@Param("userId") Integer userId);
    
    // 스터디 그룹의 오늘 세션 목록
    @Query("SELECT s FROM StudySession s WHERE s.studyGroup.id = :groupId " +
           "AND DATE(s.startTime) = CURRENT_DATE")
    List<StudySession> findTodaySessionsByGroup(@Param("groupId") Long groupId);
}
