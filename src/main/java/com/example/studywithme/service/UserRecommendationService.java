package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.UserActivity.ActionType;
import com.example.studywithme.entity.UserPreference;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserActivityRepository;
import com.example.studywithme.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRecommendationService {

    private final UserActivityRepository userActivityRepository;
    private final PostRepository postRepository;
    private final PythonRecommendationService pythonRecommendationService;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * AI 기반 추천 게시글 조회
     * - 1순위: Python 추천 엔진 (사용자 활동 로그 기반, 동적 추천)
     * - 2순위: 사용자 선호 카테고리 기반 간단 추천 (고정 프로필 기반)
     * - 3순위: 키워드 기반 폴백
     */
    public List<Post> recommendPosts(Integer userId, int limit) {
        // 비로그인: 전역 인기 글
        if (userId == null) {
            return postRepository.findAllByOrderByPopularityDesc(PageRequest.of(0, limit)).getContent();
        }
        
        // 1. Python 기반 추천 시도
        //    - Python 쪽에서 활동 로그 / 고정 프로필 / 콘텐츠 분석까지 모두 처리하므로
        //      여기서는 추가로 카테고리를 강하게 필터링하지 않고 그대로 신뢰한다.
        //    - 이렇게 해서 **추천 로직의 대부분이 Python 알고리즘(80% 이상)** 이 되도록 조정.
        try {
            System.out.println("🚀 Python 추천 엔진 실행 중...");
            List<Post> pythonRecommended = pythonRecommendationService.getRecommendedPosts(userId, limit);
            if (!pythonRecommended.isEmpty()) {
                System.out.println("✅ Python 추천 엔진 성공: " + pythonRecommended.size() + "개 게시글 추천");
                Map<String, Long> categoryCount = pythonRecommended.stream()
                    .filter(p -> p.getCategory() != null)
                    .collect(Collectors.groupingBy(
                        Post::getCategory,
                        Collectors.counting()
                    ));
                System.out.println("📊 추천된 게시글 카테고리 분포: " + categoryCount);
                return pythonRecommended;
            }
            
            System.out.println("⚠️ Python 추천 엔진: 추천 결과가 비어있습니다.");
        } catch (Exception e) {
            System.err.println("❌ Python 추천 엔진 실패: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Python 결과가 비었을 때만 고정 프로필/키워드 기반 폴백 사용
        //    (기존 로직을 유지하지만, 우선순위는 항상 Python 추천이 가장 높음)
        System.out.println("📌 고정 프로필 기반 추천 또는 키워드 기반 폴백 사용");
        
        // 사용자 선호 카테고리 기반 간단 추천
        List<Post> byPreference = recommendByUserPreference(userId, limit);
        if (!byPreference.isEmpty()) {
            return byPreference;
        }

        // 마지막 폴백: 기존 키워드 기반 추천
        System.out.println("📌 키워드 기반 폴백 추천 사용");
        return recommendPostsByKeyword(userId, limit);
    }

    /**
     * 사용자 선호 카테고리(UserPreference) 기반 간단 추천
     * - 각 카테고리별 인기순으로 가져와서, 선호 점수 높은 카테고리부터 채움
     */
    private List<Post> recommendByUserPreference(Integer userId, int limit) {
        List<UserPreference> preferences = userPreferenceRepository.findByUser_Id(userId);
        if (preferences.isEmpty()) {
            return List.of();
        }

        // 선호 점수 내림차순 정렬
        preferences.sort((a, b) -> Float.compare(
                Optional.ofNullable(b.getPreferenceScore()).orElse(0f),
                Optional.ofNullable(a.getPreferenceScore()).orElse(0f)
        ));

        // 카테고리 매핑 (프로그래밍/코딩/언어 → 실제 DB 카테고리명)
        Map<String, String> categoryMapping = Map.of(
                "프로그래밍", "개발",
                "코딩", "개발",
                "언어", "영어"
        );

        List<Post> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (UserPreference pref : preferences) {
            if (result.size() >= limit) break;

            String raw = pref.getCategoryName();
            if (raw == null || raw.isBlank()) continue;
            String category = categoryMapping.getOrDefault(raw, raw);

            var page = postRepository.findByCategoryOrderByPopularityDesc(
                    category,
                    PageRequest.of(0, limit)
            );

            for (Post p : page) {
                if (result.size() >= limit) break;
                if (seenIds.add(p.getId())) {
                    result.add(p);
                }
            }
        }

        return result;
    }

    /**
     * 키워드 기반 추천 (기존 방식)
     */
    private List<Post> recommendPostsByKeyword(Integer userId, int limit) {
        // 비로그인 또는 활동 부족: 최신 글
        if (userId == null) {
            return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
        }

        // 1. 사용자 선호 카테고리 조회 (AI 추천 프로필에서 선택한 카테고리)
        List<UserPreference> preferences = userPreferenceRepository.findByUser_Id(userId);
        // 카테고리명이 "프로그래밍" 등으로 저장되어 있을 수 있으므로 Python 쪽과 동일한 매핑 적용
        Map<String, String> categoryMapping = Map.of(
                "프로그래밍", "개발",
                "코딩", "개발",
                "언어", "영어"
        );

        Set<String> preferredCategories = new java.util.HashSet<>();
        for (UserPreference pref : preferences) {
            String raw = pref.getCategoryName();
            if (raw == null || raw.isBlank()) continue;
            String mapped = categoryMapping.getOrDefault(raw, raw);
            preferredCategories.add(mapped);
        }

        var rows = userActivityRepository.findTopKeywords(
                userId,
                List.of(ActionType.SEARCH, ActionType.CLICK, ActionType.LIKE),
                PageRequest.of(0, 5)
        );

        if (rows.isEmpty()) {
            // 활동 키워드가 없으면: 선호 카테고리 안에서 인기순/최신순 게시글 반환
            if (!preferredCategories.isEmpty()) {
                return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                        .filter(p -> p.getCategory() != null && preferredCategories.contains(p.getCategory()))
                        .toList();
            }
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
                // 2. 폴백 추천에서도 "선호 카테고리"에 속한 게시글만 점수 계산
                if (!preferredCategories.isEmpty()) {
                    String category = p.getCategory();
                    if (category == null || !preferredCategories.contains(category)) {
                        continue;
                    }
                }
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


