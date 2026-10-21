-- ============================================================
--  星空贪吃蛇 · 分数表（增量迁移，幂等，可重复执行）
--
--  排名规则：
--    1. 分数高的在前（score DESC）
--    2. 同分比用时，duration 短的在前
--    3. 再相同则提交早的在前（id ASC，自增即时间序）
--
--  nickname 存提交时的昵称快照：避免排行榜 JOIN user 表，
--  账号注销 / 改名后历史成绩依然可读。
-- ============================================================

USE springboot_test;

CREATE TABLE IF NOT EXISTS `game_score` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`    BIGINT       NOT NULL COMMENT '提交分数的用户 id',
  `nickname`   VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '提交时的昵称快照',
  `score`      INT          NOT NULL COMMENT '本局得分',
  `length`     INT          NOT NULL DEFAULT 0 COMMENT '结束时蛇身长度（节）',
  `duration`   INT          NOT NULL DEFAULT 0 COMMENT '本局时长（秒）',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_game_score_rank` (`score` DESC, `duration` ASC, `id` ASC),
  KEY `idx_game_score_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='星空贪吃蛇分数表';

SELECT 'migrate-game-score ok' AS result;
