-- =============================================================
-- 浏览历史 + 文章点赞（幂等，可重复执行）
-- 用法：mysql -uroot -p123456 --default-character-set=utf8mb4 springboot_test < 本文件
-- =============================================================

USE springboot_test;

-- 浏览历史：user_id + article_id 联合唯一，重复浏览只更新浏览时间
CREATE TABLE IF NOT EXISTS read_history (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  article_id BIGINT       NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_read_history_user_article (user_id, article_id),
  KEY idx_read_history_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史';

-- 文章点赞：取消点赞即删除记录
CREATE TABLE IF NOT EXISTS article_like (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  user_id    BIGINT       NOT NULL,
  article_id BIGINT       NOT NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_article_like_user_article (user_id, article_id),
  KEY idx_article_like_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞';

-- 结果：history / like 两张表已就绪
SELECT 'migrate history and like ok' AS result;
