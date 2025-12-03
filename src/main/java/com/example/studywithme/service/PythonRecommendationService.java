package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonRecommendationService {

    private final PostRepository postRepository;
    private final PythonScriptExecutor pythonScriptExecutor;

    @Value("${python.script.path:python/ai_recommendation.py}")
    private String pythonScriptPath;

    @Value("${db.host:localhost}")
    private String dbHost;

    @Value("${db.port:3306}")
    private String dbPort;

    @Value("${db.user:root}")
    private String dbUser;

    @Value("${db.password:Xmflslxl2@}")
    private String dbPassword;

    @Value("${db.name:studywithmever2}")
    private String dbName;

    /**
     * Python 스크립트를 실행하여 사용자에게 추천할 게시글을 가져옵니다.
     * 
     * @param userId 사용자 ID
     * @param limit 추천할 게시글 수
     * @return 추천 게시글 목록
     */
    public List<Post> getRecommendedPosts(Integer userId, int limit) {
        try {
            // 입력 검증
            if (userId == null || userId <= 0) {
                log.warn("잘못된 사용자 ID: {}", userId);
                return getFallbackRecommendations(limit);
            }

            // 데이터베이스 연결 정보를 환경 변수로 전달
            Map<String, String> envVars = new HashMap<>();
            envVars.put("DB_HOST", dbHost);
            envVars.put("DB_PORT", dbPort);
            envVars.put("DB_USER", dbUser);
            envVars.put("DB_PASSWORD", dbPassword);
            envVars.put("DB_NAME", dbName);

            // 공통 Python 스크립트 실행 서비스 사용
            JsonNode rootNode = pythonScriptExecutor.executeScript(
                    pythonScriptPath,
                    envVars,
                    String.valueOf(userId),
                    String.valueOf(limit)
            );

            // 추천 게시글 ID 목록 추출
            JsonNode recommendedPosts = rootNode.get("recommended_posts");
            if (recommendedPosts == null || !recommendedPosts.isArray()) {
                log.warn("추천 게시글이 없습니다.");
                return getFallbackRecommendations(limit);
            }

            List<Long> postIds = new ArrayList<>();
            for (JsonNode postNode : recommendedPosts) {
                if (postNode.has("id")) {
                    postIds.add(postNode.get("id").asLong());
                }
            }

            if (postIds.isEmpty()) {
                return getFallbackRecommendations(limit);
            }

            // 게시글 조회 (순서 유지)
            List<Post> posts = new ArrayList<>();
            for (Long postId : postIds) {
                if (postId != null) {
                    postRepository.findById(postId).ifPresent(posts::add);
                }
            }

            // 최종 필터링: 활동 로그에 개발 관련 키워드가 있으면 영어/독서 강제 제외
            if (!posts.isEmpty()) {
                // 활동 로그 확인 (간단히 키워드로 판단)
                // 실제로는 Python에서 이미 필터링했지만, 안전장치로 한 번 더
                List<Post> filtered = new ArrayList<>();
                
                for (Post post : posts) {
                    // 영어/독서 카테고리는 제외하지 않음 (Python에서 이미 필터링됨)
                    // 여기서는 로그만 출력
                    filtered.add(post);
                }
                
                log.info("사용자 {}에게 {}개의 게시글을 추천했습니다.", userId, filtered.size());
                
                // 카테고리 분포 로그
                Map<String, Long> categoryCount = filtered.stream()
                    .filter(p -> p.getCategory() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                        Post::getCategory,
                        java.util.stream.Collectors.counting()
                    ));
                log.info("추천된 게시글 카테고리 분포: {}", categoryCount);
                
                return filtered;
            }

            log.info("사용자 {}에게 {}개의 게시글을 추천했습니다.", userId, posts.size());
            return posts;

        } catch (TimeoutException e) {
            log.error("Python 추천 스크립트 실행 타임아웃", e);
            return getFallbackRecommendations(limit);
        } catch (Exception e) {
            log.error("Python 추천 시스템 실행 중 오류 발생", e);
            return getFallbackRecommendations(limit);
        }
    }

    /**
     * Python 스크립트 실행 실패 시 대체 추천 (최신 게시글)
     */
    private List<Post> getFallbackRecommendations(int limit) {
        log.info("대체 추천 시스템 사용 (최신 게시글)");
        return postRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)
        ).getContent();
    }

    /**
     * 사용자 선호도 정보 조회 (선택적)
     */
    public Map<String, Object> getUserPreferences(Integer userId) {
        try {
            if (userId == null || userId <= 0) {
                return Map.of("error", "잘못된 사용자 ID");
            }

            // 데이터베이스 연결 정보를 환경 변수로 전달
            Map<String, String> envVars = new HashMap<>();
            envVars.put("DB_HOST", dbHost);
            envVars.put("DB_PORT", dbPort);
            envVars.put("DB_USER", dbUser);
            envVars.put("DB_PASSWORD", dbPassword);
            envVars.put("DB_NAME", dbName);

            // 공통 Python 스크립트 실행 서비스 사용
            JsonNode rootNode = pythonScriptExecutor.executeScript(
                    pythonScriptPath,
                    envVars,
                    String.valueOf(userId),
                    "1"  // limit=1 (선호도만 필요)
            );

            if (rootNode.has("preferences")) {
                // JsonNode를 Map으로 변환
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {};
                Map<String, Object> preferencesMap = mapper.convertValue(rootNode.get("preferences"), typeRef);
                return preferencesMap;
            }

            return Map.of("error", "선호도 정보를 찾을 수 없습니다.");

        } catch (TimeoutException e) {
            log.error("사용자 선호도 조회 타임아웃", e);
            return Map.of("error", "타임아웃: " + e.getMessage());
        } catch (Exception e) {
            log.error("사용자 선호도 조회 중 오류 발생", e);
            return Map.of("error", e.getMessage());
        }
    }
}

