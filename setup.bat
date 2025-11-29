@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
REM Study With Me 프로젝트 자동 설정 스크립트 (Windows)

echo ==========================================
echo Study With Me 프로젝트 설정 시작
echo ==========================================

REM 1. application.properties 확인 및 생성
if not exist "src\main\resources\application.properties" (
    echo 📝 application.properties 파일 생성 중...
    copy "src\main\resources\application.properties.example" "src\main\resources\application.properties"
    echo.
    echo 🔑 데이터베이스 비밀번호 설정
    echo    Docker Compose를 사용하시면 'studypass'를 사용하세요.
    set /p db_password="DB 비밀번호를 입력하세요 (Enter = studypass): "
    if "!db_password!"=="" set db_password=studypass
    
    REM PowerShell을 사용하여 파일 내용 변경
    powershell -Command "(Get-Content 'src\main\resources\application.properties' -Encoding UTF8) -replace 'your_password_here', '!db_password!' | Set-Content 'src\main\resources\application.properties' -Encoding UTF8"
    powershell -Command "(Get-Content 'src\main\resources\application.properties' -Encoding UTF8) -replace '\$\{DB_PASSWORD:your_password_here\}', '!db_password!' | Set-Content 'src\main\resources\application.properties' -Encoding UTF8"
    echo ✅ application.properties 파일이 생성되고 비밀번호가 설정되었습니다.
) else (
    echo ✅ application.properties 파일이 이미 존재합니다.
)

REM 2. Python 확인
echo.
echo 🐍 Python 환경 확인 중...
where python >nul 2>&1
if %errorlevel% equ 0 (
    python --version
    echo ✅ Python 설치됨
    
    REM Python 패키지 설치
    echo 📦 Python 패키지 설치 중...
    pip install -q -r python\requirements.txt
    echo ✅ Python 패키지 설치 완료
) else (
    where python3 >nul 2>&1
    if %errorlevel% equ 0 (
        python3 --version
        echo ✅ Python 설치됨
        
        REM Python 패키지 설치
        echo 📦 Python 패키지 설치 중...
        pip3 install -q -r python\requirements.txt
        echo ✅ Python 패키지 설치 완료
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
        docker-compose up -d db
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

echo.
echo ==========================================
echo ✅ 설정 완료!
echo ==========================================
echo.
echo 🚀 애플리케이션 실행:
echo    gradlew.bat bootRun
echo.
echo 📖 자세한 내용은 QUICK_START.md를 참고하세요.
pause
