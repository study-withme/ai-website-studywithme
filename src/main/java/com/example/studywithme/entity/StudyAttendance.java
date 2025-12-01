package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_attendance")
@Getter
@Setter
@ToString
public class StudyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_session_id", nullable = false)
    private StudySession studySession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt; // 출석체크 시간

    @Column(name = "is_late")
    private Boolean isLate = false; // 지각 여부

    @Column(name = "message", length = 200)
    private String message; // 출석체크 메시지

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
