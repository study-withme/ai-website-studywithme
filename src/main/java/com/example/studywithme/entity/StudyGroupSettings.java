package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "study_group_settings")
@Getter
@Setter
@ToString
public class StudyGroupSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false, unique = true)
    private StudyGroup studyGroup;

    @Column(name = "study_duration", nullable = false)
    private Integer studyDuration = 50; // 기본 학습시간 (분)

    @Column(name = "break_duration", nullable = false)
    private Integer breakDuration = 10; // 쉬는시간 (분)

    @Column(name = "cycles_per_day", nullable = false)
    private Integer cyclesPerDay = 4; // 하루 사이클 수

    @Column(name = "start_time")
    private LocalTime startTime; // 시작 시간

    @Column(name = "created_by")
    private Integer createdBy; // 설정한 사용자 ID (팀장)

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
