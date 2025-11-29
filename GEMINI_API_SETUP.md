# Google Gemini API 설정 가이드

## 📋 단계별 설정 방법

### 1단계: Google AI Studio에서 API 키 발급

1. **Google AI Studio 접속**
   - 브라우저에서 https://aistudio.google.com 접속
   - Google 계정으로 로그인

2. **API 키 생성**
   - 우측 상단 "Get API Key" 버튼 클릭
   - "Create API key in new project" 선택 (새 프로젝트 생성)
   - 또는 기존 프로젝트 선택
   - API 키가 생성되면 **즉시 복사해서 안전한 곳에 저장** (다시 볼 수 없음!)

3. **API 키 확인**
   - 생성된 API 키는 `AIza...` 형태로 시작합니다
   - 예: `AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

---

### 2단계: 프로젝트에 API 키 설정

#### 방법 A: application.properties에 추가 (개발용)
```properties
# Google Gemini API 설정
gemini.api.key=YOUR_API_KEY_HERE
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
```

#### 방법 B: 환경 변수 사용 (권장 - 보안)
```bash
# .env 파일 생성 (프로젝트 루트에)
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

---

### 3단계: Gradle 의존성 추가

`build.gradle` 파일에 다음 추가:

```gradle
dependencies {
    // ... 기존 의존성들 ...
    
    // Google Gemini API 클라이언트
    implementation 'com.google.ai.client.generativeai:generativeai:0.2.2'
    
    // 또는 HTTP 클라이언트로 직접 호출 (더 가벼움)
    implementation 'org.springframework.boot:spring-boot-starter-webflux' // WebClient 사용
}
```

---

### 4단계: 무료 티어 제한사항 확인

✅ **무료로 사용 가능한 범위:**
- **일일 요청**: 최대 1,500회
- **분당 토큰**: 1,000,000 토큰
- **모델**: `gemini-pro` (무료)
- **용도**: 개발, 테스트, 소규모 프로덕션

⚠️ **주의사항:**
- API 키는 절대 공개 저장소에 올리지 마세요!
- `.gitignore`에 `.env` 파일 추가 필수
- 클라이언트 사이드 코드에 직접 넣지 마세요

---

### 5단계: API 키 보안 설정 (선택사항)

Google Cloud Console에서:
1. https://console.cloud.google.com 접속
2. API 및 서비스 > 사용자 인증 정보
3. 생성한 API 키 클릭
4. **애플리케이션 제한사항** 설정:
   - IP 주소 제한 (특정 서버 IP만 허용)
   - HTTP 리퍼러 제한 (특정 도메인만 허용)

---

## 🚀 다음 단계

API 키를 발급받으셨다면, 이제 코드 구현을 진행하겠습니다:
1. `ChatbotService.java` - Gemini API 호출 서비스
2. `ChatbotController.java` - 챗봇 API 엔드포인트
3. 프론트엔드 챗봇 UI

---

## 📝 체크리스트

- [ ] Google AI Studio에서 API 키 발급 완료
- [ ] API 키를 안전한 곳에 저장
- [ ] `application.properties` 또는 `.env` 파일에 API 키 설정
- [ ] `.gitignore`에 `.env` 추가 확인
- [ ] Gradle 의존성 추가 (선택사항)

---

## 🔗 유용한 링크

- **Google AI Studio**: https://aistudio.google.com
- **Gemini API 문서**: https://ai.google.dev/docs
- **API 키 관리**: https://console.cloud.google.com/apis/credentials
