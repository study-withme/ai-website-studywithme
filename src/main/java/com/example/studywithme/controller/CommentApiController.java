package com.example.studywithme.controller;

import com.example.studywithme.service.CommentService;
import com.example.studywithme.service.CommentService.CommentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommentApiController {

    private final CommentService commentService;
    private final com.example.studywithme.service.UserActivityService userActivityService;

    // 댓글 목록 조회
    @GetMapping("/api/posts/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId,
                                             @RequestParam(defaultValue = "latest") String sort,
                                             HttpSession session) {
        Integer userId = null;
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser instanceof com.example.studywithme.entity.User user) {
            userId = user.getId();
        }
        return commentService.getComments(postId, sort, userId);
    }

    // 댓글 작성
    @PostMapping("/api/posts/{postId}/comments")
    public Map<String, Object> addComment(@PathVariable Long postId,
                                          @RequestParam("content") String content,
                                          @RequestParam(value = "parentId", required = false) Long parentId,
                                          HttpSession session,
                                          HttpServletRequest request) {
        var loginUser = (com.example.studywithme.entity.User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        try {
            String ip = request.getRemoteAddr();
            String ua = request.getHeader("User-Agent");
            CommentResponse res = commentService.addComment(loginUser.getId(), postId, content, parentId, ip, ua);
            // 댓글 작성 시 활동 로그 기록
            userActivityService.logComment(loginUser, postId);
            return Map.of("success", true, "comment", res);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // 댓글 좋아요 토글
    @PostMapping("/api/comments/{id}/like")
    public Map<String, Object> toggleCommentLike(@PathVariable Long id, HttpSession session) {
        var loginUser = (com.example.studywithme.entity.User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return Map.of("success", false, "message", "로그인이 필요합니다.");
        }
        try {
            boolean liked = commentService.toggleLike(loginUser.getId(), id);
            return Map.of("success", true, "liked", liked);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}


