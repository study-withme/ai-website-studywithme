package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_group_goals")
@Getter
@Setter
@ToString
public class StudyGroupGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Column(name = "goal_type", nullable = false, length = 50)
    private String goalType; // DAILY, WEEKLY, MONTHLY

    @Column(name = "target_value", nullable = false)
    private Integer targetValue; // 목표 값 (시간, 출석 수 등)

    @Column(name = "current_value", nullable = false)
    private Integer currentValue = 0; // 현재 값

    @Column(name = "goal_unit", length = 20)
    private String goalUnit; // HOURS, DAYS, SESSIONS 등

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_achieved")
    private Boolean isAchieved = false; // 달성 여부

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt; // 달성 시간

    @Column(name = "created_by")
    private Integer createdBy; // 목표 설정한 사용자

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
