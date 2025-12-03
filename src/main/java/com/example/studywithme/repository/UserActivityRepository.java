package com.example.studywithme.repository;

import com.example.studywithme.entity.UserActivity;
import com.example.studywithme.entity.UserActivity.ActionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @Query("""
           select ua.targetKeyword, count(ua)
           from UserActivity ua
           where ua.user.id = :userId
             and ua.targetKeyword is not null
             and ua.actionType in :types
           group by ua.targetKeyword
           order by count(ua) desc
           """)
    List<Object[]> findTopKeywords(@Param("userId") Integer userId,
                                   @Param("types") List<ActionType> types,
                                   Pageable pageable);

    /**
     * 사용자의 활동 로그 개수 조회 (최근 30일)
     */
    @Query("""
           select count(ua)
           from UserActivity ua
           where ua.user.id = :userId
             and ua.createdAt >= :sinceDate
           """)
    long countRecentActivities(@Param("userId") Integer userId, 
                               @Param("sinceDate") java.time.LocalDateTime sinceDate);

    /**
     * 사용자가 실제로 클릭한 게시글의 카테고리 조회 (최근 30일)
     */
    @Query("""
           select p.category, count(ua) as clickCount
           from UserActivity ua
           join Post p on ua.targetId = p.id
           where ua.user.id = :userId
             and ua.actionType = 'CLICK'
             and ua.createdAt >= :sinceDate
             and p.category is not null
           group by p.category
           order by clickCount desc
           """)
    List<Object[]> findClickedCategories(@Param("userId") Integer userId,
                                        @Param("sinceDate") java.time.LocalDateTime sinceDate);
}


