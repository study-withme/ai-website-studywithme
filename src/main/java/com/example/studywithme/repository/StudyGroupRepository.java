package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    Optional<StudyGroup> findByPost_Id(Long postId);
    
    // 사용자가 참여한 스터디 그룹 목록
    @Query("SELECT DISTINCT sg FROM StudyGroup sg " +
           "JOIN sg.members m " +
           "WHERE m.user.id = :userId AND m.status = 'ACTIVE' AND sg.status = 'ACTIVE'")
    List<StudyGroup> findActiveGroupsByUserId(@Param("userId") Integer userId);
    
    // 사용자가 리더인 스터디 그룹 목록
    List<StudyGroup> findByCreator_IdAndStatusOrderByCreatedAtDesc(Integer creatorId, StudyGroup.GroupStatus status);
    
    // 게시글 ID로 스터디 그룹 조회
    @Query("SELECT sg FROM StudyGroup sg WHERE sg.post.id = :postId")
    Optional<StudyGroup> findByPostId(@Param("postId") Long postId);
}

