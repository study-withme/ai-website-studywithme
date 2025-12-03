package com.example.studywithme.service;

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
public class AITagService {

    private final PythonScriptExecutor pythonScriptExecutor;

    @Value("${python.script.tag.path:python/ai_tag_recommendation.py}")
    private String pythonScriptPath;

    /**
     * 게시글 제목과 본문을 분석하여 태그와 카테고리를 추천합니다.
     * 
     * 주의: 큰 텍스트를 명령줄 인자로 전달하는 것은 보안 및 시스템 제한상 위험할 수 있습니다.
     * 향후 개선: 임시 파일 사용 또는 Python HTTP 서버로 전환 권장.
     */
    public Map<String, Object> recommendTags(String title, String content) {
        try {
            // HTML 태그 제거 (간단한 버전)
            String cleanTitle = title != null ? title.replaceAll("<[^>]*>", "").trim() : "";
            String cleanContent = content != null ? content.replaceAll("<[^>]*>", "").trim() : "";

            // 입력 검증
            if (cleanTitle.isEmpty() && cleanContent.isEmpty()) {
                log.warn("태그 추천할 내용이 비어있습니다.");
                return getFallbackRecommendation();
            }

            // 내용이 너무 길면 잘라서 전달
            // 참고: 향후 임시 파일 사용 또는 Python HTTP 서버로 전환 고려
            if (cleanContent.length() > 50000) {
                log.warn("본문이 너무 깁니다 ({}자). 앞부분만 사용합니다.", cleanContent.length());
                cleanContent = cleanContent.substring(0, 50000);
            }

            // 공통 Python 스크립트 실행 서비스 사용
            JsonNode rootNode = pythonScriptExecutor.executeScript(
                    pythonScriptPath,
                    cleanTitle,
                    cleanContent
            );

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

        } catch (TimeoutException e) {
            log.error("Python 태그 추천 스크립트 실행 타임아웃", e);
            return getFallbackRecommendation();
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

