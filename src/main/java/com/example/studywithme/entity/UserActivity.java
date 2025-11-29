package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity")
@Getter
@Setter
@ToString
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, columnDefinition = "ENUM('SEARCH','CLICK','LIKE','RECOMMEND','BOOKMARK','COMMENT','AI_CLICK')")
    private ActionType actionType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_keyword", length = 100)
    private String targetKeyword;

    @Column(name = "action_detail", columnDefinition = "TEXT")
    private String actionDetail;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        SEARCH,
        CLICK,
        LIKE,
        RECOMMEND,
        BOOKMARK,
        COMMENT,
        AI_CLICK
    }
}


