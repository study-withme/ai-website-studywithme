package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupResourceRepository extends JpaRepository<StudyGroupResource, Long> {
    List<StudyGroupResource> findByStudyGroup_IdOrderByCreatedAtDesc(Long studyGroupId);
    
    List<StudyGroupResource> findByStudyGroup_IdAndResourceTypeOrderByCreatedAtDesc(
            Long studyGroupId, StudyGroupResource.ResourceType resourceType);
}
