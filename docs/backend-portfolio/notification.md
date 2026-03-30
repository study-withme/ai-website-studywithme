# 도메인: `notification` — 알림 조회·읽음 처리

## 역할

- 로그인 사용자 기준 **미읽음 개수**, **최근 알림 목록**, **개별 읽음 처리**

## 패키지

```
notification/
├── controller/   # NotificationApiController (@RestController)
├── entity/       # Notification
├── repository/
└── service/      # NotificationService
```

## 사용한 어노테이션

- `@RestController`, `@RequiredArgsConstructor`
- `@GetMapping`, `@PostMapping`, `@PathVariable`, `HttpSession`

## API

| 메서드 | 경로 | 동작 |
|--------|------|------|
| GET | `/api/notifications/unread-count` | 미로그인: `{ "count": 0 }`, 로그인: DB 미읽음 수 |
| GET | `/api/notifications/recent` | 미로그인: `[]`, 로그인: 최근 알림 DTO 리스트 |
| POST | `/api/notifications/{id}/read` | 읽음 처리, 소유자 검증은 서비스에서 |

## 응답 DTO (recent)

컨트롤러 `toDto` 기준 필드:

- `id`, `type`, `title`, `body`, `linkUrl`, `isRead`, `createdAt`

## 데이터·성능

- 최근 목록은 **상한 개수**를 서비스에서 제한하는 것이 일반적 (구현 확인 권장).
- 미읽음 카운트는 **인덱스**(user_id, read 플래그)가 있으면 조회 비용 절감.

## Postman 예시

```http
GET http://localhost:8080/api/notifications/unread-count
Cookie: JSESSIONID=...
```

예상: `200`, `{ "count": 3 }`

```http
GET http://localhost:8080/api/notifications/recent
Cookie: JSESSIONID=...
```

예상: `200`, JSON 배열.

```http
POST http://localhost:8080/api/notifications/5/read
Cookie: JSESSIONID=...
```

예상: `200`, `{ "success": true }` 또는 실패 시 `{ "success": false, "message": "..." }`.

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| 항상 count 0 | 미로그인 | 로그인 후 재시도 |
| read 실패 메시지 | 다른 사용자 알림 ID | 본인 알림 id인지 확인 |
| 알림이 안 쌓임 | 댓글/지원 등 이벤트에서 생성 안 함 | `NotificationService` 호출 경로 점검 (comment 등) |
