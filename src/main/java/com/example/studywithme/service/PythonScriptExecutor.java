package com.example.studywithme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Python 스크립트 실행을 위한 공통 서비스
 * 타임아웃, 에러 처리, 리소스 정리 등을 통합 관리
 */
@Service
@Slf4j
public class PythonScriptExecutor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${python.script.timeout:30}")
    private int timeoutSeconds;

    /**
     * Python 스크립트를 실행하고 JSON 결과를 반환합니다.
     *
     * @param scriptPath Python 스크립트 경로
     * @param args 스크립트 인자
     * @return JSON 결과 노드
     * @throws IOException 파일 읽기 오류
     * @throws InterruptedException 프로세스 중단
     * @throws TimeoutException 타임아웃
     */
    public JsonNode executeScript(String scriptPath, String... args)
            throws IOException, InterruptedException, TimeoutException {
        return executeScript(scriptPath, null, args);
    }

    /**
     * Python 스크립트를 실행하고 JSON 결과를 반환합니다. (환경 변수 포함)
     *
     * @param scriptPath Python 스크립트 경로
     * @param envVars 환경 변수 맵 (null 가능)
     * @param args 스크립트 인자
     * @return JSON 결과 노드
     * @throws IOException 파일 읽기 오류
     * @throws InterruptedException 프로세스 중단
     * @throws TimeoutException 타임아웃
     */
    public JsonNode executeScript(String scriptPath, java.util.Map<String, String> envVars, String... args)
            throws IOException, InterruptedException, TimeoutException {
        
        Path script = Paths.get(scriptPath);
        if (!script.toFile().exists()) {
            throw new IOException("Python 스크립트를 찾을 수 없습니다: " + scriptPath);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                script.toAbsolutePath().toString()
        );
        processBuilder.command().addAll(Arrays.asList(args));
        processBuilder.redirectErrorStream(false); // 에러 스트림 분리

        // 환경 변수 설정
        if (envVars != null) {
            java.util.Map<String, String> env = processBuilder.environment();
            env.putAll(envVars);
        }

        log.debug("Python 스크립트 실행: {} {}", scriptPath, Arrays.toString(args));

        Process process = null;
        try {
            process = processBuilder.start();

            // 비동기로 출력 읽기
            CompletableFuture<String> outputFuture = readStream(process.getInputStream());
            CompletableFuture<String> errorFuture = readStream(process.getErrorStream());

            // 타임아웃 적용
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                log.error("Python 스크립트 실행 타임아웃 ({}초): {}", timeoutSeconds, scriptPath);
                process.destroyForcibly();
                throw new TimeoutException("Python 스크립트 실행 타임아웃 (" + timeoutSeconds + "초)");
            }

            int exitCode = process.exitValue();
            String output = outputFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            String error = errorFuture.get(timeoutSeconds, TimeUnit.SECONDS);

            if (exitCode != 0) {
                log.error("Python 스크립트 실행 실패 (exit code: {}): {}", exitCode, error);
                throw new RuntimeException("Python 스크립트 실행 실패: " + error);
            }

            // 에러 스트림에 내용이 있으면 경고
            if (error != null && !error.trim().isEmpty()) {
                log.warn("Python 스크립트 경고: {}", error);
            }

            // JSON 파싱
            String jsonOutput = output.trim();
            if (jsonOutput.isEmpty()) {
                throw new RuntimeException("Python 스크립트가 빈 결과를 반환했습니다.");
            }

            JsonNode rootNode = objectMapper.readTree(jsonOutput);

            if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                log.error("Python 스크립트 오류: {}", errorMsg);
                throw new RuntimeException("Python 스크립트 오류: " + errorMsg);
            }

            return rootNode;

        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Python 스크립트 실행 중 예외 발생", e);
            throw new RuntimeException("Python 스크립트 실행 중 오류: " + e.getMessage(), e);
        } finally {
            if (process != null && process.isAlive()) {
                log.warn("프로세스가 아직 실행 중입니다. 강제 종료합니다.");
                process.destroyForcibly();
            }
        }
    }

    /**
     * 입력 스트림을 비동기로 읽습니다.
     */
    private CompletableFuture<String> readStream(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                log.error("스트림 읽기 오류", e);
            }
            return sb.toString();
        });
    }
}
