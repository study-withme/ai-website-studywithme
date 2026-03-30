# 도메인: `comment` — 댓글 REST·필터·알림 연동

## 역할

- 게시글별 댓글 목록/작성 (대댓글 `parentId` 지원)
- 댓글 좋아요 토글
- 작성 시 **콘텐츠 필터** + **알림** + **활동 로그**와 연계

## 패키지

```
comment/
├── controller/   # CommentApiController (@RestController)
├── entity/       # Comment, CommentLike, CommentLikeId
├── repository/
└── service/      # CommentService
```

## 사용한 어노테이션

| 클래스 | 어노테이션 |
|--------|------------|
| `CommentApiController` | `@RestController`, `@RequiredArgsConstructor` |
| | `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam` |
| `CommentService` | `@Service`, `@Transactional`, `@Transactional(readOnly = true)` |

## 처리 흐름

### 목록 조회 `GET /api/posts/{postId}/comments`

1. `commentRepository`로 게시글의 미삭제 댓글 조회.
2. `BlockedCommentRepository`에서 해당 `postId`의 **차단 유지 중** 댓글 ID 수집 후 스트림으로 제외.
3. `sort=popular` 이면 좋아요 수 기준, 아니면 최신(id 역순) 등 정렬.
4. 로그인 사용자 ID가 있으면 `CommentResponse`에 좋아요 여부 등 반영.

### 작성 `POST /api/posts/{postId}/comments`

1. 세션에서 `loginUser` 없으면 `{ "success": false, "message": "로그인이 필요합니다." }`.
2. `CommentService.addComment`:
   - `ContentFilterService.filterComment` — 차단 시 `RuntimeException` → 컨트롤러에서 `success: false` 메시지.
   - 통과 시 `Comment` 저장, 필요 시 `NotificationService`로 알림.
3. 컨트롤러에서 `UserActivityService.logComment` 호출.

### 좋아요 `POST /api/comments/{id}/like`

- 로그인 검증 후 `commentService.toggleLike` → `{ "success", "liked" }`.

## API 요약

| 메서드 | 경로 | 파라미터 | 응답 형태 |
|--------|------|----------|-----------|
| GET | `/api/posts/{postId}/comments` | `sort` (기본 `latest`) | `CommentResponse[]` JSON |
| POST | `/api/posts/{postId}/comments` | `content`, optional `parentId` | `{ success, comment? }` 또는 `{ success, message }` |
| POST | `/api/comments/{id}/like` | — | `{ success, liked? }` |

## 데이터·성능

- 목록 조회 시 차단 ID를 매번 `findAll()` 스트림 필터링하는 구조는 **소규모에 적합**. 데이터가 커지면 `postId` 기반 쿼리로 최적화하는 편이 좋음.
- `@Transactional(readOnly = true)` 로 조회 최적화 힌트.

## Postman 예시

**목록**

```http
GET http://localhost:8080/api/posts/1/comments?sort=latest
```

**작성 (로그인 쿠키 필요)**

```http
POST http://localhost:8080/api/posts/1/comments
Content-Type: application/x-www-form-urlencoded

content=안녕하세요&parentId=
```

예상 성공: `200`, 본문 예시:

```json
{
  "success": true,
  "comment": { "id": 10, "content": "...", "likeCount": 0, ... }
}
```

필터 차단 시:

```json
{
  "success": false,
  "message": "댓글이 차단되었습니다: ..."
}
```

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| 댓글이 목록에 안 보임 | `BlockedComment`에 등록됨 | 관리자 복구 플로우 확인 |
| 항상 로그인 필요 메시지 | `JSESSIONID` 누락 | Postman Cookie 저장소 확인 |
| `parentId` 대댓글 실패 | 잘못된 부모 ID | 부모 댓글이 같은 `postId`인지 확인 |
