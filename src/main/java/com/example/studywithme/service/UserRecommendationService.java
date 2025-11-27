package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.UserActivity.ActionType;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserRecommendationService {

    private final UserActivityRepository userActivityRepository;
    private final PostRepository postRepository;

    public List<Post> recommendPosts(Integer userId, int limit) {
        // 비로그인 또는 활동 부족: 최신 글
        if (userId == null) {
            return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
        }

        var rows = userActivityRepository.findTopKeywords(
                userId,
                List.of(ActionType.SEARCH, ActionType.CLICK, ActionType.LIKE),
                PageRequest.of(0, 5)
        );

        if (rows.isEmpty()) {
            return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
        }

        List<String> keywords = new ArrayList<>();
        Map<String, Integer> keywordWeight = new HashMap<>();
        int base = 5;
        int rank = 0;
        for (Object[] r : rows) {
            String kw = (String) r[0];
            if (kw == null || kw.isBlank()) continue;
            keywords.add(kw);
            keywordWeight.put(kw, base - rank); // 5,4,3,...
            rank++;
        }

        Map<Long, Score> scoreMap = new HashMap<>();

        for (String kw : keywords) {
            var page = postRepository.searchByKeyword(kw, PageRequest.of(0, limit));
            int w = keywordWeight.getOrDefault(kw, 1);
            for (Post p : page) {
                Score s = scoreMap.computeIfAbsent(p.getId(), id -> new Score(p));
                s.score += w * 10;
                s.score += Optional.ofNullable(p.getLikeCount()).orElse(0);
                s.score += Optional.ofNullable(p.getViewCount()).orElse(0) / 10;
            }
        }

        return scoreMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.score, a.score))
                .limit(limit)
                .map(s -> s.post)
                .toList();
    }

    private static class Score {
        final Post post;
        int score = 0;
        private Score(Post post) { this.post = post; }
    }
}


