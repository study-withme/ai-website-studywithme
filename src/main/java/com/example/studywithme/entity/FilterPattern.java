package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "filter_patterns")
@Getter
@Setter
@ToString
public class FilterPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pattern_name", nullable = false, length = 100)
    private String patternName;

    @Column(name = "pattern_regex", nullable = false, columnDefinition = "TEXT")
    private String patternRegex;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type")
    private PatternType patternType = PatternType.BOTH;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "block_count")
    private Integer blockCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum PatternType {
        TITLE, CONTENT, BOTH
    }
}

