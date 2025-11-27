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
}


