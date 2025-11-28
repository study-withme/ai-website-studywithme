package com.example.studywithme.repository;

import com.example.studywithme.entity.FilterKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterKeywordRepository extends JpaRepository<FilterKeyword, Long> {
    Optional<FilterKeyword> findByKeyword(String keyword);
    
    List<FilterKeyword> findByIsActiveTrue();
    
    List<FilterKeyword> findByKeywordTypeAndIsActiveTrue(FilterKeyword.KeywordType keywordType);
}

