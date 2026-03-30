# Study With Me — 백엔드 도메인별 포트폴리오 문서

[저장소 `backend` 브랜치](https://github.com/study-withme/ai-website-studywithme/tree/backend) 기준으로, **도메인(바운디드 컨텍스트)별** 구현·API·흐름·검증·트러블슈팅을 정리한 문서 모음입니다.

## 문서 목록

| 문서 | 도메인 | 한 줄 요약 |
|------|--------|------------|
| [user.md](./user.md) | `user` | 회원·세션·온보딩(application)·추천·활동 로그 |
| [board.md](./board.md) | `board` | 게시글·피드·좋아요·북마크·지원·Thymeleaf |
| [comment.md](./comment.md) | `comment` | 댓글 REST·필터 연동·알림 |
| [studygroup.md](./studygroup.md) | `studygroup` | 스터디 그룹 REST·캘린더·채팅·출석·커리큘럼 |
| [studysession.md](./studysession.md) | `studysession` | 개인 학습 세션 화면·세션 API 연동 |
| [notification.md](./notification.md) | `notification` | 알림 조회·읽음 처리 |
| [moderation.md](./moderation.md) | `moderation` | 콘텐츠 필터·차단·관리자 MVC |
| [ai.md](./ai.md) | `ai` | Python 연동·태그/요약·챗봇(Gemini) |
| [config-and-ops.md](./config-and-ops.md) | `config` 등 | 공통 설정·빌드·H2/MySQL·트러블슈팅 |
| **[troubleshooting-evidence.md](./troubleshooting-evidence.md)** | **전체** | **트러블슈팅 증빙·실측 HTTP 캡처·Postman 첨부 안내** |

## Postman·캡처·이미지 첨부 (한곳에 모음)

| 항목 | 링크 |
|------|------|
| 트러블슈팅 + 실제 호출 결과 요약 | [troubleshooting-evidence.md](./troubleshooting-evidence.md) |
| Postman 컬렉션·환경, `.http`, 캡처 원본 | [evidence/README.md](./evidence/README.md) |
| 직접 찍은 스크린샷 보관 | [evidence/screenshots/README.md](./evidence/screenshots/README.md) |
| API 패널 예시도 (SVG·PNG, 문서용) | [illustration-api-client.svg](./evidence/illustration-api-client.svg) · [portfolio-api-client-illustration.png](./evidence/portfolio-api-client-illustration.png) |

Postman에서 **Import** → `evidence/postman/StudyWithMe-Backend.postman_collection.json` 및 `StudyWithMe-Backend.postman_environment.json` 선택.

## 기술 스택 (백엔드)

- **Java 21**, **Spring Boot 3.3.4**, **Spring Data JPA**, **Thymeleaf**, **Spring Security Crypto (BCrypt)**
- **DB**: MySQL(기본) / **H2 인메모리**(`devh2`, `test` 프로파일)
- **Lombok**: `@RequiredArgsConstructor`, `@Getter`/`@Setter`, `@Slf4j` 등

## 인증·세션 (전 도메인 공통)

- 로그인 성공 시 `HttpSession`에 `loginUser`(`User` 엔티티) 저장.
- 다수 REST/API는 **세션 쿠키(`JSESSIONID`)** 가 있어야 동작합니다.
- **Postman**: 로그인 요청 후 **Cookies 자동 저장** 켜기, 또는 `POST /auth` 응답의 `Set-Cookie`를 다음 요청에 수동 전달.

## 서버 기본 URL

- 로컬: `http://localhost:8080` (기본)
- 포트 충돌 시: `--server.port=8081` 등으로 변경 가능

## Postman 검증 시 주의

- **폼 로그인** (`/auth`, `/register`)은 `x-www-form-urlencoded` 또는 `form-data`로 `email`, `password` 등 전송.
- **JSON 전용이 아닌 엔드포인트**가 많음 (`@RequestParam` 기반).
- 아래 각 문서의 **예상 응답**은 실제 DB 상태·세션 유무에 따라 달라질 수 있습니다.
