# 도메인: `ai` — Python 연동·태그·요약·챗봇

## 역할

- **AITagService** / **AISummaryService**: 게시글 제목·본문 기반 태그 추천·텍스트 요약 → 내부에서 `PythonScriptExecutor`로 `python/` 스크립트 실행
- **PythonRecommendationService**: 추천 점수·후보 게시글 (user 도메인 추천과 연계)
- **ChatbotService** + **ChatbotController**: Gemini API 기반 대화, 액션(마이페이지·북마크·검색 등) 분기
- **AiPostApiController**: 태그·요약 **순수 REST** (`/api/posts/ai-tags`, `/api/posts/{id}/ai-summary`)

## 패키지

```
ai/
├── controller/
│   ├── AiPostApiController.java
│   └── ChatbotController.java    # @RequestMapping("/api/chatbot")
├── entity/          # ChatMessage 등
├── repository/
└── service/
```

## 사용한 어노테이션

| 클래스 | 어노테이션 |
|--------|------------|
| `AiPostApiController` | `@RestController`, `@PostMapping`, `@PathVariable`, `@RequestParam` |
| `ChatbotController` | `@RestController`, `@RequestMapping("/api/chatbot")`, `@Slf4j` |
| | `ResponseEntity<Map<String, Object>>` |
| `PythonScriptExecutor` | `@Service`, `@Value` (`python.executable`, `python.script.timeout`) |
| 기타 서비스 | `@Service` |

## 흐름: 태그 추천

1. `POST /api/posts/ai-tags` — `title`, `content` 쿼리/폼 파라미터.
2. `AITagService.recommendTags` → Python 프로세스 실행 → JSON 파싱 후 Map 반환.
3. 예외 시 `{ "error": "메시지" }`.

## 흐름: 요약

1. `POST /api/posts/{id}/ai-summary?maxLength=200` — `PostService.getPost`로 본문 로드.
2. HTML 태그 제거·공백 정리 후 `AISummaryService.summarizeContent`.
3. **동일 URL 패턴**이 `PostWebController`에도 있을 수 있음(화면 폼용) — 클라이언트는 REST는 `AiPostApiController` 기준으로 맞추면 됨.

## 흐름: 챗봇

1. `POST /api/chatbot/message` — `message`, 선택 `confirmed`, `actionType`.
2. `ChatbotService.processMessage` — Gemini 호출 및 의도/action 결정.
3. `needsConfirmation` 플로우: 확인 전에는 데이터 반영 지연, `confirmed=true` 시 `handleAction`에서 북마크/검색/마이페이지 데이터 조회 등.
4. 오류 시에도 `200 OK` + 본문에 `message`, `error` 필드 (프론트 호환용).

### 챗봇 부가 API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/chatbot/history` | 대화 이력 |
| DELETE | `/api/chatbot/history` | 이력 삭제 |

## Python 실행·성능·안정성

- **PythonScriptExecutor**:
  - `Process` + **타임아웃** (`python.script.timeout`, 기본 30초) — 장시간 스크립트로 서버 스레드 점유 방지.
  - `CompletableFuture` 등으로 비동기 실행 후 대기 패턴.
- **실패 시**: Python 미설치·경로 오류·스크립트 예외 → 서비스에서 로그 + 사용자용 에러 메시지.
- **devh2 프로파일**: `python.auto-init.enabled=false` 등으로 로컬 스모크 시 Python 부하 줄일 수 있음 ([config-and-ops.md](./config-and-ops.md)).

## 설정 (application / .env)

- `gemini.api.key` — 챗봇 필수 (없으면 해당 기능 실패).
- `python.executable` — Windows는 `python`, Linux는 `python3` 등.

## Postman 예시

**태그 (폼)**

```http
POST http://localhost:8080/api/posts/ai-tags
Content-Type: application/x-www-form-urlencoded

title=스프링 공부&content=JPA와 QueryDSL을 학습합니다
```

예상: `200`, 태그 리스트·점수 등 스크립트 반환 형식에 따름. 실패 시 `{ "error": "..." }`.

**요약**

```http
POST http://localhost:8080/api/posts/1/ai-summary?maxLength=200
```

**챗봇**

```http
POST http://localhost:8080/api/chatbot/message
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=...   (선택: 유저 컨텍스트)

message=추천 게시글 보여줘
```

예상: `200`, `{ "message", "action", "needsConfirmation", ... }`.

## 트러블슈팅

| 증상 | 원인 후보 | 조치 |
|------|-----------|------|
| 태그/요약 항상 error | Python 미설치·PATH | `python --version`, `python.executable` 확인 |
| 타임아웃 | 데이터 크거나 딥러닝 스크립트 | `python.script.timeout` 상향 또는 입력 길이 제한 |
| 챗봇 응답 이상 | API 키 누락·쿼터 | `gemini.api.key`, 콘솔 로그 확인 |
| 한글 경로에서 Gradle 이슈 | Windows 인코딩/경로 | `subst`로 짧은 경로 사용 ([config-and-ops.md](./config-and-ops.md)) |
