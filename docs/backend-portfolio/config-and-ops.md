# 공통 설정·빌드·운영 (`config` 및 프로젝트 전반)

**실측 캡처·Postman 컬렉션·스크린샷 가이드**는 [troubleshooting-evidence.md](./troubleshooting-evidence.md) 와 [evidence/README.md](./evidence/README.md) 를 참고하세요.

## `config` 패키지

- **SecurityConfig**: `@Configuration`, `@Bean BCryptPasswordEncoder` — **비밀번호 BCrypt 해싱만** 담당. 폼 로그인·세션은 `UserWebController` 등 MVC에서 직접 `HttpSession`으로 처리하며, Spring Security의 `SecurityFilterChain` 기반 폼 로그인은 사용하지 않음.
- **PythonInitializer** 등: 앱 기동 시 Python 환경 검증 (프로퍼티로 비활성화 가능).

> 세부 클래스는 `com.example.studywithme.config` 패키지를 IDE에서 열어 확인.

## 빌드 (`build.gradle` 요약)

- Spring Boot **3.3.4**, Java **21** toolchain
- `spring-boot-starter-web`, `thymeleaf`, `data-jpa`
- `spring-security-crypto` (BCrypt)
- 런타임 DB: **MySQL** + **H2** (프로파일로 선택)

## 프로파일

| 프로파일 | 용도 |
|----------|------|
| `local` (기본 bootRun) | 로컬 MySQL 등 (`.env` / `application-local.properties`) |
| `devh2` | 인메모리 H2, DB 없이 스모크 — `gradlew bootRun -PbootProfile=devh2` |
| `test` | `@ActiveProfiles("test")` 단위/통합 테스트 |

## 트러블슈팅 (실제 겪은 이슈 위주)

### 1. Windows + 한글 경로 + Gradle 테스트

- **증상**: `ClassNotFoundException: DemoApplicationTests` 등 테스트 워커가 클래스패스를 못 잡는 현상.
- **조치**: `subst W: "프로젝트_전체_경로"` 후 `W:\`에서 `gradlew test` 실행.
- **해제**: `subst W: /d` (다른 작업에 `W:` 없을 때).

### 2. 포트 8080 사용 중

- **증상**: `Web server failed to start. Port 8080 was already in use.`
- **조치**: `netstat -ano | findstr :8080` 으로 PID 확인 후 프로세스 종료, 또는  
  `gradlew bootRun --args="--server.port=8081"` .

### 3. H2 + Hibernate DDL 경고

- **증상**: 기동 로그에 `alter table ... drop foreign key` 관련 경고, `MySQLDialect` 경고.
- **설명**: JPA `ddl-auto`와 DB 벤더 다이얼렉트 조합에 따른 **스키마 관리 로그**인 경우가 많고, 앱이 기동 완료되면 스모크는 가능.
- **조치**: 프로덕션은 `validate`/`none` + Flyway/Liquibase 권장.

### 4. 세션 기반 API (Postman)

- 로그인(`POST /auth`) 후 **쿠키 저장** 안 하면 모든 보호 API가 실패.
- **Redirect**: 302를 따라가면 `JSESSIONID`가 자동 저장되는지 Postman 설정 확인.

### 5. `.env` / 비밀 커밋 방지

- `application.properties`, API 키, DB 비밀번호는 **GitHub에 올리지 않기**.
- `bootRun`이 `DOTENV_PATH`를 참조하므로 로컬 `.env`만 사용.

## API 테스트 체크리스트 

1. **공개**: `GET /`, `GET /api/posts`
2. **인증**: `POST /auth` → 쿠키 확보
3. **댓글**: `GET/POST /api/posts/1/comments`
4. **알림**: `GET /api/notifications/unread-count`
5. **AI**: `POST /api/posts/ai-tags` (Python 필요)
6. **스터디**: `POST /api/study-groups/{id}/sessions/start` (멤버·데이터 시드 필요)
