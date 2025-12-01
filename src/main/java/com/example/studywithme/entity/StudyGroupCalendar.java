package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_group_calendar")
@Getter
@Setter
@ToString
public class StudyGroupCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 작성자

    @Column(name = "event_date", nullable = false)
    private java.time.LocalDate eventDate; // 일정 날짜

    @Column(name = "event_time")
    private java.time.LocalTime eventTime; // 일정 시간 (선택사항)

    @Column(name = "title", nullable = false, length = 200)
    private String title; // 일정 제목

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 일정 내용

    @Column(name = "event_type", length = 50)
    private String eventType; // 일정 타입 (STUDY, BREAK, MEETING, DEADLINE 등)

    @Column(name = "color", length = 20)
    private String color; // 일정 색상

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
