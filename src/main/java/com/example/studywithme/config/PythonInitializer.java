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
     * Python 스크립트 파일 존재 확인
     */
    private List<String> checkPythonScripts() {
        List<String> missingScripts = new ArrayList<>();
        List<String> scripts = List.of(
                recommendationScriptPath,
                tagScriptPath,
                summaryScriptPath
        );

        for (String scriptPath : scripts) {
            Path path = Paths.get(scriptPath);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                log.info("✓ Python 스크립트 확인: {}", scriptPath);
            } else {
                log.warn("✗ Python 스크립트 없음: {}", scriptPath);
                missingScripts.add(scriptPath);
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
     * Python 스크립트 문법 검사
     */
    private void validatePythonScripts() {
        List<String> scripts = List.of(
                recommendationScriptPath,
                tagScriptPath,
                summaryScriptPath
        );

        log.info("Python 스크립트 문법 검사 중...");
        for (String scriptPath : scripts) {
            Path path = Paths.get(scriptPath);
            if (!Files.exists(path)) {
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
                    } else {
                        log.warn("✗ 문법 오류 발견: {} - {}", scriptPath, output.toString().trim());
                    }
                }
            } catch (Exception e) {
                log.warn("✗ 문법 검사 실패: {} - {}", scriptPath, e.getMessage());
            }
        }
    }

    /**
     * Python 스크립트 테스트 실행 (간단한 테스트)
     */
    private void testPythonScripts() {
        log.info("Python 스크립트 테스트 실행 중...");

        // 1. 요약 스크립트 테스트
        testSummaryScript();

        // 2. 태그 추천 스크립트 테스트
        testTagScript();

        // 3. 추천 스크립트는 DB 연결이 필요하므로 스킵
        log.info("✓ Python 스크립트 테스트 완료 (추천 스크립트는 DB 연결 필요로 스킵)");
    }

    /**
     * 요약 스크립트 테스트
     */
    private void testSummaryScript() {
        Path path = Paths.get(summaryScriptPath);
        if (!Files.exists(path)) {
            return;
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
            } else {
                log.warn("✗ 요약 스크립트 테스트 실패: {} (exit code: {})", summaryScriptPath, exitCode);
            }
        } catch (Exception e) {
            log.warn("✗ 요약 스크립트 테스트 실패: {} - {}", summaryScriptPath, e.getMessage());
        }
    }

    /**
     * 태그 추천 스크립트 테스트
     */
    private void testTagScript() {
        Path path = Paths.get(tagScriptPath);
        if (!Files.exists(path)) {
            return;
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
            } else {
                log.warn("✗ 태그 추천 스크립트 테스트 실패: {} (exit code: {})", tagScriptPath, exitCode);
            }
        } catch (Exception e) {
            log.warn("✗ 태그 추천 스크립트 테스트 실패: {} - {}", tagScriptPath, e.getMessage());
        }
    }
}
