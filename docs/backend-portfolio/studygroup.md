# 도메인: `studygroup` — 그룹·세션·채팅·출석·커리큘럼·캘린더

## 역할

- 스터디 그룹 **REST API** 중심 (`/api/study-groups/...`): 멤버, 채팅, 학습 세션 시작/종료, 출석, 학습 일지, 목표, 설정, 커리큘럼, 자료, 그룹 내 댓글, 그룹 CRUD
- **캘린더 API** (`/api/study-groups/{groupId}/calendar/...`)
- **화면 라우트**: `StudyGroupWebController` — 목록/상세 페이지

## 패키지

```
studygroup/
├── controller/
│   ├── StudyGroupApiController.java      # @RequestMapping("/api/study-groups")
│   ├── StudyGroupCalendarApiController.java
│   └── StudyGroupWebController.java      # Thymeleaf
├── entity/
├── repository/
└── service/
```

**연관**: `studysession` 도메인의 `StudySessionService`, `StudyJournalService` 등을 API 컨트롤러에서 주입해 사용.

## 사용한 어노테이션

| 구분 | 어노테이션 |
|------|------------|
| REST | `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` |
| | `@PathVariable`, `@RequestParam`, `ResponseEntity<T>` |
| MVC | `@Controller`, `@GetMapping` |
| 공통 | `@RequiredArgsConstructor` (Lombok 생성자 주입) |

## 인증 패턴

- 대부분 API: `HttpSession` → `(User) session.getAttribute("loginUser")`.
- 미로그인 시 `401` + `{ "error": "로그인이 필요합니다." }` 패턴 다수.

## API 맵 (StudyGroupApiController, base: `/api/study-groups`)

> 그룹·멤버 권한 검증은 서비스 레이어에서 수행.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/{groupId}/sessions/start` | 학습 세션 시작 + 시스템 채팅 메시지 |
| POST | `/sessions/{sessionId}/end` | 세션 종료 |
| POST | `/sessions/{sessionId}/attendance` | 출석 체크 |
| POST | `/{groupId}/chat` | 채팅 메시지 전송 |
| GET | `/{groupId}/chat` | 채팅 이력 |
| GET | `/{groupId}/members` | 멤버 목록 |
| POST | `/{groupId}/journal` | 학습 일지 작성 |
| GET | `/{groupId}/journal` | 일지 목록 |
| GET | `/{groupId}/journal/{userId}` | 사용자별 일지 |
| GET | `/{groupId}/goals/check` | 목표 달성 체크 |
| POST | `/{groupId}/status` | 온라인/상태 메시지 |
| GET | `/sessions/active` | 활성 세션 조회 |
| POST | `/sessions/{sessionId}/cycle` | 포모도로 사이클 등 |
| GET | `/sessions/{sessionId}/attendance` | 세션 출석 조회 |
| GET | `/{groupId}/settings` | 설정 조회 |
| GET | `/{groupId}/curriculum` | 커리큘럼 |
| POST | `/{groupId}/curriculum` | 커리큘럼 추가 |
| PUT | `/curriculum/{curriculumId}` | 수정 |
| DELETE | `/curriculum/{curriculumId}` | 삭제 |
| GET | `/{groupId}/resources` | 자료 목록 |
| POST | `/{groupId}/resources` | 자료 추가 |
| PUT | `/resources/{resourceId}` | 수정 |
| DELETE | `/resources/{resourceId}` | 삭제 |
| GET | `/{groupId}/comments` | 그룹 댓글 |
| POST | `/{groupId}/comments` | 댓글 작성 |
| PUT | `/comments/{commentId}` | 수정 |
| DELETE | `/comments/{commentId}` | 삭제 |
| GET | `/{groupId}` | 그룹 상세 |
| PUT | `/{groupId}` | 그룹 수정 |
| DELETE | `/{groupId}` | 그룹 삭제 |
| POST | `/{groupId}/leave` | 그룹 나가기 |
| POST | `/{groupId}/members/{userId}/remove` | 멤버 강퇴 |

## 캘린더 API (base: `/api/study-groups/{groupId}/calendar`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/events` | 일정 생성 |
| PUT | `/events/{eventId}` | 수정 |
| DELETE | `/events/{eventId}` | 삭제 |
| GET | `/events/date/{date}` | 일별 |
| GET | `/events/month/{year}/{month}` | 월별 |

## 화면 라우트

| GET | 경로 |
|-----|------|
| | `/study-groups` |
| | `/study-groups/{id}` |

## 데이터·성능

- 세션·채팅·출석 등 **다수 엔티티**가 한 플로우에 연결되므로, 트랜잭션 경계는 각 `@Transactional` 서비스 메서드에 따름.
- 채팅 목록 조회는 **시간 역순·제한 개수** 등 서비스 구현에 따라 성능이 갈림 — 대량 시 페이징 권장.

## Postman 예시

**세션 시작**

```http
POST http://localhost:8080/api/study-groups/1/sessions/start
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=...

statusMessage=집중 모드
```

예상: `200`, `{ "success": true, "session": { "id", "startTime", "statusMessage" } }`  
미로그인: `401`, `{ "error": "로그인이 필요합니다." }`

**채팅 조회**

```http
GET http://localhost:8080/api/study-groups/1/chat
Cookie: JSESSIONID=...
```

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| 401 연속 | 세션 만료·쿠키 미전송 | Postman에 로그인 후 쿠키 유지 |
| 403/비즈니스 예외 메시지 | 그룹 비멤버 | 해당 `groupId` 멤버십 확인 |
| 캘린더 날짜 형식 오류 | `LocalDate` 파싱 실패 | `yyyy-MM-dd` 형식으로 전달 |
