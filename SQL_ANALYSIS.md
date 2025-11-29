# SQL 파일 분석 결과

## 📊 현재 상태 분석

### ✅ 사용 중인 테이블 (엔티티 존재)

1. **users** ✅ - User.java
2. **posts** ✅ - Post.java
3. **comments** ✅ - Comment.java
4. **comment_likes** ✅ - CommentLike.java
5. **post_likes** ✅ - PostLike.java
6. **bookmarks** ✅ - Bookmark.java
7. **user_activity** ✅ - UserActivity.java
8. **user_profiles** ✅ - UserProfile.java
9. **post_applications** ✅ - PostApplication.java
10. **notifications** ✅ - Notification.java
11. **study_groups** ✅ - StudyGroup.java
12. **study_group_members** ✅ - StudyGroupMember.java
13. **blocked_posts** ✅ - BlockedPost.java
14. **blocked_comments** ✅ - BlockedComment.java
15. **filter_words** ✅ - FilterWord.java
16. **filter_patterns** ✅ - FilterPattern.java
17. **filter_keywords** ✅ - FilterKeyword.java
18. **ai_learning_data** ✅ - AILearningData.java
19. **chat_messages** ✅ - ChatMessage.java (최근 추가)

---

## ❌ 사용하지 않는 테이블 (엔티티 없음, 삭제 고려)

### 1. 결제 관련 (현재 프로젝트에서 사용 안 함)
- **payment_methods** - 결제 수단 저장
- **payment_transactions** - 결제 거래 내역

### 2. 로깅/모니터링 (현재 프로젝트에서 사용 안 함)
- **community_activity_logs** - 커뮤니티 활동 로그
- **page_visit_logs** - 페이지 방문 로그
- **security_events** - 보안 이벤트 로그
- **comment_reports** - 댓글 신고

### 3. AI/ML 고급 기능 (현재 미구현)
- **user_ai_profile** - 사용자 AI 프로필 (현재는 user_activity로 대체)
- **user_preferences** - 사용자 선호도 (현재는 user_activity로 대체)
- **post_keywords** - 게시글 키워드 추출
- **user_post_similarity** - 사용자-게시글 유사도
- **user_user_similarity** - 사용자-사용자 유사도
- **user_study_group_similarity** - 사용자-모임 유사도
- **recommendations** - AI 추천 결과 저장
- **post_embeddings** - 게시글 임베딩 벡터
- **user_embeddings** - 사용자 임베딩 벡터
- **study_group_embeddings** - 모임 임베딩 벡터
- **study_group_invitations** - 모임 초대

### 4. 뷰
- **v_user_ai_summary** - 뷰 (엔티티 불필요)

---

## ⚠️ 필요한 테이블 (엔티티는 있지만 SQL에 없음)

### 1. **chat_messages** - 챗봇 메시지 (최근 추가됨)
```sql
-- chat_messages_table.sql 파일 참고
-- 이미 별도 파일로 생성됨
```

---

## 🔧 SQL 파일 수정 권장사항

### 1. users 테이블에 role 컬럼 추가 필요
현재 User.java에 `role` 필드가 있지만, SQL에는 주석 처리되어 있음:
```sql
-- 현재 주석 처리됨 (라인 1147)
-- ALTER TABLE `users` ADD COLUMN `role` int(11) DEFAULT 0 COMMENT '0: 일반유저, 1: 어드민' AFTER `email_verified`;
```

**필요한 작업:**
```sql
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `role` int(11) DEFAULT 0 COMMENT '0: 일반유저, 1: 어드민' AFTER `email_verified`;
```

### 2. posts 테이블 컬럼 확인
Post.java와 SQL 비교:
- ✅ id, user_id, title, content, category, tags, view_count, like_count, created_at, updated_at
- ✅ ai_analyzed, ai_analyzed_at, embedding_updated_at (SQL에 있음, 엔티티에는 없음 - 사용 안 함)

### 3. user_activity 테이블 확인
UserActivity.java와 SQL 비교:
- ✅ id, user_id, action_type, target_id, target_keyword, action_detail, created_at
- ❌ SQL에 `real_name` 컬럼이 있지만 엔티티에는 없음 (불필요)

---

## 📝 정리 권장사항

### 삭제해도 되는 테이블 (현재 미사용)
```sql
-- 결제 관련
DROP TABLE IF EXISTS `payment_methods`;
DROP TABLE IF EXISTS `payment_transactions`;

-- 로깅 (필요시 나중에 추가 가능)
DROP TABLE IF EXISTS `community_activity_logs`;
DROP TABLE IF EXISTS `page_visit_logs`;
DROP TABLE IF EXISTS `security_events`;
DROP TABLE IF EXISTS `comment_reports`;

-- AI 고급 기능 (향후 구현 예정이면 유지)
-- 현재는 사용 안 함
```

### 유지해야 할 테이블 (향후 사용 예정)
- AI/ML 관련 테이블들은 향후 딥러닝 기능 추가 시 필요할 수 있으므로 유지 권장
- 단, 현재는 사용하지 않으므로 주석 처리하거나 별도 파일로 분리 권장

---

## ✅ 최종 권장사항

1. **즉시 추가 필요:**
   - `chat_messages` 테이블 (이미 별도 파일로 생성됨)
   - `users.role` 컬럼 추가

2. **정리 권장:**
   - 사용하지 않는 결제/로깅 테이블 삭제 또는 주석 처리
   - `user_activity.real_name` 컬럼 제거 (엔티티와 불일치)

3. **유지 권장:**
   - AI/ML 관련 테이블은 향후 확장을 위해 유지
   - 단, 주석으로 "향후 사용 예정" 표시
