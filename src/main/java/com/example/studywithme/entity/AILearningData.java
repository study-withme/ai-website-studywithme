package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_learning_data")
@Getter
@Setter
@ToString
public class AILearningData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType = ContentType.POST;

    @Column(name = "content_sample", nullable = false, columnDefinition = "TEXT")
    private String contentSample;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "detected_pattern", columnDefinition = "TEXT")
    private String detectedPattern;

    @Column(name = "frequency")
    private Integer frequency = 1;

    @Column(name = "last_detected_at", insertable = false, updatable = false)
    private LocalDateTime lastDetectedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ContentType {
        POST, COMMENT
    }
}

