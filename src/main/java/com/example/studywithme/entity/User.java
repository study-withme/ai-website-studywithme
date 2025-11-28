package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;  // BCrypt 해시 저장

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(name = "role", nullable = false)
    private Integer role = 0; // 0: 일반유저, 1: 어드민

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // 관리자 여부 확인 헬퍼 메서드
    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
