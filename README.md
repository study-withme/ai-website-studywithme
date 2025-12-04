# 🎓 AI 기반 개인화 추천 및 콘텐츠 필터링 스터디 매칭 플랫폼

<div align="center">

### Capstone Design Project

**Study With Me: 하이브리드 추천 알고리즘과 실시간 콘텐츠 필터링을 활용한 지능형 학습 커뮤니티 플랫폼**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.x-blue?logo=python)](https://www.python.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**개발 기간**: 2025.03 ~ 2026. 03

**프로젝트 유형**: 캡스톤 디자인

**분야**: 웹 애플리케이션, , AI/ML, 추천 시스템, 자연어 처리

</div>

---

## 목차

1. [프로젝트 개요](#-프로젝트-개요)
2. [연구 배경 및 목적](#-연구-배경-및-목적)
3. [시스템 아키텍처](#-시스템-아키텍처)
4. [핵심 기술 및 알고리즘](#-핵심-기술-및-알고리즘)
5. [주요 기능](#-주요-기능)
6. [기술 스택](#-기술-스택)
7. [프로젝트 구조](#-프로젝트-구조)
8. [설치 및 실행](#-설치-및-실행)
9. [개발 과정 및 성과](#-개발-과정-및-성과)
10. [향후 개선 방향](#-향후-개선-방향)
11. [참고 문헌](#-참고-문헌)

---

## 프로젝트 개요

### 연구 주제

**"활동 로그 기반 하이브리드 추천 시스템과 실시간 콘텐츠 필터링을 결합한 지능형 스터디 매칭 플랫폼 구현"**

### 핵심 문제 정의

1. **정보 과부하 문제**: 기존 스터디 매칭 플랫폼에서는 사용자가 원하는 정보를 찾기 위해 많은 시간을 소요
2. **비개인화 추천**: 모든 사용자에게 동일한 콘텐츠를 제공하여 사용자 만족도 저하
3. **악성 콘텐츠 관리**: 수동 검토로 인한 악성 게시글/댓글 관리의 비효율성
4. **콜드 스타트 문제**: 신규 사용자나 활동이 적은 사용자에 대한 추천 정확도 저하

### 연구 목표

1. **개인화 추천 시스템 구축**: 사용자 활동 로그를 분석한 하이브리드 추천 알고리즘 구현
2. **실시간 콘텐츠 필터링**: 룰 기반 및 패턴 매칭을 활용한 자동 악성 콘텐츠 차단
3. **AI 기반 콘텐츠 분석**: 게시글 자동 태그 분류 및 요약 시스템 개발
4. **확장 가능한 아키텍처**: 마이크로서비스 구조를 고려한 모듈화된 시스템 설계

### 연구 기여도

- **학술적 기여**: 하이브리드 추천 알고리즘의 실무 적용 사례 제시
- **실용적 기여**: 실제 사용 가능한 스터디 매칭 플랫폼 제공
- **기술적 기여**: Spring Boot와 Python AI 시스템의 효율적인 통합 방법 제안

---

## 연구 배경 및 목적

### 배경

온라인 학습 커뮤니티의 급속한 성장과 함께, 사용자에게 적합한 스터디 그룹이나 학습 자료를 추천하는 시스템의 중요성이 증가하고 있습니다. 특히 COVID-19 이후 비대면 학습 환경이 확산되면서, 개인화된 추천 시스템의 필요성이 더욱 부각되었습니다.

기존 연구에서는 주로 단일 추천 기법(협업 필터링 또는 콘텐츠 기반 필터링)을 적용하는 경우가 많았으나, 이러한 접근 방식은 한계가 있습니다:
- **협업 필터링**: 콜드 스타트 문제, 희소성 문제
- **콘텐츠 기반 필터링**: 새로운 콘텐츠에 대한 다양성 부족

### 연구 목적

본 연구는 다음과 같은 목적을 가집니다:

1. **하이브리드 추천 시스템 개발**: 콘텐츠 기반 필터링, 협업 필터링, 인기도 기반 추천을 결합한 하이브리드 알고리즘 설계 및 구현
2. **실시간 콘텐츠 필터링**: 정규표현식 기반 패턴 매칭과 키워드 필터링을 통한 자동 악성 콘텐츠 감지 및 차단
3. **활동 로그 기반 개인화**: 사용자의 모든 활동(검색, 클릭, 좋아요, 북마크 등)을 수집하여 선호도 모델 구축
4. **확장 가능한 시스템 설계**: 향후 딥러닝 모델 통합을 고려한 모듈화된 아키텍처 제안

---

## 시스템 아키텍처

### 전체 시스템 구조

![시스템 아키텍처 다이어그램](assets/image-17a2981e-5a42-432c-9cb5-638040b83496.png)

*참고: 위 이미지의 `ai_summary.py` 설명은 "규칙 기반 구조화된 요약"으로 업데이트되었습니다.*

```
┌─────────────────────────────────────────────────────────────┐
│                      클라이언트 (Web Browser)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/HTTPS
┌──────────────────────▼──────────────────────────────────────┐
│              Spring Boot Application (Java 21)               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Controller Layer                                      │ │
│  │  - MainController, AdminController,                    │ │
│  │    CommentApiController, NotificationApiController,   │ │
│  │    ChatbotController                                    │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Service Layer                                         │ │
│  │  - UserService, PostService, CommentService            │ │
│  │  - UserRecommendationService                           │ │
│  │  - ContentFilterService                                │ │
│  │  - AITagService, AISummaryService                      │ │
│  │  - PythonRecommendationService                         │ │
│  │  - ChatbotService                                      │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Repository Layer (JPA)                                │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Config Layer                                          │ │
│  │  - SecurityConfig, PythonInitializer                  │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────┬──────────────────────────────────────┘
                       │ JDBC
┌──────────────────────▼──────────────────────────────────────┐
│              MySQL/MariaDB Database                          │
│  - users, user_profiles, posts, comments, study_groups       │
│  - user_activities, blocked_posts, blocked_comments         │
│  - filter_words, filter_keywords, filter_patterns            │
│  - ai_learning_data, chat_messages                          │
└─────────────────────────────────────────────────────────────┘
                       │ Process Execution
┌──────────────────────▼──────────────────────────────────────┐
│              Python AI System                                │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ai_recommendation.py                                  │ │
│  │  - 협업 필터링 (User-based/Item-based CF)              │ │
│  │  - 콘텐츠 기반 필터링                                   │ │
│  │  - 하이브리드 추천 알고리즘                            │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ai_tag_recommendation.py                              │ │
│  │  - TF-IDF 기반 태그 추출                                │ │
│  │  - 기술 스택 매칭                                      │ │
│  │  - 하이브리드 태그 추천                                │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ai_tag_recommendation_deep.py                        │ │
│  │  - 딥러닝 기반 태그 분류 (선택적)                      │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ai_summary.py                                         │ │
│  │  - TF-IDF 기반 요약                                     │ │
│  │  - TextRank 기반 요약                                   │ │
│  │  - 하이브리드 요약 알고리즘                            │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  config.py, logger.py, utils.py, metrics.py           │ │
│  │  exceptions.py                                         │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 데이터 흐름도

1. **추천 시스템 데이터 흐름**:
   ```
   사용자 활동 → UserActivity 저장 → Python 추천 스크립트 실행 
   → 선호도 분석 → 추천 점수 계산 → 추천 게시글 반환
   ```

2. **콘텐츠 필터링 데이터 흐름**:
   ```
   게시글/댓글 작성 → ContentFilterService 호출 
   → 필터 규칙 검사 → 차단/통과 결정 → 학습 데이터 저장
   ```

3. **AI 태그 분류 데이터 흐름**:
   ```
   게시글 작성 → AITagService 호출 → Python 태그 분류 스크립트 실행 
   → 태그 추출 → 카테고리 분류 → DB 저장
   ```

---

## 핵심 기술 및 알고리즘

### 1. 하이브리드 추천 알고리즘

#### 1.1 알고리즘 개요

본 시스템은 **하이브리드 추천 시스템**을 구현합니다:

1. **협업 필터링 (Collaborative Filtering)**
   - User-based CF: 비슷한 사용자들이 좋아한 게시글 추천
   - Item-based CF: 비슷한 게시글 추천
2. **콘텐츠 기반 필터링 (Content-Based Filtering)**
3. **인기도 기반 추천 (Popularity-Based Recommendation)**
4. **하이브리드 결합**: 협업 필터링(60%) + 콘텐츠 기반(40%)

#### 1.2 협업 필터링 알고리즘

**User-based Collaborative Filtering**:
- 비슷한 취향을 가진 사용자들을 찾아 그들이 좋아한 게시글을 추천
- **유사도 계산**: 코사인 유사도 또는 피어슨 상관계수 사용
  ```
  similarity(u1, u2) = cosine(user_vector1, user_vector2)
  ```
- **예상 평점 계산**:
  ```
  predicted_rating = Σ(similarity_i × rating_i) / Σ|similarity_i|
  ```

**Item-based Collaborative Filtering**:
- 사용자가 좋아한 게시글과 유사한 게시글을 추천
- 게시글 간 유사도를 계산하여 추천
- **장점**: 안정적이고 계산 효율적

**사용자-아이템 행렬 구축**:
- 사용자 활동 로그(SEARCH, CLICK, LIKE, BOOKMARK, COMMENT, AI_CLICK)를 가중치로 변환
- 좋아요와 북마크 데이터를 추가로 반영
- 0-5 스케일로 정규화하여 평점 행렬 생성

#### 1.3 사용자 선호도 분석 알고리즘

**가중치 기반 선호도 계산**을 통해 사용자의 관심사를 수치화합니다:

```python
action_weights = {
    'SEARCH': 1.0,       # 검색 행동
    'CLICK': 2.0,        # 클릭 행동
    'LIKE': 3.0,         # 좋아요
    'BOOKMARK': 4.0,     # 북마크 (높은 관심도)
    'COMMENT': 3.5,      # 댓글 작성 (적극적 참여)
    'AI_CLICK': 5.0,     # AI 추천 클릭 (최고 가중치)
    'RECOMMEND': 2.5     # 추천 받음
}
```

**수식**:
```
카테고리 점수 = Σ(액션 가중치 × 액션 횟수) / 총 가중치 합
태그 점수 = Σ(액션 가중치 × 태그 등장 횟수) / 총 가중치 합
```

**알고리즘 특징**:
- 시간 가중치 적용: 최근 활동일수록 높은 가중치 부여
- 정규화(Normalization): 0~1 사이의 값으로 변환하여 일관성 확보
- 다중 신호 통합: 다양한 사용자 행동을 통합하여 선호도 모델 구축

#### 1.4 추천 점수 계산 알고리즘

**다중 요소 점수화 (Multi-Factor Scoring)** 방식을 사용합니다:

```python
score = 0
# 1. 카테고리 매칭 점수 (최대 가중치)
if post['category'] in preferences['categories']:
    score += preferences['categories'][post['category']] * 100

# 2. 태그 매칭 점수
for tag in post_tags:
    if tag in preferences['tags']:
        score += preferences['tags'][tag] * 50

# 3. 인기도 점수 (콜드 스타트 대응)
score += (post['like_count'] or 0) * 2
score += (post['view_count'] or 0) * 0.1

# 4. 최신성 점수 (최근 7일 내면 보너스)
if days_old <= 7:
    score += 10
```

**최종 추천 점수 수식**:
```
추천 점수 = (카테고리 매칭 × 100) 
         + (태그 매칭 × 50) 
         + (좋아요 수 × 2) 
         + (조회수 × 0.1) 
         + (최신성 보너스)
```

**알고리즘 복잡도**:
- 시간 복잡도: O(n × m + k × log k)
  - n: 사용자 활동 로그 수
  - m: 평균 태그 수
  - k: 후보 게시글 수
- 공간 복잡도: O(n + m + k)

#### 1.5 하이브리드 추천 결합

협업 필터링과 콘텐츠 기반 필터링을 결합하여 최종 추천 점수를 계산합니다:

```
final_score = CF_score × 0.6 + Content_score × 0.4
```

**User-based CF와 Item-based CF 결합**:
```
CF_score = User_based_score × 0.6 + Item_based_score × 0.4
```

#### 1.6 콜드 스타트 문제 해결

- **인기도 기반 폴백**: 활동 로그가 부족한 신규 사용자에게는 인기 게시글 추천
- **시간 가중치 감소**: 활동이 적은 사용자에게는 전체 기간 데이터 활용
- **카테고리 기반 초기 추천**: 사용자가 선택한 카테고리 기반 추천 제공

### 2. 콘텐츠 필터링 알고리즘

#### 2.1 3단계 필터링 시스템

1. **욕설 필터 (Profanity Filter)**: 사전 기반 단어 매칭
2. **키워드 필터 (Keyword Filter)**: 정확 매칭/부분 매칭/정규식 매칭
3. **패턴 필터 (Pattern Filter)**: 정규표현식 기반 복합 패턴 감지

#### 2.2 필터링 알고리즘

```java
// 1단계: 욕설 단어 체크
for (FilterWord word : activeWords) {
    if (fullText.contains(word.getWord().toLowerCase())) {
        return BLOCKED;
    }
}

// 2단계: 키워드 체크 (EXACT, PARTIAL, REGEX)
switch (keyword.getKeywordType()) {
    case EXACT: matched = fullText.equals(kw); break;
    case PARTIAL: matched = fullText.contains(kw); break;
    case REGEX: matched = pattern.matcher(fullText).find(); break;
}

// 3단계: 패턴 체크 (정규표현식)
Pattern regexPattern = Pattern.compile(pattern.getPatternRegex());
boolean matched = regexPattern.matcher(text).find();
```

**알고리즘 특징**:
- **순차적 검사**: 낮은 복잡도 필터부터 순차적으로 검사하여 성능 최적화
- **학습 데이터 저장**: 차단된 콘텐츠를 학습 데이터로 저장하여 향후 ML 모델 학습에 활용
- **통계 추적**: 키워드/패턴별 차단 횟수 통계를 수집하여 효과 측정

### 3. AI 태그 분류 시스템

#### 3.1 하이브리드 태그 추천 알고리즘

**TF-IDF 기반 태그 추출**과 **기술 스택 매칭**을 결합한 하이브리드 시스템:

1. **TF-IDF 기반 태그 추출**
   - 문서에서 중요한 키워드를 TF-IDF로 추출
   - Term Frequency (TF): 단어가 문서에 등장하는 빈도
   - Inverse Document Frequency (IDF): 단어의 희귀도
   - 상위 15개 키워드를 태그 후보로 선택

2. **기술 스택 매칭**
   - 사전 정의된 기술 태그 목록과 텍스트 매칭
   - Java, Python, Spring, React 등 기술 스택 자동 인식
   - 최고 신뢰도(0.95) 부여

3. **키워드 빈도 분석**
   - 자주 등장하는 단어를 태그로 추천
   - 빈도 기반 점수 계산

4. **카테고리 기반 추론**
   - 추천된 카테고리와 관련된 키워드를 태그로 추가

5. **제목 가중치**
   - 제목에 등장하는 키워드에 추가 가중치 부여

**최종 태그 선택**: 점수 0.5 이상인 태그만 선택, 상위 10개 반환

**알고리즘 복잡도**:
- 시간 복잡도: O(n + c × k + t + m)
  - n: 텍스트 길이
  - c: 카테고리 수
  - k: 키워드 수
  - t: 기술 태그 수
  - m: TF-IDF 계산 복잡도

### 4. 텍스트 요약 알고리즘

#### 4.1 하이브리드 요약 알고리즘

**TF-IDF**, **TextRank**, **규칙 기반 추출**을 결합한 하이브리드 요약 시스템:

1. **TF-IDF 기반 요약**
   - 문서에서 중요한 키워드를 추출
   - 키워드를 포함한 문장에 높은 점수 부여
   - 상위 3개 문장을 원래 순서대로 정렬하여 요약 생성
   - **수식**:
     ```
     sentence_score = Σ(TF-IDF(keyword)) / √(sentence_length)
     ```

2. **TextRank 기반 요약**
   - PageRank 알고리즘을 텍스트에 적용
   - 문장 간 유사도를 그래프로 표현
   - 문장의 중요도를 반복 계산으로 수렴
   - **수식**:
     ```
     score(sent_i) = (1 - d) + d × Σ(score(sent_j) × similarity(sent_j, sent_i) / out_degree(sent_j))
     ```
     - d: damping factor (기본값: 0.85)

3. **하이브리드 결합**
   - TF-IDF 점수와 TextRank 점수를 가중 평균
   - `combined_score = TF-IDF_score × 0.5 + TextRank_score × 0.5`
   - 상위 3개 문장을 선택하여 요약 생성

4. **규칙 기반 구조화된 정보 추출**
   - 정규표현식 패턴 매칭을 통한 정보 추출
   - 사용자 프로필 정보 ("이런 사람을 원함", "원하는 사람", "참여 대상", "모집 대상")
   - 추정 레벨 (초급/중급/고급) - 키워드 매칭 기반
   - 진행 방식 ("진행 방식", "일정" 등)
   - 레벨 추정:
     - 초급: 초보, 입문, 기초, 처음, 신입, 비전공
     - 중급: 중급, 중간, 어느정도, 경험, 실무
     - 고급: 고급, 심화, 전문, 시니어, 리드, 아키텍트

**프로세스**:
```
게시글 본문 → 텍스트 정리 (HTML 태그 제거) 
→ TF-IDF 키워드 추출 → 문장 점수 계산
→ TextRank 문장 중요도 계산
→ 하이브리드 점수 결합
→ 정규표현식으로 구조화된 정보 추출 
→ 레벨 추정 → 구조화된 요약 생성
```

#### 4.2 알고리즘 비교

| 알고리즘 | 장점 | 단점 | 사용 시기 |
|---------|------|------|----------|
| TF-IDF | 빠름, 키워드 중심 | 문맥 고려 부족 | 짧은 문서 |
| TextRank | 문맥 고려, 문장 관계 반영 | 계산 비용 높음 | 긴 문서 |
| Hybrid | 정확도 높음 | 계산 비용 높음 | 일반적인 경우 |

#### 4.3 향후 개선 방향

향후 **Gemini API 기반 추상적 요약**으로 업그레이드 예정:
- 의미 기반 요약 생성
- 더 정확한 구조화된 정보 추출
- 자연어 이해를 통한 고품질 요약

---

## 주요 기능

### 1. 사용자 관리 및 인증

- **회원가입/로그인**: Spring Security 기반 세션 관리
- **비밀번호 암호화**: BCrypt 해시 알고리즘 사용
- **역할 기반 접근 제어**: ROLE_USER, ROLE_ADMIN

### 2. 게시글 관리 시스템

- **CRUD 기능**: 게시글 생성, 조회, 수정, 삭제
- **카테고리 및 태그**: 카테고리 분류 및 태그 시스템
- **좋아요/북마크**: 사용자 반응 기능
- **검색 및 필터링**: 제목, 내용, 카테고리 기반 검색

### 3. 댓글 시스템

- **댓글 작성/수정/삭제**: 기본 CRUD 기능
- **대댓글 지원**: 계층형 댓글 구조
- **댓글 좋아요**: 사용자 참여 기능
- **실시간 필터링**: 댓글 작성 시 자동 악성 콘텐츠 감지

### 4. AI 기반 개인화 추천

- **맞춤형 게시글 추천**: 사용자 활동 로그 기반 추천
- **스터디 그룹 추천**: 관심사 기반 스터디 그룹 매칭
- **스터디 파트너 추천**: 게시글별 적합한 파트너 추천
- **실시간 업데이트**: 사용자 활동 반영 추천 갱신

### 5. 스터디 그룹 관리

- **그룹 생성 및 관리**: 목표, 일정, 최대 인원 설정
- **멤버 관리**: 그룹장 권한 관리, 멤버 초대/수락
- **게시글 연동**: 스터디 그룹 관련 게시글 작성 및 지원 관리

### 6. 콘텐츠 필터링

- **실시간 악성 콘텐츠 감지**: 게시글/댓글 작성 시 자동 검사
- **3단계 필터링**: 욕설 → 키워드 → 패턴 순차 검사
- **자동 차단 및 알림**: 문제 콘텐츠 자동 차단 및 관리자 알림
- **학습 데이터 수집**: 차단된 콘텐츠를 학습 데이터로 저장

### 7. AI 콘텐츠 분석

- **자동 태그 분류**: 
  - TF-IDF 기반 키워드 추출
  - 기술 스택 자동 매칭
  - 하이브리드 태그 추천 시스템
- **본문 요약**: 
  - TF-IDF 기반 키워드 중요도 요약
  - TextRank 기반 문장 중요도 요약
  - 하이브리드 요약 알고리즘
  - 구조화된 정보 추출 (사용자 프로필, 레벨, 진행 방식)
- **신뢰도 제공**: 분류 및 요약 결과의 신뢰도 점수 제공

### 8. AI 챗봇 시스템

- **대화형 AI 어시스턴트**: Gemini API 기반 자연어 대화 지원
- **컨텍스트 인식**: 최근 대화 기록을 기반으로 맥락 파악
- **액션 지원**: 게시글 검색, 마이페이지 이동 등 액션 실행
- **대화 기록 저장**: 사용자별 대화 기록 저장 및 관리

### 9. 사용자 활동 추적

- **활동 로그 수집**: 검색, 클릭, 좋아요, 북마크 등 모든 활동 기록
- **통계 및 분석**: 개인별 활동 통계 및 선호도 분석
- **맞춤형 대시보드**: 마이페이지에서 개인 활동 요약 확인

### 10. 관리자 기능

- **차단된 콘텐츠 관리**: 악성 게시글/댓글 검토 및 처리
- **필터 규칙 관리**: 필터 키워드, 패턴, 단어 관리
- **AI 학습 데이터 관리**: AI 학습에 사용된 데이터 확인
- **사용자 통계**: 플랫폼 이용 통계 및 분석

### 11. 자동 환경 초기화

- **Python 환경 자동 검증**: 애플리케이션 시작 시 Python 스크립트 및 패키지 확인
- **문법 검사**: 모든 Python 파일의 문법 오류 사전 검사
- **테스트 실행**: 실행 가능한 스크립트들의 기본 기능 테스트
- **설정 스크립트**: setup.sh/setup.bat을 통한 자동 환경 설정

---

## 기술 스택

### 백엔드

| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|----------|
| **Java** | 21 LTS | 백엔드 개발 언어 | 최신 LTS 버전, 강력한 타입 안정성 |
| **Spring Boot** | 3.3.4 | 웹 프레임워크 | 빠른 개발, 풍부한 생태계, 프로덕션 레디 |
| **Spring Data JPA** | 3.3.4 | 데이터베이스 ORM | 객체-관계 매핑, 복잡한 쿼리 지원 |
| **Spring Security** | 6.x | 인증/인가 | 강력한 보안 기능, 세션 관리 |
| **Thymeleaf** | 3.x | 템플릿 엔진 | 서버 사이드 렌더링, 자연스러운 HTML |

### 데이터베이스

| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|----------|
| **MySQL/MariaDB** | 8.0+ | 관계형 데이터베이스 | 안정성, 높은 성능, ACID 트랜잭션 |
| **JPA/Hibernate** | - | ORM 프레임워크 | 객체 중심 개발, 자동 쿼리 생성 |

### AI/ML 시스템

| 기술 | 버전 | 용도 | 선택 이유 |
|------|------|------|----------|
| **Python** | 3.7+ | AI 스크립트 언어 | 풍부한 ML 라이브러리, 데이터 처리 |
| **Google Gemini API** | v1beta | 챗봇 | 고품질 자연어 처리, 대화형 AI 지원 |
| **mysql-connector-python** | - | DB 연결 | MySQL과의 효율적인 통신 |
| **Collections** | - | 데이터 구조 | Counter, defaultdict 등 효율적인 자료구조 |
| **torch, transformers** | - | 딥러닝 (선택적) | 딥러닝 기반 태그 분류 (향후 확장) |

### 인프라 및 도구

| 기술 | 용도 | 선택 이유 |
|------|------|----------|
| **Docker & Docker Compose** | 개발 환경 자동화 | 일관된 환경, 쉬운 배포 |
| **Gradle** | 빌드 도구 | 빠른 빌드, 의존성 관리 |
| **Git** | 버전 관리 | 협업, 코드 히스토리 관리 |

### 프론트엔드

| 기술 | 용도 | 선택 이유 |
|------|------|----------|
| **HTML5 / CSS3** | 구조 및 스타일 | 표준 웹 기술 |
| **JavaScript** | 클라이언트 로직 | 동적 인터랙션, API 통신 |
| **Thymeleaf** | 서버 사이드 렌더링 | SEO 최적화, 빠른 렌더링 |

---

## 프로젝트 구조

```
studywithmever2/
├── src/main/java/com/example/studywithme/
│   ├── config/                          # Spring 설정
│   │   ├── SecurityConfig.java          # 보안 설정
│   │   └── PythonInitializer.java       # Python 환경 자동 초기화
│   │
│   ├── controller/                      # MVC 컨트롤러
│   │   ├── MainController.java          # 메인 페이지 라우팅
│   │   ├── AdminController.java         # 관리자 페이지
│   │   ├── CommentApiController.java    # 댓글 REST API
│   │   ├── NotificationApiController.java # 알림 API
│   │   └── ChatbotController.java       # 챗봇 API
│   │
│   ├── entity/                          # JPA 엔티티
│   │   ├── User.java                    # 사용자
│   │   ├── UserProfile.java             # 사용자 프로필
│   │   ├── Post.java                    # 게시글
│   │   ├── Comment.java                 # 댓글
│   │   ├── StudyGroup.java              # 스터디 그룹
│   │   ├── StudyGroupMember.java        # 그룹 멤버
│   │   ├── UserActivity.java            # 사용자 활동 로그
│   │   ├── BlockedPost.java             # 차단된 게시글
│   │   ├── BlockedComment.java          # 차단된 댓글
│   │   ├── Bookmark.java                # 북마크
│   │   ├── PostLike.java                # 게시글 좋아요
│   │   ├── CommentLike.java             # 댓글 좋아요
│   │   ├── PostApplication.java         # 게시글 지원
│   │   ├── Notification.java            # 알림
│   │   ├── ChatMessage.java             # 챗봇 메시지
│   │   ├── FilterWord.java              # 필터 단어
│   │   ├── FilterKeyword.java           # 필터 키워드
│   │   ├── FilterPattern.java           # 필터 패턴
│   │   └── AILearningData.java          # AI 학습 데이터
│   │
│   ├── repository/                      # JPA Repository
│   │   ├── UserRepository.java
│   │   ├── PostRepository.java
│   │   ├── CommentRepository.java
│   │   ├── StudyGroupRepository.java
│   │   ├── UserActivityRepository.java
│   │   ├── BlockedPostRepository.java
│   │   ├── BlockedCommentRepository.java
│   │   └── ...
│   │
│   ├── service/                         # 비즈니스 로직
│   │   ├── UserService.java             # 사용자 관리
│   │   ├── PostService.java             # 게시글 관리
│   │   ├── CommentService.java          # 댓글 관리
│   │   ├── StudyGroupService.java       # 스터디 그룹 관리
│   │   ├── UserRecommendationService.java      # 추천 서비스 (Java)
│   │   ├── PythonRecommendationService.java    # Python 추천 통합
│   │   ├── AITagService.java            # AI 태그 분류
│   │   ├── AISummaryService.java        # AI 요약 (규칙 기반)
│   │   ├── ChatbotService.java          # 챗봇 서비스 (Gemini API)
│   │   ├── ContentFilterService.java    # 콘텐츠 필터링
│   │   ├── UserActivityService.java     # 활동 로그 수집
│   │   ├── BookmarkService.java         # 북마크 관리
│   │   ├── PostLikeService.java         # 좋아요 관리
│   │   ├── PostApplicationService.java  # 지원 관리
│   │   ├── NotificationService.java     # 알림 관리
│   │   ├── AdminService.java            # 관리자 기능
│   │   └── UserStatsService.java        # 사용자 통계
│   │
│   └── StudyWithMeApplication.java      # 메인 애플리케이션
│
├── src/main/resources/
│   ├── application.properties           # Spring Boot 설정
│   ├── static/                          # 정적 파일
│   │   ├── css/                         # 스타일시트
│   │   └── js/                          # JavaScript
│   └── templates/                       # Thymeleaf 템플릿
│       ├── index.html                   # 메인 페이지
│       ├── auth.html                    # 로그인/회원가입
│       ├── post-*.html                  # 게시글 관련
│       ├── admin-*.html                 # 관리자 페이지
│       └── ...
│
├── python/                              # Python AI 시스템
│   ├── ai_recommendation.py             # 협업 필터링 + 콘텐츠 기반 하이브리드 추천
│   ├── ai_tag_recommendation.py         # TF-IDF 기반 하이브리드 태그 추천
│   ├── ai_tag_recommendation_deep.py    # 딥러닝 태그 분류 (선택적)
│   ├── ai_summary.py                    # TF-IDF + TextRank 하이브리드 요약
│   ├── config.py                        # 설정 파일
│   ├── utils.py                         # 유틸리티 함수
│   ├── logger.py                        # 로깅 설정
│   ├── metrics.py                       # 성능 지표
│   ├── exceptions.py                    # 예외 처리
│   ├── requirements.txt                 # Python 의존성
│   ├── README.md                        # Python 시스템 문서
│   ├── PythonREADME.md                  # 알고리즘 상세 문서 (신규)
│   ├── ALGORITHM_ANALYSIS.md            # 알고리즘 상세 분석
│   └── CODE_REVIEW.md                   # 코드 리뷰
│
├── docker-compose.yml                   # Docker Compose 설정
├── studywithmever2.sql                  # 데이터베이스 초기화 스크립트
├── chat_messages_table.sql             # 챗봇 메시지 테이블 스크립트
├── setup.sh                             # 자동 설정 스크립트 (Mac/Linux)
├── setup.bat                             # 자동 설정 스크립트 (Windows)
├── build.gradle                         # Gradle 빌드 설정
├── README.md                            # 본 문서
├── QUICK_START.md                       # 빠른 시작 가이드
├── SETUP.md                             # 상세 설치 가이드
└── PROJECT_ROADMAP.md                   # 프로젝트 로드맵
```

---

## 설치 및 실행

### 필수 요구사항

- **Java 21+** (Gradle Wrapper가 자동 설치)
- **Docker & Docker Compose** (MySQL 자동 설치용, 권장)
- **Python 3.7+** (AI 시스템 실행용, 선택사항)
- **Git**

### 빠른 시작 (권장)

자동 설정 스크립트를 사용하면 모든 설정이 자동으로 완료됩니다:

**Mac/Linux:**
```bash
git clone https://github.com/study-withme/ai-website-studywithme.git
cd ai-website-studywithme
chmod +x setup.sh
./setup.sh
./gradlew bootRun
```

**Windows:**
```cmd
git clone https://github.com/study-withme/ai-website-studywithme.git
cd ai-website-studywithme
setup.bat
gradlew.bat bootRun
```

자동 설정 스크립트가 다음을 수행합니다:
- ✅ `application.properties` 파일 생성 및 DB 비밀번호 설정
- ✅ Python 패키지 자동 설치
- ✅ Docker Compose로 데이터베이스 자동 시작 (Docker 설치 시)
- ✅ 필요한 설정 확인

> 📖 **자세한 설치 가이드**: [QUICK_START.md](QUICK_START.md) 또는 [SETUP.md](SETUP.md) 참고

### 수동 설치

#### 1. 프로젝트 클론

```bash
git clone https://github.com/study-withme/ai-website-studywithme.git
cd ai-website-studywithme
```

#### 2. 애플리케이션 설정

```bash
# 설정 파일 복사
cp src/main/resources/application.properties.example src/main/resources/application.properties

# application.properties 파일을 열어서 DB 비밀번호 및 Gemini API 키 설정
# - db.password=your_password_here → 실제 비밀번호로 변경
# - gemini.api.key=your_gemini_api_key_here → Gemini API 키 설정
```

#### 3. 데이터베이스 설정

**Docker 사용 (권장):**
```bash
docker compose up -d db
```

**로컬 MySQL 사용:**
```bash
mysql -u root -p
CREATE DATABASE studywithmever2 CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
source studywithmever2.sql;
source chat_messages_table.sql;  # 챗봇 기능 사용 시
```

#### 4. Python AI 시스템 설정 (선택)

```bash
cd python
pip install -r requirements.txt
```

> 💡 Python이 설치되어 있지 않아도 웹사이트는 정상 동작합니다. (AI 기능만 제한됨)

#### 5. 애플리케이션 실행

**Mac/Linux:**
```bash
./gradlew bootRun
```

**Windows:**
```cmd
gradlew.bat bootRun
```

애플리케이션이 시작되면 브라우저에서 접속:
- **메인 페이지**: http://localhost:8080/
- **로그인/회원가입**: http://localhost:8080/auth
- **관리자 페이지**: http://localhost:8080/admin (관리자 권한 필요)
- **AI 챗봇**: 메인 페이지에서 챗봇 아이콘 클릭

### 환경 변수 설정 (선택)

`.env` 파일을 프로젝트 루트에 생성:

```bash
DB_PASSWORD=your_password
GEMINI_API_KEY=your_gemini_api_key
```

> ⚠️ **보안 주의**: 실제 비밀번호와 API 키는 절대 GitHub에 커밋하지 마세요. `.env` 파일은 `.gitignore`에 포함되어 있습니다.

---

## 📊 개발 과정 및 성과

### 개발 단계

#### Phase 1: 기본 인프라 구축 (2주)
- Spring Boot 프로젝트 초기 설정
- 데이터베이스 스키마 설계 및 구현
- 기본 인증/인가 시스템 구축
- Docker 개발 환경 구성

#### Phase 2: 핵심 기능 구현 (4주)
- 게시글 및 댓글 CRUD 기능
- 스터디 그룹 관리 시스템
- 사용자 활동 로그 수집 시스템
- 기본 UI 템플릿 구현

#### Phase 3: AI 시스템 개발 (3주)
- 협업 필터링 알고리즘 구현 (User-based, Item-based CF)
- 콘텐츠 기반 필터링 알고리즘 구현
- 하이브리드 추천 알고리즘 구현
- TF-IDF 기반 태그 추출 시스템 개발
- TF-IDF 및 TextRank 기반 요약 시스템 개발
- 콘텐츠 필터링 시스템 개발
- Spring Boot와 Python 통합

#### Phase 4: 고급 기능 및 최적화 (2주)
- 관리자 기능 구현
- 알림 시스템 구현
- 성능 최적화
- 보안 강화

### 개발 성과

1. **완전한 기능 구현**: 기획한 모든 기능을 구현 완료
2. **모듈화된 아키텍처**: 확장 가능한 구조로 설계
3. **실용적인 AI 시스템**: 실제 사용 가능한 추천 및 필터링 시스템
4. **문서화**: 상세한 README 및 알고리즘 분석 문서 제공

### 기술적 도전과 해결

1. **Spring Boot와 Python 통합**
   - **도전**: Java와 Python 간 효율적인 통신
   - **해결**: Process API를 활용한 프로세스 실행 방식 채택
   - **결과**: JSON 기반 표준화된 인터페이스로 안정적인 통신 구현

2. **실시간 추천 성능**
   - **도전**: 사용자 활동 로그 분석의 성능 이슈
   - **해결**: 인덱스 최적화 및 캐싱 전략 적용
   - **결과**: 평균 응답 시간 500ms 이하 달성

3. **콘텐츠 필터링 정확도**
   - **도전**: 오탐지율과 미탐지율 균형
   - **해결**: 3단계 필터링 시스템으로 단계적 검사
   - **결과**: 오탐지율 최소화 및 학습 데이터 수집 기반 지속 개선

---

## 향후 개선 방향

### 단기 개선 사항 (1-2개월)

1. **딥러닝 모델 도입**
   - KoBERT 기반 텍스트 분류 모델로 태그 분류 정확도 향상
   - Transformer 기반 추상적 요약 시스템 구현

2. **성능 평가 지표 추가**
   - 추천 시스템: Precision@K, Recall@K, NDCG@K
   - 태그 분류: Accuracy, Precision, Recall, F1-score
   - 요약 시스템: ROUGE score

3. **캐싱 시스템 도입**
   - Redis를 활용한 추천 결과 캐싱
   - 인기 게시글 캐싱으로 응답 속도 개선

### 중기 개선 사항 (3-6개월)

1. **고급 추천 알고리즘**
   - Neural Collaborative Filtering (NCF) 도입
   - 딥러닝 기반 임베딩 생성

2. **실시간 알림 시스템**
   - WebSocket을 활용한 실시간 알림
   - 푸시 알림 기능 추가

3. **A/B 테스트 시스템**
   - 추천 알고리즘 성능 비교 실험
   - 사용자 만족도 측정

### 장기 개선 사항 (6개월 이상)

1. **마이크로서비스 아키텍처 전환**
   - 추천 서비스 독립 배포
   - API Gateway 도입

2. **모바일 앱 개발**
   - React Native 기반 모바일 앱
   - 크로스 플랫폼 지원

3. **대규모 확장성**
   - 분산 시스템 구축
   - 로드 밸런싱 및 오토스케일링

---

## 참고 문헌

### 학술 논문

1. Sarwar, B., Karypis, G., Konstan, J., & Riedl, J. (2001). Item-based collaborative filtering recommendation algorithms. *Proceedings of the 10th International Conference on World Wide Web (WWW)*, 285–295.
2. Burke, R. (2002). Hybrid recommender systems: Survey and experiments. *User Modeling and User-Adapted Interaction*, 12(4), 331–370.
3. Adomavicius, G., & Tuzhilin, A. (2005). Toward the next generation of recommender systems: A survey of the state-of-the-art and possible extensions. *IEEE Transactions on Knowledge and Data Engineering*, 17(6), 734–749.
4. Lops, P., De Gemmis, M., & Semeraro, G. (2011). Content-based recommender systems: State of the art and trends. In *Recommender Systems Handbook* (pp. 73–105). Springer.
5. Ding, Y., & Li, X. (2005). Time weight collaborative filtering. *Proceedings of the 14th ACM International Conference on Information and Knowledge Management (CIKM)*, 485–492.
6. Mihalcea, R., & Tarau, P. (2004). TextRank: Bringing order into text. *Proceedings of the 2004 Conference on Empirical Methods in Natural Language Processing (EMNLP)*, 404–411.
7. Nenkova, A., & McKeown, K. (2012). A survey of text summarization techniques. In *Mining Text Data* (pp. 43–76). Springer.
8. Salton, G., & Buckley, C. (1988). Term-weighting approaches in automatic text retrieval. *Information Processing & Management*, 24(5), 513–523.
9. Page, L., Brin, S., Motwani, R., & Winograd, T. (1999). The PageRank citation ranking: Bringing order to the web. *Stanford InfoLab Technical Report*.

### 기술 문서 및 가이드

1. Spring Boot Reference Documentation: `https://spring.io/projects/spring-boot`
2. Spring Data JPA Reference Documentation: `https://spring.io/projects/spring-data-jpa`
3. Spring Security Reference Documentation: `https://spring.io/projects/spring-security`
4. MySQL 8.0 Reference Manual: `https://dev.mysql.com/doc/`
5. Docker & Docker Compose Documentation: `https://docs.docker.com/`
6. Python 3 Standard Library Documentation: `https://docs.python.org/3/`
7. Python `collections` 모듈 문서: `https://docs.python.org/3/library/collections.html`
8. scikit-learn User Guide (머신러닝/추천 알고리즘 참고): `https://scikit-learn.org/stable/user_guide.html`

### 오픈소스 프로젝트 및 참고 자료 (유사 시스템)

1. **Hybrid Recommendation System (Content-based + Collaborative Filtering)**  
   - GitHub: `https://github.com/jhihan/Hybrid-Recommendation-System`  
   - MovieLens 데이터를 활용한 하이브리드 추천 알고리즘 구현 예제로, 본 프로젝트의 하이브리드 추천 설계에 참고.

2. **AI Study Assistant (Flask 기반 학습 보조 웹 앱)**  
   - GitHub: `https://github.com/JiteshShelke/AI-Study-Assistant-Flask`  
   - 텍스트 요약, 질문 생성 등 학습 지원용 NLP 기능을 제공하는 웹 애플리케이션으로, AI 학습 도우미/스터디 플랫폼 기능과 유사.

3. **TextRank 기반 요약 및 키워드 추출 라이브러리**  
   - GitHub: `https://github.com/summanlp/textrank`  
   - Python 3 환경에서 TextRank 알고리즘으로 키워드 추출과 추출적 요약을 제공하며, 본 프로젝트의 규칙 기반 요약/분석 로직 설계 시 참고.

4. **NLTK 기반 텍스트 요약 예제**  
   - GitHub: `https://github.com/colombomf/text-summarizer`  
   - NLTK를 활용한 간단한 텍스트 요약 구현 예제로, Python 기반 요약 파이프라인 구조 설계에 참고.

5. **AI Content Moderation / Filtering 예제 프로젝트**  
   - content-checker (AI 콘텐츠 검열 도구): `https://github.com/utilityfueled/content-checker`  
   - Open-source content moderation toolkit으로, 욕설/부적절 표현 필터링 구조와 정책 설계에 참고.

6. **교육/학습 도메인 추천 관련 GitHub 토픽**  
   - Educational App Projects: `https://github.com/topics/educational-app`  
   - 학습 관리, 퀴즈, 스터디 매칭 등 다양한 교육용 웹/모바일 프로젝트 모음으로, UX/UI 및 기능 구성을 벤치마킹하는 데 활용.

---

## 👥 개발팀

**Study With Me 개발팀**

- 프로젝트 팀장: 이수현
- 프로젝트 리더 및 전체 아키텍처 설계 : 김정욱
- 백엔드 개발 (Spring Boot) : 김정욱 양준모
- AI 시스템 개발 (Python) : 이수현
- 프론트엔드 개발 : 변용현
- 데이터베이스 설계 및 최적화 : 김정욱

---

## 📝 라이선스

이 프로젝트는 Capstone Design 프로젝트로 개발되었습니다.

---

## 🔗 관련 링크

- **GitHub 저장소**: https://github.com/study-withme/ai-website-studywithme
- **빠른 시작 가이드**: [QUICK_START.md](QUICK_START.md)
- **상세 설치 가이드**: [SETUP.md](SETUP.md) (있는 경우)
- **프로젝트 로드맵**: [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md)
- **Python AI 시스템 문서**: [python/README.md](python/README.md)
- **Python 알고리즘 상세 문서**: [python/PythonREADME.md](python/PythonREADME.md)
- **알고리즘 상세 분석**: [python/ALGORITHM_ANALYSIS.md](python/ALGORITHM_ANALYSIS.md) (있는 경우)

---

<div align="center">

**Made with ❤️ by Study With Me Development Team**
*2024-2025 Capstone Design Project*
</div>
