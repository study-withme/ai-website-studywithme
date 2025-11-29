package com.example.studywithme.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AITagService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.script.tag.path:python/ai_tag_recommendation.py}")
    private String pythonScriptPath;

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    /**
     * 게시글 제목과 본문을 분석하여 태그와 카테고리를 추천합니다.
     */
    public Map<String, Object> recommendTags(String title, String content) {
        try {
            Path scriptPath = Paths.get(pythonScriptPath);
            if (!scriptPath.toFile().exists()) {
                log.warn("Python 태그 추천 스크립트를 찾을 수 없습니다: {}", scriptPath);
                return getFallbackRecommendation();
            }

            // HTML 태그 제거 (간단한 버전)
            String cleanTitle = title != null ? title.replaceAll("<[^>]*>", "").trim() : "";
            String cleanContent = content != null ? content.replaceAll("<[^>]*>", "").trim() : "";

            // Python 스크립트 실행
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath.toAbsolutePath().toString(),
                    cleanTitle,
                    cleanContent
            );

            processBuilder.redirectErrorStream(true);
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
                log.error("Python 태그 추천 스크립트 실행 실패 (exit code: {}): {}", exitCode, output.toString());
                return getFallbackRecommendation();
            }

            // JSON 파싱
            String jsonOutput = output.toString().trim();
            JsonNode rootNode = objectMapper.readTree(jsonOutput);

            // 에러 체크
            if (rootNode.has("error")) {
                log.error("Python 태그 추천 오류: {}", rootNode.get("error").asText());
                return getFallbackRecommendation();
            }

            // 결과 반환
            Map<String, Object> result = new HashMap<>();
            result.put("category", rootNode.has("category") ? rootNode.get("category").asText() : "기타");
            result.put("category_confidence", rootNode.has("category_confidence") ? 
                      rootNode.get("category_confidence").asDouble() : 0.0);
            
            List<String> tags = new ArrayList<>();
            if (rootNode.has("tags") && rootNode.get("tags").isArray()) {
                for (JsonNode tagNode : rootNode.get("tags")) {
                    tags.add(tagNode.asText());
                }
            }
            result.put("tags", tags);

            log.info("AI 태그 추천 완료: 카테고리={}, 태그 수={}", result.get("category"), tags.size());
            return result;

        } catch (Exception e) {
            log.error("AI 태그 추천 중 오류 발생", e);
            return getFallbackRecommendation();
        }
    }

    private Map<String, Object> getFallbackRecommendation() {
        Map<String, Object> result = new HashMap<>();
        result.put("category", "기타");
        result.put("category_confidence", 0.0);
        result.put("tags", new ArrayList<String>());
        return result;
    }
}

