# SQL 마이그레이션 필요 사항

## 📋 3일 전 SQL 파일과 현재 SQL 파일 비교 결과

### ✅ 추가해야 할 테이블/컬럼

#### 1. **blocked_comments** 테이블 (새로 추가됨)
현재 SQL 파일에 있지만 3일 전 파일에는 없습니다.

```sql
CREATE TABLE IF NOT EXISTS `blocked_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment_id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `block_reason` varchar(255) DEFAULT NULL COMMENT '차단 사유 (욕설, 스팸, 광고 등)',
  `block_type` enum('PROFANITY','SPAM','AD','PATTERN','KEYWORD','AI_DETECTED') DEFAULT 'AI_DETECTED',
  `detected_keywords` text DEFAULT NULL COMMENT '감지된 키워드 목록 (JSON)',
  `ai_confidence` float DEFAULT NULL COMMENT 'AI 신뢰도 (0.0 ~ 1.0)',
  `blocked_by` int(11) DEFAULT NULL COMMENT '차단한 관리자 ID (NULL이면 AI 자동)',
  `is_reviewed` tinyint(1) DEFAULT 0 COMMENT '관리자 검토 여부',
  `reviewed_by` int(11) DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `status` enum('BLOCKED','RESTORED','PENDING') DEFAULT 'BLOCKED',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_blocked_comment` (`comment_id`),
  KEY `idx_blocked_comment_user` (`user_id`),
  KEY `idx_blocked_comment_post` (`post_id`),
  KEY `idx_blocked_comment_type` (`block_type`),
  KEY `idx_blocked_comment_status` (`status`),
  CONSTRAINT `blocked_comments_ibfk_1` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_4` FOREIGN KEY (`blocked_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `blocked_comments_ibfk_5` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

#### 2. **chat_messages** 테이블 (최근 추가됨)
현재 프로젝트에 엔티티가 있지만 SQL 파일에는 없습니다.
별도 파일로 생성됨: `chat_messages_table.sql`

```sql
-- chat_messages_table.sql 파일 참고
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

---

### ❌ 제거된 테이블 (3일 전에는 있었지만 현재는 없음)

#### 1. **tmp_numbers** 테이블
임시 테이블로 보이며 현재 SQL 파일에서 제거되었습니다.
- **의도**: 임시 테이블이므로 제거된 것으로 보임
- **조치**: 추가할 필요 없음 (임시 테이블)

---

### 🔄 변경된 사항

#### 1. **IF NOT EXISTS 추가**
일부 테이블에 `CREATE TABLE IF NOT EXISTS` 구문이 추가되었습니다:
- `ai_learning_data`
- `blocked_posts`
- `filter_keywords`
- `filter_patterns`
- `filter_words`
- `notifications`
- `study_groups`
- `blocked_comments`

**의도**: 안전한 마이그레이션을 위해 추가된 것으로 보임

---

## 📝 마이그레이션 스크립트

### 3일 전 데이터베이스에 적용해야 할 SQL

```sql
-- 1. blocked_comments 테이블 추가
CREATE TABLE IF NOT EXISTS `blocked_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comment_id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `content` text NOT NULL,
  `block_reason` varchar(255) DEFAULT NULL COMMENT '차단 사유 (욕설, 스팸, 광고 등)',
  `block_type` enum('PROFANITY','SPAM','AD','PATTERN','KEYWORD','AI_DETECTED') DEFAULT 'AI_DETECTED',
  `detected_keywords` text DEFAULT NULL COMMENT '감지된 키워드 목록 (JSON)',
  `ai_confidence` float DEFAULT NULL COMMENT 'AI 신뢰도 (0.0 ~ 1.0)',
  `blocked_by` int(11) DEFAULT NULL COMMENT '차단한 관리자 ID (NULL이면 AI 자동)',
  `is_reviewed` tinyint(1) DEFAULT 0 COMMENT '관리자 검토 여부',
  `reviewed_by` int(11) DEFAULT NULL,
  `reviewed_at` timestamp NULL DEFAULT NULL,
  `status` enum('BLOCKED','RESTORED','PENDING') DEFAULT 'BLOCKED',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_blocked_comment` (`comment_id`),
  KEY `idx_blocked_comment_user` (`user_id`),
  KEY `idx_blocked_comment_post` (`post_id`),
  KEY `idx_blocked_comment_type` (`block_type`),
  KEY `idx_blocked_comment_status` (`status`),
  CONSTRAINT `blocked_comments_ibfk_1` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_comments_ibfk_4` FOREIGN KEY (`blocked_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `blocked_comments_ibfk_5` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 2. chat_messages 테이블 추가 (챗봇 기능용)
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

---

## ✅ 체크리스트

- [ ] `blocked_comments` 테이블 추가
- [ ] `chat_messages` 테이블 추가
- [ ] 외래키 제약조건 확인
- [ ] 인덱스 확인

---

## 📌 참고사항

1. **tmp_numbers 테이블**: 임시 테이블이므로 추가할 필요 없음
2. **IF NOT EXISTS**: 안전한 마이그레이션을 위해 사용됨
3. **users.role 컬럼**: 두 파일 모두 주석 처리되어 있음 (별도로 추가 필요)
