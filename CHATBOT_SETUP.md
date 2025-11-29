# AI 챗봇 설정 가이드

## ✅ 구현 완료 기능

### 1. AI 챗봇 기능
- ✅ Google Gemini API 통합
- ✅ 웹사이트 기능 이해 및 안내
- ✅ 사용자 요청 파싱 및 액션 처리
- ✅ 대화 내역 30일 보관
- ✅ 반응형 카드 슬라이더 (게시글 표시)

### 2. 주요 기능
- **고객센터 봇**: 웹사이트 기능 설명 및 사용법 안내
- **마이페이지 조회**: "마이페이지 보여줘" → 내 게시글 카드 표시
- **게시글 검색**: "프로그래밍 스터디 찾아줘" → 유사도 높은 게시글 카드 표시
- **북마크 조회**: "북마크 보여줘" → 저장한 게시글 카드 표시
- **AI 추천**: "AI 추천 받고 싶어" → 추천 페이지로 이동

### 3. UI/UX
- ✅ 오른쪽 하단 파란색 플로팅 버튼
- ✅ 부드러운 애니메이션
- ✅ 반응형 디자인 (모바일 지원)
- ✅ 다크모드 지원
- ✅ 카드 슬라이더 (스와이프 가능)

## 📋 설정 방법

### 1. 데이터베이스 테이블 생성

```sql
-- chat_messages_table.sql 파일 실행
mysql -u root -p studywithmever2 < chat_messages_table.sql
```

또는 직접 실행:
```sql
CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  `message` text NOT NULL,
  `response` text NOT NULL,
  `role` enum('USER','ASSISTANT') NOT NULL,
  `action_type` varchar(50) DEFAULT NULL,
  `action_data` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_chat_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

### 2. API 키 확인

`application.properties`에 Gemini API 키가 설정되어 있는지 확인:
```properties
gemini.api.key=AIzaSyBB8BUXs97us9UNwbP_NFQZbXBU3NY_h2w
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
```

### 3. 의존성 확인

`build.gradle`에 RestTemplate 사용을 위한 의존성이 있는지 확인 (Spring Boot Web에 기본 포함)

### 4. 서버 재시작

변경사항 적용을 위해 서버 재시작:
```bash
./gradlew bootRun
```

## 🎯 사용 방법

### 사용자 예시 질문:
1. **"마이페이지 보여줘"** → 내가 작성한 게시글 카드 표시
2. **"프로그래밍 스터디 찾아줘"** → 프로그래밍 관련 게시글 카드 표시
3. **"북마크 보여줘"** → 저장한 게시글 카드 표시
4. **"게시글 작성하는 방법 알려줘"** → AI가 설명 제공
5. **"AI 추천 받고 싶어"** → /recommend 페이지로 이동

### 카드 인터랙션:
- 카드 클릭 → 해당 게시글 상세 페이지로 이동
- 카드 슬라이더 → 마우스 드래그 또는 터치로 좌우 스크롤
- 반응형 → 모바일에서도 최적화된 카드 크기

## 🔧 커스터마이징

### 웹사이트 기능 설명 수정
`ChatbotService.java`의 `WEBSITE_CONTEXT` 상수를 수정하여 웹사이트 기능 설명을 변경할 수 있습니다.

### 요청 파싱 로직 수정
`ChatbotService.java`의 `parseUserRequest()` 메서드를 수정하여 새로운 액션을 추가할 수 있습니다.

### UI 스타일 수정
`chatbot.css` 파일을 수정하여 챗봇 UI를 커스터마이징할 수 있습니다.

## 📝 주의사항

1. **API 키 보안**: `application.properties`에 API 키가 하드코딩되어 있습니다. 운영 환경에서는 환경 변수로 관리하세요.
2. **30일 보관**: 대화 내역은 30일간 보관됩니다. 자동 삭제를 원하면 MySQL 이벤트 스케줄러를 설정하세요.
3. **무료 티어 제한**: Gemini API 무료 티어는 일일 1,500회 요청 제한이 있습니다.

## 🐛 문제 해결

### 챗봇이 응답하지 않을 때
1. API 키가 올바른지 확인
2. 네트워크 연결 확인
3. 서버 로그 확인 (`ChatbotService` 로그)

### 카드가 표시되지 않을 때
1. 브라우저 콘솔에서 JavaScript 오류 확인
2. API 응답 데이터 확인
3. `handleAction()` 메서드 로직 확인

## 🚀 향후 개선 사항

- [ ] 더 정교한 자연어 처리 (NLP)
- [ ] 대화 맥락 개선
- [ ] 음성 입력 지원
- [ ] 다국어 지원
- [ ] 챗봇 학습 데이터 수집
