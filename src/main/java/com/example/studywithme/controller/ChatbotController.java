package com.example.studywithme.controller;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.service.ChatbotService;
import com.example.studywithme.service.PostService;
import com.example.studywithme.service.BookmarkService;
import com.example.studywithme.entity.Bookmark;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final PostService postService;
    private final BookmarkService bookmarkService;

    /**
     * 챗봇 메시지 전송
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam("message") String message,
            HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            Integer userId = loginUser != null ? loginUser.getId() : null;

            Map<String, Object> response = chatbotService.processMessage(message, userId);

            // 액션에 따라 추가 데이터 제공
            String action = (String) response.get("action");
            if (action != null) {
                handleAction(action, userId, response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("챗봇 메시지 처리 오류", e);
            return ResponseEntity.ok(Map.of(
                "message", "죄송합니다. 일시적인 오류가 발생했습니다.",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 액션 처리
     */
    private void handleAction(String action, Integer userId, Map<String, Object> response) {
        switch (action) {
            case "SHOW_MYPAGE":
                if (userId != null) {
                    var posts = postService.getPostsByUserId(userId, PageRequest.of(0, 10));
                    response.put("data", Map.of(
                        "type", "posts",
                        "posts", posts.getContent()
                    ));
                }
                break;

            case "SHOW_BOOKMARKS":
                if (userId != null) {
                    var bookmarks = bookmarkService.getBookmarks(userId, PageRequest.of(0, 10));
                    List<Post> bookmarkPosts = bookmarks.getContent().stream()
                        .map(Bookmark::getPost)
                        .collect(java.util.stream.Collectors.toList());
                    response.put("data", Map.of(
                        "type", "posts",
                        "posts", bookmarkPosts
                    ));
                }
                break;

            case "SEARCH_POSTS":
                String keyword = (String) response.get("actionData");
                if (keyword != null && !keyword.isEmpty()) {
                    List<Map<String, Object>> results = chatbotService.searchSimilarPosts(keyword, userId, 10);
                    response.put("data", Map.of(
                        "type", "posts",
                        "posts", results,
                        "keyword", keyword
                    ));
                }
                break;

            case "SHOW_RECOMMENDATIONS":
                response.put("data", Map.of(
                    "type", "redirect",
                    "url", "/recommend"
                ));
                break;
        }
    }

    /**
     * 대화 내역 조회
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory(HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            Integer userId = loginUser != null ? loginUser.getId() : null;

            var messages = chatbotService.getChatHistory(userId, 50);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("대화 내역 조회 오류", e);
            return ResponseEntity.ok(List.of());
        }
    }
}
