package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyGroupCommentRepository extends JpaRepository<StudyGroupComment, Long> {
    @Query("SELECT c FROM StudyGroupComment c WHERE c.studyGroup.id = :groupId AND c.deleted = false AND c.parentComment IS NULL ORDER BY c.createdAt DESC")
    List<StudyGroupComment> findTopLevelCommentsByStudyGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT c FROM StudyGroupComment c WHERE c.parentComment.id = :parentId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<StudyGroupComment> findRepliesByParentId(@Param("parentId") Long parentId);
    
    List<StudyGroupComment> findByStudyGroup_IdAndDeletedFalseOrderByCreatedAtDesc(Long studyGroupId);
}
