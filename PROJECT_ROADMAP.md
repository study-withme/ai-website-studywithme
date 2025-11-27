# Study With Me 프로젝트 개발 로드맵

## 📊 현재 프로젝트 상태 분석

### ✅ 완료된 기능
1. **기본 인프라**
   - Spring Boot 3.3.4 (Java 21)
   - JPA/Hibernate 설정
   - MySQL 데이터베이스 연결
   - Thymeleaf 템플릿 엔진
   - BCrypt 비밀번호 암호화

2. **사용자 인증**
   - 회원가입 기능
   - 로그인/로그아웃 기능
   - 세션 기반 인증
   - User, UserProfile 엔티티

3. **프론트엔드**
   - 기본 UI 템플릿 (index, auth, register, ai, recommend)
   - 반응형 디자인
   - JavaScript 기반 인터랙션

### ❌ 미구현 기능
1. **데이터베이스 엔티티**
   - Post, Comment 엔티티 없음
   - StudyGroup 관련 엔티티 없음
   - 활동 로그 엔티티 없음
   - AI 관련 엔티티 없음

2. **비즈니스 로직**
   - 게시글 CRUD 기능 없음
   - 댓글 기능 없음
   - 모임 생성/관리 기능 없음
   - AI 추천 로직 없음
   - 활동 로그 수집 기능 없음

3. **API 엔드포인트**
   - REST API 없음 (현재는 페이지 라우팅만)
   - 추천 API 없음
   - 활동 로그 API 없음

---

## 🎯 단계별 개발 계획

### Phase 1: 핵심 엔티티 및 Repository 구현 (1-2주)

#### 1.1 엔티티 클래스 생성
```
entity/
├── Post.java                    # 게시글
├── Comment.java                 # 댓글
├── PostLike.java                # 게시글 좋아요
├── CommentLike.java            # 댓글 좋아요
├── StudyGroup.java              # 스터디 모임
├── StudyGroupMember.java        # 모임 멤버
├── UserActivity.java            # 사용자 활동 로그
├── UserAIPProfile.java          # AI 프로필
├── PostKeyword.java             # 게시글 키워드
├── PostEmbedding.java           # 게시글 임베딩
├── UserEmbedding.java           # 사용자 임베딩
├── UserPostSimilarity.java      # 사용자-게시글 유사도
├── UserUserSimilarity.java      # 사용자-사용자 유사도
├── UserStudyGroupSimilarity.java # 사용자-모임 유사도
├── Recommendation.java          # 추천 결과
└── StudyGroupInvitation.java    # 모임 초대
```

#### 1.2 Repository 인터페이스 생성
- 각 엔티티에 대한 JpaRepository 생성
- 커스텀 쿼리 메서드 추가 (유사도 조회, 추천 조회 등)

**우선순위:**
1. Post, Comment, StudyGroup (핵심 기능)
2. UserActivity (활동 로그 수집)
3. AI 관련 엔티티 (추천 시스템)

---

### Phase 2: 활동 로그 수집 시스템 (1주)

#### 2.1 UserActivityService 구현
```java
@Service
public class UserActivityService {
    // 활동 로그 기록
    void logSearch(Long userId, String keyword);
    void logClick(Long userId, Long targetId, String targetType);
    void logLike(Long userId, Long targetId, String targetType);
    void logRecommend(Long userId, Long targetId, String targetType);
    
    // 활동 통계 조회
    UserActivityStats getActivityStats(Long userId);
}
```

#### 2.2 AOP 또는 인터셉터로 자동 로깅
- 컨트롤러 메서드에 `@LogActivity` 어노테이션 추가
- 자동으로 활동 로그 수집

#### 2.3 활동 로그 수집 포인트
- 게시글 조회 (CLICK)
- 게시글 좋아요 (LIKE)
- 검색 (SEARCH)
- 추천 클릭 (RECOMMEND)

---

### Phase 3: AI 서비스 통합 (2-3주)

#### 3.1 AI 서비스 선택
**옵션 1: OpenAI API (추천)**
- GPT-4를 이용한 텍스트 임베딩
- `text-embedding-3-small` 또는 `text-embedding-3-large` 사용
- 비용: $0.02/1M tokens (small), $0.13/1M tokens (large)

**옵션 2: 한국어 특화 모델**
- KoBERT, KoSimCSE 등
- 자체 서버에 배포 필요

**옵션 3: 하이브리드**
- OpenAI API (프로덕션)
- 로컬 모델 (개발/테스트)

#### 3.2 EmbeddingService 구현
```java
@Service
public class EmbeddingService {
    // 게시글 임베딩 생성
    float[] generatePostEmbedding(Post post);
    
    // 사용자 임베딩 생성 (활동 로그 기반)
    float[] generateUserEmbedding(Long userId);
    
    // 모임 임베딩 생성
    float[] generateStudyGroupEmbedding(StudyGroup group);
    
    // 유사도 계산 (코사인 유사도)
    double calculateSimilarity(float[] vec1, float[] vec2);
}
```

#### 3.3 배치 작업 설정
- Spring Batch 또는 @Scheduled 사용
- 주기적으로 임베딩 업데이트
- 유사도 점수 재계산

---

### Phase 4: 추천 시스템 구현 (2주)

#### 4.1 RecommendationService 구현
```java
@Service
public class RecommendationService {
    // 사용자에게 게시글 추천
    List<PostRecommendation> recommendPosts(Long userId, int limit);
    
    // 사용자에게 모임 추천
    List<StudyGroupRecommendation> recommendStudyGroups(Long userId, int limit);
    
    // 게시글 작성자에게 스터디원 추천
    List<UserRecommendation> recommendStudyPartners(Long postId, int limit);
    
    // 유사도 점수 계산 및 저장
    void calculateAndStoreSimilarities(Long userId);
}
```

#### 4.2 추천 알고리즘
1. **콘텐츠 기반 필터링**
   - 게시글/모임의 키워드, 카테고리 매칭
   - 사용자 활동 로그 기반 선호도 분석

2. **협업 필터링**
   - 유사한 사용자들이 좋아한 게시글/모임 추천
   - UserUserSimilarity 활용

3. **하이브리드 추천**
   - 임베딩 기반 유사도 + 활동 로그 가중치
   - 최종 점수 = (임베딩 유사도 * 0.6) + (활동 기반 점수 * 0.4)

#### 4.3 추천 결과 캐싱
- Redis 또는 인메모리 캐시 사용
- 추천 결과는 1일마다 갱신
- 실시간 활동 반영을 위한 부분 업데이트

---

### Phase 5: 게시글 및 모임 기능 (2주)

#### 5.1 PostService 구현
```java
@Service
public class PostService {
    // 게시글 CRUD
    Post createPost(Long userId, PostCreateRequest request);
    Post updatePost(Long postId, PostUpdateRequest request);
    void deletePost(Long postId);
    Post getPost(Long postId);
    Page<Post> getPosts(PostSearchRequest request);
    
    // 게시글 작성 후 AI 분석 트리거
    void analyzePostWithAI(Post post);
}
```

#### 5.2 StudyGroupService 구현
```java
@Service
public class StudyGroupService {
    // 모임 CRUD
    StudyGroup createGroup(Long creatorId, StudyGroupCreateRequest request);
    void joinGroup(Long groupId, Long userId);
    void leaveGroup(Long groupId, Long userId);
    
    // 모임 초대
    void inviteUser(Long groupId, Long inviterId, Long inviteeId, Long postId);
    void acceptInvitation(Long invitationId);
    void rejectInvitation(Long invitationId);
}
```

#### 5.3 CommentService 구현
- 댓글 CRUD
- 대댓글 지원
- 댓글 좋아요

---

### Phase 6: API 엔드포인트 구현 (1-2주)

#### 6.1 REST API 컨트롤러
```java
@RestController
@RequestMapping("/api")
public class ApiController {
    // 추천 API
    @GetMapping("/recommendations/posts")
    @GetMapping("/recommendations/study-groups")
    @GetMapping("/recommendations/partners/{postId}")
    
    // 게시글 API
    @PostMapping("/posts")
    @GetMapping("/posts")
    @PutMapping("/posts/{id}")
    @DeleteMapping("/posts/{id}")
    
    // 모임 API
    @PostMapping("/study-groups")
    @PostMapping("/study-groups/{id}/join")
    @PostMapping("/study-groups/{id}/invite")
    
    // 활동 로그 API
    @PostMapping("/activities/log")
    @GetMapping("/activities/stats")
}
```

#### 6.2 프론트엔드 연동
- JavaScript에서 Fetch API 사용
- 추천 결과 실시간 업데이트
- 무한 스크롤 또는 페이지네이션

---

### Phase 7: 성능 최적화 및 모니터링 (1주)

#### 7.1 데이터베이스 최적화
- 인덱스 튜닝
- 쿼리 최적화
- N+1 문제 해결 (Fetch Join, @EntityGraph)

#### 7.2 캐싱 전략
- Redis 도입
- 자주 조회되는 추천 결과 캐싱
- 사용자 활동 통계 캐싱

#### 7.3 모니터링
- 로깅 설정 (Logback)
- 성능 모니터링 (Actuator)
- 에러 추적

---

## 🛠 기술 스택 추가 필요

### 필수 의존성
```gradle
// OpenAI API 클라이언트
implementation 'com.theokanning.openai-gpt3-java:service:0.18.2'

// JSON 처리
implementation 'com.fasterxml.jackson.core:jackson-databind'

// 벡터 유사도 계산
implementation 'org.apache.commons:commons-math3:3.6.1'

// 스케줄링
implementation 'org.springframework.boot:spring-boot-starter-quartz'

// 캐싱 (선택)
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### 선택적 의존성
```gradle
// 배치 작업
implementation 'org.springframework.boot:spring-boot-starter-batch'

// 비동기 처리
implementation 'org.springframework.boot:spring-boot-starter-webflux'

// API 문서화
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

---

## 📝 개발 우선순위

### 🔴 High Priority (즉시 시작)
1. **Post, Comment, StudyGroup 엔티티 생성**
2. **기본 CRUD 기능 구현**
3. **활동 로그 수집 시스템**

### 🟡 Medium Priority (1-2주 내)
4. **AI 서비스 통합 (OpenAI API)**
5. **임베딩 생성 및 저장**
6. **기본 추천 로직 구현**

### 🟢 Low Priority (2-4주 내)
7. **고급 추천 알고리즘**
8. **성능 최적화**
9. **모니터링 및 로깅**

---

## 🎨 프론트엔드 개선 사항

### 현재 상태
- 정적 HTML/CSS/JS
- 하드코딩된 데이터
- API 연동 없음

### 개선 필요
1. **API 연동**
   - Fetch API로 백엔드와 통신
   - 동적 데이터 렌더링

2. **사용자 경험 개선**
   - 로딩 상태 표시
   - 에러 처리
   - 무한 스크롤

3. **반응형 디자인 강화**
   - 모바일 최적화
   - 터치 제스처 지원

---

## 🔐 보안 고려사항

1. **API 인증**
   - JWT 토큰 도입 고려
   - 현재는 세션 기반 유지 가능

2. **입력 검증**
   - @Valid 어노테이션 활용
   - XSS 방지

3. **SQL Injection 방지**
   - JPA 사용으로 자동 방지
   - 네이티브 쿼리 사용 시 주의

4. **AI API 키 관리**
   - 환경 변수로 관리
   - .env 파일 사용 (이미 설정됨)

---

## 📈 예상 개발 일정

| Phase | 기간 | 누적 |
|-------|------|------|
| Phase 1: 엔티티 및 Repository | 1-2주 | 2주 |
| Phase 2: 활동 로그 수집 | 1주 | 3주 |
| Phase 3: AI 서비스 통합 | 2-3주 | 5-6주 |
| Phase 4: 추천 시스템 | 2주 | 7-8주 |
| Phase 5: 게시글/모임 기능 | 2주 | 9-10주 |
| Phase 6: API 엔드포인트 | 1-2주 | 10-12주 |
| Phase 7: 최적화 및 모니터링 | 1주 | 11-13주 |

**총 예상 기간: 3-4개월** (풀타임 기준)

---

## 🚀 다음 단계 (즉시 시작 가능)

1. **Post 엔티티 생성** (30분)
2. **PostRepository 생성** (10분)
3. **PostService 기본 구조** (30분)
4. **게시글 목록 API** (1시간)
5. **프론트엔드 연동** (1시간)

**총 3-4시간이면 기본 게시글 기능 완성 가능**

---

## 💡 추가 제안

### 단기 (1개월 내)
- 기본 게시글/댓글 기능
- 활동 로그 수집
- 간단한 추천 (키워드 기반)

### 중기 (2-3개월)
- AI 임베딩 통합
- 고급 추천 알고리즘
- 모임 기능

### 장기 (4-6개월)
- 실시간 알림
- 채팅 기능
- 모바일 앱

---

작성일: 2025-01-11
버전: 1.0

