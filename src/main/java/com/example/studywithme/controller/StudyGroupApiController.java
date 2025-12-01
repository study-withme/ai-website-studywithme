package com.example.studywithme.controller;

import com.example.studywithme.entity.*;
import com.example.studywithme.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study-groups")
@RequiredArgsConstructor
public class StudyGroupApiController {

    private final StudySessionService sessionService;
    private final StudyAttendanceService attendanceService;
    private final StudyGroupChatService chatService;
    private final StudyJournalService journalService;
    private final StudyGroupGoalService goalService;
    private final UserOnlineStatusService onlineStatusService;
    private final StudyGroupService studyGroupService;
    private final com.example.studywithme.repository.StudyGroupSettingsRepository settingsRepository;
    private final com.example.studywithme.repository.UserStudyStatsRepository statsRepository;

    /**
     * 학습 세션 시작
     */
    @PostMapping("/{groupId}/sessions/start")
    public ResponseEntity<Map<String, Object>> startSession(
            @PathVariable Long groupId,
            @RequestParam(required = false) String statusMessage,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudySession studySession = sessionService.startSession(
                    loginUser.getId(), groupId, statusMessage);
            
            // 시스템 메시지 전송
            chatService.sendSystemMessage(groupId, 
                    loginUser.getRealName() + "님이 학습을 시작했습니다! 🔥");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "session", Map.of(
                            "id", studySession.getId(),
                            "startTime", studySession.getStartTime(),
                            "statusMessage", studySession.getStatusMessage()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 학습 세션 종료
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable Long sessionId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudySession studySession = sessionService.endSession(sessionId, loginUser.getId());
            
            // 시스템 메시지 전송
            chatService.sendSystemMessage(studySession.getStudyGroup().getId(),
                    loginUser.getRealName() + "님이 학습을 완료했습니다! 수고하셨습니다! 💪");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "session", Map.of(
                            "id", studySession.getId(),
                            "studyDuration", studySession.getStudyDuration()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 출석체크
     */
    @PostMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<Map<String, Object>> checkAttendance(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String message,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyAttendance attendance = attendanceService.checkAttendance(
                    sessionId, loginUser.getId(), message);

            // 시스템 메시지 전송
            StudySession studySession = sessionService.getActiveSession(loginUser.getId())
                    .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다."));
            chatService.sendSystemMessage(studySession.getStudyGroup().getId(),
                    loginUser.getRealName() + "님이 출석체크를 완료했습니다! ✅");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendance", Map.of(
                            "id", attendance.getId(),
                            "checkedAt", attendance.getCheckedAt(),
                            "message", attendance.getMessage()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 채팅 메시지 전송
     */
    @PostMapping("/{groupId}/chat")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long groupId,
            @RequestParam String message,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyGroupChat chat = chatService.sendMessage(groupId, loginUser.getId(), 
                    message, StudyGroupChat.MessageType.TEXT);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "chat", Map.of(
                            "id", chat.getId(),
                            "message", chat.getMessage(),
                            "userName", chat.getUser().getRealName(),
                            "createdAt", chat.getCreatedAt()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 최근 채팅 메시지 조회
     */
    @GetMapping("/{groupId}/chat")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<StudyGroupChat> messages = chatService.getRecentMessages(groupId, limit);
            
            List<Map<String, Object>> messageList = messages.stream()
                    .map(m -> {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("id", m.getId());
                        msgMap.put("message", m.getMessage());
                        msgMap.put("userName", m.getUser().getRealName());
                        msgMap.put("userId", m.getUser().getId());
                        msgMap.put("messageType", m.getMessageType().toString());
                        msgMap.put("createdAt", m.getCreatedAt());
                        return msgMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "messages", messageList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 팀원 정보 조회 (공부 시간, 스타일, 온라인 상태)
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<Map<String, Object>> getMembers(
            @PathVariable Long groupId) {
        try {
            List<StudyGroupMember> members = studyGroupService.getGroupMembers(groupId);
            
            List<Map<String, Object>> memberList = members.stream()
                    .map(member -> {
                        User user = member.getUser();
                        
                        // 온라인 상태 조회
                        UserOnlineStatus onlineStatus = onlineStatusService.getUserStatus(user.getId())
                                .orElse(null);
                        
                        // 학습 통계 조회
                        UserStudyStats stats = statsRepository.findByUser_IdAndStudyGroup_Id(
                                user.getId(), groupId).orElse(null);
                        
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("id", user.getId());
                        memberInfo.put("name", user.getRealName());
                        memberInfo.put("role", member.getRole().toString());
                        
                        // 온라인 상태
                        if (onlineStatus != null) {
                            memberInfo.put("isOnline", onlineStatus.getIsOnline());
                            memberInfo.put("status", onlineStatus.getCurrentStatus().toString());
                            memberInfo.put("statusMessage", onlineStatus.getStatusMessage());
                            memberInfo.put("lastActiveTime", onlineStatus.getLastActiveTime());
                        } else {
                            memberInfo.put("isOnline", false);
                            memberInfo.put("status", "OFFLINE");
                            memberInfo.put("lastActiveTime", null);
                        }
                        
                        // 학습 통계
                        if (stats != null) {
                            memberInfo.put("todayStudyTime", stats.getTodayStudyTime());
                            memberInfo.put("weekStudyTime", stats.getWeekStudyTime());
                            memberInfo.put("totalStudyTime", stats.getTotalStudyTime());
                            memberInfo.put("consecutiveDays", stats.getConsecutiveDays());
                            memberInfo.put("studyStyle", stats.getStudyStyle());
                        } else {
                            memberInfo.put("todayStudyTime", 0);
                            memberInfo.put("weekStudyTime", 0);
                            memberInfo.put("totalStudyTime", 0);
                            memberInfo.put("consecutiveDays", 0);
                            memberInfo.put("studyStyle", null);
                        }
                        
                        return memberInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "members", memberList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 학습 일지 저장
     */
    @PostMapping("/{groupId}/journal")
    public ResponseEntity<Map<String, Object>> saveJournal(
            @PathVariable Long groupId,
            @RequestParam(required = false) String studyContent,
            @RequestParam(required = false) String feeling,
            @RequestParam(required = false) String nextGoal,
            @RequestParam(required = false) Integer moodRating,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyJournal journal = journalService.saveJournal(
                    loginUser.getId(), groupId, LocalDate.now(),
                    studyContent, feeling, nextGoal, moodRating);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "journal", Map.of(
                            "id", journal.getId(),
                            "date", journal.getJournalDate()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 학습 일지 조회
     */
    @GetMapping("/{groupId}/journal")
    public ResponseEntity<Map<String, Object>> getJournal(
            @PathVariable Long groupId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyJournal journal = journalService.getTodayJournal(loginUser.getId(), groupId)
                    .orElse(null);

            if (journal == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "journal", null
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "journal", Map.of(
                            "id", journal.getId(),
                            "studyContent", journal.getStudyContent(),
                            "feeling", journal.getFeeling(),
                            "nextGoal", journal.getNextGoal(),
                            "moodRating", journal.getMoodRating(),
                            "date", journal.getJournalDate()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 목표 달성 확인
     */
    @GetMapping("/{groupId}/goals/check")
    public ResponseEntity<Map<String, Object>> checkGoals(@PathVariable Long groupId) {
        try {
            List<StudyGroupGoal> activeGoals = goalService.getActiveGoals(groupId);
            List<Map<String, Object>> achievedGoals = new java.util.ArrayList<>();
            
            for (StudyGroupGoal goal : activeGoals) {
                if (goalService.checkAndCelebrateGoal(goal.getId())) {
                    Map<String, Object> goalMap = new HashMap<>();
                    goalMap.put("id", goal.getId());
                    goalMap.put("goalType", goal.getGoalType());
                    goalMap.put("targetValue", goal.getTargetValue());
                    goalMap.put("currentValue", goal.getCurrentValue());
                    achievedGoals.add(goalMap);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "achievedGoals", achievedGoals,
                    "hasAchievement", !achievedGoals.isEmpty()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 상태 메시지 업데이트
     */
    @PostMapping("/{groupId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long groupId,
            @RequestParam String status,
            @RequestParam(required = false) String statusMessage,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            UserOnlineStatus.OnlineStatus onlineStatus = 
                    UserOnlineStatus.OnlineStatus.valueOf(status.toUpperCase());
            
            UserOnlineStatus updated = onlineStatusService.updateStatus(
                    loginUser.getId(), onlineStatus, groupId, statusMessage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", Map.of(
                            "status", updated.getCurrentStatus().toString(),
                            "statusMessage", updated.getStatusMessage(),
                            "lastActiveTime", updated.getLastActiveTime()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 활성 세션 조회
     */
    @GetMapping("/sessions/active")
    public ResponseEntity<Map<String, Object>> getActiveSession(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            Optional<StudySession> sessionOpt = sessionService.getActiveSession(loginUser.getId());
            if (sessionOpt.isPresent()) {
                StudySession studySession = sessionOpt.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "session", Map.of(
                                "id", studySession.getId(),
                                "groupId", studySession.getStudyGroup().getId(),
                                "startTime", studySession.getStartTime(),
                                "statusMessage", studySession.getStatusMessage() != null ? studySession.getStatusMessage() : "",
                                "cyclesCompleted", studySession.getCyclesCompleted(),
                                "targetCycles", studySession.getTargetCycles()
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of("success", true, "session", null));
            }
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 사이클 완료
     */
    @PostMapping("/sessions/{sessionId}/cycle")
    public ResponseEntity<Map<String, Object>> completeCycle(
            @PathVariable Long sessionId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudySession studySession = sessionService.completeCycle(sessionId, loginUser.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cyclesCompleted", studySession.getCyclesCompleted()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 출석 현황 조회
     */
    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<Map<String, Object>> getAttendanceStatus(@PathVariable Long sessionId) {
        try {
            List<StudyAttendance> attendances = attendanceService.getSessionAttendances(sessionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attendances", attendances.stream()
                            .map(a -> Map.of(
                                    "userId", a.getUser().getId(),
                                    "userName", a.getUser().getRealName(),
                                    "checkedAt", a.getCheckedAt(),
                                    "message", a.getMessage() != null ? a.getMessage() : ""
                            ))
                            .collect(Collectors.toList()),
                    "count", attendances.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 스터디 그룹 설정 조회/수정 (팀장만)
     */
    @GetMapping("/{groupId}/settings")
    public ResponseEntity<Map<String, Object>> getSettings(@PathVariable Long groupId) {
        try {
            Optional<StudyGroupSettings> settingsOpt = settingsRepository.findByStudyGroup_Id(groupId);
            if (settingsOpt.isPresent()) {
                StudyGroupSettings settings = settingsOpt.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "settings", Map.of(
                                "studyDuration", settings.getStudyDuration(),
                                "breakDuration", settings.getBreakDuration(),
                                "cyclesPerDay", settings.getCyclesPerDay(),
                                "startTime", settings.getStartTime() != null ? settings.getStartTime().toString() : null
                        )
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "settings", Map.of(
                                "studyDuration", 50,
                                "breakDuration", 10,
                                "cyclesPerDay", 4,
                                "startTime", null
                        )
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
