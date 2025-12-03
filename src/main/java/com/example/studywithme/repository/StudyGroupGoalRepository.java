package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyGroupGoalRepository extends JpaRepository<StudyGroupGoal, Long> {
    // 스터디 그룹의 활성 목표 목록
    @Query("SELECT g FROM StudyGroupGoal g WHERE g.studyGroup.id = :groupId " +
           "AND g.endDate >= CURRENT_DATE AND g.isAchieved = false " +
           "ORDER BY g.endDate ASC")
    List<StudyGroupGoal> findActiveGoalsByGroup(@Param("groupId") Long groupId);
    
    // 달성된 목표 목록
    @Query("SELECT g FROM StudyGroupGoal g WHERE g.studyGroup.id = :groupId " +
           "AND g.isAchieved = true ORDER BY g.achievedAt DESC")
    List<StudyGroupGoal> findAchievedGoalsByGroup(@Param("groupId") Long groupId);
}
