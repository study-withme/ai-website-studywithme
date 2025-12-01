package com.example.studywithme.repository;

import com.example.studywithme.entity.UserOnlineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserOnlineStatusRepository extends JpaRepository<UserOnlineStatus, Long> {
    Optional<UserOnlineStatus> findByUser_Id(Integer userId);
    
    // 스터디 그룹의 온라인 멤버 목록
    @Query("SELECT s FROM UserOnlineStatus s " +
           "WHERE s.currentStudyGroupId = :groupId " +
           "AND s.isOnline = true " +
           "AND s.lastActiveTime >= :threshold")
    List<UserOnlineStatus> findOnlineMembersByGroup(@Param("groupId") Long groupId, 
                                                     @Param("threshold") LocalDateTime threshold);
    
    // 자리비움 상태 업데이트 (5분 이상 활동 없음)
    @Query("SELECT s FROM UserOnlineStatus s " +
           "WHERE s.isOnline = true " +
           "AND s.lastActiveTime < :threshold")
    List<UserOnlineStatus> findAwayUsers(@Param("threshold") LocalDateTime threshold);
}
