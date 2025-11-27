## Study With Me (Spring Boot 3 + MySQL)

**Study With Me**는 스터디/모임 추천 및 커뮤니티 기능을 제공하는 Spring Boot 기반 웹 프로젝트입니다.  
이 문서는 깃허브에서 코드를 클론한 사용자가 **별도 수동 설정 최소화**로 바로 실행할 수 있도록 환경 세팅 방법과 DB 초기화 방법을 안내합니다.

---

### 1. 필수 요구사항

- **Java 21** (Gradle가 toolchain으로 자동 설치 가능)
- **Gradle**: 저장소에 포함된 `./gradlew`(Gradle Wrapper) 사용 권장
- **Docker / Docker Compose** (MySQL 자동 설치 및 DB 스키마 자동 생성용)

> Docker를 사용하지 않고 직접 MySQL을 설치해서 써도 되지만,  
> 이 README는 *Docker 기반 자동 초기화*를 기준으로 설명합니다.

---

### 2. 프로젝트 클론

```bash
git clone https://github.com/your-id/studywithmever2.git
cd studywithmever2
```

---

### 3. 데이터베이스 자동 생성 (Docker 사용)

이 프로젝트 루트에는 `studywithmever2.sql` 이 포함되어 있습니다.  
`docker-compose.yml` 에서는 이 파일을 MySQL 컨테이너 초기화 스크립트로 마운트하여,

- DB 생성
- 테이블/뷰 생성
- 제약조건 및 인덱스 설정

을 **컨테이너 최초 실행 시 자동으로** 수행합니다.

#### 3-1. (선택) 환경 변수 파일 준비

`.env` 파일을 루트에 두면 `docker-compose.yml` 과 Spring Boot 둘 다 공통으로 사용할 수 있습니다.

예시:

```bash
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=studywithmever2
MYSQL_USER=study_user
MYSQL_PASSWORD=studypass
MYSQL_PORT=3306

SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/studywithmever2?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=study_user
SPRING_DATASOURCE_PASSWORD=studypass

SPRING_PROFILES_ACTIVE=local
```

> 운영/개인 환경에서는 **절대 실제 비밀번호를 커밋하지 말고**,  
> 각자 로컬에서만 `.env` 를 만들어 사용하세요.

#### 3-2. MySQL 컨테이너 기동

프로젝트 루트에서 아래 명령을 실행합니다.

```bash
docker compose up -d db
```

- 이미지: `mariadb:10.4`
- 포트: `MYSQL_PORT`(기본 3306) → 호스트로 노출
- 첫 기동 시 `studywithmever2.sql` 이 자동 실행됩니다.

DB 상태를 확인하고 싶다면:

```bash
docker logs -f studywithme-db      # 초기화 로그 확인
docker exec -it studywithme-db bash
mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}"
```

---

### 4. Spring Boot 애플리케이션 설정

`src/main/resources/application.properties` 는 **환경변수 기반 설정**을 사용하도록 되어 있습니다.

- `SPRING_DATASOURCE_URL` (기본: `jdbc:mysql://localhost:3306/studywithmever2?...`)
- `SPRING_DATASOURCE_USERNAME` (기본: `root`)
- `SPRING_DATASOURCE_PASSWORD` (기본: `password`)

환경변수를 지정하지 않으면 위 기본값으로 실행되므로,  
실제 사용 시에는 쉘에서 직접 export 하거나 `.env` 를 사용하여 덮어쓰는 것을 권장합니다.

#### 4-1. 쉘에서 직접 환경변수 설정 (예시)

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/studywithmever2?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
export SPRING_DATASOURCE_USERNAME="study_user"
export SPRING_DATASOURCE_PASSWORD="studypass"
export SPRING_PROFILES_ACTIVE="local"
```

---

### 5. 애플리케이션 실행 방법

루트 디렉토리에서 Gradle Wrapper로 실행합니다.

```bash
./gradlew bootRun
```

정상 기동 후 브라우저에서 아래 주소로 접속합니다.

- `http://localhost:8080/`

기본적으로 다음과 같은 페이지 템플릿이 포함되어 있습니다.

- `index.html` (메인)
- `auth.html`, `register.html` (로그인/회원가입)
- `ai.html`, `recommend.html` (AI/추천 관련 UI)
- `post-*.html`, `mypage.html`, `bookmarks.html` 등

---

### 6. DB 스키마 커스터마이징

- `studywithmever2.sql` 에 모든 테이블/뷰/제약조건이 정의되어 있습니다.
- 새로운 컬럼/테이블을 추가하고 싶다면,
  - 로컬 DB에서 직접 `ALTER TABLE` 등을 실행한 뒤
  - 필요 시 수정된 덤프를 다시 떠서 `studywithmever2.sql` 를 교체하세요.

**주의:**  
기존 컨테이너에 이미 데이터가 있는 상태에서 SQL을 바꾸더라도,  
`/docker-entrypoint-initdb.d` 는 *최초 생성 시 한 번만 실행*되므로  
스키마를 변경하려면 DB 볼륨을 삭제하고 다시 기동해야 합니다.

```bash
docker compose down -v   # 볼륨 포함 완전 삭제
docker compose up -d db  # 다시 초기화 (studywithmever2.sql 재실행)
```

---

### 7. 개발 환경 요약

- **백엔드**
  - Spring Boot 3.3.x
  - Java 21 (Gradle toolchain)
  - Spring Web, Thymeleaf
  - Spring Data JPA + MySQL/MariaDB
- **빌드**
  - Gradle Wrapper (`./gradlew`)
- **DB**
  - Docker 기반 MariaDB 10.4
  - 초기 스키마: `studywithmever2.sql` 자동 실행

---

### 8. 자주 하는 질문 (FAQ)

- **Q. Docker 없이 실행해도 되나요?**  
  A. 가능합니다. 직접 MySQL/MariaDB를 설치하고, `studywithmever2` 데이터베이스를 만든 뒤 `studywithmever2.sql` 을 수동으로 실행하면 됩니다. 그리고 `SPRING_DATASOURCE_*` 값만 해당 DB 정보로 맞춰주면 됩니다.

- **Q. 비밀번호를 깃허브에 올려도 되나요?**  
  A. 실제 서비스/개인용 비밀번호는 절대 올리면 안 됩니다. 이 프로젝트는 환경변수 기반으로 되어 있으니, `.env` 파일은 `.gitignore` 에 추가하여 로컬에서만 관리하세요.

---

### 9. 기여 및 이슈

- 버그, 개선사항, 기능 제안은 깃허브 이슈로 등록해주세요.
- PR 시에는
  - 빌드 (`./gradlew build`)
  - 기본 실행 (`./gradlew bootRun`)
  - DB 초기화(Docker 또는 로컬 MySQL)를 모두 확인한 뒤 제출해 주세요.
