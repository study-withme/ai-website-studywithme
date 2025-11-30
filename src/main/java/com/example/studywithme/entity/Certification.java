package com.example.studywithme.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "certifications")
@Getter
@Setter
@ToString
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name; // 자격증 이름

    @Column(length = 100)
    private String category; // 카테고리 (IT, 언어, 기타 등)

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    @Column(name = "exam_date")
    private LocalDate examDate; // 시험일정

    @Column(name = "registration_start")
    private LocalDate registrationStart; // 접수 시작일

    @Column(name = "registration_end")
    private LocalDate registrationEnd; // 접수 종료일

    @Column(length = 255)
    private String website; // 공식 웹사이트

    @Column(length = 500)
    private String imageUrl; // 이미지 URL

    @Column(name = "is_active")
    private Boolean isActive = true; // 활성화 여부

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.time.LocalDateTime updatedAt;
}
