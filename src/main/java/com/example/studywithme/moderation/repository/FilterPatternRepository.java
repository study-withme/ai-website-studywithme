package com.example.studywithme.moderation.repository;

import com.example.studywithme.moderation.entity.FilterPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterPatternRepository extends JpaRepository<FilterPattern, Long> {
    List<FilterPattern> findByIsActiveTrue();
    
    List<FilterPattern> findByPatternTypeAndIsActiveTrue(FilterPattern.PatternType patternType);
}

