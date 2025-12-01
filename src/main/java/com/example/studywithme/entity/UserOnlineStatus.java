package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_online_status")
@Getter
@Setter
@ToString
public class UserOnlineStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private OnlineStatus currentStatus = OnlineStatus.OFFLINE;

    @Column(name = "current_study_group_id")
    private Long currentStudyGroupId; // 현재 학습 중인 스터디

    @Column(name = "status_message", length = 200)
    private String statusMessage; // 상태 메시지

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum OnlineStatus {
        ONLINE,      // 온라인
        AWAY,        // 자리비움
        STUDYING,    // 학습 중
        BREAK,       // 쉬는 시간
        OFFLINE      // 오프라인
    }
}
