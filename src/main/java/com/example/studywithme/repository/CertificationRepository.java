package com.example.studywithme.repository;

import com.example.studywithme.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    
    // 활성화된 자격증 목록 조회
    List<Certification> findByIsActiveTrueOrderByExamDateAsc();
    
    // 다가오는 시험일정 조회 (최대 10개)
    @Query("SELECT c FROM Certification c WHERE c.isActive = true AND c.examDate >= :today ORDER BY c.examDate ASC")
    List<Certification> findUpcomingExams(LocalDate today);
    
    // 카테고리별 조회
    List<Certification> findByCategoryAndIsActiveTrueOrderByExamDateAsc(String category);
}
