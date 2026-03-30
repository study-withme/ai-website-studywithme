package com.example.studywithme.studygroup.controller;

import com.example.studywithme.studysession.entity.StudyJournal;
import com.example.studywithme.studysession.entity.StudySession;
import com.example.studywithme.studysession.service.StudyJournalService;
import com.example.studywithme.studysession.service.StudySessionService;
import com.example.studywithme.studygroup.entity.StudyAttendance;
import com.example.studywithme.studygroup.entity.StudyGroup;
import com.example.studywithme.studygroup.entity.StudyGroupChat;
import com.example.studywithme.studygroup.entity.StudyGroupComment;
import com.example.studywithme.studygroup.entity.StudyGroupCurriculum;
import com.example.studywithme.studygroup.entity.StudyGroupGoal;
import com.example.studywithme.studygroup.entity.StudyGroupMember;
import com.example.studywithme.studygroup.entity.StudyGroupResource;
import com.example.studywithme.studygroup.entity.StudyGroupSettings;
import com.example.studywithme.studygroup.service.StudyAttendanceService;
import com.example.studywithme.studygroup.service.StudyGroupChatService;
import com.example.studywithme.studygroup.service.StudyGroupGoalService;
import com.example.studywithme.studygroup.service.StudyGroupManagementService;
import com.example.studywithme.studygroup.service.StudyGroupService;
import com.example.studywithme.user.entity.User;
import com.example.studywithme.user.entity.UserOnlineStatus;
import com.example.studywithme.user.entity.UserStudyStats;
import com.example.studywithme.user.service.UserOnlineStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
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
    private final StudyGroupManagementService managementService;
    private final com.example.studywithme.studygroup.repository.StudyGroupSettingsRepository settingsRepository;
    private final com.example.studywithme.user.repository.UserStudyStatsRepository statsRepository;
    private final com.example.studywithme.user.repository.UserRepository userRepository;

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
    @org.springframework.transaction.annotation.Transactional
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
     * 학습 일지 조회 (본인)
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
     * 특정 사용자의 학습 일지 조회 (스터디 그룹 멤버만 가능)
     */
    @GetMapping("/{groupId}/journal/{userId}")
    public ResponseEntity<Map<String, Object>> getUserJournal(
            @PathVariable Long groupId,
            @PathVariable int userId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            // 스터디 그룹 멤버인지 확인
            List<StudyGroupMember> members = studyGroupService.getGroupMembers(groupId);
            boolean isMember = members.stream().anyMatch(m -> m.getUser().getId().equals(loginUser.getId()));
            if (!isMember) {
                return ResponseEntity.status(403).body(Map.of("error", "스터디 그룹 멤버만 조회할 수 있습니다."));
            }
            
            StudyJournal journal = journalService.getTodayJournal(userId, groupId)
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
            @RequestParam(required = false, defaultValue = "STUDYING") String status,
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

    // ========== 커리큘럼 관리 ==========
    
    @GetMapping("/{groupId}/curriculum")
    public ResponseEntity<Map<String, Object>> getCurriculum(@PathVariable Long groupId) {
        try {
            List<StudyGroupCurriculum> curriculums = managementService.getCurriculums(groupId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "curriculums", curriculums.stream().map(c -> Map.of(
                            "id", c.getId(),
                            "weekNumber", c.getWeekNumber(),
                            "title", c.getTitle(),
                            "description", c.getDescription() != null ? c.getDescription() : "",
                            "status", c.getStatus().name(),
                            "startDate", c.getStartDate() != null ? c.getStartDate().toString() : null,
                            "endDate", c.getEndDate() != null ? c.getEndDate().toString() : null
                    )).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{groupId}/curriculum")
    public ResponseEntity<Map<String, Object>> createCurriculum(
            @PathVariable Long groupId,
            @RequestParam Integer weekNumber,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            java.time.LocalDate start = startDate != null && !startDate.isEmpty() ? 
                    java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate end = endDate != null && !endDate.isEmpty() ? 
                    java.time.LocalDate.parse(endDate) : null;
            
            StudyGroupCurriculum curriculum = managementService.createCurriculum(
                    groupId, loginUser.getId(), weekNumber, title, description, start, end);
            return ResponseEntity.ok(Map.of("success", true, "curriculum", Map.of("id", curriculum.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/curriculum/{curriculumId}")
    public ResponseEntity<Map<String, Object>> updateCurriculum(
            @PathVariable Long curriculumId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String status,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroupCurriculum.CurriculumStatus statusEnum = null;
            if (status != null) {
                statusEnum = StudyGroupCurriculum.CurriculumStatus.valueOf(status);
            }
            StudyGroupCurriculum curriculum = managementService.updateCurriculum(
                    curriculumId, loginUser.getId(), title, description, statusEnum);
            return ResponseEntity.ok(Map.of("success", true, "curriculum", Map.of("id", curriculum.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/curriculum/{curriculumId}")
    public ResponseEntity<Map<String, Object>> deleteCurriculum(
            @PathVariable Long curriculumId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.deleteCurriculum(curriculumId, loginUser.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 리소스 관리 ==========
    
    @GetMapping("/{groupId}/resources")
    public ResponseEntity<Map<String, Object>> getResources(
            @PathVariable Long groupId,
            @RequestParam(required = false) String resourceType) {
        try {
            List<StudyGroupResource> resources;
            if (resourceType != null) {
                resources = managementService.getResourcesByType(
                        groupId, StudyGroupResource.ResourceType.valueOf(resourceType));
            } else {
                resources = managementService.getResources(groupId);
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "resources", resources.stream().map(r -> Map.of(
                            "id", r.getId(),
                            "resourceType", r.getResourceType().name(),
                            "name", r.getName(),
                            "url", r.getUrl() != null ? r.getUrl() : "",
                            "description", r.getDescription() != null ? r.getDescription() : "",
                            "createdBy", r.getCreatedBy() != null ? r.getCreatedBy().getRealName() : "알 수 없음"
                    )).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{groupId}/resources")
    public ResponseEntity<Map<String, Object>> createResource(
            @PathVariable Long groupId,
            @RequestParam String resourceType,
            @RequestParam String name,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String description,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroupResource resource = managementService.createResource(
                    groupId, loginUser.getId(),
                    StudyGroupResource.ResourceType.valueOf(resourceType),
                    name, url, description);
            return ResponseEntity.ok(Map.of("success", true, "resource", Map.of("id", resource.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/resources/{resourceId}")
    public ResponseEntity<Map<String, Object>> updateResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String description,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroupResource resource = managementService.updateResource(
                    resourceId, loginUser.getId(), name, url, description);
            return ResponseEntity.ok(Map.of("success", true, "resource", Map.of("id", resource.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/resources/{resourceId}")
    public ResponseEntity<Map<String, Object>> deleteResource(
            @PathVariable Long resourceId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.deleteResource(resourceId, loginUser.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 댓글 관리 ==========
    
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/{groupId}/comments")
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable Long groupId) {
        try {
            List<StudyGroupComment> comments = managementService.getComments(groupId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "comments", comments.stream().map(c -> {
                        try {
                            return Map.of(
                                    "id", c.getId(),
                                    "userId", c.getUser().getId(),
                                    "userName", c.getUser().getRealName() != null ? c.getUser().getRealName() : "알 수 없음",
                                    "content", c.getContent(),
                                    "createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
                            );
                        } catch (Exception e) {
                            return Map.of(
                                    "id", c.getId(),
                                    "userId", 0,
                                    "userName", "알 수 없음",
                                    "content", c.getContent() != null ? c.getContent() : "",
                                    "createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
                            );
                        }
                    }).collect(Collectors.toList())
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{groupId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long groupId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentCommentId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroupComment comment = managementService.createComment(
                    groupId, loginUser.getId(), content, parentCommentId);
            return ResponseEntity.ok(Map.of("success", true, "comment", Map.of("id", comment.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> updateComment(
            @PathVariable Long commentId,
            @RequestParam String content,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroupComment comment = managementService.updateComment(
                    commentId, loginUser.getId(), content);
            return ResponseEntity.ok(Map.of("success", true, "comment", Map.of("id", comment.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.deleteComment(commentId, loginUser.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 스터디 그룹 조회 ==========
    
    @GetMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> getStudyGroup(@PathVariable Long groupId) {
        try {
            StudyGroup group = studyGroupService.getGroupById(groupId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "group", Map.of(
                            "id", group.getId(),
                            "title", group.getTitle(),
                            "description", group.getDescription() != null ? group.getDescription() : "",
                            "category", group.getCategory() != null ? group.getCategory() : "기타",
                            "maxMembers", group.getMaxMembers() != null ? group.getMaxMembers() : 0,
                            "currentMembers", group.getCurrentMembers() != null ? group.getCurrentMembers() : 0,
                            "creatorId", group.getCreator().getId()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 스터디 그룹 수정/삭제 ==========
    
    @PutMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> updateStudyGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer maxMembers,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            StudyGroup group = managementService.updateStudyGroup(
                    groupId, loginUser.getId(), title, description, maxMembers);
            return ResponseEntity.ok(Map.of("success", true, "group", Map.of("id", group.getId())));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, Object>> deleteStudyGroup(
            @PathVariable Long groupId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.deleteStudyGroup(groupId, loginUser.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    // ========== 멤버 탈퇴 / 추방 ==========
    
    /**
     * 스터디 그룹 탈퇴 (본인이 직접)
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, Object>> leaveStudyGroup(
            @PathVariable Long groupId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.leaveStudyGroup(groupId, loginUser.getId());
            
            // 시스템 메시지 전송
            chatService.sendSystemMessage(groupId, 
                    loginUser.getRealName() + "님이 스터디 그룹을 떠났습니다.");
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 멤버 추방 (리더만 가능)
     */
    @PostMapping("/{groupId}/members/{userId}/remove")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable Long groupId,
            @PathVariable int userId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        try {
            managementService.removeMember(groupId, loginUser.getId(), userId);
            
            // 시스템 메시지 전송
            User removedUser = userRepository.findById(userId).orElse(null);
            String userName = removedUser != null ? removedUser.getRealName() : "한 멤버";
            chatService.sendSystemMessage(groupId, 
                    userName + "님이 그룹에서 제거되었습니다.");
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
