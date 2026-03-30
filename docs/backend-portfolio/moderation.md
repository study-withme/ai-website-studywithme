# 도메인: `moderation` — 필터·차단·관리자

## 역할

- **ContentFilterService**: 게시글/댓글 **3단계 필터** (욕설 단어 → 키워드 EXACT/PARTIAL/REGEX → 패턴 정규식), HTML 태그 제거 후 검사, 차단 시 `BlockedPost` / `BlockedComment` / `AILearningData` 등 저장
- **AdminController**: `@Controller` + Thymeleaf — 차단 목록, 필터 규칙 CRUD, AI 학습 데이터 열람, 일괄 재분류 등
- **AdminService**: 통계·페이징 조회

## 패키지

```
moderation/
├── controller/   # AdminController — @RequestMapping("/admin")
├── entity/       # FilterWord, FilterKeyword, FilterPattern, BlockedPost, BlockedComment, AILearningData
├── repository/
└── service/      # ContentFilterService, AdminService
```

## 사용한 어노테이션

| 구분 | 어노테이션 |
|------|------------|
| MVC | `@Controller`, `@RequestMapping("/admin")`, `@GetMapping`, `@PostMapping` |
| | `@RequestParam`, `Model`, `HttpSession` |
| 서비스 | `@Service`, `@Transactional` |
| 페이징 | `Page<T>`, `Pageable` (관리자 목록) |

## 권한

- `isAdmin(session)`: `User.isAdmin()` — `role == 1`
- 비관리자는 `redirect:/?error=admin_required`

## 관리자 주요 라우트 (요약)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/admin` | 대시보드·통계 |
| GET | `/admin/blocked-posts` | 차단 게시글 페이징 |
| POST | `/admin/blocked-posts/{id}/restore` | 복구 |
| GET/POST | `/admin/filter-words`, `filter-keywords`, `filter-patterns` | 규칙 관리 |
| GET | `/admin/ai-learning` | 학습 데이터 |
| GET | `/admin/blocked-comments` | 차단 댓글 |
| POST | `/admin/blocked-comments/{id}/restore` | 댓글 복구 |
| POST | `/admin/reclassify-posts` | 게시글 재분류 배치 |
| POST | `/admin/fix-post-categories-by-tags` | 태그 기준 카테고리 보정 |

## 다른 도메인과의 연결

- **board** / **comment**: 저장 전·후 `ContentFilterService` 호출로 차단.
- **comment** 목록: 차단된 댓글 ID 필터링 ([comment.md](./comment.md)).

## 데이터·성능

- 필터 시 **매 요청마다** `findByIsActiveTrue()` 등으로 규칙 전량 로드 → 규칙 수가 많아지면 **캐시**(Spring Cache) 고려.
- 정규식은 **컴파일 비용** — 패턴 엔티티에서 미리 `Pattern.compile` 캐싱하는 방식이 유리할 수 있음.

## Postman / 브라우저

- 관리자 기능은 **폼 POST** + 세션 + **역할**이 필요해 Postman보다 **브라우저 로그인 후** 검증하기 쉬움.
- API화되어 있지 않은 엔드포인트가 많음.

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| 관리자 페이지 리다이렉트 | role 일반 사용자 | DB `users.role` 또는 시드 데이터 확인 |
| 정상 텍스트 차단 | 키워드 과다 매칭 | `PARTIAL` 규칙·오탐 로그 확인 |
| 필터는 통과했는데 목록에 없음 | 다른 조건(삭제 플래그 등) | 게시글/댓글 엔티티 상태 확인 |
