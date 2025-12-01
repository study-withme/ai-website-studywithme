package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_study_stats", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "study_group_id"}))
@Getter
@Setter
@ToString
public class UserStudyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Column(name = "total_study_time")
    private Integer totalStudyTime = 0; // 전체 학습 시간 (분)

    @Column(name = "today_study_time")
    private Integer todayStudyTime = 0; // 오늘 학습 시간 (분)

    @Column(name = "week_study_time")
    private Integer weekStudyTime = 0; // 이번 주 학습 시간 (분)

    @Column(name = "consecutive_days")
    private Integer consecutiveDays = 0; // 연속 학습 일수 (스트릭)

    @Column(name = "max_consecutive_days")
    private Integer maxConsecutiveDays = 0; // 최대 연속 일수

    @Column(name = "study_style", length = 50)
    private String studyStyle; // 공부 스타일 (집중형, 꾸준형, 야행형 등)

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate; // 마지막 학습일

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate; // 마지막 출석일

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
