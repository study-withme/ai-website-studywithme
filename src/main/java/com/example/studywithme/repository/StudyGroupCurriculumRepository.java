package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyGroupCurriculumRepository extends JpaRepository<StudyGroupCurriculum, Long> {
    List<StudyGroupCurriculum> findByStudyGroup_IdOrderByWeekNumberAsc(Long studyGroupId);
    
    @Query("SELECT c FROM StudyGroupCurriculum c WHERE c.studyGroup.id = :groupId AND c.status = :status")
    List<StudyGroupCurriculum> findByStudyGroup_IdAndStatus(@Param("groupId") Long groupId, 
                                                             @Param("status") StudyGroupCurriculum.CurriculumStatus status);
}
