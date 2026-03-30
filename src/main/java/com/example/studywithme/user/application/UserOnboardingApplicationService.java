package com.example.studywithme.user.application;

import com.example.studywithme.user.entity.User;
import com.example.studywithme.user.entity.UserPreference;
import com.example.studywithme.user.repository.UserPreferenceRepository;
import com.example.studywithme.user.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 회원가입 직후 AI 카테고리 선택 등 온보딩 시나리오.
 */
@Service
@RequiredArgsConstructor
public class UserOnboardingApplicationService {

    private final UserActivityService userActivityService;
    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional
    public void completeAiProfile(User loginUser, List<String> categories) {
        userActivityService.logAIClick(loginUser, String.join(",", categories));
        userPreferenceRepository.deleteAllByUserId(loginUser.getId());
        for (String selectedCategory : categories) {
            String categoryName = selectedCategory.trim();
            if (categoryName.isEmpty()) {
                continue;
            }
            for (String actualCategory : mapCategoriesToActualCategories(categoryName)) {
                UserPreference preference = new UserPreference();
                preference.setUser(loginUser);
                preference.setRealName(loginUser.getRealName());
                preference.setCategoryName(actualCategory);
                preference.setPreferenceScore(5.0f);
                userPreferenceRepository.save(preference);
            }
        }
    }

    private List<String> mapCategoriesToActualCategories(String selectedCategory) {
        String trimmed = selectedCategory.trim();
        if (trimmed.contains("코딩") || trimmed.contains("개발") || trimmed.contains("프로그래밍")) {
            return List.of("개발");
        } else if (trimmed.contains("자격증") || trimmed.contains("기술")) {
            return List.of("자격증");
        } else if (trimmed.contains("영어") || trimmed.contains("어학")) {
            return List.of("영어");
        } else if (trimmed.contains("취업") || trimmed.contains("면접")) {
            return List.of("취업");
        } else if (trimmed.contains("독서") || trimmed.contains("인문학")) {
            return List.of("독서");
        } else if (trimmed.contains("AI") || trimmed.contains("데이터")) {
            return List.of("개발");
        } else if (trimmed.contains("자기계발") || trimmed.contains("루틴")) {
            return List.of("기타");
        } else if (trimmed.contains("디자인") || trimmed.contains("창의")) {
            return List.of("기타");
        }
        return List.of(trimmed);
    }
}
