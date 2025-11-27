package com.example.studywithme.controller;

import com.example.studywithme.entity.Notification;
import com.example.studywithme.entity.User;
import com.example.studywithme.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Object> getUnreadCount(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("count", 0);
        }
        long count = notificationService.getUnreadCount(loginUser.getId());
        return Map.of("count", count);
    }

    @GetMapping("/api/notifications/recent")
    public List<Map<String, Object>> getRecent(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return List.of();
        }
        return notificationService.getRecent(loginUser.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/notifications/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        try {
            notificationService.markAsRead(id, loginUser.getId());
            return Map.of("success", true);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private Map<String, Object> toDto(Notification n) {
        return Map.of(
                "id", n.getId(),
                "type", n.getType(),
                "title", n.getTitle(),
                "body", n.getBody(),
                "linkUrl", n.getLinkUrl(),
                "isRead", n.getRead(),
                "createdAt", n.getCreatedAt()
        );
    }
}


