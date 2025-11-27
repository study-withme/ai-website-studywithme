package com.example.studywithme.repository;

import com.example.studywithme.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUser_IdAndReadFalse(Integer userId);

    List<Notification> findTop10ByUser_IdOrderByCreatedAtDesc(Integer userId);
}


