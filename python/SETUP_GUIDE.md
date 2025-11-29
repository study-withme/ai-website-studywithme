# Python 스크립트 설정 가이드

## 개요

이 프로젝트는 Java Spring Boot 애플리케이션에서 Python 스크립트를 호출하여 AI 기반 기능을 제공합니다.

## 필수 요구사항

1. **Python 3.7 이상** 설치 필요
2. **MySQL 데이터베이스** 연결 필요 (ai_recommendation.py 사용 시)

## 설치 방법

### 1. Python 패키지 설치

```bash
cd python
pip install -r requirements.txt
```

또는

```bash
pip3 install -r python/requirements.txt
```

### 2. 환경 변수 설정

Spring Boot 애플리케이션이 자동으로 환경 변수를 설정하지만, 직접 실행할 경우:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password
export DB_NAME=studywithmever2
```

## 사용되는 Python 스크립트

### 1. ai_recommendation.py
- **용도**: 사용자 맞춤형 게시글 추천
- **호출**: `PythonRecommendationService`
- **입력**: user_id, limit
- **출력**: JSON 형식의 추천 게시글 목록

### 2. ai_summary.py
- **용도**: 게시글 본문 요약
- **호출**: `AISummaryService`
- **입력**: content, max_length
- **출력**: JSON 형식의 요약 결과

### 3. ai_tag_recommendation.py
- **용도**: 게시글 태그 및 카테고리 자동 추천
- **호출**: `AITagService`
- **입력**: title, content
- **출력**: JSON 형식의 태그 및 카테고리 추천

### 4. ai_tag_recommendation_deep.py
- **용도**: 딥러닝 기반 태그 추천 (선택적)
- **설명**: 딥러닝 라이브러리가 없으면 자동으로 규칙 기반으로 폴백

## Spring Boot 설정

`application.properties` 파일에 다음 설정이 포함되어 있습니다:

```properties
# Python 추천 시스템 설정
python.script.path=python/ai_recommendation.py
python.executable=python3
db.host=localhost
db.port=3306
db.user=root
db.password=your_password
db.name=studywithmever2

# Python AI 태그/요약 시스템 설정
python.script.tag.path=python/ai_tag_recommendation.py
python.script.summary.path=python/ai_summary.py
```

## 테스트

### 개별 스크립트 테스트

```bash
# 요약 테스트
python3 python/ai_summary.py "테스트 내용입니다." 50

# 태그 추천 테스트
python3 python/ai_tag_recommendation.py "제목" "본문 내용"

# 추천 시스템 테스트 (DB 연결 필요)
python3 python/ai_recommendation.py 1 10
```

## 문제 해결

### 1. Python 스크립트를 찾을 수 없음
- 프로젝트 루트 디렉토리에서 Spring Boot 애플리케이션을 실행해야 합니다
- `python/` 디렉토리가 프로젝트 루트에 있는지 확인하세요

### 2. 데이터베이스 연결 실패
- `application.properties`의 DB 설정을 확인하세요
- 환경 변수가 올바르게 설정되었는지 확인하세요

### 3. 모듈을 찾을 수 없음
- `requirements.txt`의 패키지가 모두 설치되었는지 확인하세요
- `pip install -r requirements.txt`를 다시 실행하세요

## 파일 구조

```
python/
├── ai_recommendation.py          # 추천 시스템 메인 스크립트
├── ai_summary.py                 # 요약 시스템
├── ai_tag_recommendation.py      # 태그 추천 (규칙 기반)
├── ai_tag_recommendation_deep.py # 태그 추천 (딥러닝 기반)
├── config.py                     # 설정 관리
├── logger.py                     # 로깅 설정
├── utils.py                      # 유틸리티 함수
├── exceptions.py                 # 예외 클래스
├── metrics.py                    # 성능 지표
├── requirements.txt              # Python 패키지 목록
└── README.md                     # 상세 문서
```

## 주의사항

1. **보안**: `application.properties`에 DB 비밀번호가 하드코딩되어 있습니다. 운영 환경에서는 환경 변수나 별도 설정 파일을 사용하세요.

2. **경로**: Java 서비스는 프로젝트 루트 디렉토리에서 실행되어야 합니다. 상대 경로 `python/`을 사용합니다.

3. **권한**: Python 스크립트 파일에 실행 권한이 있는지 확인하세요 (`chmod +x python/*.py`).
