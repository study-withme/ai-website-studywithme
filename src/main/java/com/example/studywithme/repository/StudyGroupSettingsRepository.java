package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyGroupSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyGroupSettingsRepository extends JpaRepository<StudyGroupSettings, Long> {
    Optional<StudyGroupSettings> findByStudyGroup_Id(Long studyGroupId);
}
