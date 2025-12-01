package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {
    Optional<StudyGroupMember> findByStudyGroup_IdAndUser_Id(Long studyGroupId, Integer userId);
    
    boolean existsByStudyGroup_IdAndUser_Id(Long studyGroupId, Integer userId);
    
    List<StudyGroupMember> findByStudyGroup_IdAndStatus(Long studyGroupId, StudyGroupMember.MemberStatus status);
    
    List<StudyGroupMember> findByUser_IdAndStatus(Integer userId, StudyGroupMember.MemberStatus status);
    
    long countByStudyGroup_IdAndStatus(Long studyGroupId, StudyGroupMember.MemberStatus status);
}

