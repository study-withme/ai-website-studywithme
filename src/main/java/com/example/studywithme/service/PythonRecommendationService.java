package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonRecommendationService {

    private final PostRepository postRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.script.path:python/ai_recommendation.py}")
    private String pythonScriptPath;

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${db.host:localhost}")
    private String dbHost;

    @Value("${db.port:3306}")
    private String dbPort;

    @Value("${db.user:root}")
    private String dbUser;

    @Value("${db.password:password}")
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
            // Python 스크립트 경로 확인
            Path scriptPath = Paths.get(pythonScriptPath);
            if (!scriptPath.toFile().exists()) {
                log.warn("Python 스크립트를 찾을 수 없습니다: {}", scriptPath);
                return getFallbackRecommendations(limit);
            }

            // 환경 변수 설정
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath.toAbsolutePath().toString(),
                    String.valueOf(userId),
                    String.valueOf(limit)
            );

            // 데이터베이스 연결 정보를 환경 변수로 전달
            Map<String, String> env = processBuilder.environment();
            env.put("DB_HOST", dbHost);
            env.put("DB_PORT", dbPort);
            env.put("DB_USER", dbUser);
            env.put("DB_PASSWORD", dbPassword);
            env.put("DB_NAME", dbName);

            processBuilder.redirectErrorStream(true);

            // Python 스크립트 실행
            Process process = processBuilder.start();

            // 결과 읽기
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("Python 스크립트 실행 실패 (exit code: {}): {}", exitCode, output.toString());
                return getFallbackRecommendations(limit);
            }

            // JSON 파싱
            String jsonOutput = output.toString().trim();
            JsonNode rootNode = objectMapper.readTree(jsonOutput);

            // 에러 체크
            if (rootNode.has("error")) {
                log.error("Python 스크립트 오류: {}", rootNode.get("error").asText());
                return getFallbackRecommendations(limit);
            }

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
                postRepository.findById(postId).ifPresent(posts::add);
            }

            log.info("사용자 {}에게 {}개의 게시글을 추천했습니다.", userId, posts.size());
            return posts;

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
            Path scriptPath = Paths.get(pythonScriptPath);
            if (!scriptPath.toFile().exists()) {
                return Map.of("error", "Python 스크립트를 찾을 수 없습니다.");
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath.toAbsolutePath().toString(),
                    String.valueOf(userId),
                    "1"  // limit=1 (선호도만 필요)
            );

            Map<String, String> env = processBuilder.environment();
            env.put("DB_HOST", dbHost);
            env.put("DB_PORT", dbPort);
            env.put("DB_USER", dbUser);
            env.put("DB_PASSWORD", dbPassword);
            env.put("DB_NAME", dbName);

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Map.of("error", "스크립트 실행 실패");
            }

            JsonNode rootNode = objectMapper.readTree(output.toString().trim());
            if (rootNode.has("preferences")) {
                return objectMapper.convertValue(rootNode.get("preferences"), Map.class);
            }

            return Map.of("error", "선호도 정보를 찾을 수 없습니다.");

        } catch (Exception e) {
            log.error("사용자 선호도 조회 중 오류 발생", e);
            return Map.of("error", e.getMessage());
        }
    }
}

