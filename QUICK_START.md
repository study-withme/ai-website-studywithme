# 빠른 시작 가이드

## 🚀 자동 설정 스크립트 사용 (권장)

### Mac/Linux
```bash
chmod +x setup.sh
./setup.sh
```

### Windows
```cmd
setup.bat
```

스크립트가 자동으로:
- ✅ `application.properties` 파일 생성 및 DB 비밀번호 설정
- ✅ Python 패키지 설치
- ✅ Docker Compose로 데이터베이스 자동 시작 (Docker 설치 시)
- ✅ 필요한 설정 확인

**Docker가 설치되어 있으면 데이터베이스까지 자동으로 설정됩니다!**

---

## 🚀 수동 설정 (3단계)

### 1단계: 애플리케이션 설정

```bash
# 설정 파일 복사
cp src/main/resources/application.properties.example src/main/resources/application.properties

# application.properties 파일을 열어서 DB 비밀번호 수정
# db.password=your_password_here → 실제 비밀번호로 변경
```

### 2단계: 데이터베이스 설정

```bash
# MySQL 실행
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE studywithmever2 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 3단계: 실행

**Windows:**
```cmd
gradlew.bat bootRun
```

**Mac/Linux:**
```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080` 접속!

---

## 📦 Python AI 기능 사용하기 (선택사항)

Python AI 기능(추천, 요약, 태그)을 사용하려면:

```bash
# Python 패키지 설치
pip install -r python/requirements.txt
```

설치하지 않아도 웹사이트는 정상 동작합니다. (AI 기능만 제한됨)

---

## ⚙️ 환경 변수 사용 (권장)

`.env` 파일을 만들어서 사용하세요:

```bash
DB_PASSWORD=your_password
GEMINI_API_KEY=your_api_key
```

---

## ❓ 문제 해결

- **포트 8080이 이미 사용 중**: `application.properties`에서 `server.port=8081`로 변경
- **데이터베이스 연결 실패**: MySQL이 실행 중인지 확인
- **Python 오류**: Python 3.7 이상이 설치되어 있는지 확인
