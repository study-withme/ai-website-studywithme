package com.example.studywithme.repository;

import com.example.studywithme.entity.AILearningData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AILearningDataRepository extends JpaRepository<AILearningData, Long> {
    Page<AILearningData> findAllByOrderByFrequencyDesc(Pageable pageable);
    
    @Query("SELECT a FROM AILearningData a WHERE a.frequency >= :minFrequency ORDER BY a.frequency DESC")
    Page<AILearningData> findHighFrequencyPatterns(int minFrequency, Pageable pageable);
}

