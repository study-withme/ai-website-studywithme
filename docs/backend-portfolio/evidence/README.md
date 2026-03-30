# 증빙·첨부 자료 (Postman / 터미널 캡처)

포트폴리오에 **그대로 복사·Import** 하거나, **스크린샷 촬영 후 `screenshots/`에 PNG로 추가**하면 됩니다.

## 포함 파일

| 경로 | 설명 |
|------|------|
| [postman/StudyWithMe-Backend.postman_collection.json](./postman/StudyWithMe-Backend.postman_collection.json) | Postman **컬렉션** (Import) |
| [postman/StudyWithMe-Backend.postman_environment.json](./postman/StudyWithMe-Backend.postman_environment.json) | `baseUrl` 등 **환경 변수** |
| [http/studywithme-api.http](./http/studywithme-api.http) | VS Code **REST Client**용 `.http` |
| [captures/](./captures/) | 실제 서버 호출 **텍스트 캡처** (날짜·환경에 따라 내용 상이) |
| [screenshots/README.md](./screenshots/README.md) | **직접 찍은 사진** 저장 위치 안내 |
| [illustration-api-client.svg](./illustration-api-client.svg) | API 클라이언트 패널 **벡터 예시도** (문서용) |
| [portfolio-api-client-illustration.png](./portfolio-api-client-illustration.png) | API 클라이언트 **PNG 예시 이미지** (문서용 생성) |

## Postman 사용 순서

1. Postman → **Import** → 컬렉션 JSON + 환경 JSON 선택.
2. 우측 상단 환경에서 **StudyWithMe — local** 선택.
3. `baseUrl`을 실제 포트에 맞게 수정 (예: `http://localhost:8082`).
4. **POST /auth** 로 로그인 → Cookies에 `JSESSIONID` 저장되는지 확인 (자동 쿠키 권장).
5. 세션이 필요한 요청(댓글 작성, 알림 recent 등) 실행.
6. **Send 결과 화면**을 캡처해 `screenshots/`에 저장 (파일명 예: `postman-get-api-posts-200.png`).

## 캡처 파일 갱신 방법

PowerShell (서버 기동 후):

```powershell
$base = "http://localhost:8082"  # 또는 8080
Invoke-WebRequest -Uri "$base/api/posts?page=0&size=2" -UseBasicParsing | Select-Object StatusCode, @{n='Body';e={$_.Content}}
```

출력을 복사해 `captures/` 아래 텍스트 파일에 덮어쓰거나 새 번호로 저장합니다.
