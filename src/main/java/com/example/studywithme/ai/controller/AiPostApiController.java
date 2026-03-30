package com.example.studywithme.ai.controller;

import com.example.studywithme.ai.service.AISummaryService;
import com.example.studywithme.ai.service.AITagService;
import com.example.studywithme.board.entity.Post;
import com.example.studywithme.board.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 게시글 연동 AI REST API (태그 추천, 본문 요약).
 */
@RestController
@RequiredArgsConstructor
public class AiPostApiController {

    private final AITagService aiTagService;
    private final AISummaryService aiSummaryService;
    private final PostService postService;

    @PostMapping("/api/posts/ai-tags")
    public Map<String, Object> recommendAITags(@RequestParam("title") String title,
                                                @RequestParam("content") String content) {
        try {
            return aiTagService.recommendTags(title, content);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/api/posts/{id}/ai-summary")
    public Map<String, Object> getAISummary(@PathVariable Long id,
                                            @RequestParam(defaultValue = "200") int maxLength) {
        try {
            Post post = postService.getPost(id);
            String content = post.getContent();
            content = content.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
            return aiSummaryService.summarizeContent(content, maxLength);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
