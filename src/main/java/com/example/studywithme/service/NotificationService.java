package com.example.studywithme.service;

import com.example.studywithme.entity.Notification;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.NotificationRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notify(Integer userId, String type, String title, String body, String linkUrl) {
        if (userId == null) return;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 대상을 찾을 수 없습니다."));
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLinkUrl(linkUrl);
        n.setRead(false);
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecent(Integer userId) {
        return notificationRepository.findTop10ByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long id, Integer userId) {
        if (id == null) {
            throw new RuntimeException("알림 ID가 필요합니다.");
        }
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));
        if (!n.getUser().getId().equals(userId)) {
            throw new RuntimeException("알림을 읽을 권한이 없습니다.");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }
}


