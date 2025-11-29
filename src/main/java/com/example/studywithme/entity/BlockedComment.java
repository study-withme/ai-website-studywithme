package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_comments")
@Getter
@Setter
@ToString
public class BlockedComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false, unique = true)
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type")
    private BlockType blockType = BlockType.AI_DETECTED;

    @Column(name = "detected_keywords", columnDefinition = "TEXT")
    private String detectedKeywords; // JSON 형식

    @Column(name = "ai_confidence")
    private Float aiConfidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by")
    private User blockedBy; // 관리자

    @Column(name = "is_reviewed")
    private Boolean isReviewed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BlockStatus status = BlockStatus.BLOCKED;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BlockType {
        PROFANITY, SPAM, AD, PATTERN, KEYWORD, AI_DETECTED
    }

    public enum BlockStatus {
        BLOCKED, RESTORED, PENDING
    }
}

