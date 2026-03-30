package com.example.studywithme.user.controller;

import com.example.studywithme.board.entity.Post;
import com.example.studywithme.board.service.PostService;
import com.example.studywithme.user.entity.User;
import com.example.studywithme.user.application.UserOnboardingApplicationService;
import com.example.studywithme.user.entity.UserPreference;
import com.example.studywithme.user.entity.UserProfile;
import com.example.studywithme.user.repository.UserPreferenceRepository;
import com.example.studywithme.user.repository.UserProfileRepository;
import com.example.studywithme.user.repository.UserRepository;
import com.example.studywithme.user.service.UserRecommendationService;
import com.example.studywithme.user.service.UserService;
import com.example.studywithme.user.service.UserStatsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 인증·회원·마이페이지·온보딩(AI 프로필)·추천 API.
 */
@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserService userService;
    private final PostService postService;
    private final UserStatsService userStatsService;
    private final UserOnboardingApplicationService userOnboardingApplicationService;
    private final UserRecommendationService userRecommendationService;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @GetMapping("/auth")
    public String auth(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error);
        return "auth";
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("error", error);
        return "register";
    }

    @GetMapping("/ai")
    public String aiProfile(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        model.addAttribute("loginUser", loginUser);
        return "ai";
    }

    @GetMapping("/recommend")
    public String recommend(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        model.addAttribute("loginUser", loginUser);
        return "recommend";
    }

    @PostMapping("/ai/complete")
    public String completeAiProfile(@RequestParam("categories") List<String> categories, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        try {
            userOnboardingApplicationService.completeAiProfile(loginUser, categories);
            return "redirect:/recommend?success=ai_profile_completed";
        } catch (Exception e) {
            System.err.println("AI 프로필 완료 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/ai?error=save_failed";
        }
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size) {
        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser == null) {
            return "redirect:/auth?error=login_required";
        }
        Integer userId = sessionUser.getId();
        if (userId == null) {
            return "redirect:/auth?error=login_required";
        }
        User loginUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        if (!loginUser.getId().equals(sessionUser.getId())) {
            session.invalidate();
            return "redirect:/auth?error=session_invalid";
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> myPosts = postService.getPostsByUserId(loginUser.getId(), pageable);
        UserStatsService.UserStats stats = userStatsService.getUserStats(loginUser.getId());
        UserProfile profile = userProfileRepository.findByUser_Id(loginUser.getId()).orElse(null);
        if (profile != null && !profile.getUser().getId().equals(loginUser.getId())) {
            profile = null;
        }
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("posts", myPosts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", myPosts.getTotalPages());
        model.addAttribute("stats", stats);
        model.addAttribute("profile", profile);
        return "mypage";
    }

    @PostMapping("/mypage/profile-image")
    public String updateProfileImage(@RequestParam(value = "imageUrl", required = false) String imageUrl,
                                     HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        userService.updateProfileImage(loginUser.getId(), imageUrl);
        return "redirect:/mypage";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam("realName") String realName,
            @RequestParam("birthDate") String birthDateStr,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm,
            HttpSession session) {
        if (!password.equals(passwordConfirm)) {
            return "redirect:/register?error=pwd_mismatch";
        }
        if (password.length() < 7 || password.length() > 20) {
            return "redirect:/register?error=pwd_length";
        }
        LocalDate birthDate = LocalDate.parse(birthDateStr);
        if (!userService.register(realName, birthDate, email, password)) {
            return "redirect:/register?error=email_exists";
        }
        var userOpt = userService.login(email, password);
        userOpt.ifPresent(user -> session.setAttribute("loginUser", user));
        return "redirect:/ai";
    }

    @PostMapping("/auth")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session) {
        var userOpt = userService.login(email, password);
        if (userOpt.isEmpty()) {
            return "redirect:/auth?error=invalid";
        }
        session.setAttribute("loginUser", userOpt.get());
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/api/recommendations/posts")
    @ResponseBody
    public List<Post> recommendPosts(HttpSession session, @RequestParam(defaultValue = "10") int size) {
        User loginUser = (User) session.getAttribute("loginUser");
        Integer userId = loginUser != null ? loginUser.getId() : null;
        return userRecommendationService.recommendPosts(userId, size);
    }

    @GetMapping("/api/user/preferences")
    @ResponseBody
    public List<UserPreference> getUserPreferences(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Collections.emptyList();
        }
        return userPreferenceRepository.findByUser_Id(loginUser.getId());
    }
}
