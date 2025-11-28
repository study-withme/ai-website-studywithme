package com.example.studywithme.service;

import com.example.studywithme.entity.User;
import com.example.studywithme.entity.UserProfile;
import com.example.studywithme.repository.UserProfileRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public boolean register(String realName,
                            LocalDate birthDate,
                            String email,
                            String rawPassword) {

        // 이메일 중복 검사
        if (userRepository.existsByEmail(email)) {
            return false;
        }

        User user = new User();
        user.setRealName(realName);
        user.setBirthDate(birthDate);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(0); // 기본값: 일반유저

        User saved = userRepository.save(user);

        // 기본 프로필 생성
        UserProfile profile = new UserProfile();
        profile.setUser(saved);
        profile.setRealName(realName);
        profile.setNickname(realName); // 기본 닉네임 = 실명
        userProfileRepository.save(profile);

        return true;
    }

    // 로그인
    public Optional<User> login(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return Optional.empty();

        User user = userOpt.get();
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * 프로필 이미지 URL 업데이트
     */
    @Transactional
    public void updateProfileImage(Integer userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        UserProfile profile = userProfileRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setUser(user);
                    p.setRealName(user.getRealName());
                    p.setNickname(user.getRealName());
                    return p;
                });

        String trimmed = imageUrl != null ? imageUrl.trim() : null;
        profile.setProfileImage((trimmed != null && !trimmed.isEmpty()) ? trimmed : null);
        userProfileRepository.save(profile);
    }
}
