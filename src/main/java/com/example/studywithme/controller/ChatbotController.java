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
            @RequestParam(value = "confirmed", required = false) String confirmed,
            @RequestParam(value = "actionType", required = false) String actionType,
            HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            Integer userId = loginUser != null ? loginUser.getId() : null;

            Map<String, Object> response = chatbotService.processMessage(message, userId);

            // 액션에 따라 추가 데이터 제공
            String action = (String) response.get("action");
            if (action != null) {
                // 확인이 필요한 액션인 경우
                if (!"true".equals(confirmed)) {
                    // 확인 필요 플래그 추가
                    response.put("needsConfirmation", true);
                    response.put("confirmationMessage", getConfirmationMessage(action));
                    // 데이터는 아직 처리하지 않음 (확인 후 처리)
                } else {
                    // 확인 완료 후 액션 처리
                    handleAction(action, userId, response);
                }
                // JavaScript가 기대하는 형식으로 action 필드 명시적으로 설정
                response.put("action", action);
            }

            log.debug("챗봇 응답: action={}, hasData={}, needsConfirmation={}", 
                action, response.containsKey("data"), response.get("needsConfirmation"));
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
     * 액션별 확인 메시지 생성
     */
    private String getConfirmationMessage(String action) {
        switch (action) {
            case "SHOW_MYPAGE":
                return "마이페이지를 보여드릴까요?";
            case "SHOW_BOOKMARKS":
                return "북마크 목록을 보여드릴까요?";
            case "SEARCH_POSTS":
                return "검색 결과를 보여드릴까요?";
            case "SHOW_RECOMMENDATIONS":
                return "AI 추천 페이지로 이동할까요?";
            default:
                return "이 작업을 진행할까요?";
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
                        "type", "mypage",
                        "posts", posts.getContent(),
                        "redirectUrl", "/mypage"
                    ));
                } else {
                    // 비로그인 사용자는 로그인 페이지로 리다이렉트
                    response.put("data", Map.of(
                        "type", "redirect",
                        "url", "/auth?error=login_required"
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
                        "type", "bookmarks",
                        "posts", bookmarkPosts,
                        "redirectUrl", "/bookmarks"
                    ));
                } else {
                    // 비로그인 사용자는 로그인 페이지로 리다이렉트
                    response.put("data", Map.of(
                        "type", "redirect",
                        "url", "/auth?error=login_required"
                    ));
                }
                break;

            case "SEARCH_POSTS":
                String keyword = (String) response.get("actionData");
                if (keyword != null && !keyword.isEmpty()) {
                    List<Map<String, Object>> results = chatbotService.searchSimilarPosts(keyword, userId, 10);
                    // 검색 키워드를 URL 파라미터로 전달
                    String searchUrl = "/?search=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
                    response.put("data", Map.of(
                        "type", "posts",
                        "posts", results,
                        "keyword", keyword,
                        "redirectUrl", searchUrl
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

    /**
     * 대화 내역 초기화 (삭제)
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory(HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            Integer userId = loginUser != null ? loginUser.getId() : null;

            chatbotService.clearChatHistory(userId);
            log.info("사용자 ID {}의 대화 내역 초기화 완료", userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "대화 내역이 초기화되었습니다."
            ));
        } catch (Exception e) {
            log.error("대화 내역 초기화 오류", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "대화 내역 초기화 중 오류가 발생했습니다."
            ));
        }
    }
}
