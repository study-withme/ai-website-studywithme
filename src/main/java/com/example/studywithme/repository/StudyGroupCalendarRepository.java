package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyGroupCalendarRepository extends JpaRepository<StudyGroupCalendar, Long> {
    // 특정 날짜의 일정 조회
    List<StudyGroupCalendar> findByStudyGroup_IdAndEventDate(Long studyGroupId, LocalDate eventDate);
    
    // 특정 월의 일정 조회
    @Query("SELECT c FROM StudyGroupCalendar c WHERE c.studyGroup.id = :groupId " +
           "AND YEAR(c.eventDate) = :year AND MONTH(c.eventDate) = :month " +
           "ORDER BY c.eventDate ASC, c.eventTime ASC")
    List<StudyGroupCalendar> findByStudyGroupAndMonth(@Param("groupId") Long groupId,
                                                        @Param("year") int year,
                                                        @Param("month") int month);
    
    // 특정 기간의 일정 조회
    @Query("SELECT c FROM StudyGroupCalendar c WHERE c.studyGroup.id = :groupId " +
           "AND c.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.eventDate ASC, c.eventTime ASC")
    List<StudyGroupCalendar> findByStudyGroupAndDateRange(@Param("groupId") Long groupId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);
}
