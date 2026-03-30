# 도메인: `studysession` — 개인 학습 세션·일지

## 역할

- **엔티티·서비스**: `StudySession`, `StudyJournal` 및 비즈니스 로직 (`StudySessionService`, `StudyJournalService`).
- **화면**: `StudySessionWebController` — 그룹 상세 맥락에서 학습 세션 UI 진입.
- **API의 상당수**는 `studygroup` 패키지의 `StudyGroupApiController`에 노출됨 (세션 시작/종료, 일지 CRUD 등).

## 패키지

```
studysession/
├── controller/   # StudySessionWebController
├── entity/
├── repository/
└── service/
```

## 사용한 어노테이션

- `StudySessionWebController`: `@Controller`, `@GetMapping`, `@PathVariable`
- 서비스 계층: `@Service`, `@Transactional`

## 화면 라우트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/study-groups/{id}/session` | 그룹 ID 기준 학습 세션 화면 (로그인·멤버십은 템플릿/다른 레이어와 연계) |

## REST와의 관계

실제 **세션 시작/종료·출석·일지 API**는 다음을 참고:

- [studygroup.md](./studygroup.md) — `/api/study-groups/.../sessions/...`, `.../journal` 등

## 설계 포인트

- **도메인 분리**: 그룹(`studygroup`) vs 개인 세션(`studysession`) 엔티티를 나누고, API는 그룹 단위 리소스로 묶어 프론트가 한 번에 그룹 컨텍스트를 유지하기 쉽게 함.
- 트랜잭션: 세션 시작 시 채팅 시스템 메시지 전송 등 **여러 서비스 호출**이 이어지므로, 실패 시 롤백 범위를 서비스 메서드 단위로 설계.

## Postman

직접 엔드포인트가 적으므로, 세션 관련 검증은 **studygroup API**와 동일한 쿠키로 호출하면 됨.

## 트러블슈팅

| 증상 | 조치 |
|------|------|
| 화면은 뜨는데 API 실패 | 동일 그룹에 가입되어 있는지, 세션 만료 여부 확인 |
| 일지가 비어 있음 | `GET .../journal` 호출 시 `groupId`·권한 확인 |
