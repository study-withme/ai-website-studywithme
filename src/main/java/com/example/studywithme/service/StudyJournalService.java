package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudyJournalService {

    private final StudyJournalRepository journalRepository;
    private final StudyGroupRepository studyGroupRepository;

    /**
     * 학습 일지 작성/수정
     */
    @Transactional
    public StudyJournal saveJournal(Integer userId, Long groupId, LocalDate date,
                                     String studyContent, String feeling, String nextGoal, 
                                     Integer moodRating) {
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        StudyJournal journal = journalRepository.findByUser_IdAndStudyGroup_IdAndJournalDate(
                userId, groupId, date)
                .orElseGet(() -> {
                    StudyJournal newJournal = new StudyJournal();
                    newJournal.setUser(group.getMembers().stream()
                            .filter(m -> m.getUser().getId().equals(userId))
                            .findFirst()
                            .map(StudyGroupMember::getUser)
                            .orElseThrow(() -> new RuntimeException("스터디 멤버가 아닙니다.")));
                    newJournal.setStudyGroup(group);
                    newJournal.setJournalDate(date);
                    return newJournal;
                });

        journal.setStudyContent(studyContent);
        journal.setFeeling(feeling);
        journal.setNextGoal(nextGoal);
        journal.setMoodRating(moodRating);

        return journalRepository.save(journal);
    }

    /**
     * 오늘 일지 조회
     */
    public Optional<StudyJournal> getTodayJournal(Integer userId, Long groupId) {
        return journalRepository.findByUser_IdAndStudyGroup_IdAndJournalDate(
                userId, groupId, LocalDate.now());
    }

    /**
     * 사용자의 일지 목록
     */
    public List<StudyJournal> getUserJournals(Integer userId, Long groupId) {
        return journalRepository.findByUserAndGroup(userId, groupId);
    }

    /**
     * 스터디 그룹의 오늘 일지 목록
     */
    public List<StudyJournal> getTodayGroupJournals(Long groupId) {
        return journalRepository.findByStudyGroup_IdAndJournalDate(groupId, LocalDate.now());
    }
}
