package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudyAttendanceService {

    private final StudyAttendanceRepository attendanceRepository;
    private final StudySessionRepository sessionRepository;
    private final UserStudyStatsRepository statsRepository;
    private final StudyGroupSettingsRepository settingsRepository;

    /**
     * 출석체크
     */
    @Transactional
    public StudyAttendance checkAttendance(Long sessionId, Integer userId, String message) {
        StudySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 세션만 출석체크할 수 있습니다.");
        }

        // 이미 출석체크 했는지 확인
        Optional<StudyAttendance> existing = attendanceRepository.findByStudySession_IdAndUser_Id(sessionId, userId);
        if (existing.isPresent()) {
            throw new RuntimeException("이미 출석체크를 완료했습니다.");
        }

        // 쉬는시간인지 확인 (간단 버전 - 실제로는 타이머 상태 확인 필요)
        StudyGroupSettings settings = settingsRepository.findByStudyGroup_Id(session.getStudyGroup().getId())
                .orElseThrow(() -> new RuntimeException("스터디 설정을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        boolean isLate = false;
        
        // 지각 여부 확인 (쉬는시간이 끝났는지)
        // 실제로는 타이머 상태를 확인해야 함

        // 출석체크 생성
        StudyAttendance attendance = new StudyAttendance();
        attendance.setStudySession(session);
        attendance.setUser(session.getUser());
        attendance.setCheckedAt(now);
        attendance.setIsLate(isLate);
        attendance.setMessage(message);

        attendance = attendanceRepository.save(attendance);

        // 세션에 출석체크 완료 표시
        session.setAttendanceChecked(true);
        sessionRepository.save(session);

        // 연속 출석 일수 업데이트
        updateConsecutiveDays(userId, session.getStudyGroup().getId());

        return attendance;
    }

    /**
     * 연속 출석 일수 업데이트
     */
    @Transactional
    public void updateConsecutiveDays(Integer userId, Long groupId) {
        UserStudyStats stats = statsRepository.findByUser_IdAndStudyGroup_Id(userId, groupId)
                .orElse(null);

        if (stats == null) return;

        LocalDate today = LocalDate.now();
        LocalDate lastAttendance = stats.getLastAttendanceDate();

        if (lastAttendance == null) {
            // 첫 출석
            stats.setConsecutiveDays(1);
            stats.setMaxConsecutiveDays(1);
        } else if (lastAttendance.equals(today.minusDays(1))) {
            // 연속 출석
            stats.setConsecutiveDays(stats.getConsecutiveDays() + 1);
            if (stats.getConsecutiveDays() > stats.getMaxConsecutiveDays()) {
                stats.setMaxConsecutiveDays(stats.getConsecutiveDays());
            }
        } else if (!lastAttendance.equals(today)) {
            // 연속이 끊김
            stats.setConsecutiveDays(1);
        }

        stats.setLastAttendanceDate(today);
        statsRepository.save(stats);
    }

    /**
     * 오늘 출석 여부 확인
     */
    public boolean isCheckedToday(Integer userId, Long groupId) {
        List<StudyAttendance> todayAttendances = attendanceRepository.findByUserAndDate(
                userId, LocalDate.now());
        return todayAttendances.stream()
                .anyMatch(a -> a.getStudySession().getStudyGroup().getId().equals(groupId));
    }

    /**
     * 세션의 출석 현황
     */
    public List<StudyAttendance> getSessionAttendances(Long sessionId) {
        return attendanceRepository.findByStudySession_Id(sessionId);
    }
}
