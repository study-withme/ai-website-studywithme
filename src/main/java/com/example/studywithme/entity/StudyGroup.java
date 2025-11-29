package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "study_groups")
@Getter
@Setter
@ToString
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post; // 연결된 게시글 (nullable)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator; // 리더 (creator_id로 매핑)

    @Column(nullable = false, length = 200)
    private String title; // 그룹 이름

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 255)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.ACTIVE;

    @Column(name = "max_members")
    private Integer maxMembers;

    @Column(name = "current_members")
    private Integer currentMembers = 1; // 리더 포함

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type")
    private MeetingType meetingType = MeetingType.ONLINE;

    @Column(length = 255)
    private String location;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "studyGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StudyGroupMember> members;

    public enum GroupStatus {
        ACTIVE,  // 진행중
        CLOSED,  // 종료
        FULL     // 정원 초과
    }

    public enum MeetingType {
        ONLINE,   // 온라인
        OFFLINE,  // 오프라인
        HYBRID    // 혼합
    }
}

