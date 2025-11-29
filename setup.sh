#!/bin/bash

# Study With Me 프로젝트 자동 설정 스크립트 (Mac/Linux)

echo "=========================================="
echo "Study With Me 프로젝트 설정 시작"
echo "=========================================="

# 1. application.properties 확인 및 생성
if [ ! -f "src/main/resources/application.properties" ]; then
    echo "📝 application.properties 파일 생성 중..."
    cp src/main/resources/application.properties.example src/main/resources/application.properties
    
    echo ""
    echo "🔑 데이터베이스 비밀번호 설정"
    echo "   Docker Compose를 사용하시면 'studypass'를 사용하세요."
    read -p "DB 비밀번호를 입력하세요 (Enter = studypass): " db_password
    db_password=${db_password:-studypass}
    
    # macOS와 Linux에서 sed 명령어가 다를 수 있음
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/your_password_here/$db_password/g" src/main/resources/application.properties
        sed -i '' "s/\${DB_PASSWORD:your_password_here}/$db_password/g" src/main/resources/application.properties
    else
        # Linux
        sed -i "s/your_password_here/$db_password/g" src/main/resources/application.properties
        sed -i "s/\${DB_PASSWORD:your_password_here}/$db_password/g" src/main/resources/application.properties
    fi
    echo "✅ application.properties 파일이 생성되고 비밀번호가 설정되었습니다."
else
    echo "✅ application.properties 파일이 이미 존재합니다."
fi

# 2. Python 확인
echo ""
echo "🐍 Python 환경 확인 중..."
if command -v python3 &> /dev/null; then
    python_version=$(python3 --version)
    echo "✅ Python 설치됨: $python_version"
    
    # Python 패키지 설치
    echo "📦 Python 패키지 설치 중..."
    pip3 install -q -r python/requirements.txt
    echo "✅ Python 패키지 설치 완료"
else
    echo "⚠️  Python 3가 설치되어 있지 않습니다."
    echo "   설치 방법: https://www.python.org/downloads/"
    echo "   또는: brew install python3 (macOS)"
fi

# 3. Docker Compose 확인 및 데이터베이스 설정
echo ""
echo "🐳 Docker Compose 확인 중..."
if command -v docker &> /dev/null && command -v docker-compose &> /dev/null; then
    echo "✅ Docker 설치됨"
    echo "📊 Docker Compose로 데이터베이스 시작 중..."
    docker-compose up -d db
    if [ $? -eq 0 ]; then
        echo "✅ 데이터베이스 컨테이너 시작 완료"
        echo "   잠시 후 데이터베이스가 준비됩니다 (약 10초)..."
        sleep 10
    else
        echo "⚠️  Docker Compose 실행 실패"
        echo "   수동으로 실행: docker-compose up -d db"
    fi
else
    echo "⚠️  Docker가 설치되어 있지 않습니다."
    echo ""
    echo "📊 로컬 MySQL 사용 시:"
    if command -v mysql &> /dev/null; then
        echo "✅ MySQL 설치됨"
        read -p "데이터베이스를 자동으로 생성하시겠습니까? (y/n): " create_db
        if [ "$create_db" = "y" ] || [ "$create_db" = "Y" ]; then
            read -p "MySQL root 비밀번호를 입력하세요: " mysql_password
            mysql -u root -p"$mysql_password" -e "CREATE DATABASE IF NOT EXISTS studywithmever2 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>/dev/null
            if [ $? -eq 0 ]; then
                echo "✅ 데이터베이스 생성 완료"
            else
                echo "⚠️  데이터베이스 생성 실패. 수동으로 생성해주세요:"
                echo "   mysql -u root -p"
                echo "   CREATE DATABASE studywithmever2 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
            fi
        fi
    else
        echo "⚠️  MySQL이 설치되어 있지 않습니다."
        echo "   Docker 설치: https://www.docker.com/get-started"
        echo "   또는 MySQL 설치: brew install mysql (macOS)"
    fi
fi

# 4. Gradle Wrapper 권한 확인
echo ""
echo "🔧 Gradle Wrapper 권한 확인 중..."
chmod +x gradlew
echo "✅ 권한 설정 완료"

echo ""
echo "=========================================="
echo "✅ 설정 완료!"
echo "=========================================="
echo ""
echo "🚀 애플리케이션 실행:"
echo "   ./gradlew bootRun"
echo ""
echo "📖 자세한 내용은 QUICK_START.md를 참고하세요."
