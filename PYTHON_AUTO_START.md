# Python 자동 초기화 가이드

## 개요

Spring Boot 애플리케이션이 시작될 때 자동으로 Python 환경을 확인하고 초기화하는 기능이 추가되었습니다.

## 기능

`PythonInitializer` 클래스가 애플리케이션 시작 시 다음 작업을 자동으로 수행합니다:

### 1. Python 실행 파일 확인
- `python3` 명령어가 시스템에 설치되어 있는지 확인
- Python 버전 정보 출력

### 2. Python 버전 검증
- Python 3.7 이상인지 확인
- 버전이 낮으면 경고 메시지 출력

### 3. Python 스크립트 파일 확인
- 다음 스크립트 파일들의 존재 확인:
  - `python/ai_recommendation.py` (추천 시스템)
  - `python/ai_tag_recommendation.py` (태그 추천)
  - `python/ai_summary.py` (요약 시스템)

### 4. Python 패키지 확인
- 필수 패키지 설치 여부 확인:
  - `mysql.connector` (데이터베이스 연결)
  - `json`, `sys`, `re`, `collections` (기본 라이브러리)

### 5. Python 스크립트 문법 검사
- 모든 Python 스크립트의 문법 오류 확인
- 문법 오류가 있으면 경고 메시지 출력

### 6. Python 스크립트 테스트 실행
- 요약 스크립트와 태그 추천 스크립트를 실제로 실행하여 동작 확인
- 추천 스크립트는 DB 연결이 필요하므로 스킵

## 로그 출력 예시

애플리케이션 시작 시 다음과 같은 로그가 출력됩니다:

```
============================================================
Python 환경 초기화 시작
============================================================
✓ Python 실행 파일 확인: python3 (Python 3.11.0)
✓ Python 버전: Python 3.11.0
✓ Python 버전 요구사항 충족 (3.7 이상)
✓ Python 스크립트 확인: python/ai_recommendation.py
✓ Python 스크립트 확인: python/ai_tag_recommendation.py
✓ Python 스크립트 확인: python/ai_summary.py
Python 패키지 확인 중...
✓ 패키지 확인: mysql.connector
✓ 패키지 확인: json
✓ 패키지 확인: sys
✓ 패키지 확인: re
✓ 패키지 확인: collections
Python 스크립트 문법 검사 중...
✓ 문법 검사 통과: python/ai_recommendation.py
✓ 문법 검사 통과: python/ai_tag_recommendation.py
✓ 문법 검사 통과: python/ai_summary.py
Python 스크립트 테스트 실행 중...
✓ 요약 스크립트 테스트 성공: python/ai_summary.py
✓ 태그 추천 스크립트 테스트 성공: python/ai_tag_recommendation.py
✓ Python 스크립트 테스트 완료 (추천 스크립트는 DB 연결 필요로 스킵)
============================================================
Python 환경 초기화 완료
============================================================
```

## 설정

`application.properties`에서 다음 설정을 확인할 수 있습니다:

```properties
# Python 실행 파일 경로
python.executable=python3

# Python 스크립트 경로
python.script.path=python/ai_recommendation.py
python.script.tag.path=python/ai_tag_recommendation.py
python.script.summary.path=python/ai_summary.py
```

## 문제 해결

### Python을 찾을 수 없음
- 시스템에 Python 3가 설치되어 있는지 확인
- `python3 --version` 명령어로 확인
- 설치되어 있지 않으면: `brew install python3` (macOS) 또는 시스템 패키지 매니저 사용

### Python 스크립트를 찾을 수 없음
- 프로젝트 루트 디렉토리에서 애플리케이션을 실행해야 합니다
- `python/` 디렉토리가 프로젝트 루트에 있는지 확인

### 패키지가 설치되지 않음
```bash
pip install -r python/requirements.txt
```

### 문법 오류
- 로그에 표시된 오류 메시지를 확인하고 Python 스크립트를 수정하세요

## 비활성화 방법

Python 자동 초기화를 비활성화하려면:

1. `PythonInitializer` 클래스에 `@Component` 대신 `@Component`를 제거하거나
2. `application.properties`에 다음을 추가:
   ```properties
   python.auto-init.enabled=false
   ```

그리고 `PythonInitializer`에서 이 설정을 확인하도록 수정:

```java
@Value("${python.auto-init.enabled:true}")
private boolean autoInitEnabled;

@Override
public void run(ApplicationArguments args) throws Exception {
    if (!autoInitEnabled) {
        return;
    }
    // ... 기존 코드
}
```

## 주의사항

1. **시작 시간**: Python 환경 확인은 애플리케이션 시작 시간에 약간의 지연을 추가할 수 있습니다 (보통 1-2초)
2. **에러 처리**: Python 환경에 문제가 있어도 애플리케이션은 정상적으로 시작됩니다. 단지 경고 메시지만 출력됩니다.
3. **프로덕션 환경**: 프로덕션 환경에서는 Python 환경이 이미 설정되어 있을 것으로 예상되므로, 초기화 시간을 줄이기 위해 일부 검사를 비활성화할 수 있습니다.
