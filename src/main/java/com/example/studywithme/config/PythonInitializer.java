package com.example.studywithme.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot 애플리케이션 시작 시 Python 환경을 확인하고 초기화합니다.
 */
@Component
@Slf4j
@Order(1) // 다른 초기화 작업보다 먼저 실행
public class PythonInitializer implements ApplicationRunner {

    @Value("${python.executable:python3}")
    private String pythonExecutable;

    @Value("${python.script.path:python/ai_recommendation.py}")
    private String recommendationScriptPath;

    @Value("${python.script.tag.path:python/ai_tag_recommendation.py}")
    private String tagScriptPath;

    @Value("${python.script.summary.path:python/ai_summary.py}")
    private String summaryScriptPath;

    @Value("${python.auto-init.enabled:true}")
    private boolean autoInitEnabled;

    @Value("${python.auto-init.test-scripts:true}")
    private boolean testScriptsEnabled;

    @Value("${python.auto-init.check-packages:true}")
    private boolean checkPackagesEnabled;

    @Value("${python.auto-init.validate-syntax:true}")
    private boolean validateSyntaxEnabled;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 자동 초기화가 비활성화되어 있으면 스킵
        if (!autoInitEnabled) {
            log.info("Python 자동 초기화가 비활성화되어 있습니다. (python.auto-init.enabled=false)");
            return;
        }

        log.info("=".repeat(60));
        log.info("Python 환경 초기화 시작");
        log.info("=".repeat(60));

        // 1. Python 실행 파일 확인
        if (!checkPythonExecutable()) {
            log.warn("Python 실행 파일을 찾을 수 없습니다. Python 기능이 제한될 수 있습니다.");
            return;
        }

        // 2. Python 버전 확인
        checkPythonVersion();

        // 3. Python 스크립트 파일 존재 확인
        List<String> missingScripts = checkPythonScripts();
        if (!missingScripts.isEmpty()) {
            log.warn("다음 Python 스크립트를 찾을 수 없습니다: {}", missingScripts);
        }

        // 4. Python 패키지 확인 (설정에 따라)
        if (checkPackagesEnabled) {
            checkPythonPackages();
        } else {
            log.info("Python 패키지 확인이 비활성화되어 있습니다.");
        }

        // 5. Python 스크립트 문법 검사 (설정에 따라)
        if (validateSyntaxEnabled) {
            validatePythonScripts();
        } else {
            log.info("Python 스크립트 문법 검사가 비활성화되어 있습니다.");
        }

        // 6. Python 스크립트 테스트 실행 (설정에 따라)
        if (testScriptsEnabled) {
            testPythonScripts();
        } else {
            log.info("Python 스크립트 테스트 실행이 비활성화되어 있습니다.");
        }

        log.info("=".repeat(60));
        log.info("Python 환경 초기화 완료");
        log.info("=".repeat(60));
    }

    /**
     * Python 실행 파일 존재 확인
     */
    private boolean checkPythonExecutable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                if (version != null) {
                    log.info("✓ Python 실행 파일 확인: {} ({})", pythonExecutable, version);
                    return process.waitFor() == 0;
                }
            }
        } catch (Exception e) {
            log.error("Python 실행 파일 확인 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Python 버전 확인
     */
    private void checkPythonVersion() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, "--version");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                if (version != null) {
                    log.info("✓ Python 버전: {}", version);
                    
                    // Python 3.7 이상인지 확인
                    if (version.contains("Python 3")) {
                        String[] parts = version.split("\\s+");
                        if (parts.length >= 2) {
                            String versionNumber = parts[1];
                            String[] versionParts = versionNumber.split("\\.");
                            if (versionParts.length >= 2) {
                                int major = Integer.parseInt(versionParts[0]);
                                int minor = Integer.parseInt(versionParts[1]);
                                if (major >= 3 && minor >= 7) {
                                    log.info("✓ Python 버전 요구사항 충족 (3.7 이상)");
                                } else {
                                    log.warn("⚠ Python 버전이 3.7 미만입니다. 일부 기능이 동작하지 않을 수 있습니다.");
                                }
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.error("Python 버전 확인 실패: {}", e.getMessage());
        }
    }

    /**
     * Python 스크립트 파일 존재 확인 (모든 Python 파일 확인)
     */
    private List<String> checkPythonScripts() {
        List<String> missingScripts = new ArrayList<>();
        
        // 실행 가능한 메인 스크립트들
        List<String> mainScripts = List.of(
                recommendationScriptPath,
                tagScriptPath,
                summaryScriptPath,
                "python/ai_tag_recommendation_deep.py"  // 딥러닝 태그 추천 스크립트 추가
        );

        // 유틸리티/모듈 파일들
        List<String> moduleFiles = List.of(
                "python/config.py",
                "python/exceptions.py",
                "python/logger.py",
                "python/metrics.py",
                "python/utils.py"
        );

        log.info("Python 메인 스크립트 확인 중...");
        for (String scriptPath : mainScripts) {
            Path path = Paths.get(scriptPath);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                log.info("✓ Python 메인 스크립트 확인: {}", scriptPath);
            } else {
                log.warn("✗ Python 메인 스크립트 없음: {}", scriptPath);
                missingScripts.add(scriptPath);
            }
        }

        log.info("Python 모듈 파일 확인 중...");
        for (String modulePath : moduleFiles) {
            Path path = Paths.get(modulePath);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                log.info("✓ Python 모듈 파일 확인: {}", modulePath);
            } else {
                log.warn("✗ Python 모듈 파일 없음: {}", modulePath);
                missingScripts.add(modulePath);
            }
        }

        return missingScripts;
    }

    /**
     * 필수 Python 패키지 확인
     */
    private void checkPythonPackages() {
        List<String> requiredPackages = List.of(
                "mysql.connector",
                "json",
                "sys",
                "re",
                "collections"
        );

        log.info("Python 패키지 확인 중...");
        for (String packageName : requiredPackages) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        pythonExecutable,
                        "-c",
                        String.format("import %s; print('OK')", packageName)
                );
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String output = reader.readLine();
                    if (output != null && output.contains("OK")) {
                        log.info("✓ 패키지 확인: {}", packageName);
                    } else {
                        log.warn("✗ 패키지 없음: {} (pip install 필요)", packageName);
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                log.warn("✗ 패키지 확인 실패: {} - {}", packageName, e.getMessage());
            }
        }
    }

    /**
     * Python 스크립트 문법 검사 (모든 Python 파일 검사)
     */
    private void validatePythonScripts() {
        // 모든 Python 파일 목록
        List<String> allPythonFiles = List.of(
                recommendationScriptPath,
                tagScriptPath,
                summaryScriptPath,
                "python/ai_tag_recommendation_deep.py",
                "python/config.py",
                "python/exceptions.py",
                "python/logger.py",
                "python/metrics.py",
                "python/utils.py"
        );

        log.info("Python 스크립트 문법 검사 중... (총 {}개 파일)", allPythonFiles.size());
        int successCount = 0;
        int failCount = 0;

        for (String scriptPath : allPythonFiles) {
            Path path = Paths.get(scriptPath);
            if (!Files.exists(path)) {
                log.warn("✗ 파일 없음 (문법 검사 스킵): {}", scriptPath);
                failCount++;
                continue;
            }

            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        pythonExecutable,
                        "-m",
                        "py_compile",
                        path.toAbsolutePath().toString()
                );
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }

                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        log.info("✓ 문법 검사 통과: {}", scriptPath);
                        successCount++;
                    } else {
                        log.warn("✗ 문법 오류 발견: {} - {}", scriptPath, output.toString().trim());
                        failCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("✗ 문법 검사 실패: {} - {}", scriptPath, e.getMessage());
                failCount++;
            }
        }

        log.info("문법 검사 완료: 성공 {}개, 실패 {}개", successCount, failCount);
    }

    /**
     * Python 스크립트 테스트 실행 (간단한 테스트)
     */
    private void testPythonScripts() {
        log.info("Python 스크립트 테스트 실행 중...");

        int testCount = 0;
        int successCount = 0;

        // 1. 요약 스크립트 테스트
        testCount++;
        if (testSummaryScript()) {
            successCount++;
        }

        // 2. 태그 추천 스크립트 테스트
        testCount++;
        if (testTagScript()) {
            successCount++;
        }

        // 3. 딥러닝 태그 추천 스크립트 테스트 (선택적)
        Path deepTagPath = Paths.get("python/ai_tag_recommendation_deep.py");
        if (Files.exists(deepTagPath)) {
            testCount++;
            if (testDeepTagScript()) {
                successCount++;
            }
        }

        // 4. 추천 스크립트는 DB 연결이 필요하므로 스킵
        log.info("✓ Python 스크립트 테스트 완료: 성공 {}/{} (추천 스크립트는 DB 연결 필요로 스킵)", 
                 successCount, testCount);
    }

    /**
     * 요약 스크립트 테스트
     * @return 테스트 성공 여부
     */
    private boolean testSummaryScript() {
        Path path = Paths.get(summaryScriptPath);
        if (!Files.exists(path)) {
            log.warn("✗ 요약 스크립트 파일 없음: {}", summaryScriptPath);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    path.toAbsolutePath().toString(),
                    "테스트 내용입니다.",
                    "50"
            );
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
            if (exitCode == 0 && output.toString().contains("summary")) {
                log.info("✓ 요약 스크립트 테스트 성공: {}", summaryScriptPath);
                return true;
            } else {
                log.warn("✗ 요약 스크립트 테스트 실패: {} (exit code: {})", summaryScriptPath, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.warn("✗ 요약 스크립트 테스트 실패: {} - {}", summaryScriptPath, e.getMessage());
            return false;
        }
    }

    /**
     * 태그 추천 스크립트 테스트
     * @return 테스트 성공 여부
     */
    private boolean testTagScript() {
        Path path = Paths.get(tagScriptPath);
        if (!Files.exists(path)) {
            log.warn("✗ 태그 추천 스크립트 파일 없음: {}", tagScriptPath);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    path.toAbsolutePath().toString(),
                    "테스트 제목",
                    "테스트 본문 내용입니다."
            );
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
            if (exitCode == 0 && output.toString().contains("category")) {
                log.info("✓ 태그 추천 스크립트 테스트 성공: {}", tagScriptPath);
                return true;
            } else {
                log.warn("✗ 태그 추천 스크립트 테스트 실패: {} (exit code: {})", tagScriptPath, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.warn("✗ 태그 추천 스크립트 테스트 실패: {} - {}", tagScriptPath, e.getMessage());
            return false;
        }
    }

    /**
     * 딥러닝 태그 추천 스크립트 테스트 (선택적)
     * @return 테스트 성공 여부
     */
    private boolean testDeepTagScript() {
        Path path = Paths.get("python/ai_tag_recommendation_deep.py");
        if (!Files.exists(path)) {
            log.info("ℹ 딥러닝 태그 추천 스크립트 없음 (선택적 기능): {}", path);
            return false;
        }

        try {
            // 딥러닝 라이브러리 의존성이 있을 수 있으므로 간단한 import 테스트만 수행
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable,
                    "-c",
                    "import sys; sys.path.insert(0, 'python'); import ai_tag_recommendation_deep; print('OK')"
            );
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
            if (exitCode == 0 && output.toString().contains("OK")) {
                log.info("✓ 딥러닝 태그 추천 스크립트 모듈 로드 성공: python/ai_tag_recommendation_deep.py");
                return true;
            } else {
                log.warn("✗ 딥러닝 태그 추천 스크립트 모듈 로드 실패 (선택적 기능, torch/transformers 필요할 수 있음): {} (exit code: {})", 
                         path, exitCode);
                return false;
            }
        } catch (Exception e) {
            log.warn("✗ 딥러닝 태그 추천 스크립트 테스트 실패 (선택적 기능): {} - {}", path, e.getMessage());
            return false;
        }
    }
}
