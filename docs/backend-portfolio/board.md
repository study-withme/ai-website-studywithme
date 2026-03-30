# 도메인: `board` — 게시글·피드·좋아요·북마크·지원

## 역할

- 메인 피드(`/`): 카테고리·검색·정렬, 자격증 일정 노출
- 게시글 상세/작성/수정/삭제, 좋아요·북마크·스터디 지원(어플리케이션)
- REST: 공개 게시글 페이지 JSON (`/api/posts`)
- 일부 AI 요약은 `PostWebController`에서 처리 (본문 기반)

## 패키지

```
board/
├── controller/   # HomeController, PostWebController
├── entity/       # Post, PostLike, Bookmark, PostApplication, Certification, ...
├── repository/
└── service/      # PostService, PostLikeService, BookmarkService, PostApplicationService, ...
```

## 사용한 어노테이션 (요약)

| 클래스 | 어노테이션 |
|--------|------------|
| `HomeController`, `PostWebController` | `@Controller`, `@RequiredArgsConstructor`, `@Slf4j` |
| | `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam` |
| | `@ResponseBody` (JSON API 구간) |
| 엔티티 | `@Entity`, `@ManyToOne`, `@OneToMany` 등 (관계형 모델) |
| 서비스 | `@Service`, `@Transactional` (읽기 전용/쓰기 구분) |

## 흐름: 메인 피드 (`GET /`)

1. `HomeController.index` — 세션에서 `loginUser` optional.
2. `category` → `postService.getPostsByCategory`
3. `keyword` → `postService.searchPosts` + 로그인 시 `userActivityService.logSearch`
4. 그 외 → `postService.getPosts(pageable, sort)`
5. 예외 시 빈 `Page`로 폴백 후 `index` 템플릿 렌더.

## 흐름: 게시글 상세 (`GET /posts/{id}`)

1. `PostWebController.viewPost` — `Post` 로드, 로그인 시 좋아요/북마크/지원 여부·작성자 통계·프로필 등 모델에 주입.
2. `userActivityService.logViewPost` — 추천용 조회 로그.

## 주요 엔드포인트

### 페이지 (MVC)

| 메서드 | 경로 | 비고 |
|--------|------|------|
| GET | `/` | 피드 |
| GET | `/posts/write` | 작성 (로그인 필요) |
| GET | `/posts/{id}` | 상세 |
| GET | `/posts/{id}/edit` | 수정 폼 |
| POST | `/posts/write` | 작성 처리 |
| POST | `/posts/{id}/edit` | 수정 처리 |
| POST | `/posts/{id}/delete` | 삭제 |
| POST | `/posts/{id}/like` | 좋아요 토글 |
| POST | `/posts/{id}/bookmark` | 북마크 토글 |
| POST | `/posts/{id}/apply` | 스터디 지원 |
| POST | `/posts/{id}/cancel-apply` | 지원 취소 |
| POST | `/posts/{id}/ai-summary` | (화면용) AI 요약 |
| GET | `/bookmarks` | 북마크 목록 |
| GET | `/my-applications` | 내 지원 목록 |
| GET | `/posts/{id}/applications` | 작성자용 지원자 목록 |
| POST | `/api/applications/{id}/accept` | 지원 수락 |
| POST | `/api/applications/{id}/reject` | 지원 거절 |

### JSON

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/posts?page=&size=&sort=&category=` | `Page<Post>` JSON (Jackson 직렬화) |

## 데이터·성능

- **페이징**: `Pageable` / `PageRequest`로 목록 부하 제한.
- **N+1 완화**: 필요 시 서비스·리포지토리에서 `fetch join` 또는 배치 크기(프로젝트 설정에 따름).
- **필터 연동**: 게시글 저장 시 `ContentFilterService` 호출 여부는 `PostService` 구현을 따름 (차단 시 예외 또는 차단 엔티티 저장).

## Postman 예시

**공개 피드 (세션 불필요)**

```http
GET http://localhost:8080/api/posts?page=0&size=10&sort=latest
```

예상: `200`, Spring Data `Page` JSON 구조 (`content`, `totalElements`, `totalPages`, …).

**좋아요 (세션 필요)**

```http
POST http://localhost:8080/posts/1/like
Cookie: JSESSIONID=...
```

예상: `302` 또는 AJAX라면 프로젝트 프론트 구현에 따름 — Thymeleaf 폼 제출이면 리다이렉트.

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| `/api/posts` JSON 깨짐/순환 참조 | 엔티티 양방향 직렬화 | `@JsonIgnore` 또는 DTO 분리 검토 |
| 검색 시 추천에 반영 안 됨 | 비로그인 검색 | 로그인 상태에서 검색해야 `logSearch` 기록 |
| 게시글 작성 실패 메시지 | 필터 차단 | 관리자에서 필터 규칙·차단 로그 확인 ([moderation.md](./moderation.md)) |
