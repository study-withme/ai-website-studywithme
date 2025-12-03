package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyGroupGoalService {

    private final StudyGroupGoalRepository goalRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final UserStudyStatsRepository statsRepository;

    /**
     * 목표 생성
     */
    @Transactional
    public StudyGroupGoal createGoal(Long groupId, Integer userId, String goalType,
                                     Integer targetValue, String goalUnit,
                                     LocalDate startDate, LocalDate endDate) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        // 팀장만 목표 설정 가능
        boolean isCreator = group.getCreator().getId().equals(userId);
        if (!isCreator) {
            throw new RuntimeException("팀장만 목표를 설정할 수 있습니다.");
        }

        StudyGroupGoal goal = new StudyGroupGoal();
        goal.setStudyGroup(group);
        goal.setGoalType(goalType);
        goal.setTargetValue(targetValue);
        goal.setCurrentValue(0);
        goal.setGoalUnit(goalUnit);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        goal.setCreatedBy(userId);

        return goalRepository.save(goal);
    }

    /**
     * 목표 진행률 업데이트
     */
    @Transactional
    public void updateGoalProgress(Long goalId) {
        if (goalId == null) {
            throw new RuntimeException("목표 ID가 필요합니다.");
        }
        StudyGroupGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("목표를 찾을 수 없습니다."));

        // 목표 타입에 따라 진행률 계산
        int currentValue = 0;
        
        if ("DAILY_STUDY_TIME".equals(goal.getGoalType())) {
            // 오늘의 총 학습 시간
            List<UserStudyStats> allStats = statsRepository.findByStudyGroup_Id(goal.getStudyGroup().getId());
            currentValue = allStats.stream()
                    .mapToInt(UserStudyStats::getTodayStudyTime)
                    .sum();
        } else if ("WEEKLY_STUDY_TIME".equals(goal.getGoalType())) {
            // 이번 주 총 학습 시간
            List<UserStudyStats> allStats = statsRepository.findByStudyGroup_Id(goal.getStudyGroup().getId());
            currentValue = allStats.stream()
                    .mapToInt(UserStudyStats::getWeekStudyTime)
                    .sum();
        } else if ("ATTENDANCE_DAYS".equals(goal.getGoalType())) {
            // 출석 일수 (간단 버전)
            // 실제로는 출석 기록을 확인해야 함
        }

        goal.setCurrentValue(currentValue);

        // 목표 달성 확인
        if (currentValue >= goal.getTargetValue() && !goal.getIsAchieved()) {
            goal.setIsAchieved(true);
            goal.setAchievedAt(LocalDateTime.now());
        }

        goalRepository.save(goal);
    }

    /**
     * 목표 달성 여부 확인 및 알림
     */
    @Transactional
    public boolean checkAndCelebrateGoal(Long goalId) {
        if (goalId == null) {
            throw new RuntimeException("목표 ID가 필요합니다.");
        }
        StudyGroupGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("목표를 찾을 수 없습니다."));

        updateGoalProgress(goalId);
        goal = goalRepository.findById(goalId).orElse(goal);

        return goal.getIsAchieved() && goal.getAchievedAt() != null;
    }

    /**
     * 활성 목표 목록
     */
    public List<StudyGroupGoal> getActiveGoals(Long groupId) {
        return goalRepository.findActiveGoalsByGroup(groupId);
    }

    /**
     * 달성된 목표 목록
     */
    public List<StudyGroupGoal> getAchievedGoals(Long groupId) {
        return goalRepository.findAchievedGoalsByGroup(groupId);
    }
}
