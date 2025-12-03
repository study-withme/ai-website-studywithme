package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 스터디 그룹 커리큘럼 엔티티
 */
@Entity
@Table(name = "study_group_curriculum")
@Getter
@Setter
public class StudyGroupCurriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Column(nullable = false)
    private Integer weekNumber; // 주차 번호

    @Column(nullable = false, length = 200)
    private String title; // 주차 제목

    @Column(columnDefinition = "TEXT")
    private String description; // 주차 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurriculumStatus status = CurriculumStatus.PENDING; // 진행 상태

    @Column(name = "start_date")
    private java.time.LocalDate startDate; // 시작 날짜

    @Column(name = "end_date")
    private java.time.LocalDate endDate; // 종료 날짜

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum CurriculumStatus {
        PENDING,   // 대기중
        IN_PROGRESS, // 진행중
        COMPLETED   // 완료
    }
}
