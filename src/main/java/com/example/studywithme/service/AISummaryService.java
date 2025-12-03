package com.example.studywithme.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final PythonScriptExecutor pythonScriptExecutor;

    @Value("${python.script.summary.path:python/ai_summary.py}")
    private String pythonScriptPath;

    /**
     * 게시글 본문을 요약합니다.
     * 
     * 주의: 큰 텍스트를 명령줄 인자로 전달하는 것은 보안 및 시스템 제한상 위험할 수 있습니다.
     * 향후 개선: 임시 파일 사용 또는 Python HTTP 서버로 전환 권장.
     */
    public Map<String, Object> summarizeContent(String content, int maxLength) {
        try {
            // 입력 검증
            if (content == null || content.trim().isEmpty()) {
                log.warn("요약할 내용이 비어있습니다.");
                return getFallbackSummary(content, maxLength);
            }

            // 내용이 너무 길면 잘라서 전달 (명령줄 인자 길이 제한 고려)
            // 참고: 향후 임시 파일 사용 또는 Python HTTP 서버로 전환 고려
            String contentToProcess = content;
            if (contentToProcess.length() > 50000) {
                log.warn("본문이 너무 깁니다 ({}자). 앞부분만 사용합니다.", contentToProcess.length());
                contentToProcess = contentToProcess.substring(0, 50000);
            }

            log.info("Python 요약 스크립트 실행: maxLength={}, contentLength={}", maxLength, contentToProcess.length());
            
            // 공통 Python 스크립트 실행 서비스 사용
            JsonNode rootNode = pythonScriptExecutor.executeScript(
                    pythonScriptPath,
                    contentToProcess,
                    String.valueOf(maxLength)
            );

            // 결과 반환
            Map<String, Object> result = new HashMap<>();
            String summary = rootNode.has("summary") ? rootNode.get("summary").asText() : "";
            int originalLength = rootNode.has("original_length") ? 
                      rootNode.get("original_length").asInt() : content.length();
            int summaryLength = rootNode.has("summary_length") ? 
                      rootNode.get("summary_length").asInt() : 0;
            
            result.put("summary", summary);
            result.put("original_length", originalLength);
            result.put("summary_length", summaryLength);

            log.info("AI 요약 완료: 원본 길이={}, 요약 길이={}, 요약 내용 길이={}", 
                    originalLength, summaryLength, summary.length());
            
            // 요약이 비어있으면 fallback 사용
            if (summary == null || summary.trim().isEmpty()) {
                log.warn("Python 스크립트가 빈 요약을 반환했습니다. Fallback 사용.");
                return getFallbackSummary(content, maxLength);
            }
            
            return result;

        } catch (TimeoutException e) {
            log.error("Python 요약 스크립트 실행 타임아웃", e);
            return getFallbackSummary(content, maxLength);
        } catch (Exception e) {
            log.error("AI 요약 중 오류 발생", e);
            return getFallbackSummary(content, maxLength);
        }
    }

    private Map<String, Object> getFallbackSummary(String content, int maxLength) {
        // 간단한 대체 요약 (앞부분만 자르기)
        String cleanContent = content.replaceAll("<[^>]*>", "").trim();
        String summary = cleanContent.length() > maxLength ? 
                cleanContent.substring(0, maxLength - 3) + "..." : cleanContent;
        
        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("original_length", cleanContent.length());
        result.put("summary_length", summary.length());
        return result;
    }
}

