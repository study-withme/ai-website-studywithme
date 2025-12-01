package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyAttendanceRepository extends JpaRepository<StudyAttendance, Long> {
    Optional<StudyAttendance> findByStudySession_IdAndUser_Id(Long sessionId, Integer userId);
    
    // 사용자의 특정 날짜 출석 기록
    @Query("SELECT a FROM StudyAttendance a WHERE a.user.id = :userId " +
           "AND DATE(a.checkedAt) = :date")
    List<StudyAttendance> findByUserAndDate(@Param("userId") Integer userId, @Param("date") LocalDate date);
    
    // 세션의 출석 기록
    List<StudyAttendance> findByStudySession_Id(Long sessionId);
    
    // 사용자의 연속 출석 일수 계산용
    @Query("SELECT COUNT(DISTINCT DATE(a.checkedAt)) FROM StudyAttendance a " +
           "WHERE a.user.id = :userId AND a.studySession.studyGroup.id = :groupId " +
           "AND DATE(a.checkedAt) >= :startDate")
    Long countConsecutiveDays(@Param("userId") Integer userId, 
                              @Param("groupId") Long groupId,
                              @Param("startDate") LocalDate startDate);
}
