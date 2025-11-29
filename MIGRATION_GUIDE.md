# 마이그레이션 가이드

## 📋 현재 상황
- **사용 중인 데이터베이스**: 3일 전 버전
- **목표**: 최신 버전으로 업데이트

## ✅ 실행 방법

### 방법 1: MySQL Workbench / phpMyAdmin에서 실행
1. MySQL Workbench 또는 phpMyAdmin 열기
2. `studywithmever2` 데이터베이스 선택
3. `migration_from_3days_ago.sql` 파일 열기
4. 전체 SQL 실행

### 방법 2: 터미널에서 실행
```bash
mysql -u root -p studywithmever2 < migration_from_3days_ago.sql
```

### 방법 3: MySQL 클라이언트에서 실행
```sql
USE studywithmever2;
SOURCE /Users/wook-mac/Downloads/studywithmever2/migration_from_3days_ago.sql;
```

## 📝 추가되는 내용

### 1. `blocked_comments` 테이블
- 댓글 차단 기능용
- AI가 차단한 댓글 저장

### 2. `chat_messages` 테이블
- AI 챗봇 대화 내역 저장
- 30일간 보관

### 3. `users.role` 컬럼
- 관리자 기능용
- 0: 일반유저, 1: 어드민
- 기존 사용자는 자동으로 0으로 설정됨

## ⚠️ 주의사항

1. **백업 권장**: 실행 전 데이터베이스 백업
   ```bash
   mysqldump -u root -p studywithmever2 > backup_before_migration.sql
   ```

2. **기존 데이터 보존**: 
   - 기존 테이블과 데이터는 그대로 유지됩니다
   - 새로운 테이블만 추가됩니다

3. **role 컬럼**: 
   - 이미 존재하면 자동으로 건너뜁니다
   - 에러 발생하지 않습니다

## ✅ 실행 후 확인

```sql
-- 테이블이 제대로 생성되었는지 확인
SHOW TABLES LIKE 'blocked_comments';
SHOW TABLES LIKE 'chat_messages';

-- role 컬럼이 추가되었는지 확인
DESCRIBE users;
```

## 🎯 완료!

마이그레이션 완료 후:
- ✅ 댓글 차단 기능 사용 가능
- ✅ AI 챗봇 기능 사용 가능
- ✅ 관리자 기능 사용 가능
