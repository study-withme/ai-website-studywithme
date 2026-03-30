# 도메인: `user` — 회원·온보딩·추천·활동

## 역할

- 회원가입/로그인/로그아웃, 마이페이지, 프로필 이미지
- AI 온보딩(카테고리 선택) 완료 시 **선호도·활동 로그**를 한 트랜잭션에 가깝게 묶는 **application 계층**
- 하이브리드 추천용 **게시글 추천 API**, **사용자 선호도 API**

## 패키지 구조

```
user/
├── application/     # UserOnboardingApplicationService (@Transactional 시나리오)
├── controller/      # UserWebController (@Controller + 일부 @ResponseBody)
├── entity/          # User, UserProfile, UserPreference, UserActivity, ...
├── repository/
└── service/         # UserService, UserActivityService, UserRecommendationService, ...
```

## 사용한 Spring / JPA 어노테이션 (요약)

| 위치 | 어노테이션 | 용도 |
|------|------------|------|
| `UserWebController` | `@Controller`, `@RequiredArgsConstructor` | MVC, 생성자 주입 |
| | `@GetMapping`, `@PostMapping` | 라우팅 |
| | `@RequestParam`, `@ResponseBody` | 폼 파라미터·JSON API |
| `UserOnboardingApplicationService` | `@Service`, `@Transactional` | 온보딩 유스케이스, 쓰기 트랜잭션 |
| `User` 엔티티 | `@Entity`, `@Table`, `@Id`, `@GeneratedValue` | JPA 매핑 |
| | `@Column` | 컬럼 제약·길이 |
| Repository | `JpaRepository` (인터페이스) | CRUD·쿼리 메서드 |

## 요청 흐름 (온보딩 예시)

1. 사용자가 `GET /ai` 로 온보딩 화면 진입 (세션에 로그인 필요).
2. `POST /ai/complete` + `categories` 리스트 → `UserWebController.completeAiProfile`.
3. `UserOnboardingApplicationService.completeAiProfile` 호출:
   - `UserActivityService.logAIClick` — 추천/로그용 활동 기록
   - 기존 `UserPreference` 삭제 후 카테고리 매핑(`mapCategoriesToActualCategories`)으로 실제 카테고리명에 맞춰 `UserPreference` 재저장

## 주요 엔드포인트

### MVC (Thymeleaf)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/auth` | 로그인 페이지 |
| GET | `/register` | 회원가입 페이지 |
| POST | `/auth` | 로그인 (`email`, `password`) → 세션에 `loginUser` |
| POST | `/register` | 회원가입 후 자동 로그인 → `redirect:/ai` |
| GET | `/logout` | `session.invalidate()` |
| GET | `/ai` | AI 온보딩 화면 |
| POST | `/ai/complete` | 온보딩 완료 (`categories` 다중 파라미터) |
| GET | `/recommend` | 추천 페이지 |
| GET | `/mypage` | 마이페이지 (페이징 파라미터 `page`, `size`) |
| POST | `/mypage/profile-image` | 프로필 이미지 URL 갱신 |

### JSON API (`@ResponseBody`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/recommendations/posts?size=10` | 추천 게시글 목록 (비로그인 시 `userId` null로 동작) |
| GET | `/api/user/preferences` | 로그인 사용자의 `UserPreference` 리스트 (미로그인 시 `[]`) |

## 데이터·성능·설계 포인트

- **BCrypt**: `SecurityConfig`의 `BCryptPasswordEncoder` 빈을 통해 비밀번호 해시 저장.
- **추천**: `UserRecommendationService`에서 Python 스크립트·DB 조회 등을 조합 (상세는 [ai.md](./ai.md)와 연계).
- **활동 로그**: 검색·댓글·게시글 조회 등은 각 도메인 컨트롤러/서비스에서 `UserActivityService` 호출로 수집.
- **페이징**: 마이페이지 등에서 `PageRequest` 사용.

## Postman 예시 (세션 필요 API)

1. **로그인**  
   - `POST http://localhost:8080/auth`  
   - Body: `x-www-form-urlencoded` — `email`, `password`  
   - 예상: `302` + `Set-Cookie: JSESSIONID=...`

2. **선호도 조회**  
   - `GET http://localhost:8080/api/user/preferences`  
   - Headers: `Cookie: JSESSIONID=...`  
   - 예상: `200`, JSON 배열 `[{ "id", "categoryName", "preferenceScore", ... }, ...]`

3. **추천 게시글**  
   - `GET http://localhost:8080/api/recommendations/posts?size=5`  
   - 쿠키 있으면 개인화, 없으면 전역/인기 로직에 따른 리스트  
   - 예상: `200`, `Post` 직렬화 배열 (필드는 Jackson 설정·엔티티 관계에 따름)

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| `/api/user/preferences` 가 항상 `[]` | 미로그인 또는 선호 미설정 | 먼저 로그인 후 `/ai/complete` 또는 DB에 `user_preferences` 확인 |
| 온보딩 후 추천이 약함 | 카테고리 문자열이 매핑 규칙에 안 맞음 | `UserOnboardingApplicationService.mapCategoriesToActualCategories` 규칙 확인 |
| 로그인 후 302만 보임 | Postman이 리다이렉트 따라가기 | Settings에서 리다이렉트 허용 또는 Location URL로 수동 이동 |
