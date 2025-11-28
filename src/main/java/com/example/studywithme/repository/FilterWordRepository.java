package com.example.studywithme.repository;

import com.example.studywithme.entity.FilterWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterWordRepository extends JpaRepository<FilterWord, Long> {
    Optional<FilterWord> findByWord(String word);
    
    List<FilterWord> findByIsActiveTrue();
    
    List<FilterWord> findByWordTypeAndIsActiveTrue(FilterWord.WordType wordType);
}

