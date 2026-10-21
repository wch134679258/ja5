-- ============================================================
--  ja3-main · 收藏功能增量迁移（幂等，可重复执行）
--  适用：已有库（跑过 init-db.sql / migrate-category.sql）增量升级
-- ============================================================

USE springboot_test;

-- ============================================================
-- 用户收藏表（favorite）
--   一个用户对一篇文章只有一条收藏记录（联合唯一索引保证）
--   取消收藏 = 删除记录；列表按收藏时间倒序
-- ============================================================
CREATE TABLE IF NOT EXISTS `favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT   NOT NULL COMMENT '收藏用户 id（user.id）',
  `article_id` BIGINT   NOT NULL COMMENT '被收藏文章 id（article.id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fav_user_article` (`user_id`, `article_id`),
  KEY `idx_fav_article` (`article_id`),
  KEY `idx_fav_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏表';

SELECT 'migrate favorite ok' AS result;
