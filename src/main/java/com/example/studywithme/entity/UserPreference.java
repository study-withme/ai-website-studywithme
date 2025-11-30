package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@ToString
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "preference_score", nullable = false)
    private Float preferenceScore = 1.0f;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
