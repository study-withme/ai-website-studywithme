package com.example.studywithme.controller;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.PostApplication;
import com.example.studywithme.entity.StudyGroup;
import com.example.studywithme.entity.StudyGroupMember;
import com.example.studywithme.entity.User;
import com.example.studywithme.entity.UserPreference;
import com.example.studywithme.repository.UserRepository;
import com.example.studywithme.repository.UserPreferenceRepository;
import com.example.studywithme.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final UserService userService;
    private final PostService postService;
    private final PostLikeService postLikeService;
    private final BookmarkService bookmarkService;
    private final PostApplicationService postApplicationService;
    private final UserStatsService userStatsService;
    private final UserActivityService userActivityService;
    private final UserRecommendationService userRecommendationService;
    private final com.example.studywithme.repository.UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final com.example.studywithme.service.AITagService aiTagService;
    private final com.example.studywithme.service.AISummaryService aiSummaryService;
    private final com.example.studywithme.service.StudyGroupService studyGroupService;
    private final com.example.studywithme.repository.CertificationRepository certificationRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /* ===========================
       PAGE ROUTING (GET)
       =========================== */

    @GetMapping("/")
    public String index(HttpSession session, Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "9") int size,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "latest") String sort) {
        // 로그인 유저 세션에서 꺼내기
        User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);

        // 게시글 목록 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = Page.empty(pageable); // 기본값으로 빈 페이지 설정

        try {
            // 1순위: 명시적인 카테고리 선택
            if (category != null && !category.trim().isEmpty()) {
                posts = postService.getPostsByCategory(category, pageable, sort);
                model.addAttribute("category", category);
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                // 2순위: 키워드 검색 (카테고리 없을 때만)
                posts = postService.searchPosts(keyword, pageable);
                model.addAttribute("keyword", keyword);
                if (loginUser != null) {
                    userActivityService.logSearch(loginUser, keyword);
                }
            } else {
                posts = postService.getPosts(pageable, sort);
            }
        } catch (Exception e) {
            // 오류 발생 시 빈 페이지 반환
            log.error("게시글 목록 조회 오류", e);
            posts = Page.empty(pageable);
        }
        
        // posts가 null이 되지 않도록 보장 (이중 체크)
        if (posts == null) {
            log.warn("posts가 null입니다. 빈 페이지로 대체합니다.");
            posts = Page.empty(pageable);
        }

        // 안전하게 모델에 추가
        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts != null ? posts.getTotalPages() : 0);
        model.addAttribute("sort", sort);

        // 자격증 정보 조회 (다가오는 시험일정 최대 10개)
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            List<com.example.studywithme.entity.Certification> certifications = 
                certificationRepository.findUpcomingExams(today);
            model.addAttribute("certifications", certifications);
        } catch (Exception e) {
            // 자격증 정보가 없어도 메인 페이지는 정상 동작
            model.addAttribute("certifications", java.util.Collections.emptyList());
        }

        return "index";  // templates/index.html
    }
    
    // API: 게시글 목록 (JSON)
    @GetMapping("/api/posts")
    @ResponseBody
    public Page<Post> getPostsApi(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "100") int size,
                                  @RequestParam(defaultValue = "latest") String sort,
                                  @RequestParam(required = false) String category) {
        Pageable pageable = PageRequest.of(page, size);
        if (category != null && !category.trim().isEmpty()) {
            return postService.getPostsByCategory(category, pageable, sort);
        }
        return postService.getPosts(pageable, sort);
    }

    @GetMapping("/auth")
    public String auth(@RequestParam(required = false) String error,
                       Model model) {
        model.addAttribute("error", error);
        return "auth";   // templates/auth.html (로그인 페이지)
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error,
                           Model model) {
        model.addAttribute("error", error);
        return "register"; // templates/register.html
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
    
    /**
     * 사용자가 선택한 카테고리를 실제 게시글 카테고리로 매핑
     */
    private List<String> mapCategoriesToActualCategories(String selectedCategory) {
        String trimmed = selectedCategory.trim();
        
        // 카테고리 매핑 규칙 (실제 DB 카테고리: 개발, 자격증, 영어, 독서, 취업, 기타)
        if (trimmed.contains("코딩") || trimmed.contains("개발") || trimmed.contains("프로그래밍")) {
            return List.of("개발");  // "프로그래밍" 제거 - 실제 DB에는 "개발"만 존재
        } else if (trimmed.contains("자격증") || trimmed.contains("기술")) {
            return List.of("자격증");
        } else if (trimmed.contains("영어") || trimmed.contains("어학")) {
            return List.of("영어");  // "언어" 제거 - 실제 DB에는 "영어"만 존재
        } else if (trimmed.contains("취업") || trimmed.contains("면접")) {
            return List.of("취업");
        } else if (trimmed.contains("독서") || trimmed.contains("인문학")) {
            return List.of("독서");
        } else if (trimmed.contains("AI") || trimmed.contains("데이터")) {
            return List.of("개발");  // "프로그래밍" 제거
        } else if (trimmed.contains("자기계발") || trimmed.contains("루틴")) {
            return List.of("기타");
        } else if (trimmed.contains("디자인") || trimmed.contains("창의")) {
            return List.of("기타");
        }
        
        // 매핑 규칙에 없으면 원본 반환
        return List.of(trimmed);
    }
    
    // AI 프로필 분석 완료 처리
    @PostMapping("/ai/complete")
    @Transactional
    public String completeAiProfile(@RequestParam("categories") List<String> categories,
                                   HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        
        try {
            // 활동 로그 기록
            String categoriesStr = String.join(",", categories);
            userActivityService.logAIClick(loginUser, categoriesStr);
            
            // 기존 선호도 삭제 (새로운 선택으로 덮어쓰기)
            userPreferenceRepository.deleteAllByUserId(loginUser.getId());
            
            // 선택한 카테고리를 실제 게시글 카테고리로 매핑하여 저장
            for (String selectedCategory : categories) {
                // 카테고리 이름 정리 (앞뒤 공백 제거)
                String categoryName = selectedCategory.trim();
                if (categoryName.isEmpty()) {
                    continue;
                }
                
                // 실제 게시글 카테고리로 매핑
                List<String> actualCategories = mapCategoriesToActualCategories(categoryName);
                
                // 매핑된 각 카테고리를 저장
                for (String actualCategory : actualCategories) {
                    UserPreference preference = new UserPreference();
                    preference.setUser(loginUser);
                    preference.setRealName(loginUser.getRealName());
                    preference.setCategoryName(actualCategory);
                    preference.setPreferenceScore(5.0f); // AI 선택은 높은 점수 부여
                    userPreferenceRepository.save(preference);
                }
            }
            
            return "redirect:/recommend?success=ai_profile_completed";
        } catch (Exception e) {
            // 에러 발생 시 로그 출력하고 홈으로 리다이렉트
            System.err.println("AI 프로필 완료 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/ai?error=save_failed";
        }
    }

    // 게시글 작성 페이지
    @GetMapping("/posts/write")
    public String writePost(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        model.addAttribute("loginUser", loginUser);
        return "post-write";
    }

    // 게시글 상세 페이지
    @GetMapping("/posts/{id}")
    public String viewPost(@PathVariable Long id, HttpSession session, Model model) {
        try {
            Post post = postService.getPost(id);
            User loginUser = (User) session.getAttribute("loginUser");
            boolean isLiked = false;
            boolean isBookmarked = false;
            boolean hasApplied = false;
            int applicationCount = 0;

            PostApplication.ApplicationStatus applicationStatus = null;
            if (loginUser != null) {
                isLiked = postLikeService.isLiked(loginUser.getId(), id);
                isBookmarked = bookmarkService.isBookmarked(loginUser.getId(), id);
                hasApplied = postApplicationService.hasApplied(loginUser.getId(), id);
                userActivityService.logViewPost(loginUser, id, post.getTitle(), post.getTags());
                
                // 지원 상태 가져오기 (지원한 경우)
                if (hasApplied) {
                    PostApplication application = postApplicationService.getApplicationByUserAndPost(loginUser.getId(), id);
                    if (application != null) {
                        applicationStatus = application.getStatus();
                    }
                }
            }
            applicationCount = postApplicationService.getApplicationCount(id);

            // 작성자 활동 통계 & 프로필
            UserStatsService.UserStats authorStats = userStatsService.getUserStats(post.getUser().getId());
            com.example.studywithme.entity.UserProfile authorProfile =
                    userProfileRepository.findByUser_Id(post.getUser().getId()).orElse(null);

            // 작성자의 다른 게시글 (상위 5개)
            java.util.List<Post> authorPosts = postService.getOtherPostsByAuthor(post.getUser().getId(), id);
            
            model.addAttribute("post", post);
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("isAuthor", loginUser != null && loginUser.getId().equals(post.getUser().getId()));
            model.addAttribute("isLiked", isLiked);
            model.addAttribute("isBookmarked", isBookmarked);
            model.addAttribute("hasApplied", hasApplied);
            model.addAttribute("applicationStatus", applicationStatus);
            model.addAttribute("applicationCount", applicationCount);
            model.addAttribute("authorPosts", authorPosts);
            model.addAttribute("authorStats", authorStats);
            model.addAttribute("authorProfile", authorProfile);
            return "post-detail";
        } catch (RuntimeException e) {
            return "redirect:/?error=post_not_found";
        }
    }

    // 게시글 수정 페이지
    @GetMapping("/posts/{id}/edit")
    public String editPost(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            Post post = postService.getPost(id);
            if (!post.getUser().getId().equals(loginUser.getId())) {
                return "redirect:/posts/" + id + "?error=no_permission";
            }
            model.addAttribute("post", post);
            model.addAttribute("loginUser", loginUser);
            return "post-edit";
        } catch (RuntimeException e) {
            return "redirect:/?error=post_not_found";
        }
    }

    // 마이페이지
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size) {
        User sessionUser = (User) session.getAttribute("loginUser");
        if (sessionUser == null) {
            return "redirect:/auth?error=login_required";
        }

        // 세션의 사용자 ID로 DB에서 최신 사용자 정보를 다시 조회 (보안 강화)
        Integer userId = sessionUser.getId();
        if (userId == null) {
            return "redirect:/auth?error=login_required";
        }
        User loginUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 세션의 사용자 정보와 DB의 사용자 정보가 일치하는지 확인
        if (!loginUser.getId().equals(sessionUser.getId())) {
            session.invalidate();
            return "redirect:/auth?error=session_invalid";
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> myPosts = postService.getPostsByUserId(loginUser.getId(), pageable);

        // 사용자 활동 통계
        UserStatsService.UserStats stats = userStatsService.getUserStats(loginUser.getId());
        // 프로필
        com.example.studywithme.entity.UserProfile profile =
                userProfileRepository.findByUser_Id(loginUser.getId()).orElse(null);

        // 프로필이 존재하는 경우, 프로필의 사용자 ID가 현재 로그인 사용자와 일치하는지 확인
        if (profile != null && !profile.getUser().getId().equals(loginUser.getId())) {
            // 프로필의 사용자 ID가 일치하지 않으면 null로 설정
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

    // 마이페이지: 프로필 이미지 업데이트
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

    /* ===========================
       FORM HANDLING (POST)
       =========================== */

    // 회원가입 처리
    @PostMapping("/register")
    public String registerUser(
            @RequestParam("realName") String realName,
            @RequestParam("birthDate") String birthDateStr, // yyyy-MM-dd
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("passwordConfirm") String passwordConfirm,
            HttpSession session
    ) {
        // 기본 검증
        if (!password.equals(passwordConfirm)) {
            return "redirect:/register?error=pwd_mismatch";
        }
        if (password.length() < 7 || password.length() > 20) {
            return "redirect:/register?error=pwd_length";
        }

        LocalDate birthDate = LocalDate.parse(birthDateStr);

        boolean ok = userService.register(realName, birthDate, email, password);
        if (!ok) {
            return "redirect:/register?error=email_exists";
        }

        // 회원가입 성공 → 자동 로그인 처리
        var userOpt = userService.login(email, password);
        if (userOpt.isPresent()) {
            session.setAttribute("loginUser", userOpt.get());
        }

        // AI 프로필 선택 페이지로
        return "redirect:/ai";
    }

    // 로그인 처리
    @PostMapping("/auth")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        HttpSession session) {

        var userOpt = userService.login(email, password);
        if (userOpt.isEmpty()) {
            return "redirect:/auth?error=invalid";
        }

        User user = userOpt.get();
        session.setAttribute("loginUser", user);  // 세션에 저장

        return "redirect:/";
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // 게시글 작성 처리
    @PostMapping("/posts/write")
    public String createPost(@RequestParam("title") String title,
                             @RequestParam("content") String content,
                             @RequestParam(value = "category", required = false) String category,
                             @RequestParam(value = "tags", required = false) String tags,
                             HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        // 입력 검증
        if (title == null || title.trim().isEmpty()) {
            return "redirect:/posts/write?error=title_required";
        }
        if (content == null || content.trim().isEmpty() || content.trim().equals("<br>") || content.trim().equals("<p></p>")) {
            return "redirect:/posts/write?error=content_required";
        }

        try {
            // HTML 태그 제거하여 텍스트만 확인
            String textContent = content.replaceAll("<[^>]*>", "").trim();
            if (textContent.isEmpty()) {
                return "redirect:/posts/write?error=content_required";
            }
            
            postService.createPost(loginUser.getId(), title.trim(), content, 
                                                      (category != null && !category.trim().isEmpty()) ? category.trim() : null,
                                                      (tags != null && !tags.trim().isEmpty()) ? tags.trim() : null);
            // 게시글 작성 후 메인 페이지로 리다이렉트 (최신순으로 표시되도록)
            return "redirect:/?success=post_created";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/posts/write?error=create_failed&msg=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // 게시글 수정 처리
    @PostMapping("/posts/{id}/edit")
    public String updatePost(@PathVariable Long id,
                             @RequestParam("title") String title,
                             @RequestParam("content") String content,
                             @RequestParam(value = "category", required = false) String category,
                             @RequestParam(value = "tags", required = false) String tags,
                             HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            postService.updatePost(id, loginUser.getId(), title, content, category, tags);
            return "redirect:/posts/" + id + "?success=post_updated";
        } catch (RuntimeException e) {
            return "redirect:/posts/" + id + "/edit?error=" + e.getMessage();
        }
    }

    // 게시글 삭제 처리
    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            postService.deletePost(id, loginUser.getId());
            return "redirect:/?success=post_deleted";
        } catch (RuntimeException e) {
            return "redirect:/posts/" + id + "?error=" + e.getMessage();
        }
    }

    // 좋아요 토글
    @PostMapping("/posts/{id}/like")
    @ResponseBody
    public Map<String, Object> toggleLike(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            boolean isLiked = postLikeService.toggleLike(loginUser.getId(), id);
            long likeCount = postLikeService.getLikeCount(id);
            if (isLiked) {
                userActivityService.logLikePost(loginUser, id);
            }
            return Map.of("success", true, "isLiked", isLiked, "likeCount", (int)likeCount);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 북마크 토글
    @PostMapping("/posts/{id}/bookmark")
    @ResponseBody
    public Map<String, Object> toggleBookmark(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            boolean isBookmarked = bookmarkService.toggleBookmark(loginUser.getId(), id);
            // 북마크 시 활동 로그 기록
            if (isBookmarked) {
                userActivityService.logBookmark(loginUser, id);
            }
            return Map.of("success", true, "isBookmarked", isBookmarked);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 게시글 지원하기
    @PostMapping("/posts/{id}/apply")
    @ResponseBody
    public Map<String, Object> applyToPost(@PathVariable Long id,
                                            @RequestParam(value = "message", required = false) String message,
                                            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            postApplicationService.applyToPost(loginUser.getId(), id, message);
            int applicationCount = postApplicationService.getApplicationCount(id);
            return Map.of("success", true, "message", "지원이 완료되었습니다.", "applicationCount", applicationCount);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 게시글 지원 취소
    @PostMapping("/posts/{id}/cancel-apply")
    @ResponseBody
    public Map<String, Object> cancelApplication(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            postApplicationService.cancelApplication(loginUser.getId(), id);
            int applicationCount = postApplicationService.getApplicationCount(id);
            return Map.of("success", true, "message", "지원이 취소되었습니다.", "applicationCount", applicationCount);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // AI 요약 생성
    @PostMapping("/posts/{id}/ai-summary")
    @ResponseBody
    public Map<String, Object> generateAISummary(@PathVariable Long id) {
        try {
            Post post = postService.getPost(id);
            String content = post.getContent();
            
            // HTML 태그 제거
            String textContent = content.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            
            // 간단한 요약 로직 (실제로는 AI API를 호출해야 함)
            String summary = generateSimpleSummary(textContent, post.getTitle());
            
            return Map.of("success", true, "summary", summary);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private String generateSimpleSummary(String content, String title) {
        // 간단한 요약 로직 (실제로는 AI API를 호출해야 함)
        if (content.length() > 500) {
            return content.substring(0, 500) + "...";
        }
        
        // 키워드 기반 요약
        StringBuilder summary = new StringBuilder();
        summary.append("📌 ").append(title).append("\n\n");
        
        if (content.contains("스터디 소개") || content.contains("소개")) {
            summary.append("• 스터디 소개: 게시글 내용을 확인하세요.\n");
        }
        if (content.contains("진행 방식") || content.contains("방식")) {
            summary.append("• 진행 방식: 게시글에서 확인 가능합니다.\n");
        }
        if (content.contains("커리큘럼") || content.contains("커리")) {
            summary.append("• 커리큘럼: 게시글에 상세히 기재되어 있습니다.\n");
        }
        if (content.contains("좋아요") || content.contains("조건")) {
            summary.append("• 참여 조건: 게시글을 확인해주세요.\n");
        }
        
        if (summary.length() == title.length() + 5) {
            summary.append("• 게시글 내용을 요약한 정보입니다.\n");
            summary.append("• 자세한 내용은 본문을 확인해주세요.");
        }
        
        return summary.toString();
    }

    // 북마크 목록 페이지
    @GetMapping("/bookmarks")
    public String bookmarks(HttpSession session, Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        Pageable pageable = PageRequest.of(page, size);
        var bookmarks = bookmarkService.getBookmarks(loginUser.getId(), pageable);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("bookmarks", bookmarks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookmarks.getTotalPages());

        return "bookmarks";
    }

    // 추천 게시글 API (활동 로그 기반)
    @GetMapping("/api/recommendations/posts")
    @ResponseBody
    public java.util.List<Post> recommendPosts(HttpSession session,
                                               @RequestParam(defaultValue = "10") int size) {
        User loginUser = (User) session.getAttribute("loginUser");
        Integer userId = loginUser != null ? loginUser.getId() : null;
        return userRecommendationService.recommendPosts(userId, size);
    }

    // 사용자 선호도 조회 API
    @GetMapping("/api/user/preferences")
    @ResponseBody
    public java.util.List<com.example.studywithme.entity.UserPreference> getUserPreferences(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return java.util.Collections.emptyList();
        }
        return userPreferenceRepository.findByUser_Id(loginUser.getId());
    }

    // AI 태그 추천 API
    @PostMapping("/api/posts/ai-tags")
    @ResponseBody
    public Map<String, Object> recommendAITags(@RequestParam("title") String title,
                                                @RequestParam("content") String content) {
        try {
            return aiTagService.recommendTags(title, content);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // AI 요약 API
    @PostMapping("/api/posts/{id}/ai-summary")
    @ResponseBody
    public Map<String, Object> getAISummary(@PathVariable Long id,
                                            @RequestParam(defaultValue = "200") int maxLength) {
        try {
            Post post = postService.getPost(id);
            String content = post.getContent();
            // HTML 태그 제거
            content = content.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            return aiSummaryService.summarizeContent(content, maxLength);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    // 내 지원현황 페이지
    @GetMapping("/my-applications")
    public String myApplications(HttpSession session, Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String status) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        PostApplication.ApplicationStatus appStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                appStatus = PostApplication.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 무시
            }
        }

        List<PostApplication> applications = postApplicationService.getApplicationsByUser(
                loginUser.getId(), appStatus);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("applications", applications);
        model.addAttribute("status", status);
        
        return "my-applications";
    }

    // 지원받기 페이지 (게시글 작성자가 받은 지원 목록)
    @GetMapping("/posts/{id}/applications")
    public String postApplications(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            Post post = postService.getPost(id);
            
            // 권한 확인 (게시글 작성자만)
            if (!post.getUser().getId().equals(loginUser.getId())) {
                return "redirect:/posts/" + id + "?error=no_permission";
            }

            // 모든 상태의 지원 표시 (필터링은 템플릿에서 처리)
            List<PostApplication> applications = postApplicationService.getApplicationsByPost(id, null);

            model.addAttribute("loginUser", loginUser);
            model.addAttribute("post", post);
            model.addAttribute("applications", applications);
            
            return "post-applications";
        } catch (RuntimeException e) {
            return "redirect:/?error=" + e.getMessage();
        }
    }

    // 지원 승인 API
    @PostMapping("/api/applications/{id}/accept")
    @ResponseBody
    public Map<String, Object> acceptApplication(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            postApplicationService.acceptApplication(id, loginUser.getId());
            return Map.of("success", true, "message", "지원이 승인되었습니다.");
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 지원 거절 API
    @PostMapping("/api/applications/{id}/reject")
    @ResponseBody
    public Map<String, Object> rejectApplication(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }

        try {
            postApplicationService.rejectApplication(id, loginUser.getId());
            return Map.of("success", true, "message", "지원이 거절되었습니다.");
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 진행중인 스터디 페이지
    @GetMapping("/study-groups")
    public String studyGroups(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            List<StudyGroup> groups = studyGroupService.getActiveGroupsByUser(loginUser.getId());
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("groups", groups != null ? groups : java.util.Collections.emptyList());
        } catch (Exception e) {
            // 에러 발생 시 빈 리스트로 설정
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("groups", java.util.Collections.emptyList());
        }
        
        return "study-groups";
    }

    // 스터디 그룹 상세 페이지
    @GetMapping("/study-groups/{id}")
    public String studyGroupDetail(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            StudyGroup group = studyGroupService.getGroupById(id);
            List<StudyGroupMember> members = studyGroupService.getGroupMembers(id);

            model.addAttribute("loginUser", loginUser);
            model.addAttribute("group", group);
            model.addAttribute("members", members);
            
            return "study-group-detail";
        } catch (RuntimeException e) {
            return "redirect:/study-groups?error=" + e.getMessage();
        }
    }

    // 학습 세션 페이지
    @GetMapping("/study-groups/{id}/session")
    public String studySession(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }

        try {
            StudyGroup group = studyGroupService.getGroupById(id);
            
            // 사용자가 멤버인지 확인
            boolean isMember = studyGroupService.getGroupMembers(id).stream()
                    .anyMatch(m -> m.getUser().getId().equals(loginUser.getId()));
            
            if (!isMember) {
                return "redirect:/study-groups?error=스터디 멤버만 접근할 수 있습니다.";
            }

            model.addAttribute("loginUser", loginUser);
            model.addAttribute("group", group);
            
            return "study-session";
        } catch (RuntimeException e) {
            return "redirect:/study-groups?error=" + e.getMessage();
        }
    }
}
