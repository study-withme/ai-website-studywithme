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
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AISummaryService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.script.summary.path:python/ai_summary.py}")
    private String pythonScriptPath;

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    /**
     * 게시글 본문을 요약합니다.
     */
    public Map<String, Object> summarizeContent(String content, int maxLength) {
        try {
            Path scriptPath = Paths.get(pythonScriptPath);
            if (!scriptPath.toFile().exists()) {
                log.warn("Python 요약 스크립트를 찾을 수 없습니다: {}", scriptPath);
                return getFallbackSummary(content, maxLength);
            }

            // Python 스크립트 실행
            // 내용이 너무 길면 잘라서 전달 (명령줄 인자 길이 제한 고려)
            String contentToProcess = content;
            if (contentToProcess.length() > 50000) {
                log.warn("본문이 너무 깁니다 ({}자). 앞부분만 사용합니다.", contentToProcess.length());
                contentToProcess = contentToProcess.substring(0, 50000);
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath.toAbsolutePath().toString(),
                    contentToProcess,
                    String.valueOf(maxLength)
            );

            processBuilder.redirectErrorStream(true);
            log.info("Python 요약 스크립트 실행: maxLength={}, contentLength={}", maxLength, contentToProcess.length());
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
                log.error("Python 요약 스크립트 실행 실패 (exit code: {}): {}", exitCode, output.toString());
                return getFallbackSummary(content, maxLength);
            }

            // JSON 파싱
            String jsonOutput = output.toString().trim();
            JsonNode rootNode = objectMapper.readTree(jsonOutput);

            // 에러 체크
            if (rootNode.has("error")) {
                log.error("Python 요약 오류: {}", rootNode.get("error").asText());
                return getFallbackSummary(content, maxLength);
            }

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

