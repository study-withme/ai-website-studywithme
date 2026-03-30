package com.example.studywithme.board.controller;

import com.example.studywithme.board.entity.Certification;
import com.example.studywithme.board.entity.Post;
import com.example.studywithme.board.repository.CertificationRepository;
import com.example.studywithme.board.service.PostService;
import com.example.studywithme.user.entity.User;
import com.example.studywithme.user.service.UserActivityService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 메인 피드(게시판 홈) 및 공개 게시글 목록 API.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final PostService postService;
    private final UserActivityService userActivityService;
    private final CertificationRepository certificationRepository;

    @GetMapping("/")
    public String index(HttpSession session, Model model,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "9") int size,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "latest") String sort) {
        User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("loginUser", loginUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<Post> posts = Page.empty(pageable);

        try {
            if (category != null && !category.trim().isEmpty()) {
                posts = postService.getPostsByCategory(category, pageable, sort);
                model.addAttribute("category", category);
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                posts = postService.searchPosts(keyword, pageable);
                model.addAttribute("keyword", keyword);
                if (loginUser != null) {
                    userActivityService.logSearch(loginUser, keyword);
                }
            } else {
                posts = postService.getPosts(pageable, sort);
            }
        } catch (Exception e) {
            log.error("게시글 목록 조회 오류", e);
            posts = Page.empty(pageable);
        }

        if (posts == null) {
            log.warn("posts가 null입니다. 빈 페이지로 대체합니다.");
            posts = Page.empty(pageable);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts != null ? posts.getTotalPages() : 0);
        model.addAttribute("sort", sort);

        try {
            LocalDate today = LocalDate.now();
            List<Certification> certifications = certificationRepository.findUpcomingExams(today);
            model.addAttribute("certifications", certifications);
        } catch (Exception e) {
            model.addAttribute("certifications", Collections.emptyList());
        }

        return "index";
    }

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
}
