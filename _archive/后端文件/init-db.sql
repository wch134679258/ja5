-- 凡人修仙传 · 阅读后端（注册/登录）建库建表脚本
-- 数据库：springboot_test  字符集：utf8mb4

CREATE DATABASE IF NOT EXISTS springboot_test
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE springboot_test;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
  `email`      VARCHAR(128) NOT NULL COMMENT '邮箱（唯一登录名）',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 邮箱验证码表（注册/找回密码时使用）
CREATE TABLE IF NOT EXISTS `verify_code` (
  `id`         BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `email`      VARCHAR(128) NOT NULL COMMENT '邮箱',
  `code`       VARCHAR(12)  NOT NULL COMMENT '验证码明文（本地演示，生产应只存哈希）',
  `scene`      VARCHAR(20)  NOT NULL DEFAULT 'register' COMMENT '场景：register / reset',
  `expire_at`  DATETIME     NOT NULL COMMENT '过期时间',
  `used`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已使用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_email_scene` (`email`, `scene`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

SELECT 'init ok' AS result;
