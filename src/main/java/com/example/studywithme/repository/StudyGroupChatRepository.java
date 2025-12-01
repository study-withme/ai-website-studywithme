package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyGroupChatRepository extends JpaRepository<StudyGroupChat, Long> {
    // 스터디 그룹의 최근 메시지 목록
    @Query("SELECT c FROM StudyGroupChat c WHERE c.studyGroup.id = :groupId " +
           "ORDER BY c.createdAt DESC")
    List<StudyGroupChat> findRecentMessagesByGroup(@Param("groupId") Long groupId);
    
    // 특정 시간 이후의 메시지
    @Query("SELECT c FROM StudyGroupChat c WHERE c.studyGroup.id = :groupId " +
           "AND c.createdAt > :since ORDER BY c.createdAt ASC")
    List<StudyGroupChat> findMessagesSince(@Param("groupId") Long groupId, 
                                            @Param("since") LocalDateTime since);
}
