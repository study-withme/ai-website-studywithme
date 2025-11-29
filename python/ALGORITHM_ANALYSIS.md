# AI 시스템 알고리즘 및 기법 상세 분석

## 1. 시스템 개요

본 프로젝트는 스터디 매칭 플랫폼을 위한 AI 기반 개인화 추천 시스템을 구현합니다. 
총 3개의 Python 스크립트로 구성되어 있으며, 각각 다른 AI/ML 기법을 활용합니다.

### 1.1 시스템 구성
- **ai_recommendation.py**: 사용자 맞춤형 게시글 추천 시스템
- **ai_tag_recommendation.py**: 게시글 태그 및 카테고리 자동 분류 시스템
- **ai_summary.py**: 게시글 본문 자동 요약 시스템

### 1.2 기술 스택
- **언어**: Python 3.x
- **데이터베이스**: MySQL/MariaDB (mysql-connector-python)
- **데이터 처리**: collections (Counter, defaultdict)
- **텍스트 처리**: Regular Expression (re)
- **통신**: JSON (입출력 인터페이스)

---

## 2. 알고리즘 상세 분석

### 2.1 사용자 맞춤형 추천 시스템 (ai_recommendation.py)

#### 2.1.1 알고리즘 분류
- **유형**: 하이브리드 추천 시스템 (Hybrid Recommendation System)
- **구성 요소**:
  1. 콘텐츠 기반 필터링 (Content-Based Filtering)
  2. 협업 필터링 (Collaborative Filtering) 요소
  3. 인기도 기반 추천 (Popularity-Based Recommendation)
  4. 시간 가중치 (Temporal Weighting)

#### 2.1.2 핵심 알고리즘

**A. 사용자 선호도 분석 알고리즘**

```python
# 가중치 기반 선호도 계산
action_weights = {
    'SEARCH': 1.0,      # 검색 행동
    'CLICK': 2.0,       # 클릭 행동
    'LIKE': 3.0,        # 좋아요
    'BOOKMARK': 4.0,    # 북마크
    'COMMENT': 3.5,     # 댓글 작성
    'AI_CLICK': 5.0,    # AI 추천 클릭 (가장 높은 가중치)
    'RECOMMEND': 2.5    # 추천 받음
}
```

**알고리즘 설명**:
- 사용자 활동 로그를 분석하여 각 행동 유형에 가중치를 부여
- 가중치 합산을 통해 카테고리별/태그별 선호도 점수 계산
- 정규화(Normalization)를 통해 0~1 사이의 값으로 변환

**수식**:
```
카테고리 점수 = Σ(액션 가중치 × 액션 횟수) / 총 가중치 합
태그 점수 = Σ(액션 가중치 × 태그 등장 횟수) / 총 가중치 합
```

**B. 게시글 추천 점수 계산 알고리즘**

```python
score = 0
# 1. 카테고리 매칭 점수
if post['category'] in preferences['categories']:
    score += preferences['categories'][post['category']] * 100

# 2. 태그 매칭 점수
for tag in post_tags:
    if tag in preferences['tags']:
        score += preferences['tags'][tag] * 50

# 3. 인기도 점수
score += (post['like_count'] or 0) * 2
score += (post['view_count'] or 0) * 0.1

# 4. 최신성 점수 (최근 7일 내면 보너스)
if days_old <= 7:
    score += 10
```

**알고리즘 설명**:
- 다중 요소 점수화 (Multi-Factor Scoring)
- 가중 합산을 통한 최종 추천 점수 계산
- 콜드 스타트 문제 해결을 위한 인기도 기반 폴백

**수식**:
```
추천 점수 = (카테고리 매칭 × 100) + (태그 매칭 × 50) + (좋아요 × 2) + (조회수 × 0.1) + (최신성 보너스)
```

#### 2.1.3 사용된 데이터 구조
- **defaultdict**: 카테고리/태그별 점수 누적
- **Counter**: 액션 타입별 빈도 계산
- **Dictionary**: 사용자 선호도 프로필 저장

#### 2.1.4 시간 복잡도
- 사용자 활동 조회: O(n) (n = 활동 로그 수)
- 선호도 분석: O(n × m) (m = 평균 태그 수)
- 추천 게시글 조회: O(k × log k) (k = 후보 게시글 수)
- **전체**: O(n × m + k × log k)

#### 2.1.5 공간 복잡도
- O(n + m + k) (n = 활동 수, m = 고유 태그 수, k = 추천 게시글 수)

---

### 2.2 태그 자동 분류 시스템 (ai_tag_recommendation.py)

#### 2.2.1 알고리즘 분류
- **유형**: 규칙 기반 분류 시스템 (Rule-Based Classification)
- **하위 유형**: 키워드 매칭 기반 분류 (Keyword Matching Classification)

#### 2.2.2 핵심 알고리즘

**A. 키워드 추출 알고리즘**

```python
def extract_keywords(self, text: str) -> List[str]:
    # 1. HTML 태그 제거
    text = re.sub(r'<[^>]+>', '', text)
    
    # 2. 특수문자 제거 (한글, 영문, 숫자만)
    text = re.sub(r'[^\w\s가-힣]', ' ', text)
    
    # 3. 공백으로 분리
    words = text.split()
    
    # 4. 2글자 이상인 단어만 필터링
    keywords = [w.strip() for w in words if len(w.strip()) >= 2]
    
    return keywords
```

**알고리즘 설명**:
- 정규표현식을 활용한 텍스트 전처리
- 불용어 제거 (1글자 단어 제외)
- 한국어와 영문 모두 지원

**B. 카테고리 분류 알고리즘**

```python
def recommend_category(self, title: str, content: str) -> Tuple[str, float]:
    full_text = (title + " " + content).lower()
    
    category_scores = {}
    for category, keywords in self.category_keywords.items():
        score = 0
        for keyword in keywords:
            count = full_text.count(keyword.lower())
            score += count * 2  # 키워드 매칭 시 점수
        
        if score > 0:
            category_scores[category] = score
    
    # 가장 높은 점수의 카테고리 선택
    best_category = max(category_scores.items(), key=lambda x: x[1])
    max_score = max(category_scores.values())
    total_score = sum(category_scores.values())
    
    confidence = max_score / total_score if total_score > 0 else 0.0
    
    return best_category[0], min(confidence, 1.0)
```

**알고리즘 설명**:
- 키워드 빈도 기반 점수 계산
- 다중 카테고리 후보 중 최고 점수 선택
- 신뢰도(Confidence) 계산: 최고 점수 / 전체 점수 합

**수식**:
```
카테고리 점수 = Σ(키워드 등장 횟수 × 2)
신뢰도 = 최고 점수 / 전체 점수 합
```

**C. 태그 추천 알고리즘**

```python
# 1. 기술 스택 태그 매칭 (높은 신뢰도: 0.9)
for tech_tag in self.tech_tags:
    if tech_tag.lower() in full_text:
        tag_scores[tech_tag] = 0.9

# 2. 키워드 빈도 분석
keyword_counter = Counter(keywords)
top_keywords = keyword_counter.most_common(10)

for keyword, count in top_keywords:
    if len(keyword) >= 2:
        score = min(count / 5.0, 0.8)  # 최대 0.8
        tag_scores[keyword] = score

# 3. 카테고리 관련 키워드 추가 (신뢰도: 0.7)
if category in self.category_keywords:
    for keyword in self.category_keywords[category]:
        if keyword.lower() in full_text:
            tag_scores[keyword] = 0.7
```

**알고리즘 설명**:
- 3단계 태그 추천 시스템
  1. 기술 스택 매칭 (정확도 높음)
  2. 키워드 빈도 분석 (TF 기반)
  3. 카테고리 관련 키워드 보강
- 신뢰도 기반 태그 필터링 (0.5 이상만 추천)

#### 2.2.3 사용된 데이터 구조
- **Dictionary**: 카테고리별 키워드 사전
- **List**: 기술 스택 태그 목록
- **Counter**: 키워드 빈도 계산

#### 2.2.4 시간 복잡도
- 키워드 추출: O(n) (n = 텍스트 길이)
- 카테고리 분류: O(c × k) (c = 카테고리 수, k = 키워드 수)
- 태그 추천: O(t + k) (t = 기술 태그 수, k = 키워드 수)
- **전체**: O(n + c × k + t)

#### 2.2.5 한계점
- 딥러닝 미사용으로 인한 정확도 제한
- 사전 정의된 키워드에 의존
- 문맥 이해 부족

---

### 2.3 텍스트 요약 시스템 (ai_summary.py)

#### 2.3.1 알고리즘 분류
- **유형**: 추출적 요약 (Extractive Summarization)
- **하위 유형**: 휴리스틱 기반 문장 선택 (Heuristic-Based Sentence Selection)

#### 2.3.2 핵심 알고리즘

**A. 문장 중요도 점수 계산 알고리즘**

```python
def calculate_sentence_score(self, sentence: str, keywords: List[str]) -> float:
    score = 0.0
    
    # 1. 키워드 포함 여부
    for keyword in keywords:
        if keyword.lower() in sentence_lower:
            score += 2.0
    
    # 2. 문장 길이 가중치
    length = len(sentence)
    if 20 <= length <= 100:
        score += 1.0
    elif length < 10 or length > 150:
        score -= 0.5
    
    return score
```

**알고리즘 설명**:
- 키워드 빈도 기반 점수화
- 이상적인 문장 길이 범위에 가중치 부여
- 너무 짧거나 긴 문장은 감점

**B. 위치 가중치 알고리즘**

```python
# 앞부분 문장에 가중치
position_weight = 1.0 - (i / len(sentences)) * 0.3
score *= position_weight
```

**알고리즘 설명**:
- 문서 앞부분 문장에 높은 가중치 부여
- 일반적으로 문서의 앞부분이 핵심 내용을 포함한다는 가정
- 선형 감소 함수 사용

**수식**:
```
위치 가중치 = 1.0 - (문장 인덱스 / 전체 문장 수) × 0.3
최종 점수 = 문장 점수 × 위치 가중치
```

**C. 요약 생성 알고리즘**

```python
# 1. 문장 점수 계산 및 정렬
sentence_scores.sort(key=lambda x: x[1], reverse=True)

# 2. 최대 길이 내에서 상위 문장 선택
summary_sentences = []
current_length = 0

for sentence, score, original_index in sentence_scores:
    if current_length + len(sentence) <= max_length:
        summary_sentences.append((original_index, sentence))
        current_length += len(sentence) + 1
    else:
        break

# 3. 원래 순서대로 정렬
summary_sentences.sort(key=lambda x: x[0])
summary = ' '.join([s for _, s in summary_sentences])
```

**알고리즘 설명**:
- 그리디 알고리즘 (Greedy Algorithm) 기반 문장 선택
- 점수 순으로 정렬 후 최대 길이 내에서 선택
- 원문 순서 유지를 통한 가독성 향상

#### 2.3.3 사용된 데이터 구조
- **List**: 문장 리스트 및 점수 저장
- **Dictionary**: 단어 빈도 계산

#### 2.3.4 시간 복잡도
- 텍스트 정리: O(n) (n = 텍스트 길이)
- 문장 추출: O(n)
- 키워드 추출: O(n)
- 문장 점수 계산: O(s × k) (s = 문장 수, k = 키워드 수)
- 정렬: O(s × log s)
- **전체**: O(n + s × k + s × log s)

#### 2.3.5 한계점
- 추상적 요약 불가 (추출적 요약만 가능)
- 문맥 이해 부족
- 딥러닝 미사용으로 인한 품질 제한

---

## 3. 딥러닝 시스템 여부 분석

### 3.1 현재 상태
**결론: 현재 시스템은 딥러닝을 사용하지 않습니다.**

### 3.2 근거
1. **신경망 모델 부재**: 모든 스크립트에서 딥러닝 라이브러리(TensorFlow, PyTorch, Keras 등) 미사용
2. **규칙 기반 시스템**: 사전 정의된 규칙과 휴리스틱에 의존
3. **통계적 방법**: 빈도 분석, 가중치 합산 등 전통적인 통계 기법 사용
4. **학습 과정 부재**: 모델 학습, 최적화, 검증 과정 없음

### 3.3 현재 시스템의 정확한 분류
- **ai_recommendation.py**: 하이브리드 추천 시스템 (협업 필터링 + 콘텐츠 기반)
- **ai_tag_recommendation.py**: 규칙 기반 분류 시스템
- **ai_summary.py**: 휴리스틱 기반 추출적 요약 시스템

---

## 4. 졸업논문 주제 적합성 평가

### 4.1 현재 상태 평가

#### ✅ 장점
1. **실용적 시스템**: 실제 웹 애플리케이션과 통합된 실용적 시스템
2. **다양한 AI 기법**: 추천, 분류, 요약 등 다양한 AI/ML 기법 포함
3. **완전한 구현**: 이론뿐만 아니라 실제 동작하는 시스템
4. **데이터 수집**: 사용자 활동 로그 수집 및 활용

#### ⚠️ 개선 필요 사항
1. **딥러닝 부재**: 최신 딥러닝 기법 미적용
2. **성능 평가 부재**: 정확도, 재현율, F1-score 등 정량적 평가 지표 없음
3. **실험 및 비교 부재**: 다른 알고리즘과의 성능 비교 없음
4. **이론적 배경 부족**: 사용된 알고리즘의 이론적 근거 설명 부족

### 4.2 졸업논문 주제로의 개선 방향

#### 방향 1: 딥러닝 기반 시스템으로 전환
**제목 예시**: "딥러닝 기반 스터디 매칭 플랫폼의 개인화 추천 시스템"

**개선 사항**:
- **추천 시스템**: Matrix Factorization → Neural Collaborative Filtering (NCF)
- **태그 분류**: 규칙 기반 → BERT/KoBERT 기반 텍스트 분류
- **요약 시스템**: 휴리스틱 → Transformer 기반 요약 (BART, T5)

**필요 라이브러리**:
```python
# requirements.txt에 추가
torch>=2.0.0
transformers>=4.30.0
sentencepiece>=0.1.99
sklearn>=1.3.0
numpy>=1.24.0
```

#### 방향 2: 하이브리드 시스템 비교 연구
**제목 예시**: "전통적 ML과 딥러닝 기반 추천 시스템의 성능 비교 연구"

**연구 내용**:
- 현재 규칙 기반 시스템 vs 딥러닝 시스템 성능 비교
- 정확도, 재현율, F1-score, NDCG 등 지표 비교
- 사용자 만족도 설문 조사

#### 방향 3: 실시간 학습 시스템
**제목 예시**: "온라인 학습 기반 개인화 추천 시스템 구현"

**개선 사항**:
- 사용자 피드백을 실시간으로 반영하는 온라인 학습
- 강화학습(Reinforcement Learning) 적용
- A/B 테스트를 통한 성능 검증

### 4.3 권장 사항

**최소 요구사항 (졸업논문 수준)**:
1. ✅ 성능 평가 지표 추가 (정확도, 재현율, F1-score)
2. ✅ 실험 데이터셋 구축 및 평가
3. ✅ 베이스라인 알고리즘과의 비교
4. ✅ 사용자 만족도 조사

**권장 사항 (더 나은 논문)**:
1. ⭐ 딥러닝 모델 1개 이상 도입 (태그 분류 또는 요약)
2. ⭐ 실험 결과 및 분석 섹션 추가
3. ⭐ 관련 연구 리뷰 (Related Work)
4. ⭐ 시스템 아키텍처 다이어그램

---

## 5. 사용된 알고리즘 및 기법 요약

### 5.1 ai_recommendation.py

| 기법 | 설명 | 참고 논문/이론 |
|------|------|---------------|
| **협업 필터링** | 사용자 활동 패턴 분석 | "Item-based Collaborative Filtering" (Sarwar et al., 2001) |
| **콘텐츠 기반 필터링** | 카테고리/태그 매칭 | "Content-Based Recommendation Systems" (Lops et al., 2011) |
| **가중치 기반 점수화** | 액션 타입별 가중치 부여 | "Weighted Hybrid Recommendation" (Burke, 2002) |
| **시간 가중치** | 최신성 반영 | "Temporal Collaborative Filtering" (Ding & Li, 2005) |
| **정규화** | 점수 범위 정규화 | Min-Max Normalization |

### 5.2 ai_tag_recommendation.py

| 기법 | 설명 | 참고 논문/이론 |
|------|------|---------------|
| **키워드 매칭** | 사전 정의된 키워드 매칭 | "Rule-Based Classification" |
| **TF (Term Frequency)** | 키워드 빈도 분석 | "TF-IDF" (Salton & Buckley, 1988) |
| **다중 분류** | 여러 카테고리 후보 중 선택 | "Multi-Class Classification" |
| **신뢰도 계산** | 분류 결과의 신뢰도 측정 | "Confidence Score" |

### 5.3 ai_summary.py

| 기법 | 설명 | 참고 논문/이론 |
|------|------|---------------|
| **추출적 요약** | 원문 문장 선택 | "Extractive Summarization" (Nenkova & McKeown, 2012) |
| **문장 점수화** | 키워드 기반 중요도 계산 | "Sentence Scoring" (Edmundson, 1969) |
| **위치 가중치** | 문서 위치 기반 가중치 | "Position-Based Weighting" |
| **그리디 알고리즘** | 최적 문장 선택 | "Greedy Algorithm" |

---

## 6. 코드 품질 평가

### 6.1 장점
1. ✅ **모듈화**: 클래스 기반 구조로 재사용성 높음
2. ✅ **에러 처리**: try-except 블록으로 예외 처리
3. ✅ **문서화**: docstring으로 함수 설명
4. ✅ **타입 힌트**: Python 타입 힌트 사용
5. ✅ **JSON 인터페이스**: 표준화된 입출력 형식

### 6.2 개선 필요 사항
1. ⚠️ **로깅**: print 대신 logging 모듈 사용 권장
2. ⚠️ **설정 파일**: 하드코딩된 DB 설정을 환경 변수로 분리
3. ⚠️ **단위 테스트**: 테스트 코드 부재
4. ⚠️ **성능 최적화**: 대용량 데이터 처리 시 성능 이슈 가능

---

## 7. 결론 및 제안

### 7.1 현재 시스템 평가
- **기술 수준**: 중급 (전통적 ML 기법 활용)
- **논문 적합성**: 보통 (개선 필요)
- **실용성**: 높음 (실제 시스템 통합)
- **확장성**: 보통 (딥러닝 도입 시 확장 가능)

### 7.2 졸업논문을 위한 최소 개선 사항
1. **성능 평가 지표 추가**
   - 추천 시스템: Precision@K, Recall@K, NDCG@K
   - 태그 분류: Accuracy, Precision, Recall, F1-score
   - 요약 시스템: ROUGE score

2. **실험 설계**
   - 테스트 데이터셋 구축
   - 교차 검증 (Cross-Validation)
   - 베이스라인 비교

3. **문서화 강화**
   - 알고리즘 이론적 배경 설명
   - 실험 결과 분석
   - 한계점 및 향후 연구 방향

### 7.3 권장 개선 사항 (더 나은 논문)
1. **딥러닝 모델 도입** (최소 1개)
   - 태그 분류: KoBERT fine-tuning
   - 요약: KoBART 또는 KoT5

2. **하이브리드 시스템**
   - 규칙 기반 + 딥러닝 앙상블
   - 성능 비교 실험

3. **실시간 학습**
   - 사용자 피드백 반영
   - 온라인 학습 알고리즘

---

## 8. 참고 문헌

1. Sarwar, B., Karypis, G., Konstan, J., & Riedl, J. (2001). Item-based collaborative filtering recommendation algorithms. *Proceedings of the 10th international conference on World Wide Web*.

2. Burke, R. (2002). Hybrid recommender systems: Survey and experiments. *User modeling and user-adapted interaction*, 12(4), 331-370.

3. Lops, P., De Gemmis, M., & Semeraro, G. (2011). Content-based recommender systems: State of the art and trends. *Recommender systems handbook*, 73-105.

4. Nenkova, A., & McKeown, K. (2012). A survey of text summarization techniques. *Mining text data*, 43-76.

5. Salton, G., & Buckley, C. (1988). Term-weighting approaches in automatic text retrieval. *Information processing & management*, 24(5), 513-523.

---

**작성일**: 2024년
**버전**: 1.0
**작성자**: AI 시스템 분석

