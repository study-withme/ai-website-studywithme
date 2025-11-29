# 문제 해결 가이드 (Troubleshooting Guide)

이 문서는 프로젝트에서 발생한 오류와 그 해결 방법을 기록합니다.

---

## 2025-11-29: 관리자 패널 화이트 라벨 에러 (500 Internal Server Error)

### 문제 상황
- **증상**: 관리자 권한(role=1)을 가진 사용자가 `/admin` 경로에 접근할 때 화이트 라벨 에러 페이지가 표시됨
- **에러 메시지**: "Internal Server Error, status=500"
- **발생 위치**: `AdminController.adminPanel()` → `AdminService.getStats()`

### 원인 분석

1. **JPQL 쿼리의 enum 비교 문제**
   - `BlockedPostRepository`와 `BlockedCommentRepository`의 `countBlocked()` 메서드에서 JPQL 쿼리를 사용
   - 원래 코드: `@Query("SELECT COUNT(b) FROM BlockedPost b WHERE b.status = 'BLOCKED'")`
   - JPQL에서 enum을 문자열로 직접 비교하는 방식이 JPA 구현체에 따라 문제를 일으킬 수 있음
   - 특히 `@Enumerated(EnumType.STRING)`로 저장된 enum 값과 JPQL 쿼리 간의 매핑 문제

2. **에러 처리 부재**
   - `getStats()` 메서드에서 여러 repository 호출 중 하나라도 실패하면 전체가 실패
   - 어떤 repository에서 문제가 발생했는지 파악하기 어려움

### 해결 방법

#### 1. Native Query 사용으로 변경
JPQL 대신 Native SQL 쿼리를 사용하여 enum 매핑 문제를 우회:

**BlockedPostRepository.java**
```java
// 변경 전
@Query("SELECT COUNT(b) FROM BlockedPost b WHERE b.status = 'BLOCKED'")
long countBlocked();

// 변경 후
@Query(value = "SELECT COUNT(*) FROM blocked_posts WHERE status = 'BLOCKED'", nativeQuery = true)
long countBlocked();
```

**BlockedCommentRepository.java**
```java
// 변경 전
@Query("SELECT COUNT(b) FROM BlockedComment b WHERE b.status = 'BLOCKED'")
long countBlocked();

// 변경 후
@Query(value = "SELECT COUNT(*) FROM blocked_comments WHERE status = 'BLOCKED'", nativeQuery = true)
long countBlocked();
```

#### 2. 개별 예외 처리 추가
각 repository 호출을 개별적으로 try-catch로 처리하여 하나가 실패해도 나머지는 정상 동작하도록 개선:

**AdminService.java**
```java
public AdminStats getStats() {
    AdminStats stats = new AdminStats();
    
    // 각 통계를 개별적으로 처리
    try {
        stats.setTotalBlockedPosts(blockedPostRepository.countBlocked());
    } catch (Exception e) {
        stats.setTotalBlockedPosts(0);
        System.err.println("차단된 게시글 통계 조회 실패: " + e.getMessage());
        e.printStackTrace();
    }
    
    try {
        stats.setTotalBlockedComments(blockedCommentRepository.countBlocked());
    } catch (Exception e) {
        stats.setTotalBlockedComments(0);
        System.err.println("차단된 댓글 통계 조회 실패: " + e.getMessage());
        e.printStackTrace();
    }
    
    // ... 나머지 repository 호출도 동일하게 처리
}
```

### 교훈 및 권장 사항

1. **JPQL vs Native Query**
   - JPQL은 JPA 엔티티 기반으로 작동하지만, enum 매핑 등에서 예상치 못한 문제가 발생할 수 있음
   - Native Query는 직접 SQL을 실행하므로 더 명확하고 예측 가능함
   - 단, Native Query 사용 시 데이터베이스 종속성이 생기므로 주의 필요

2. **에러 처리 전략**
   - 여러 독립적인 작업을 수행할 때는 각각을 개별적으로 예외 처리
   - 하나의 실패가 전체를 막지 않도록 방어적 프로그래밍
   - 에러 로그를 남겨 디버깅 용이성 확보

3. **Enum 사용 시 주의사항**
   - `@Enumerated(EnumType.STRING)` 사용 시 데이터베이스에 문자열로 저장됨
   - JPQL에서 enum 비교 시 파라미터 바인딩 사용 권장
   - 또는 Native Query로 직접 문자열 비교

### 관련 파일
- `src/main/java/com/example/studywithme/repository/BlockedPostRepository.java`
- `src/main/java/com/example/studywithme/repository/BlockedCommentRepository.java`
- `src/main/java/com/example/studywithme/service/AdminService.java`
- `src/main/java/com/example/studywithme/controller/AdminController.java`

---

## 향후 오류 발생 시 작성 형식

### 문제 상황
- 증상 설명
- 에러 메시지
- 발생 위치

### 원인 분석
- 기술적 원인
- 근본 원인

### 해결 방법
- 구체적인 수정 내용
- 코드 변경 사항 (Before/After)

### 교훈 및 권장 사항
- 유사한 문제 방지 방법
- 베스트 프랙티스

### 관련 파일
- 수정된 파일 목록
