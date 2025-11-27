package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@ToString
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // users.id 와 1:1
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(length = 50)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "join_purpose", length = 255)
    private String joinPurpose;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
