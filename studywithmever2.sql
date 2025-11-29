-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- 생성 시간: 25-11-11 08:35
-- 서버 버전: 10.4.28-MariaDB
-- PHP 버전: 8.0.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
-- START TRANSACTION; -- 주석 처리: 각 테이블을 독립적으로 생성
SET time_zone = "+00:00";

-- Character set 설정 (필요시 주석 해제)
-- /*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
-- /*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
-- /*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 데이터베이스: `studywithmever2`
--

-- --------------------------------------------------------

--
-- 테이블 구조 `comments`
--

CREATE TABLE `comments` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `parent_comment_id` bigint(20) DEFAULT NULL,
  `content` text NOT NULL,
  `like_count` int(11) DEFAULT 0,
  `report_count` int(11) DEFAULT 0,
  `is_deleted` tinyint(1) DEFAULT 0,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `comment_likes`
--

CREATE TABLE `comment_likes` (
  `user_id` int(11) NOT NULL,
  `comment_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `comment_reports`
--

CREATE TABLE `comment_reports` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `comment_id` bigint(20) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `community_activity_logs`
--

CREATE TABLE `community_activity_logs` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `action_type` enum('CREATE_POST','EDIT_POST','DELETE_POST','CREATE_COMMENT','DELETE_COMMENT','LIKE_POST','UNLIKE_POST','LIKE_COMMENT','UNLIKE_COMMENT','REPORT','FOLLOW_USER') NOT NULL,
  `target_id` bigint(20) DEFAULT NULL,
  `target_type` varchar(50) DEFAULT NULL,
  `extra_info` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`extra_info`)),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `page_visit_logs`
--

CREATE TABLE `page_visit_logs` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `session_id` varchar(100) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `referrer` varchar(255) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `payment_methods`
--

CREATE TABLE `payment_methods` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `pg_customer_id` varchar(100) NOT NULL,
  `card_brand` varchar(50) DEFAULT NULL,
  `card_last4` varchar(4) DEFAULT NULL,
  `is_default` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `payment_transactions`
--

CREATE TABLE `payment_transactions` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `payment_method_id` bigint(20) DEFAULT NULL,
  `amount` int(11) NOT NULL,
  `currency` varchar(10) DEFAULT 'KRW',
  `status` enum('PENDING','PAID','FAILED','CANCELED') NOT NULL,
  `external_id` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `posts`
--

CREATE TABLE `posts` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `view_count` int(11) DEFAULT 0,
  `like_count` int(11) DEFAULT 0,
  `ai_analyzed` tinyint(1) DEFAULT 0,
  `ai_analyzed_at` timestamp NULL DEFAULT NULL,
  `embedding_updated_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `post_likes`
--

CREATE TABLE `post_likes` (
  `user_id` int(11) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `bookmarks` - 북마크
--

CREATE TABLE `bookmarks` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `security_events`
--

CREATE TABLE `security_events` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `event_type` enum('LOGIN_SUCCESS','LOGIN_FAIL','LOGOUT','PASSWORD_CHANGE','REGISTER') NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `birth_date` date NOT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email_verified` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_activity`
--

CREATE TABLE `user_activity` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `action_type` enum('SEARCH','CLICK','LIKE','RECOMMEND','BOOKMARK','COMMENT','AI_CLICK') NOT NULL,
  `target_id` bigint(20) DEFAULT NULL,
  `target_keyword` varchar(100) DEFAULT NULL,
  `action_detail` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 기존 테이블에 action_type enum 값 추가 (마이그레이션)
-- ALTER TABLE `user_activity` MODIFY COLUMN `action_type` enum('SEARCH','CLICK','LIKE','RECOMMEND','BOOKMARK','COMMENT','AI_CLICK') NOT NULL;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_ai_profile`
--

CREATE TABLE `user_ai_profile` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `top_category` varchar(100) DEFAULT NULL,
  `top_keyword` varchar(100) DEFAULT NULL,
  `total_searches` int(11) DEFAULT 0,
  `total_clicks` int(11) DEFAULT 0,
  `total_likes` int(11) DEFAULT 0,
  `total_comments` int(11) DEFAULT 0,
  `total_recommends` int(11) DEFAULT 0,
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_preferences`
--

CREATE TABLE `user_preferences` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `category_name` varchar(100) NOT NULL,
  `preference_score` float DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_profiles`
--

CREATE TABLE `user_profiles` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `real_name` varchar(50) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `bio` text DEFAULT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `join_purpose` varchar(255) DEFAULT NULL,
  `last_login` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Stand-in structure for view `v_user_ai_summary`
-- (See below for the actual view)
--
CREATE TABLE `v_user_ai_summary` (
`user_id` int(11)
,`real_name` varchar(50)
,`email` varchar(100)
,`total_actions` bigint(21)
,`clicks` decimal(23,0)
,`likes` decimal(23,0)
,`searches` decimal(23,0)
,`recommends` decimal(23,0)
,`top_category` varchar(100)
,`top_keyword` varchar(100)
);

-- --------------------------------------------------------

--
-- 뷰 구조 `v_user_ai_summary`
--
DROP TABLE IF EXISTS `v_user_ai_summary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_user_ai_summary`  AS SELECT `u`.`id` AS `user_id`, `u`.`real_name` AS `real_name`, `u`.`email` AS `email`, count(`a`.`id`) AS `total_actions`, sum(`a`.`action_type` = 'CLICK') AS `clicks`, sum(`a`.`action_type` = 'LIKE') AS `likes`, sum(`a`.`action_type` = 'SEARCH') AS `searches`, sum(`a`.`action_type` = 'RECOMMEND') AS `recommends`, `p`.`top_category` AS `top_category`, `p`.`top_keyword` AS `top_keyword` FROM ((`users` `u` left join `user_activity` `a` on(`u`.`id` = `a`.`user_id`)) left join `user_ai_profile` `p` on(`u`.`id` = `p`.`user_id`)) GROUP BY `u`.`id`, `u`.`real_name`, `u`.`email`, `p`.`top_category`, `p`.`top_keyword` ;

--
-- 덤프된 테이블의 인덱스
--

--
-- 테이블의 인덱스 `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `parent_comment_id` (`parent_comment_id`),
  ADD KEY `idx_comments_post` (`post_id`),
  ADD KEY `idx_comments_user` (`user_id`);

--
-- 테이블의 인덱스 `comment_likes`
--
ALTER TABLE `comment_likes`
  ADD PRIMARY KEY (`user_id`,`comment_id`),
  ADD KEY `comment_id` (`comment_id`);

--
-- 테이블의 인덱스 `comment_reports`
--
ALTER TABLE `comment_reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `comment_id` (`comment_id`),
  ADD KEY `idx_comment_reports_user` (`user_id`);

--
-- 테이블의 인덱스 `community_activity_logs`
--
ALTER TABLE `community_activity_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_comm_user` (`user_id`),
  ADD KEY `idx_comm_action` (`action_type`);

--
-- 테이블의 인덱스 `page_visit_logs`
--
ALTER TABLE `page_visit_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_visit_user` (`user_id`),
  ADD KEY `idx_visit_url` (`url`);

--
-- 테이블의 인덱스 `payment_methods`
--
ALTER TABLE `payment_methods`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_paymethod_user` (`user_id`);

--
-- 테이블의 인덱스 `payment_transactions`
--
ALTER TABLE `payment_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `payment_method_id` (`payment_method_id`),
  ADD KEY `idx_pay_user` (`user_id`),
  ADD KEY `idx_pay_status` (`status`);

--
-- 테이블의 인덱스 `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_posts_user` (`user_id`),
  ADD KEY `idx_posts_category` (`category`);

--
-- 테이블의 인덱스 `post_likes`
--
ALTER TABLE `post_likes`
  ADD PRIMARY KEY (`user_id`,`post_id`),
  ADD KEY `post_id` (`post_id`);

--
-- 테이블의 인덱스 `bookmarks`
--
ALTER TABLE `bookmarks`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_bookmark_user_post` (`user_id`,`post_id`),
  ADD KEY `idx_bookmark_post` (`post_id`);

--
-- 테이블의 인덱스 `security_events`
--
ALTER TABLE `security_events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_sec_user` (`user_id`),
  ADD KEY `idx_sec_event` (`event_type`);

--
-- 테이블의 인덱스 `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- 테이블의 인덱스 `user_activity`
--
ALTER TABLE `user_activity`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_uact_user` (`user_id`),
  ADD KEY `idx_uact_type` (`action_type`),
  ADD KEY `idx_uact_keyword` (`target_keyword`);

--
-- 테이블의 인덱스 `user_ai_profile`
--
ALTER TABLE `user_ai_profile`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_ai_profile_user` (`user_id`);

--
-- 테이블의 인덱스 `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_pref_user` (`user_id`),
  ADD KEY `idx_pref_category` (`category_name`);

--
-- 테이블의 인덱스 `user_profiles`
--
ALTER TABLE `user_profiles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_profile_user` (`user_id`);

--
-- 덤프된 테이블의 AUTO_INCREMENT
--

--
-- 테이블의 AUTO_INCREMENT `comments`
--
ALTER TABLE `comments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `comment_reports`
--
ALTER TABLE `comment_reports`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `community_activity_logs`
--
ALTER TABLE `community_activity_logs`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `page_visit_logs`
--
ALTER TABLE `page_visit_logs`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `payment_methods`
--
ALTER TABLE `payment_methods`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `payment_transactions`
--
ALTER TABLE `payment_transactions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `posts`
--
ALTER TABLE `posts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `security_events`
--
ALTER TABLE `security_events`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_activity`
--
ALTER TABLE `user_activity`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_ai_profile`
--
ALTER TABLE `user_ai_profile`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_preferences`
--
ALTER TABLE `user_preferences`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_profiles`
--
ALTER TABLE `user_profiles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- 덤프된 테이블의 제약사항
--

--
-- 테이블의 제약사항 `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_3` FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `comment_likes`
--
ALTER TABLE `comment_likes`
  ADD CONSTRAINT `comment_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comment_likes_ibfk_2` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `comment_reports`
--
ALTER TABLE `comment_reports`
  ADD CONSTRAINT `comment_reports_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comment_reports_ibfk_2` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `community_activity_logs`
--
ALTER TABLE `community_activity_logs`
  ADD CONSTRAINT `community_activity_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `page_visit_logs`
--
ALTER TABLE `page_visit_logs`
  ADD CONSTRAINT `page_visit_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- 테이블의 제약사항 `payment_methods`
--
ALTER TABLE `payment_methods`
  ADD CONSTRAINT `payment_methods_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `payment_transactions`
--
ALTER TABLE `payment_transactions`
  ADD CONSTRAINT `payment_transactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `payment_transactions_ibfk_2` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_methods` (`id`) ON DELETE SET NULL;

--
-- 테이블의 제약사항 `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `post_likes`
--
ALTER TABLE `post_likes`
  ADD CONSTRAINT `post_likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_likes_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE;

--
-- 테이블의 AUTO_INCREMENT `bookmarks`
--
ALTER TABLE `bookmarks`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 제약사항 `bookmarks`
--
ALTER TABLE `bookmarks`
  ADD CONSTRAINT `bookmarks_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `bookmarks_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `security_events`
--
ALTER TABLE `security_events`
  ADD CONSTRAINT `security_events_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

--
-- 테이블의 제약사항 `user_activity`
--
ALTER TABLE `user_activity`
  ADD CONSTRAINT `user_activity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_ai_profile`
--
ALTER TABLE `user_ai_profile`
  ADD CONSTRAINT `user_ai_profile_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_preferences`
--
ALTER TABLE `user_preferences`
  ADD CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_profiles`
--
ALTER TABLE `user_profiles`
  ADD CONSTRAINT `user_profiles_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- --------------------------------------------------------

--
-- 테이블 구조 `study_groups` - 스터디 모임 정보
--

CREATE TABLE IF NOT EXISTS `study_groups` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) DEFAULT NULL,
  `creator_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(100) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `max_members` int(11) DEFAULT NULL,
  `current_members` int(11) DEFAULT 1,
  `status` enum('ACTIVE','CLOSED','FULL') DEFAULT 'ACTIVE',
  `meeting_type` enum('ONLINE','OFFLINE','HYBRID') DEFAULT 'ONLINE',
  `location` varchar(255) DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `ai_analyzed` tinyint(1) DEFAULT 0,
  `ai_analyzed_at` timestamp NULL DEFAULT NULL,
  `embedding_updated_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_post_group` (`post_id`),
  KEY `creator_id` (`creator_id`),
  CONSTRAINT `study_groups_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `study_groups_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 기존 테이블에 post_id 컬럼 추가 (마이그레이션)
-- ALTER TABLE `study_groups` ADD COLUMN `post_id` bigint(20) DEFAULT NULL AFTER `id`;
-- ALTER TABLE `study_groups` ADD UNIQUE KEY `unique_post_group` (`post_id`);
-- ALTER TABLE `study_groups` ADD CONSTRAINT `study_groups_ibfk_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE SET NULL;

-- --------------------------------------------------------

--
-- 테이블 구조 `study_group_members` - 모임 참가자
--

CREATE TABLE `study_group_members` (
  `id` bigint(20) NOT NULL,
  `study_group_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `role` enum('CREATOR','MEMBER') DEFAULT 'MEMBER',
  `joined_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('ACTIVE','LEFT','REMOVED') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `post_keywords` - 게시글 키워드 추출 (AI 분석용)
--

CREATE TABLE `post_keywords` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `keyword` varchar(100) NOT NULL,
  `weight` float DEFAULT 1.0,
  `extracted_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_post_similarity` - 사용자-게시글 유사도 점수
--

CREATE TABLE `user_post_similarity` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `similarity_score` float NOT NULL,
  `calculation_method` varchar(50) DEFAULT 'AI_EMBEDDING',
  `calculated_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_user_similarity` - 사용자-사용자 유사도 점수 (스터디원 추천용)
--

CREATE TABLE `user_user_similarity` (
  `id` bigint(20) NOT NULL,
  `user1_id` int(11) NOT NULL,
  `user2_id` int(11) NOT NULL,
  `similarity_score` float NOT NULL,
  `calculation_method` varchar(50) DEFAULT 'AI_EMBEDDING',
  `based_on_post_id` bigint(20) DEFAULT NULL,
  `calculated_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_study_group_similarity` - 사용자-모임 유사도 점수
--

CREATE TABLE `user_study_group_similarity` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `study_group_id` bigint(20) NOT NULL,
  `similarity_score` float NOT NULL,
  `calculation_method` varchar(50) DEFAULT 'AI_EMBEDDING',
  `calculated_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `recommendations` - AI 추천 결과 저장
--

CREATE TABLE `recommendations` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `recommendation_type` enum('POST','STUDY_GROUP','USER') NOT NULL,
  `target_id` bigint(20) NOT NULL,
  `similarity_score` float NOT NULL,
  `rank` int(11) NOT NULL,
  `reason` text DEFAULT NULL,
  `is_viewed` tinyint(1) DEFAULT 0,
  `is_accepted` tinyint(1) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `expires_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `study_group_invitations` - 모임 참가 제안
--

CREATE TABLE `study_group_invitations` (
  `id` bigint(20) NOT NULL,
  `study_group_id` bigint(20) NOT NULL,
  `inviter_id` int(11) NOT NULL,
  `invitee_id` int(11) NOT NULL,
  `post_id` bigint(20) DEFAULT NULL,
  `message` text DEFAULT NULL,
  `similarity_score` float DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED','EXPIRED') DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `responded_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `post_embeddings` - 게시글 AI 임베딩 벡터 저장
--

CREATE TABLE `post_embeddings` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `embedding_vector` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `model_version` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `user_embeddings` - 사용자 AI 임베딩 벡터 저장
--

CREATE TABLE `user_embeddings` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `embedding_vector` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `model_version` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `study_group_embeddings` - 모임 AI 임베딩 벡터 저장
--

CREATE TABLE `study_group_embeddings` (
  `id` bigint(20) NOT NULL,
  `study_group_id` bigint(20) NOT NULL,
  `embedding_vector` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `model_version` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 덤프된 테이블의 인덱스 (새로운 테이블)
--

--
-- 테이블의 인덱스 `study_groups`
--
ALTER TABLE `study_groups`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_study_creator` (`creator_id`),
  ADD KEY `idx_study_category` (`category`),
  ADD KEY `idx_study_status` (`status`);

--
-- 테이블의 인덱스 `study_group_members`
--
ALTER TABLE `study_group_members`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_group_user` (`study_group_id`,`user_id`),
  ADD KEY `idx_member_user` (`user_id`),
  ADD KEY `idx_member_status` (`status`);

--
-- 테이블의 인덱스 `post_keywords`
--
ALTER TABLE `post_keywords`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_keyword_post` (`post_id`),
  ADD KEY `idx_keyword_name` (`keyword`);

--
-- 테이블의 인덱스 `user_post_similarity`
--
ALTER TABLE `user_post_similarity`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_user_post` (`user_id`,`post_id`),
  ADD KEY `idx_sim_post` (`post_id`),
  ADD KEY `idx_sim_score` (`similarity_score`);

--
-- 테이블의 인덱스 `user_user_similarity`
--
ALTER TABLE `user_user_similarity`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_user_pair` (`user1_id`,`user2_id`),
  ADD KEY `idx_sim_user2` (`user2_id`),
  ADD KEY `idx_sim_score_user` (`similarity_score`),
  ADD KEY `idx_sim_post` (`based_on_post_id`);

--
-- 테이블의 인덱스 `user_study_group_similarity`
--
ALTER TABLE `user_study_group_similarity`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_user_group` (`user_id`,`study_group_id`),
  ADD KEY `idx_sim_group` (`study_group_id`),
  ADD KEY `idx_sim_score_group` (`similarity_score`);

--
-- 테이블의 인덱스 `recommendations`
--
ALTER TABLE `recommendations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_rec_user` (`user_id`),
  ADD KEY `idx_rec_type` (`recommendation_type`),
  ADD KEY `idx_rec_score` (`similarity_score`),
  ADD KEY `idx_rec_created` (`created_at`);

--
-- 테이블의 인덱스 `study_group_invitations`
--
ALTER TABLE `study_group_invitations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_inv_group` (`study_group_id`),
  ADD KEY `idx_inv_inviter` (`inviter_id`),
  ADD KEY `idx_inv_invitee` (`invitee_id`),
  ADD KEY `idx_inv_status` (`status`),
  ADD KEY `idx_inv_post` (`post_id`);

--
-- 테이블의 인덱스 `post_embeddings`
--
ALTER TABLE `post_embeddings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_embed_post` (`post_id`);

--
-- 테이블의 인덱스 `user_embeddings`
--
ALTER TABLE `user_embeddings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_embed_user` (`user_id`);

--
-- 테이블의 인덱스 `study_group_embeddings`
--
ALTER TABLE `study_group_embeddings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `idx_embed_group` (`study_group_id`);

--
-- 덤프된 테이블의 AUTO_INCREMENT (새로운 테이블)
--

--
-- 테이블의 AUTO_INCREMENT `study_groups`
--
ALTER TABLE `study_groups`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `study_group_members`
--
ALTER TABLE `study_group_members`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `post_keywords`
--
ALTER TABLE `post_keywords`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_post_similarity`
--
ALTER TABLE `user_post_similarity`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_user_similarity`
--
ALTER TABLE `user_user_similarity`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_study_group_similarity`
--
ALTER TABLE `user_study_group_similarity`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `recommendations`
--
ALTER TABLE `recommendations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `study_group_invitations`
--
ALTER TABLE `study_group_invitations`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `post_embeddings`
--
ALTER TABLE `post_embeddings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `user_embeddings`
--
ALTER TABLE `user_embeddings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 테이블의 AUTO_INCREMENT `study_group_embeddings`
--
ALTER TABLE `study_group_embeddings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 덤프된 테이블의 제약사항 (새로운 테이블)
--

--
-- 테이블의 제약사항 `study_groups`
--
ALTER TABLE `study_groups`
  ADD CONSTRAINT `study_groups_ibfk_1` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `study_group_members`
--
ALTER TABLE `study_group_members`
  ADD CONSTRAINT `study_group_members_ibfk_1` FOREIGN KEY (`study_group_id`) REFERENCES `study_groups` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `study_group_members_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `post_keywords`
--
ALTER TABLE `post_keywords`
  ADD CONSTRAINT `post_keywords_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_post_similarity`
--
ALTER TABLE `user_post_similarity`
  ADD CONSTRAINT `user_post_similarity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_post_similarity_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_user_similarity`
--
ALTER TABLE `user_user_similarity`
  ADD CONSTRAINT `user_user_similarity_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_user_similarity_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_user_similarity_ibfk_3` FOREIGN KEY (`based_on_post_id`) REFERENCES `posts` (`id`) ON DELETE SET NULL;

--
-- 테이블의 제약사항 `user_study_group_similarity`
--
ALTER TABLE `user_study_group_similarity`
  ADD CONSTRAINT `user_study_group_similarity_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_study_group_similarity_ibfk_2` FOREIGN KEY (`study_group_id`) REFERENCES `study_groups` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `recommendations`
--
ALTER TABLE `recommendations`
  ADD CONSTRAINT `recommendations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `study_group_invitations`
--
ALTER TABLE `study_group_invitations`
  ADD CONSTRAINT `study_group_invitations_ibfk_1` FOREIGN KEY (`study_group_id`) REFERENCES `study_groups` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `study_group_invitations_ibfk_2` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `study_group_invitations_ibfk_3` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `study_group_invitations_ibfk_4` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE SET NULL;

--
-- 테이블의 제약사항 `post_embeddings`
--
ALTER TABLE `post_embeddings`
  ADD CONSTRAINT `post_embeddings_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `user_embeddings`
--
ALTER TABLE `user_embeddings`
  ADD CONSTRAINT `user_embeddings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블의 제약사항 `study_group_embeddings`
--
ALTER TABLE `study_group_embeddings`
  ADD CONSTRAINT `study_group_embeddings_ibfk_1` FOREIGN KEY (`study_group_id`) REFERENCES `study_groups` (`id`) ON DELETE CASCADE;

-- --------------------------------------------------------

--
-- 테이블 구조 `post_applications` - 게시글 지원하기
--

CREATE TABLE `post_applications` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `message` text DEFAULT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED','CANCELLED') DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 테이블의 인덱스 `post_applications`
--
ALTER TABLE `post_applications`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `unique_post_user` (`post_id`, `user_id`),
  ADD KEY `user_id` (`user_id`);

--
-- 테이블의 AUTO_INCREMENT `post_applications`
--
ALTER TABLE `post_applications`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_applications`
  ADD CONSTRAINT `post_applications_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `post_applications_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- 테이블 구조 `notifications` - 알림
--

CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NOT NULL,
  `type` varchar(50) NOT NULL,
  `title` varchar(200) NOT NULL,
  `body` text DEFAULT NULL,
  `link_url` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 기존 테이블 업데이트 (ALTER TABLE) - 관리자 기능 추가
-- 기존 데이터베이스에 적용하려면 아래 쿼리를 실행하세요.
-- 이미 컬럼이 존재하면 에러가 발생하므로, 먼저 확인 후 실행하세요.
--

-- 방법 1: 직접 실행 (role 컬럼이 없는 경우)
-- ALTER TABLE `users` ADD COLUMN `role` int(11) DEFAULT 0 COMMENT '0: 일반유저, 1: 어드민' AFTER `email_verified`;

-- 방법 2: 안전한 방법 (컬럼 존재 여부 확인 후 추가)
-- 아래 주석을 해제하고 실행하세요
/*
SET @exist = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
              WHERE TABLE_SCHEMA = DATABASE() 
              AND TABLE_NAME = 'users' 
              AND COLUMN_NAME = 'role');
              
SET @sqlstmt = IF(@exist = 0, 
                  'ALTER TABLE users ADD COLUMN role int(11) DEFAULT 0 COMMENT ''0: 일반유저, 1: 어드민'' AFTER email_verified',
                  'SELECT ''Column role already exists'' AS message');
                  
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
*/

-- 방법 3: 가장 간단한 방법 (MariaDB 10.5.2 이상)
-- ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `role` int(11) DEFAULT 0 COMMENT '0: 일반유저, 1: 어드민' AFTER `email_verified`;

-- --------------------------------------------------------

--
-- 테이블 구조 `blocked_posts` - AI에 의해 차단된 게시글
--

CREATE TABLE IF NOT EXISTS `blocked_posts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `post_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `title` varchar(200) NOT NULL,
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
  UNIQUE KEY `unique_blocked_post` (`post_id`),
  KEY `idx_blocked_user` (`user_id`),
  KEY `idx_blocked_type` (`block_type`),
  KEY `idx_blocked_status` (`status`),
  CONSTRAINT `blocked_posts_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_posts_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocked_posts_ibfk_3` FOREIGN KEY (`blocked_by`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `blocked_posts_ibfk_4` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `filter_words` - 욕설 필터 단어 목록
--

CREATE TABLE IF NOT EXISTS `filter_words` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL COMMENT '필터링할 단어',
  `word_type` enum('PROFANITY','SPAM','AD','CUSTOM') DEFAULT 'CUSTOM',
  `is_active` tinyint(1) DEFAULT 1,
  `created_by` int(11) DEFAULT NULL COMMENT '등록한 관리자 ID',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_word` (`word`),
  KEY `idx_word_type` (`word_type`),
  KEY `idx_word_active` (`is_active`),
  CONSTRAINT `filter_words_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `filter_patterns` - 차단 패턴 (글 형식)
--

CREATE TABLE IF NOT EXISTS `filter_patterns` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `pattern_name` varchar(100) NOT NULL COMMENT '패턴 이름',
  `pattern_regex` text NOT NULL COMMENT '정규식 패턴',
  `pattern_type` enum('TITLE','CONTENT','BOTH') DEFAULT 'BOTH',
  `description` text DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `block_count` int(11) DEFAULT 0 COMMENT '이 패턴으로 차단된 횟수',
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_pattern_active` (`is_active`),
  KEY `idx_pattern_type` (`pattern_type`),
  CONSTRAINT `filter_patterns_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `filter_keywords` - 차단 키워드 (관리자가 추가)
--

CREATE TABLE IF NOT EXISTS `filter_keywords` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `keyword` varchar(100) NOT NULL,
  `keyword_type` enum('EXACT','PARTIAL','REGEX') DEFAULT 'PARTIAL',
  `description` text DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `block_count` int(11) DEFAULT 0,
  `created_by` int(11) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_keyword` (`keyword`),
  KEY `idx_keyword_active` (`is_active`),
  KEY `idx_keyword_type` (`keyword_type`),
  CONSTRAINT `filter_keywords_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `ai_learning_data` - AI 학습 데이터 (차단 빈도가 높은 패턴)
--

CREATE TABLE IF NOT EXISTS `ai_learning_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_type` enum('POST','COMMENT') DEFAULT 'POST',
  `content_sample` text NOT NULL COMMENT '차단된 내용 샘플',
  `block_reason` varchar(255) DEFAULT NULL,
  `detected_pattern` text DEFAULT NULL COMMENT '감지된 패턴',
  `frequency` int(11) DEFAULT 1 COMMENT '유사 패턴 발생 빈도',
  `last_detected_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  KEY `idx_learning_type` (`content_type`),
  KEY `idx_learning_frequency` (`frequency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 테이블 구조 `blocked_comments` - AI에 의해 차단된 댓글
--

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

-- COMMIT; -- 주석 처리: 트랜잭션을 사용하지 않으므로 불필요

-- Character set 복원 (필요시 주석 해제)
-- /*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
-- /*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
-- /*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
