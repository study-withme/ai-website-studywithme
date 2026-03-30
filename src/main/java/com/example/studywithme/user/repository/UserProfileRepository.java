package com.example.studywithme.user.repository;

import com.example.studywithme.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

    Optional<UserProfile> findByUser_Id(Integer userId);
}
