package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 스터디 그룹 리소스 (교재, 인강 등) 엔티티
 */
@Entity
@Table(name = "study_group_resources")
@Getter
@Setter
public class StudyGroupResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    private StudyGroup studyGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType resourceType; // 리소스 타입

    @Column(nullable = false, length = 200)
    private String name; // 리소스 이름

    @Column(length = 500)
    private String url; // 리소스 링크

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // 생성자

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum ResourceType {
        BOOK,      // 교재
        VIDEO,     // 인강
        DOCUMENT,  // 문서
        LINK       // 기타 링크
    }
}
