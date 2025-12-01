package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_journals")
@Getter
@Setter
@ToString
public class StudyJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate; // 일지 작성일

    @Column(name = "study_content", columnDefinition = "TEXT")
    private String studyContent; // 오늘 배운 것

    @Column(name = "feeling", columnDefinition = "TEXT")
    private String feeling; // 느낀 점

    @Column(name = "next_goal", columnDefinition = "TEXT")
    private String nextGoal; // 다음 목표

    @Column(name = "mood_rating")
    private Integer moodRating; // 기분 점수 (1-5)

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
