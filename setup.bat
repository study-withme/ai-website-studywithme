@echo off
setlocal enabledelayedexpansion
REM Study With Me 프로젝트 자동 설정 스크립트 (Windows)

REM UTF-8 인코딩 설정 (오류 무시)
chcp 65001 >nul 2>&1

REM 현재 디렉토리 확인
if not exist "src\main\resources\application.properties.example" (
    echo.
    echo ❌ 오류: 프로젝트 루트 디렉토리에서 실행해주세요.
    echo    현재 디렉토리: %CD%
    echo    application.properties.example 파일을 찾을 수 없습니다.
    echo.
    pause
    exit /b 1
)

echo ==========================================
echo Study With Me 프로젝트 설정 시작
echo ==========================================
echo.

REM 1. application.properties 확인 및 생성
set PROPERTIES_FILE=src\main\resources\application.properties
set PROPERTIES_EXAMPLE=src\main\resources\application.properties.example

if not exist "%PROPERTIES_FILE%" (
    echo 📝 application.properties 파일 생성 중...
    if not exist "%PROPERTIES_EXAMPLE%" (
        echo ❌ 오류: %PROPERTIES_EXAMPLE% 파일을 찾을 수 없습니다.
        echo    현재 디렉토리: %CD%
        goto :end
    )
    copy "%PROPERTIES_EXAMPLE%" "%PROPERTIES_FILE%" >nul 2>&1
    if errorlevel 1 (
        echo ❌ 파일 복사 실패
        goto :end
    )
    echo.
    echo 🔑 데이터베이스 비밀번호 설정
    echo    Docker Compose를 사용하시면 환경 변수를 설정하세요.
    set /p db_password="DB 비밀번호를 입력하세요: "
    if "!db_password!"=="" (
        echo ❌ 오류: DB 비밀번호는 필수입니다.
        exit /b 1
    )
    
    REM PowerShell을 사용하여 파일 내용 변경
    powershell -NoProfile -Command "(Get-Content '%PROPERTIES_FILE%' -Encoding UTF8) -replace 'your_password_here', '!db_password!' | Set-Content '%PROPERTIES_FILE%' -Encoding UTF8" >nul 2>&1
    powershell -NoProfile -Command "(Get-Content '%PROPERTIES_FILE%' -Encoding UTF8) -replace '\$\{DB_PASSWORD:your_password_here\}', '!db_password!' | Set-Content '%PROPERTIES_FILE%' -Encoding UTF8" >nul 2>&1
    echo ✅ application.properties 파일이 생성되고 비밀번호가 설정되었습니다.
    echo    파일 위치: %CD%\%PROPERTIES_FILE%
) else (
    echo ✅ application.properties 파일이 이미 존재합니다.
    echo    파일 위치: %CD%\%PROPERTIES_FILE%
)

REM 2. Python 확인
echo.
echo 🐍 Python 환경 확인 중...
where python >nul 2>&1
if %errorlevel% equ 0 (
    python --version 2>nul
    if %errorlevel% equ 0 (
        echo ✅ Python 설치됨
        
        REM Python 패키지 설치
        echo 📦 Python 패키지 설치 중...
        pip install -q -r python\requirements.txt 2>nul
        if %errorlevel% equ 0 (
            echo ✅ Python 패키지 설치 완료
        ) else (
            echo ⚠️  Python 패키지 설치 중 오류가 발생했습니다.
        )
    )
) else (
    where python3 >nul 2>&1
    if %errorlevel% equ 0 (
        python3 --version 2>nul
        if %errorlevel% equ 0 (
            echo ✅ Python 설치됨
            
            REM Python 패키지 설치
            echo 📦 Python 패키지 설치 중...
            pip3 install -q -r python\requirements.txt 2>nul
            if %errorlevel% equ 0 (
                echo ✅ Python 패키지 설치 완료
            ) else (
                echo ⚠️  Python 패키지 설치 중 오류가 발생했습니다.
            )
        )
    ) else (
        echo ⚠️  Python이 설치되어 있지 않습니다.
        echo    설치 방법: https://www.python.org/downloads/
    )
)

REM 3. Docker Compose 확인 및 데이터베이스 설정
echo.
echo 🐳 Docker Compose 확인 중...
where docker >nul 2>&1
if %errorlevel% equ 0 (
    where docker-compose >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ Docker 설치됨
        echo 📊 Docker Compose로 데이터베이스 시작 중...
        docker-compose up -d db 2>nul
        if %errorlevel% equ 0 (
            echo ✅ 데이터베이스 컨테이너 시작 완료
            echo    잠시 후 데이터베이스가 준비됩니다 (약 10초)...
            timeout /t 10 /nobreak >nul
        ) else (
            echo ⚠️  Docker Compose 실행 실패
            echo    수동으로 실행: docker-compose up -d db
        )
    ) else (
        echo ⚠️  docker-compose가 설치되어 있지 않습니다.
        echo    설치 방법: https://docs.docker.com/compose/install/
    )
) else (
    echo ⚠️  Docker가 설치되어 있지 않습니다.
    echo.
    echo 📊 로컬 MySQL 사용 시:
    where mysql >nul 2>&1
    if %errorlevel% equ 0 (
        echo ✅ MySQL 설치됨
        echo    데이터베이스를 수동으로 생성해주세요:
        echo    mysql -u root -p
        echo    CREATE DATABASE studywithmever2 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
    ) else (
        echo ⚠️  MySQL이 설치되어 있지 않습니다.
        echo    Docker 설치: https://www.docker.com/get-started
        echo    또는 MySQL 설치: https://dev.mysql.com/downloads/installer/
    )
)

REM 4. Gradle Wrapper 권한 확인
echo.
echo 🔧 Gradle Wrapper 확인 중...
if exist "gradlew.bat" (
    echo ✅ gradlew.bat 파일 존재
) else (
    echo ⚠️  gradlew.bat 파일이 없습니다.
)

:end
echo.
echo ==========================================
echo ✅ 설정 완료!
echo ==========================================
echo.
echo 🚀 애플리케이션 실행:
echo    gradlew.bat bootRun
echo.
echo 📖 자세한 내용은 QUICK_START.md를 참고하세요.
echo.
pause
exit /b 0
