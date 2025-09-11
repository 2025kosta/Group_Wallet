/* ======================================================================
   Group Wallet 초기 스키마 및 데이터 (init.sql)
   - MySQL 8.0.x
   - docker-entrypoint-initdb.d/0-init.sql 에 위치하면 컨테이너 초기 실행 시 자동 적용됨
   ====================================================================== */

-- 스키마 생성
CREATE DATABASE IF NOT EXISTS `group_wallet` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `group_wallet`;

-- ------------------------------------------------------
-- Table structure for `users`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `name` varchar(100) NOT NULL COMMENT '사용자 이름',
  `email` varchar(150) NOT NULL COMMENT '로그인 이메일(고유)',
  `phone` varchar(32) DEFAULT NULL COMMENT '연락처(선택)',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `users` VALUES
(3,'대표','s@s.com','','2025-09-09 11:52:51.489'),
(4,'멤버원','q@q.com','','2025-09-09 11:53:50.323'),
(5,'멤버투','w@w.com','','2025-09-09 11:54:06.128');

-- ------------------------------------------------------
-- Table structure for `account`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `type` enum('PERSONAL','GROUP') NOT NULL COMMENT '계좌 종류',
  `name` varchar(100) NOT NULL COMMENT '계좌 이름',
  `owner_user_id` bigint unsigned DEFAULT NULL COMMENT 'PERSONAL일 때 소유자, GROUP이면 NULL',
  `balance` bigint unsigned NOT NULL DEFAULT '0' COMMENT '현재 잔액(원)',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
  `account_number` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_personal_owner_name` (`owner_user_id`,`name`),
  KEY `idx_account_owner` (`owner_user_id`),
  CONSTRAINT `fk_account_owner` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `ck_account_balance_nonneg` CHECK ((`balance` >= 0)),
  CONSTRAINT `ck_account_owner_personal` CHECK (
    ((`type` = 'PERSONAL' AND `owner_user_id` IS NOT NULL) OR
     (`type` = 'GROUP' AND `owner_user_id` IS NULL))
  )
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `account` VALUES
(14,'PERSONAL','개인 계좌 1',3,990,'2025-09-11 10:44:53.112','110-050-398144'),
(15,'GROUP','모임 통장 1',NULL,1000,'2025-09-11 10:45:03.147','110-896-998221');

-- ------------------------------------------------------
-- Table structure for `card`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `card`;
CREATE TABLE `card` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `account_id` bigint unsigned NOT NULL COMMENT '계좌 ID(account.id)',
  `masked_no` varchar(32) NOT NULL COMMENT '마스킹 카드번호',
  `brand` varchar(32) DEFAULT NULL COMMENT '브랜드',
  `status` enum('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE' COMMENT '상태',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_card_per_account` (`account_id`,`masked_no`),
  KEY `idx_card_account` (`account_id`),
  KEY `idx_card_status` (`status`),
  CONSTRAINT `fk_card_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `card` VALUES
(11,15,'8830-9335-1968-7873','BC','ACTIVE','2025-09-11 10:46:41.575');

-- ------------------------------------------------------
-- Table structure for `group_member`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `account_id` bigint unsigned NOT NULL COMMENT '모임 계좌 ID(account.id)',
  `user_id` bigint unsigned NOT NULL COMMENT '사용자 ID(users.id)',
  `role` enum('OWNER','MEMBER') NOT NULL COMMENT '역할',
  `joined_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '가입 시각',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_group_member` (`account_id`,`user_id`),
  KEY `idx_gm_account` (`account_id`),
  KEY `idx_gm_user` (`user_id`),
  CONSTRAINT `fk_gm_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_gm_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `group_member` VALUES
(11,15,3,'OWNER','2025-09-11 10:45:03.163'),
(12,15,4,'MEMBER','2025-09-11 10:46:08.213');

-- ------------------------------------------------------
-- Table structure for `transaction`
-- ------------------------------------------------------
DROP TABLE IF EXISTS `transaction`;
CREATE TABLE `transaction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `account_id` bigint unsigned NOT NULL COMMENT '대상 계좌 ID(account.id)',
  `kind` enum('IN','OUT') NOT NULL COMMENT '입금/출금',
  `method` enum('TRANSFER','CARD','OTHER') NOT NULL DEFAULT 'OTHER' COMMENT '거래 수단',
  `amount` bigint unsigned NOT NULL COMMENT '금액(원, 0 초과)',
  `memo` varchar(255) DEFAULT NULL COMMENT '메모',
  `occurred_at` datetime(3) NOT NULL COMMENT '실제 발생 시각',
  `transfer_key` varchar(64) DEFAULT NULL COMMENT '이체 식별 키',
  `card_id` bigint unsigned DEFAULT NULL COMMENT '카드 결제 시 card.id',
  `created_by_user_id` bigint unsigned DEFAULT NULL COMMENT '입력자 사용자 id',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '생성 시각',
  PRIMARY KEY (`id`),
  KEY `idx_transaction_account_time` (`account_id`,`occurred_at`),
  KEY `idx_transaction_transfer` (`transfer_key`),
  KEY `idx_transaction_card` (`card_id`),
  KEY `idx_transaction_creator` (`created_by_user_id`),
  CONSTRAINT `fk_transaction_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_transaction_card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_transaction_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `ck_transaction_amount_pos` CHECK ((`amount` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `transaction` VALUES
(19,15,'IN','OTHER',1000,'1000','2025-09-11 10:46:58.692',NULL,NULL,3,'2025-09-11 10:46:58.000'),
(20,15,'OUT','OTHER',1000,'1000','2025-09-11 10:47:07.147',NULL,NULL,3,'2025-09-11 10:47:07.000'),
(21,15,'OUT','CARD',10,'10','2025-09-11 10:47:16.091',NULL,11,3,'2025-09-11 10:47:16.000'),
(22,14,'OUT','TRANSFER',10,'10','2025-09-11 10:47:40.406','531eca90-ed41-4b09-a351-13b3ab347c97',NULL,3,'2025-09-11 10:47:40.000'),
(23,15,'IN','TRANSFER',10,'10','2025-09-11 10:47:40.406','531eca90-ed41-4b09-a351-13b3ab347c97',NULL,3,'2025-09-11 10:47:40.000');

