# AI 추천 시스템

사용자 활동 로그를 분석하여 개인화된 게시글 추천을 제공하는 Python 스크립트입니다.

## 설치

```bash
pip install -r requirements.txt
```

## 사용법

### 기본 사용

```bash
python ai_recommendation.py <user_id> [limit]
```

- `user_id`: 추천을 받을 사용자 ID
- `limit`: 추천할 게시글 수 (기본값: 20)

### 예시

```bash
# 사용자 ID 1에게 10개의 게시글 추천
python ai_recommendation.py 1 10
```

### 환경 변수 설정

데이터베이스 연결 정보를 환경 변수로 설정할 수 있습니다:

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=password
export DB_NAME=studywithmever2

python ai_recommendation.py 1
```

## 출력 형식

JSON 형식으로 결과를 반환합니다:

```json
{
  "user_id": 1,
  "preferences": {
    "categories": {
      "프로그래밍": 0.45,
      "스터디": 0.30,
      "모임": 0.25
    },
    "tags": {
      "Java": 0.20,
      "Spring": 0.15,
      "Python": 0.10
    },
    "action_counts": {
      "CLICK": 10,
      "LIKE": 5,
      "BOOKMARK": 3
    },
    "total_activities": 18
  },
  "recommended_posts": [
    {
      "id": 123,
      "title": "게시글 제목",
      "category": "프로그래밍",
      "tags": "Java,Spring",
      "view_count": 100,
      "like_count": 10,
      "recommendation_score": 85.5,
      "created_at": "2025-01-11T10:00:00"
    }
  ],
  "total_recommended": 10
}
```

## 추천 알고리즘

1. **활동 로그 분석**: 최근 30일간의 사용자 활동을 분석합니다.
2. **가중치 계산**: 
   - AI_CLICK: 5.0 (가장 높음)
   - BOOKMARK: 4.0
   - COMMENT: 3.5
   - LIKE: 3.0
   - RECOMMEND: 2.5
   - CLICK: 2.0
   - SEARCH: 1.0
3. **점수 계산**:
   - 카테고리 매칭: 가중치 × 100
   - 태그 매칭: 가중치 × 50
   - 인기도: 좋아요 × 2 + 조회수 × 0.1
   - 최신성: 최근 7일 내 게시글 +10점

## Spring Boot 연동

Spring Boot에서 이 스크립트를 호출하려면 `PythonRecommendationService`를 사용하세요.

