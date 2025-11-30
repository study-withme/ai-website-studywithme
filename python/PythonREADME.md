# Python AI 추천 시스템 문서

이 문서는 StudyWithMe 프로젝트의 Python 기반 AI 추천 시스템의 알고리즘과 로직을 설명합니다.

## 목차

1. [게시글 추천 시스템 (ai_recommendation.py)](#게시글-추천-시스템)
2. [텍스트 요약 시스템 (ai_summary.py)](#텍스트-요약-시스템)
3. [태그 추천 시스템 (ai_tag_recommendation.py)](#태그-추천-시스템)
4. [공통 설정 및 유틸리티](#공통-설정-및-유틸리티)

---

## 게시글 추천 시스템

**파일**: `ai_recommendation.py`

### 개요

사용자 활동 로그를 분석하여 개인화된 게시글을 추천하는 하이브리드 추천 시스템입니다.

### 주요 알고리즘

#### 1. 협업 필터링 (Collaborative Filtering)

##### 1.1 User-based Collaborative Filtering

**원리**: 비슷한 취향을 가진 사용자들이 좋아한 게시글을 추천합니다.

**알고리즘**:
1. 사용자-아이템 행렬 구축
   - 사용자 활동 로그 (SEARCH, CLICK, LIKE, BOOKMARK, COMMENT, AI_CLICK)를 가중치로 변환
   - 좋아요와 북마크 데이터를 추가로 반영
   - 0-5 스케일로 정규화

2. 유사도 계산
   - **코사인 유사도 (Cosine Similarity)**: 벡터 간 각도로 유사도 측정
     ```
     similarity = (A · B) / (||A|| × ||B||)
     ```
   - **피어슨 상관계수 (Pearson Correlation)**: 선형 관계 측정
     ```
     correlation = Σ((A_i - Ā)(B_i - B̄)) / √(Σ(A_i - Ā)² × Σ(B_i - B̄)²)
     ```

3. 예상 평점 계산
   ```
   predicted_rating = Σ(similarity_i × rating_i) / Σ|similarity_i|
   ```

**가중치**:
- AI_CLICK: 5.0
- BOOKMARK: 4.0
- COMMENT: 3.5
- LIKE: 3.0
- RECOMMEND: 2.5
- CLICK: 2.0
- SEARCH: 1.0

##### 1.2 Item-based Collaborative Filtering

**원리**: 사용자가 좋아한 게시글과 유사한 게시글을 추천합니다.

**알고리즘**:
1. 아이템-사용자 행렬 구축 (사용자-아이템 행렬의 전치)
2. 아이템 간 유사도 계산 (코사인 유사도 또는 피어슨 상관계수)
3. 사용자가 평가한 아이템의 유사 아이템을 찾아 예상 평점 계산
   ```
   predicted_rating = Σ(similarity_ij × rating_j) / Σ|similarity_ij|
   ```

#### 2. 콘텐츠 기반 필터링 (Content-based Filtering)

**원리**: 사용자의 과거 활동을 분석하여 선호하는 카테고리와 태그를 기반으로 추천합니다.

**알고리즘**:
1. 사용자 선호도 분석
   - 활동 로그에서 카테고리와 태그 점수 계산
   - 사용자가 직접 선택한 카테고리 선호도 반영 (가중치 10.0)
   - 액션 타입별 가중치 적용

2. 게시글 점수 계산
   ```
   score = category_match_score × 100
          + tag_match_score × 50
          + like_count × 2
          + view_count × 0.1
          + recency_bonus (최근 7일 내면 +10)
   ```

#### 3. 하이브리드 추천

**원리**: 협업 필터링과 콘텐츠 기반 필터링을 결합하여 더 정확한 추천을 제공합니다.

**알고리즘**:
1. 협업 필터링 결과와 콘텐츠 기반 결과를 가중 평균
   ```
   final_score = CF_score × 0.6 + Content_score × 0.4
   ```

2. User-based CF와 Item-based CF 결합
   ```
   CF_score = User_based_score × 0.6 + Item_based_score × 0.4
   ```

### 사용 예시

```bash
python ai_recommendation.py <user_id> [limit]
```

**출력 형식**:
```json
{
  "user_id": 1,
  "preferences": {
    "categories": {"개발": 0.45, "자격증": 0.30},
    "tags": {"Java": 0.20, "Spring": 0.15},
    "action_counts": {"CLICK": 10, "LIKE": 5}
  },
  "recommended_posts": [
    {
      "id": 123,
      "title": "게시글 제목",
      "recommendation_score": 85.5,
      "cf_score": 90.0,
      "content_score": 80.0
    }
  ]
}
```

---

## 텍스트 요약 시스템

**파일**: `ai_summary.py`

### 개요

게시글 본문을 분석하여 간결한 요약을 생성하는 시스템입니다.

### 주요 알고리즘

#### 1. TF-IDF (Term Frequency-Inverse Document Frequency)

**원리**: 문서 내에서 중요한 키워드를 추출하여 해당 키워드를 포함한 문장을 선택합니다.

**알고리즘**:
1. **TF (Term Frequency) 계산**
   ```
   TF(word) = count(word) / total_words
   ```

2. **IDF (Inverse Document Frequency) 계산**
   - 단일 문서 환경에서는 단어 길이와 빈도를 고려한 가중치 사용
   ```
   IDF(word) = length_weight × log(1 + frequency)
   ```

3. **TF-IDF 점수 계산**
   ```
   TF-IDF(word) = TF(word) × IDF(word)
   ```

4. **문장 점수 계산**
   ```
   sentence_score = Σ(TF-IDF(keyword)) / √(sentence_length)
   ```

5. **상위 문장 선택**: 점수가 높은 상위 3개 문장을 원래 순서대로 정렬하여 요약 생성

#### 2. TextRank

**원리**: PageRank 알고리즘을 텍스트에 적용하여 문장의 중요도를 계산합니다.

**알고리즘**:
1. **문장 간 유사도 행렬 구축**
   ```
   similarity(sent_i, sent_j) = |words_i ∩ words_j| / |words_i ∪ words_j|
   ```

2. **TextRank 점수 계산 (PageRank 알고리즘)**
   ```
   score(sent_i) = (1 - d) + d × Σ(score(sent_j) × similarity(sent_j, sent_i) / out_degree(sent_j))
   ```
   - `d`: damping factor (기본값: 0.85)
   - 반복 계산으로 수렴 (최대 100회)

3. **상위 문장 선택**: 점수가 높은 상위 3개 문장을 원래 순서대로 정렬

#### 3. 하이브리드 요약

**원리**: TF-IDF와 TextRank를 결합하여 더 정확한 요약을 생성합니다.

**알고리즘**:
```
combined_score = TF-IDF_score × 0.5 + TextRank_score × 0.5
```

#### 4. 규칙 기반 추출

**원리**: 정규표현식을 사용하여 구조화된 정보를 추출합니다.

**추출 항목**:
- **타겟 사용자**: "이런 분이면 좋아요", "원하는 사람", "참여 대상", "모집 대상"
- **레벨**: "레벨", "수준", "난이도", "경력"
- **진행 방식**: "진행 방식", "방식", "일정"

**레벨 추정**:
- 초급: "초보", "입문", "기초", "처음", "신입"
- 중급: "중급", "중간", "어느정도", "경험", "실무"
- 고급: "고급", "심화", "전문", "시니어", "리드"

### 사용 예시

```bash
python ai_summary.py "<content>" [max_length] [method]
```

**method 옵션**:
- `tfidf`: TF-IDF만 사용
- `textrank`: TextRank만 사용
- `hybrid`: TF-IDF + TextRank 결합 (기본값)

**출력 형식**:
```json
{
  "summary": "• 사용자는 이런 사람을 원함:\n  ...\n\n• 어떤 수준의 레벨로 추정됨:\n  ...",
  "original_length": 1500,
  "summary_length": 200,
  "method": "hybrid",
  "structured": {
    "target_users": "...",
    "level": "초급 (입문자/초보자 수준)",
    "process": "..."
  }
}
```

---

## 태그 추천 시스템

**파일**: `ai_tag_recommendation.py`

### 개요

게시글 제목과 본문을 분석하여 관련 태그와 카테고리를 자동으로 추천하는 시스템입니다.

### 주요 알고리즘

#### 1. TF-IDF 기반 태그 추출

**원리**: 문서에서 중요한 키워드를 TF-IDF로 추출하여 태그로 사용합니다.

**알고리즘**:
- 게시글 추천 시스템의 TF-IDF와 동일한 방식
- 상위 15개 키워드를 태그 후보로 선택
- 점수를 0-1 범위로 정규화 (최대 0.85)

#### 2. 기술 스택 매칭

**원리**: 사전 정의된 기술 태그 목록과 텍스트를 매칭합니다.

**기술 태그 목록**:
- 언어: Java, Python, JavaScript, TypeScript
- 프레임워크: React, Vue, Angular, Spring, Django, Flask
- 데이터베이스: MySQL, PostgreSQL, MongoDB, Redis
- 기타: Docker, Kubernetes, AWS, Git, REST API, GraphQL 등

**매칭 방식**:
- 대소문자 구분 없이 텍스트 내 포함 여부 확인
- 매칭된 기술 태그는 최고 신뢰도 (0.95) 부여

#### 3. 키워드 빈도 분석

**원리**: 텍스트에서 자주 등장하는 단어를 태그로 추천합니다.

**알고리즘**:
1. 단어 빈도 계산
2. 최대 빈도로 정규화
3. 점수 계산: `(count / max_count) × 0.7` (최대 0.7)

#### 4. 카테고리 기반 추론

**원리**: 추천된 카테고리에 해당하는 키워드를 태그로 추가합니다.

**카테고리별 키워드**:
- 프로그래밍: 코딩, 개발, 소프트웨어, 알고리즘, 자바, 파이썬 등
- 스터디: 공부, 학습, 독서, 토론, 발표, 과제 등
- 자격증: 자격증, 시험, 합격, 공인, 인증 등
- 취업: 취업, 면접, 포트폴리오, 이력서, 자소서 등

#### 5. 제목 가중치

**원리**: 제목에 등장하는 키워드에 추가 가중치를 부여합니다.

**알고리즘**:
- 제목 키워드: 기본 점수 0.6
- 제목과 본문 모두에 등장: 기존 점수 × 1.2 (최대 0.95)

#### 6. 하이브리드 태그 추천

**최종 점수 계산 순서**:
1. 기술 스택 매칭 (0.95)
2. TF-IDF 태그 (최대 0.85)
3. 키워드 빈도 (최대 0.7)
4. 카테고리 키워드 (0.75)
5. 제목 키워드 (0.6 또는 기존 점수 × 1.2)

**최종 태그 선택**: 점수 0.5 이상인 태그만 선택, 상위 10개 반환

### 카테고리 추천

**알고리즘**:
1. 카테고리별 키워드 매칭
2. 매칭된 키워드 수로 점수 계산
3. 가장 높은 점수의 카테고리 선택
4. 신뢰도 계산: `max_score / total_score`

### 사용 예시

```bash
python ai_tag_recommendation.py "<title>" "<content>"
```

**출력 형식**:
```json
{
  "category": "프로그래밍",
  "category_confidence": 0.85,
  "tags": ["Java", "Spring", "백엔드"],
  "tag_details": [
    {"tag": "Java", "confidence": 0.95},
    {"tag": "Spring", "confidence": 0.90},
    {"tag": "백엔드", "confidence": 0.75}
  ],
  "method": "hybrid"
}
```

---

## 공통 설정 및 유틸리티

### 설정 파일 (config.py)

**주요 설정**:
- 데이터베이스 연결 정보
- 로깅 설정
- 추천 시스템 설정 (기본 추천 개수, 분석 기간)
- 요약 시스템 설정 (기본 요약 길이, 최대 길이)

### 로깅 (logger.py)

**기능**:
- 파일 및 콘솔 로깅
- 로그 레벨 설정 (DEBUG, INFO, WARNING, ERROR)
- 타임스탬프 및 로거 이름 포함

### 예외 처리 (exceptions.py)

**커스텀 예외**:
- `ModelLoadError`: 모델 로드 실패
- `PredictionError`: 예측 실패

### 메트릭 (metrics.py)

**기능**:
- 분류 정확도 계산
- 정밀도, 재현율, F1 점수 계산

---

## 알고리즘 비교

### 추천 시스템

| 알고리즘 | 장점 | 단점 | 사용 시기 |
|---------|------|------|----------|
| User-based CF | 새로운 아이템 추천 가능, 사용자 다양성 반영 | Cold start 문제, 계산 비용 높음 | 사용자 활동이 많을 때 |
| Item-based CF | 안정적, 계산 효율적 | 새로운 아이템 추천 어려움 | 아이템 수가 많을 때 |
| Content-based | 사용자 독립적, 설명 가능 | 다양성 부족, 특징 추출 필요 | 사용자 활동이 적을 때 |
| Hybrid | 정확도 높음, 다양한 상황 대응 | 복잡도 높음 | 프로덕션 환경 |

### 요약 시스템

| 알고리즘 | 장점 | 단점 | 사용 시기 |
|---------|------|------|----------|
| TF-IDF | 빠름, 키워드 중심 | 문맥 고려 부족 | 짧은 문서 |
| TextRank | 문맥 고려, 문장 관계 반영 | 계산 비용 높음 | 긴 문서 |
| Hybrid | 정확도 높음 | 계산 비용 높음 | 일반적인 경우 |

---

## 성능 최적화

### 추천 시스템

1. **사용자-아이템 행렬 캐싱**: 자주 사용되는 행렬을 메모리에 캐싱
2. **유사도 계산 최적화**: 상위 N개 사용자/아이템만 계산
3. **병렬 처리**: 여러 사용자에 대한 추천을 병렬로 처리

### 요약 시스템

1. **문장 수 제한**: 너무 많은 문장은 처리하지 않음
2. **TextRank 반복 횟수 제한**: 최대 100회로 제한
3. **불용어 필터링**: 불필요한 단어 제거로 계산량 감소

---

## 향후 개선 방향

1. **딥러닝 모델 통합**
   - BERT 기반 텍스트 임베딩
   - 신경망 기반 협업 필터링 (Neural Collaborative Filtering)

2. **실시간 학습**
   - 사용자 피드백을 통한 모델 업데이트
   - 온라인 학습 알고리즘 적용

3. **개인화 강화**
   - 시간대별 선호도 반영
   - 계절성 및 트렌드 반영

4. **설명 가능한 AI**
   - 추천 이유 설명 기능
   - 사용자 이해도 향상

---

## 참고 자료

- [Collaborative Filtering - Wikipedia](https://en.wikipedia.org/wiki/Collaborative_filtering)
- [TF-IDF - Wikipedia](https://en.wikipedia.org/wiki/Tf%E2%80%93idf)
- [TextRank Algorithm](https://web.eecs.umich.edu/~mihalcea/papers/mihalcea.emnlp04.pdf)
- [PageRank Algorithm](https://en.wikipedia.org/wiki/PageRank)

---

**작성일**: 2025-01-11  
**버전**: 2.0  
**작성자**: AI Assistant
