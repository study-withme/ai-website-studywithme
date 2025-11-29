# 📚 Study With Me - AI 기반 맞춤형 스터디/모임 플랫폼

<div align="center">

**"Study With Me"**는 AI가 사용자의 활동 로그를 분석하여  
악성 게시글을 자동 차단하고, 개인화된 스터디/모임을 추천해주는 스마트 커뮤니티 플랫폼입니다.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.x-blue?logo=python)](https://www.python.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://www.docker.com/)

</div>

---

## 🎯 프로젝트 개요

**Study With Me**는 단순한 스터디 모임 중개 서비스를 넘어서, AI 기반의 지능형 추천 시스템과 콘텐츠 필터링을 제공하는 차세대 학습 커뮤니티 플랫폼입니다.

### 핵심 가치
- 🤖 **AI 기반 개인화 추천**: 사용자 활동 로그 분석을 통한 맞춤형 콘텐츠 추천
- 🛡️ **지능형 콘텐츠 필터링**: 악성 게시글 및 댓글 자동 감지 및 차단
- 📊 **활동 기반 분석**: 사용자 행동 패턴 분석을 통한 선호도 예측
- 👥 **스터디 그룹 관리**: 효율적인 스터디 그룹 생성 및 관리 시스템

---

## ✨ 주요 기능

### 1. AI 기반 개인화 추천 시스템
- **활동 로그 분석**: 사용자의 검색, 클릭, 좋아요, 북마크 등 모든 활동을 추적
- **맞춤형 게시글 추천**: 콘텐츠 기반 필터링 + 협업 필터링 하이브리드 방식
- **스터디 그룹 추천**: 관심사 및 학습 목표 기반 스터디 그룹 매칭
- **파트너 추천**: 게시글별 적합한 스터디 파트너 자동 추천

### 2. 지능형 콘텐츠 필터링
- **실시간 악성 콘텐츠 감지**: 룰 기반 + 패턴 매칭을 통한 욕설/악성 구문 탐지
- **자동 차단 및 관리자 알림**: 문제 있는 게시글/댓글 자동 차단 및 관리자 알림
- **학습 데이터 기반 개선**: AI 학습 데이터 축적을 통한 필터링 정확도 향상

### 3. 게시글 및 댓글 시스템
- **게시글 CRUD**: 제목, 본문, 카테고리, 태그를 포함한 게시글 관리
- **AI 자동 태그 분류**: 게시글 내용 분석을 통한 자동 태그 및 카테고리 분류
- **AI 본문 요약**: 긴 게시글의 핵심 내용 자동 요약
- **댓글 시스템**: 대댓글 지원, 좋아요 기능 포함

### 4. 스터디 그룹 관리
- **그룹 생성 및 관리**: 목표, 일정, 최대 인원 등 설정 가능
- **멤버 관리**: 그룹장 권한 관리, 멤버 초대/수락 시스템
- **게시글 연동**: 스터디 그룹에 대한 게시글 작성 및 지원 관리

### 5. 사용자 활동 추적
- **활동 로그 수집**: 모든 사용자 행동을 데이터베이스에 기록
- **통계 및 분석**: 개인별 활동 통계 및 선호도 분석
- **맞춤형 대시보드**: 마이페이지에서 개인 활동 요약 확인

### 6. 관리자 기능
- **차단된 콘텐츠 관리**: 악성 게시글/댓글 검토 및 처리
- **필터 키워드 관리**: 필터링 키워드, 패턴, 단어 관리
- **AI 학습 데이터 관리**: AI 학습에 사용된 데이터 확인 및 관리

---

## 🛠 기술 스택

### 백엔드
- **Java 21** (LTS)
- **Spring Boot 3.3.4**
  - Spring Web (RESTful API)
  - Spring Data JPA (데이터베이스)
  - Spring Security (인증/인가)
  - Thymeleaf (템플릿 엔진)
- **MySQL/MariaDB 8.0+**
- **BCrypt** (비밀번호 암호화)

### AI 시스템 (Python)
- **Python 3.x**
- **MySQL Connector** (데이터베이스 연결)
- **하이브리드 추천 알고리즘**
  - 콘텐츠 기반 필터링
  - 협업 필터링
  - 시간 가중치 적용
  - 인기도 기반 추천

### 인프라 & 도구
- **Docker & Docker Compose** (개발 환경 자동화)
- **Gradle Wrapper** (빌드 자동화)
- **GitHub** (버전 관리)

### 프론트엔드
- **HTML5 / CSS3 / JavaScript**
- **Thymeleaf 템플릿**
- **반응형 디자인**

---

## 📁 프로젝트 구조

```
studywithmever2/
├── src/main/java/com/example/studywithme/
│   ├── config/              # Spring 설정 (Security 등)
│   ├── controller/          # MVC 컨트롤러
│   │   ├── MainController.java
│   │   ├── AdminController.java
│   │   ├── CommentApiController.java
│   │   └── NotificationApiController.java
│   ├── entity/              # JPA 엔티티
│   │   ├── User.java
│   │   ├── Post.java
│   │   ├── Comment.java
│   │   ├── StudyGroup.java
│   │   ├── UserActivity.java
│   │   ├── BlockedPost.java
│   │   ├── BlockedComment.java
│   │   └── ...
│   ├── repository/          # JPA Repository
│   │   ├── UserRepository.java
│   │   ├── PostRepository.java
│   │   ├── StudyGroupRepository.java
│   │   └── ...
│   ├── service/             # 비즈니스 로직
│   │   ├── UserService.java
│   │   ├── PostService.java
│   │   ├── CommentService.java
│   │   ├── StudyGroupService.java
│   │   ├── UserRecommendationService.java
│   │   ├── PythonRecommendationService.java
│   │   ├── AITagService.java
│   │   ├── AISummaryService.java
│   │   ├── ContentFilterService.java
│   │   └── ...
│   └── StudyWithMeApplication.java
│
├── src/main/resources/
│   ├── application.properties   # Spring Boot 설정
│   ├── static/                  # 정적 파일 (CSS, JS)
│   └── templates/               # Thymeleaf 템플릿
│
├── python/                      # AI 추천 시스템 (Python)
│   ├── ai_recommendation.py     # 사용자 맞춤형 게시글 추천
│   ├── ai_tag_recommendation.py # 자동 태그 분류 (기본)
│   ├── ai_tag_recommendation_deep.py  # 자동 태그 분류 (딥러닝)
│   ├── ai_summary.py            # 게시글 본문 자동 요약
│   ├── config.py                # 설정 파일
│   ├── utils.py                 # 유틸리티 함수
│   ├── logger.py                # 로깅 설정
│   ├── requirements.txt         # Python 의존성
│   └── README.md                # Python 시스템 문서
│
├── docker-compose.yml           # Docker Compose 설정
├── studywithmever2.sql          # 데이터베이스 초기화 스크립트
├── build.gradle                 # Gradle 빌드 설정
└── README.md                    # 본 문서
```

---

## 🚀 빠른 시작

### 1. 필수 요구사항

- **Java 21+** (Gradle이 자동 설치 가능)
- **Docker & Docker Compose** (MySQL 자동 설치용)
- **Python 3.x** (AI 시스템 실행용, 선택사항)
- **Git**

### 2. 프로젝트 클론

```bash
git clone https://github.com/study-withme/ai-website-studywithme.git
cd ai-website-studywithme
```

### 3. 데이터베이스 설정 (Docker)

프로젝트 루트에서 MySQL 컨테이너를 시작합니다:

```bash
docker compose up -d db
```

이 명령은 다음을 자동으로 수행합니다:
- MariaDB 10.4 컨테이너 생성
- `studywithmever2.sql` 스크립트 자동 실행
- 데이터베이스 및 테이블 생성

데이터베이스 상태 확인:
```bash
docker logs -f studywithme-db
```

### 4. 환경 변수 설정 (선택)

`.env` 파일을 프로젝트 루트에 생성하여 데이터베이스 연결 정보를 설정할 수 있습니다:

```bash
# .env 파일 예시
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=studywithmever2
MYSQL_USER=study_user
MYSQL_PASSWORD=studypass
MYSQL_PORT=3306

SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/studywithmever2?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=study_user
SPRING_DATASOURCE_PASSWORD=studypass
```

> ⚠️ **보안 주의**: 실제 비밀번호는 절대 GitHub에 커밋하지 마세요. `.env` 파일은 `.gitignore`에 포함되어 있습니다.

또는 쉘에서 직접 환경 변수를 설정할 수 있습니다:

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/studywithmever2?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="password"
```

### 5. 애플리케이션 실행

Gradle Wrapper를 사용하여 애플리케이션을 실행합니다:

```bash
./gradlew bootRun
```

애플리케이션이 시작되면 브라우저에서 접속하세요:
- **메인 페이지**: http://localhost:8080/
- **로그인/회원가입**: http://localhost:8080/auth
- **관리자 페이지**: http://localhost:8080/admin (관리자 권한 필요)

---

## 🤖 Python AI 시스템

이 프로젝트는 Spring Boot 백엔드와 별도로 Python 기반 AI 추천 시스템을 포함하고 있습니다.

### Python 시스템 구성

1. **`ai_recommendation.py`**: 사용자 맞춤형 게시글 추천
   - 활동 로그 분석
   - 하이브리드 추천 알고리즘
   - 개인화된 점수 계산

2. **`ai_tag_recommendation.py`**: 게시글 자동 태그 분류
   - 키워드 기반 태그 추출
   - 카테고리 자동 분류

3. **`ai_tag_recommendation_deep.py`**: 딥러닝 기반 태그 분류
   - 고급 패턴 인식
   - 더 정확한 태그 매칭

4. **`ai_summary.py`**: 게시글 본문 자동 요약
   - 핵심 내용 추출
   - 간결한 요약 생성

### Python 시스템 사용법

자세한 내용은 [`python/README.md`](python/README.md)를 참고하세요.

**기본 설치:**
```bash
cd python
pip install -r requirements.txt
```

**추천 실행 예시:**
```bash
python ai_recommendation.py 1 10  # 사용자 ID 1에게 10개 게시글 추천
```

### Spring Boot와 Python 통합

Spring Boot에서 Python 스크립트를 호출하려면 `PythonRecommendationService`를 사용하세요. 이 서비스는 프로세스 실행을 통해 Python 스크립트를 호출하고 결과를 JSON으로 파싱합니다.

---

## 📚 주요 API 엔드포인트

### 게시글 API
- `GET /posts` - 게시글 목록 조회
- `GET /posts/{id}` - 게시글 상세 조회
- `POST /posts` - 게시글 작성
- `PUT /posts/{id}` - 게시글 수정
- `DELETE /posts/{id}` - 게시글 삭제

### 추천 API
- `GET /api/recommendations/posts` - 맞춤형 게시글 추천
- `GET /api/recommendations/study-groups` - 스터디 그룹 추천
- `GET /api/recommendations/partners/{postId}` - 스터디 파트너 추천

### 댓글 API
- `GET /api/comments/post/{postId}` - 게시글 댓글 조회
- `POST /api/comments` - 댓글 작성
- `DELETE /api/comments/{id}` - 댓글 삭제

### 스터디 그룹 API
- `GET /study-groups` - 그룹 목록
- `POST /study-groups` - 그룹 생성
- `POST /study-groups/{id}/join` - 그룹 가입
- `POST /study-groups/{id}/applications/{applicationId}/approve` - 가입 승인

---

## 🔒 보안 기능

- **BCrypt 비밀번호 암호화**: 사용자 비밀번호는 해시로 저장
- **Spring Security**: 역할 기반 접근 제어 (ROLE_USER, ROLE_ADMIN)
- **세션 기반 인증**: 안전한 사용자 인증 및 권한 관리
- **콘텐츠 필터링**: 악성 콘텐츠 자동 감지 및 차단
- **입력 검증**: XSS 및 SQL Injection 방지

---

## 📊 데이터베이스 스키마

주요 테이블:
- **users**: 사용자 정보
- **posts**: 게시글
- **comments**: 댓글
- **study_groups**: 스터디 그룹
- **user_activities**: 사용자 활동 로그
- **blocked_posts**: 차단된 게시글
- **blocked_comments**: 차단된 댓글
- **ai_learning_data**: AI 학습 데이터

전체 스키마는 `studywithmever2.sql` 파일을 참고하세요.

---

## 🛠 개발 환경 설정

### Docker 없이 실행하기

Docker를 사용하지 않고 로컬에 MySQL을 설치한 경우:

1. MySQL 설치 및 데이터베이스 생성
2. `studywithmever2.sql` 스크립트 실행
3. `application.properties` 또는 환경 변수로 연결 정보 설정

### DB 스키마 변경하기

스키마를 변경한 후 Docker 컨테이너를 재생성해야 합니다:

```bash
docker compose down -v   # 볼륨 포함 완전 삭제
docker compose up -d db  # 다시 초기화 (SQL 스크립트 재실행)
```

---

## 📖 추가 문서

- **[Python AI 시스템 가이드](python/README.md)**: Python 추천 시스템 상세 설명
- **[알고리즘 분석](python/ALGORITHM_ANALYSIS.md)**: AI 추천 알고리즘 상세 분석
- **[프로젝트 로드맵](PROJECT_ROADMAP.md)**: 개발 계획 및 향후 계획

---

## ❓ 자주 묻는 질문 (FAQ)

**Q. Docker 없이 실행할 수 있나요?**  
A. 네, 가능합니다. 로컬에 MySQL을 설치하고 `studywithmever2.sql`을 수동으로 실행한 뒤, 데이터베이스 연결 정보만 설정하면 됩니다.

**Q. Python AI 시스템이 필수인가요?**  
A. 아니요. Python 시스템은 선택사항입니다. Spring Boot 애플리케이션은 독립적으로 실행 가능하며, Python 시스템은 고급 추천 기능을 위해 사용됩니다.

**Q. 관리자 계정은 어떻게 생성하나요?**  
A. 데이터베이스에서 `users` 테이블의 해당 사용자 레코드에 `role` 컬럼을 `ADMIN`으로 설정하면 됩니다.

**Q. 비밀번호를 GitHub에 올려도 되나요?**  
A. 절대 안 됩니다. 실제 비밀번호는 `.env` 파일을 사용하여 로컬에서만 관리하세요. `.env` 파일은 `.gitignore`에 포함되어 있습니다.

---

## 🤝 기여하기

버그 리포트, 기능 제안, Pull Request를 환영합니다!

1. 이 저장소를 Fork하세요
2. 새로운 브랜치를 생성하세요 (`git checkout -b feature/amazing-feature`)
3. 변경사항을 커밋하세요 (`git commit -m 'Add some amazing feature'`)
4. 브랜치에 Push하세요 (`git push origin feature/amazing-feature`)
5. Pull Request를 열어주세요

### 기여 전 확인사항

- 빌드 확인: `./gradlew build`
- 실행 확인: `./gradlew bootRun`
- 데이터베이스 초기화 확인 (Docker 또는 로컬 MySQL)

---

## 📝 라이선스

이 프로젝트는 Capstone Design 프로젝트입니다.

---

## 👥 팀

**Study With Me 개발팀**

- 프로젝트 리드
- 백엔드 개발 (Spring Boot)
- AI 시스템 개발 (Python)
- 프론트엔드 개발

---

## 🔗 관련 링크

- **GitHub 저장소**: https://github.com/study-withme/ai-website-studywithme
- **문서**: 이 README 및 `python/README.md` 참고

---

<div align="center">

**Made with ❤️ by Study With Me Team**

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!

</div>
