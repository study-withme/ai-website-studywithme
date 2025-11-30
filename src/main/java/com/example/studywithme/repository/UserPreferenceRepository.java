package com.example.studywithme.repository;

import com.example.studywithme.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    
    List<UserPreference> findByUser_Id(Integer userId);
    
    Optional<UserPreference> findByUser_IdAndCategoryName(Integer userId, String categoryName);
    
    @Modifying
    @Query("DELETE FROM UserPreference up WHERE up.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);
}
