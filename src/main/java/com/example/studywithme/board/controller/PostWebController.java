package com.example.studywithme.board.controller;

import com.example.studywithme.board.entity.Post;
import com.example.studywithme.board.entity.PostApplication;
import com.example.studywithme.board.service.BookmarkService;
import com.example.studywithme.board.service.PostApplicationService;
import com.example.studywithme.board.service.PostLikeService;
import com.example.studywithme.board.service.PostService;
import com.example.studywithme.user.entity.User;
import com.example.studywithme.user.entity.UserProfile;
import com.example.studywithme.user.repository.UserProfileRepository;
import com.example.studywithme.user.service.UserActivityService;
import com.example.studywithme.user.service.UserStatsService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 게시글·북마크·지원(어플리케이션) 화면 및 관련 AJAX.
 */
@Controller
@RequiredArgsConstructor
public class PostWebController {

    private final PostService postService;
    private final PostLikeService postLikeService;
    private final BookmarkService bookmarkService;
    private final PostApplicationService postApplicationService;
    private final UserStatsService userStatsService;
    private final UserActivityService userActivityService;
    private final UserProfileRepository userProfileRepository;

    @GetMapping("/posts/write")
    public String writePost(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        model.addAttribute("loginUser", loginUser);
        return "post-write";
    }

    @GetMapping("/posts/{id}")
    public String viewPost(@PathVariable Long id, HttpSession session, Model model) {
        try {
            Post post = postService.getPost(id);
            User loginUser = (User) session.getAttribute("loginUser");
            boolean isLiked = false;
            boolean isBookmarked = false;
            boolean hasApplied = false;
            PostApplication.ApplicationStatus applicationStatus = null;
            if (loginUser != null) {
                isLiked = postLikeService.isLiked(loginUser.getId(), id);
                isBookmarked = bookmarkService.isBookmarked(loginUser.getId(), id);
                hasApplied = postApplicationService.hasApplied(loginUser.getId(), id);
                userActivityService.logViewPost(loginUser, id, post.getTitle(), post.getTags());
                if (hasApplied) {
                    PostApplication application = postApplicationService.getApplicationByUserAndPost(loginUser.getId(), id);
                    if (application != null) {
                        applicationStatus = application.getStatus();
                    }
                }
            }
            int applicationCount = postApplicationService.getApplicationCount(id);
            UserStatsService.UserStats authorStats = userStatsService.getUserStats(post.getUser().getId());
            UserProfile authorProfile = userProfileRepository.findByUser_Id(post.getUser().getId()).orElse(null);
            List<Post> authorPosts = postService.getOtherPostsByAuthor(post.getUser().getId(), id);

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
        if (title == null || title.trim().isEmpty()) {
            return "redirect:/posts/write?error=title_required";
        }
        if (content == null || content.trim().isEmpty() || content.trim().equals("<br>") || content.trim().equals("<p></p>")) {
            return "redirect:/posts/write?error=content_required";
        }
        try {
            String textContent = content.replaceAll("<[^>]*>", "").trim();
            if (textContent.isEmpty()) {
                return "redirect:/posts/write?error=content_required";
            }
            postService.createPost(loginUser.getId(), title.trim(), content,
                    (category != null && !category.trim().isEmpty()) ? category.trim() : null,
                    (tags != null && !tags.trim().isEmpty()) ? tags.trim() : null);
            return "redirect:/?success=post_created";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/posts/write?error=create_failed&msg=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

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
            return Map.of("success", true, "isLiked", isLiked, "likeCount", (int) likeCount);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/posts/{id}/bookmark")
    @ResponseBody
    public Map<String, Object> toggleBookmark(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        try {
            boolean isBookmarked = bookmarkService.toggleBookmark(loginUser.getId(), id);
            if (isBookmarked) {
                userActivityService.logBookmark(loginUser, id);
            }
            return Map.of("success", true, "isBookmarked", isBookmarked);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

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

    @PostMapping("/posts/{id}/ai-summary")
    @ResponseBody
    public Map<String, Object> generateAISummary(@PathVariable Long id) {
        try {
            Post post = postService.getPost(id);
            String textContent = post.getContent().replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            return Map.of("success", true, "summary", generateSimpleSummary(textContent, post.getTitle()));
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    private String generateSimpleSummary(String content, String title) {
        if (content.length() > 500) {
            return content.substring(0, 500) + "...";
        }
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

    @GetMapping("/my-applications")
    public String myApplications(HttpSession session, Model model,
                                 @RequestParam(required = false) String status) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        PostApplication.ApplicationStatus appStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                appStatus = PostApplication.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        List<PostApplication> applications = postApplicationService.getApplicationsByUser(loginUser.getId(), appStatus);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("applications", applications);
        model.addAttribute("status", status);
        return "my-applications";
    }

    @GetMapping("/posts/{id}/applications")
    public String postApplications(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        try {
            Post post = postService.getPost(id);
            if (!post.getUser().getId().equals(loginUser.getId())) {
                return "redirect:/posts/" + id + "?error=no_permission";
            }
            List<PostApplication> applications = postApplicationService.getApplicationsByPost(id, null);
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("post", post);
            model.addAttribute("applications", applications);
            return "post-applications";
        } catch (RuntimeException e) {
            return "redirect:/?error=" + e.getMessage();
        }
    }

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
}
