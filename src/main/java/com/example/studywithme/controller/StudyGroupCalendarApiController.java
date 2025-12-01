package com.example.studywithme.controller;

import com.example.studywithme.entity.*;
import com.example.studywithme.service.StudyGroupCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/study-groups/{groupId}/calendar")
@RequiredArgsConstructor
public class StudyGroupCalendarApiController {

    private final StudyGroupCalendarService calendarService;

    /**
     * 일정 생성
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> createEvent(
            @PathVariable Long groupId,
            @RequestParam LocalDate eventDate,
            @RequestParam(required = false) LocalTime eventTime,
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String color,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyGroupCalendar event = calendarService.createEvent(
                    groupId, loginUser.getId(), eventDate, eventTime, title, content, eventType, color);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "event", Map.of(
                            "id", event.getId(),
                            "title", event.getTitle(),
                            "eventDate", event.getEventDate(),
                            "eventTime", event.getEventTime() != null ? event.getEventTime().toString() : null,
                            "content", event.getContent() != null ? event.getContent() : "",
                            "eventType", event.getEventType(),
                            "color", event.getColor(),
                            "userName", event.getUser().getRealName(),
                            "userId", event.getUser().getId()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 일정 수정
     */
    @PutMapping("/events/{eventId}")
    public ResponseEntity<Map<String, Object>> updateEvent(
            @PathVariable Long groupId,
            @PathVariable Long eventId,
            @RequestParam(required = false) LocalDate eventDate,
            @RequestParam(required = false) LocalTime eventTime,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String color,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            StudyGroupCalendar event = calendarService.updateEvent(
                    eventId, loginUser.getId(), eventDate, eventTime, title, content, eventType, color);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "event", Map.of(
                            "id", event.getId(),
                            "title", event.getTitle(),
                            "eventDate", event.getEventDate(),
                            "eventTime", event.getEventTime() != null ? event.getEventTime().toString() : null,
                            "content", event.getContent() != null ? event.getContent() : "",
                            "eventType", event.getEventType(),
                            "color", event.getColor(),
                            "userName", event.getUser().getRealName(),
                            "userId", event.getUser().getId()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 일정 삭제
     */
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Map<String, Object>> deleteEvent(
            @PathVariable Long groupId,
            @PathVariable Long eventId,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            calendarService.deleteEvent(eventId, loginUser.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 특정 날짜의 일정 조회
     */
    @GetMapping("/events/date/{date}")
    public ResponseEntity<Map<String, Object>> getEventsByDate(
            @PathVariable Long groupId,
            @PathVariable String date) {
        try {
            LocalDate eventDate = LocalDate.parse(date);
            List<StudyGroupCalendar> events = calendarService.getEventsByDate(groupId, eventDate);
            
            List<Map<String, Object>> eventList = events.stream()
                    .map(e -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("id", e.getId());
                        eventMap.put("title", e.getTitle());
                        eventMap.put("eventDate", e.getEventDate());
                        eventMap.put("eventTime", e.getEventTime() != null ? e.getEventTime().toString() : null);
                        eventMap.put("content", e.getContent() != null ? e.getContent() : "");
                        eventMap.put("eventType", e.getEventType());
                        eventMap.put("color", e.getColor());
                        eventMap.put("userName", e.getUser().getRealName());
                        eventMap.put("userId", e.getUser().getId());
                        return eventMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "events", eventList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 특정 월의 일정 조회
     */
    @GetMapping("/events/month/{year}/{month}")
    public ResponseEntity<Map<String, Object>> getEventsByMonth(
            @PathVariable Long groupId,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            List<StudyGroupCalendar> events = calendarService.getEventsByMonth(groupId, year, month);
            
            List<Map<String, Object>> eventList = events.stream()
                    .map(e -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("id", e.getId());
                        eventMap.put("title", e.getTitle());
                        eventMap.put("eventDate", e.getEventDate());
                        eventMap.put("eventTime", e.getEventTime() != null ? e.getEventTime().toString() : null);
                        eventMap.put("content", e.getContent() != null ? e.getContent() : "");
                        eventMap.put("eventType", e.getEventType());
                        eventMap.put("color", e.getColor());
                        eventMap.put("userName", e.getUser().getRealName());
                        eventMap.put("userId", e.getUser().getId());
                        return eventMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "events", eventList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
