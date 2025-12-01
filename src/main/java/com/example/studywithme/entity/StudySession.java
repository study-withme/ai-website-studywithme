package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_sessions")
@Getter
@Setter
@ToString
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "study_duration")
    private Integer studyDuration; // 학습 시간 (분)

    @Column(name = "break_duration")
    private Integer breakDuration; // 쉬는 시간 (분)

    @Column(name = "cycles_completed")
    private Integer cyclesCompleted = 0; // 완료한 사이클 수

    @Column(name = "target_cycles")
    private Integer targetCycles; // 목표 사이클 수

    @Column(name = "attendance_checked")
    private Boolean attendanceChecked = false; // 출석체크 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "status_message", length = 200)
    private String statusMessage; // 학습 중 상태 메시지

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        IN_PROGRESS,  // 진행 중
        COMPLETED,    // 완료
        PAUSED,       // 일시정지
        ABANDONED     // 중단
    }
}
