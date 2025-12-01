package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupSettingsRepository settingsRepository;
    private final UserOnlineStatusRepository onlineStatusRepository;
    private final UserStudyStatsRepository statsRepository;
    private final com.example.studywithme.repository.UserRepository userRepository;

    /**
     * 학습 세션 시작
     */
    @Transactional
    public StudySession startSession(Integer userId, Long groupId, String statusMessage) {
        // 기존 활성 세션이 있으면 종료
        Optional<StudySession> existingSession = sessionRepository.findByUser_IdAndStatus(
                userId, StudySession.SessionStatus.IN_PROGRESS);
        if (existingSession.isPresent()) {
            endSession(existingSession.get().getId(), userId);
        }

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        // 스터디 그룹 설정 가져오기
        StudyGroupSettings settings = settingsRepository.findByStudyGroup_Id(groupId)
                .orElseGet(() -> createDefaultSettings(groupId));

        // 새 세션 생성
        StudySession session = new StudySession();
        session.setStudyGroup(group);
        session.setUser(group.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .map(StudyGroupMember::getUser)
                .orElseThrow(() -> new RuntimeException("스터디 멤버가 아닙니다.")));
        session.setStartTime(LocalDateTime.now());
        session.setStatus(StudySession.SessionStatus.IN_PROGRESS);
        session.setStatusMessage(statusMessage);
        session.setTargetCycles(settings.getCyclesPerDay());
        session.setCyclesCompleted(0);

        session = sessionRepository.save(session);

        // 온라인 상태 업데이트
        updateOnlineStatus(userId, groupId, UserOnlineStatus.OnlineStatus.STUDYING, statusMessage);

        return session;
    }

    /**
     * 학습 세션 종료
     */
    @Transactional
    public StudySession endSession(Long sessionId, Integer userId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 세션만 종료할 수 있습니다.");
        }

        session.setEndTime(LocalDateTime.now());
        session.setStatus(StudySession.SessionStatus.COMPLETED);

        // 학습 시간 계산
        if (session.getStartTime() != null && session.getEndTime() != null) {
            long minutes = java.time.Duration.between(
                    session.getStartTime(), session.getEndTime()).toMinutes();
            session.setStudyDuration((int) minutes);
        }

        session = sessionRepository.save(session);

        // 통계 업데이트
        updateUserStats(userId, session.getStudyGroup().getId(), session.getStudyDuration());

        // 온라인 상태 업데이트
        updateOnlineStatus(userId, session.getStudyGroup().getId(), 
                          UserOnlineStatus.OnlineStatus.ONLINE, null);

        return session;
    }

    /**
     * 사이클 완료
     */
    @Transactional
    public StudySession completeCycle(Long sessionId, Integer userId) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 세션만 수정할 수 있습니다.");
        }

        session.setCyclesCompleted(session.getCyclesCompleted() + 1);
        return sessionRepository.save(session);
    }

    /**
     * 사용자 통계 업데이트
     */
    @Transactional
    public void updateUserStats(Integer userId, Long groupId, Integer studyMinutes) {
        UserStudyStats stats = statsRepository.findByUser_IdAndStudyGroup_Id(userId, groupId)
                .orElseGet(() -> {
                    UserStudyStats newStats = new UserStudyStats();
                    newStats.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
                    newStats.setStudyGroup(studyGroupRepository.findById(groupId)
                            .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다.")));
                    return newStats;
                });

        // 오늘 학습 시간 업데이트
        stats.setTodayStudyTime(stats.getTodayStudyTime() + studyMinutes);
        
        // 이번 주 학습 시간 업데이트
        stats.setWeekStudyTime(stats.getWeekStudyTime() + studyMinutes);
        
        // 전체 학습 시간 업데이트
        stats.setTotalStudyTime(stats.getTotalStudyTime() + studyMinutes);
        
        // 마지막 학습일 업데이트
        stats.setLastStudyDate(java.time.LocalDate.now());

        statsRepository.save(stats);
    }

    /**
     * 온라인 상태 업데이트
     */
    @Transactional
    public void updateOnlineStatus(Integer userId, Long groupId, 
                                   UserOnlineStatus.OnlineStatus status, String statusMessage) {
        UserOnlineStatus onlineStatus = onlineStatusRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    UserOnlineStatus newStatus = new UserOnlineStatus();
                    newStatus.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
                    return newStatus;
                });

        onlineStatus.setIsOnline(status != UserOnlineStatus.OnlineStatus.OFFLINE);
        onlineStatus.setCurrentStatus(status);
        onlineStatus.setLastActiveTime(LocalDateTime.now());
        onlineStatus.setCurrentStudyGroupId(groupId);
        onlineStatus.setStatusMessage(statusMessage);

        onlineStatusRepository.save(onlineStatus);
    }

    /**
     * 기본 설정 생성
     */
    private StudyGroupSettings createDefaultSettings(Long groupId) {
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        StudyGroupSettings settings = new StudyGroupSettings();
        settings.setStudyGroup(group);
        settings.setStudyDuration(50);
        settings.setBreakDuration(10);
        settings.setCyclesPerDay(4);
        settings.setCreatedBy(group.getCreator().getId());
        return settingsRepository.save(settings);
    }

    /**
     * 활성 세션 조회
     */
    public Optional<StudySession> getActiveSession(Integer userId) {
        return sessionRepository.findByUser_IdAndStatus(userId, StudySession.SessionStatus.IN_PROGRESS);
    }

    /**
     * 스터디 그룹의 활성 세션 목록
     */
    public List<StudySession> getActiveSessions(Long groupId) {
        return sessionRepository.findByStudyGroup_IdAndStatus(groupId, StudySession.SessionStatus.IN_PROGRESS);
    }
}
