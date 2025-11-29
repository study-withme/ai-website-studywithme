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
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    scriptPath.toAbsolutePath().toString(),
                    content,
                    String.valueOf(maxLength)
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
            result.put("summary", rootNode.has("summary") ? rootNode.get("summary").asText() : "");
            result.put("original_length", rootNode.has("original_length") ? 
                      rootNode.get("original_length").asInt() : content.length());
            result.put("summary_length", rootNode.has("summary_length") ? 
                      rootNode.get("summary_length").asInt() : 0);

            log.info("AI 요약 완료: 원본 길이={}, 요약 길이={}", 
                    result.get("original_length"), result.get("summary_length"));
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

