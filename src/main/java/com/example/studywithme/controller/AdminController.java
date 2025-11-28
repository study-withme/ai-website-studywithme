package com.example.studywithme.controller;

import com.example.studywithme.entity.*;
import com.example.studywithme.service.AdminService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 관리자 권한 체크 헬퍼 메서드
    private boolean isAdmin(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        return loginUser != null && loginUser.isAdmin();
    }

    // 관리자 패널 메인 페이지
    @GetMapping
    public String adminPanel(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        AdminService.AdminStats stats = adminService.getStats();
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("stats", stats);
        return "admin";
    }

    // 차단된 게시글 목록
    @GetMapping("/blocked-posts")
    public String blockedPosts(HttpSession session, Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String status) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        BlockedPost.BlockStatus blockStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                blockStatus = BlockedPost.BlockStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 무시
            }
        }

        Page<BlockedPost> blockedPosts = adminService.getBlockedPosts(page, size, blockStatus);
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("blockedPosts", blockedPosts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blockedPosts.getTotalPages());
        model.addAttribute("status", status);
        
        return "admin-blocked-posts";
    }

    // 차단된 게시글 복구
    @PostMapping("/blocked-posts/{id}/restore")
    @ResponseBody
    public Map<String, Object> restorePost(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        User loginUser = (User) session.getAttribute("loginUser");
        try {
            adminService.restorePost(id, loginUser.getId());
            return Map.of("success", true, "message", "게시글이 복구되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 필터 단어 관리 페이지
    @GetMapping("/filter-words")
    public String filterWords(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        var filterWords = adminService.getAllFilterWords();
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("filterWords", filterWords);
        
        return "admin-filter-words";
    }

    // 필터 단어 추가
    @PostMapping("/filter-words")
    @ResponseBody
    public Map<String, Object> addFilterWord(@RequestParam("word") String word,
                                             @RequestParam(value = "wordType", defaultValue = "CUSTOM") String wordTypeStr,
                                             HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        User loginUser = (User) session.getAttribute("loginUser");
        try {
            FilterWord.WordType wordType = FilterWord.WordType.valueOf(wordTypeStr.toUpperCase());
            adminService.addFilterWord(word, wordType, loginUser.getId());
            return Map.of("success", true, "message", "필터 단어가 추가되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 필터 단어 삭제
    @PostMapping("/filter-words/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteFilterWord(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        try {
            adminService.deleteFilterWord(id);
            return Map.of("success", true, "message", "필터 단어가 삭제되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 키워드 관리 페이지
    @GetMapping("/filter-keywords")
    public String filterKeywords(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        var filterKeywords = adminService.getAllFilterKeywords();
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("filterKeywords", filterKeywords);
        
        return "admin-filter-keywords";
    }

    // 키워드 추가
    @PostMapping("/filter-keywords")
    @ResponseBody
    public Map<String, Object> addFilterKeyword(@RequestParam("keyword") String keyword,
                                                @RequestParam(value = "keywordType", defaultValue = "PARTIAL") String keywordTypeStr,
                                                @RequestParam(value = "description", required = false) String description,
                                                HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        User loginUser = (User) session.getAttribute("loginUser");
        try {
            FilterKeyword.KeywordType keywordType = FilterKeyword.KeywordType.valueOf(keywordTypeStr.toUpperCase());
            adminService.addFilterKeyword(keyword, keywordType, description, loginUser.getId());
            return Map.of("success", true, "message", "키워드가 추가되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 키워드 삭제
    @PostMapping("/filter-keywords/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteFilterKeyword(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        try {
            adminService.deleteFilterKeyword(id);
            return Map.of("success", true, "message", "키워드가 삭제되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 패턴 관리 페이지
    @GetMapping("/filter-patterns")
    public String filterPatterns(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        var filterPatterns = adminService.getAllFilterPatterns();
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("filterPatterns", filterPatterns);
        
        return "admin-filter-patterns";
    }

    // 패턴 추가
    @PostMapping("/filter-patterns")
    @ResponseBody
    public Map<String, Object> addFilterPattern(@RequestParam("patternName") String patternName,
                                                @RequestParam("patternRegex") String patternRegex,
                                                @RequestParam(value = "patternType", defaultValue = "BOTH") String patternTypeStr,
                                                @RequestParam(value = "description", required = false) String description,
                                                HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        User loginUser = (User) session.getAttribute("loginUser");
        try {
            FilterPattern.PatternType patternType = FilterPattern.PatternType.valueOf(patternTypeStr.toUpperCase());
            adminService.addFilterPattern(patternName, patternRegex, patternType, description, loginUser.getId());
            return Map.of("success", true, "message", "패턴이 추가되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 패턴 삭제
    @PostMapping("/filter-patterns/{id}/delete")
    @ResponseBody
    public Map<String, Object> deleteFilterPattern(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("success", false, "message", "관리자 권한이 필요합니다.");
        }

        try {
            adminService.deleteFilterPattern(id);
            return Map.of("success", true, "message", "패턴이 삭제되었습니다.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // AI 학습 데이터 페이지
    @GetMapping("/ai-learning")
    public String aiLearning(HttpSession session, Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(defaultValue = "0") int minFrequency) {
        if (!isAdmin(session)) {
            return "redirect:/?error=admin_required";
        }

        User loginUser = (User) session.getAttribute("loginUser");
        Page<AILearningData> learningData = adminService.getLearningData(page, size, minFrequency);
        
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("learningData", learningData);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", learningData.getTotalPages());
        model.addAttribute("minFrequency", minFrequency);
        
        return "admin-ai-learning";
    }
}

