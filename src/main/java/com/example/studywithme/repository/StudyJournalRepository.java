package com.example.studywithme.repository;

import com.example.studywithme.entity.StudyJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyJournalRepository extends JpaRepository<StudyJournal, Long> {
    Optional<StudyJournal> findByUser_IdAndStudyGroup_IdAndJournalDate(
            Integer userId, Long studyGroupId, LocalDate date);
    
    List<StudyJournal> findByStudyGroup_IdAndJournalDate(Long studyGroupId, LocalDate date);
    
    // 사용자의 일지 목록
    @Query("SELECT j FROM StudyJournal j WHERE j.user.id = :userId " +
           "AND j.studyGroup.id = :groupId ORDER BY j.journalDate DESC")
    List<StudyJournal> findByUserAndGroup(@Param("userId") Integer userId, 
                                           @Param("groupId") Long groupId);
}
