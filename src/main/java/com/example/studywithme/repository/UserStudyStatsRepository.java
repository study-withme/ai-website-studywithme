package com.example.studywithme.repository;

import com.example.studywithme.entity.UserStudyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserStudyStatsRepository extends JpaRepository<UserStudyStats, Long> {
    Optional<UserStudyStats> findByUser_IdAndStudyGroup_Id(Integer userId, Long studyGroupId);
    
    List<UserStudyStats> findByStudyGroup_Id(Long studyGroupId);
    
    // 스터디 그룹의 리더보드 (이번 주 학습 시간 순)
    @Query("SELECT s FROM UserStudyStats s WHERE s.studyGroup.id = :groupId " +
           "ORDER BY s.weekStudyTime DESC")
    List<UserStudyStats> findLeaderboardByGroup(@Param("groupId") Long groupId);
}
