-- ============================================================
--  增量迁移：把已有的 article 表升级为「两级分类」模型
--  适用场景：数据库已存在并已有 article 数据，不想重建
--
--  幂等设计：
--    - 建表用 IF NOT EXISTS
--    - 加列前用 information_schema 判断列是否已存在（兼容 MySQL 8.0.x）
--    - 回填用 UPDATE ... WHERE 条件，重复执行无副作用
--
--  执行方式（示例）：
--    mysql -uroot -p123456 < migrate-category.sql
-- ============================================================

USE springboot_test;

-- ---------- 1. 一级分类表 ----------
CREATE TABLE IF NOT EXISTS `category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`       VARCHAR(64) NOT NULL COMMENT '分类名（展示用）',
  `slug`       VARCHAR(64) NOT NULL COMMENT '分类英文名 / URL 别名',
  `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`),
  UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一级分类表';

-- ---------- 2. 二级标签表 ----------
CREATE TABLE IF NOT EXISTS `tag` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64) NOT NULL COMMENT '标签名（展示用）',
  `slug`        VARCHAR(64) NOT NULL COMMENT '标签英文名 / URL 别名',
  `category_id` BIGINT      NULL COMMENT '所属一级分类 id',
  `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序权重，越小越靠前',
  `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_name` (`name`),
  KEY `idx_tag_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二级标签表';

-- ---------- 3. 文章-标签 关联表 ----------
CREATE TABLE IF NOT EXISTS `article_tag` (
  `article_id` BIGINT NOT NULL COMMENT '文章 id',
  `tag_id`     BIGINT NOT NULL COMMENT '标签 id',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_at_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章-标签关联表';

-- ---------- 4. article 表补列 ----------
-- 4.1 category_id
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = 'springboot_test'
                    AND TABLE_NAME   = 'article'
                    AND COLUMN_NAME  = 'category_id');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `article` ADD COLUMN `category_id` BIGINT NULL COMMENT ''一级分类 id'' AFTER `author`, ADD KEY `idx_article_category_id` (`category_id`)',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4.2 热度排序用索引（views）
SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = 'springboot_test'
                    AND TABLE_NAME   = 'article'
                    AND INDEX_NAME   = 'idx_article_views');
SET @ddl := IF(@has_idx = 0,
  'ALTER TABLE `article` ADD KEY `idx_article_views` (`views`)',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. 数据初始化（分类 / 标签） ----------
INSERT INTO `category` (`name`, `slug`, `sort`) VALUES
  ('前端',   'frontend', 10),
  ('后端',   'backend',  20),
  ('设计',   'design',   30),
  ('数据库', 'database', 40),
  ('工程',   'devops',   50)
ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`);

INSERT INTO `tag` (`name`, `slug`, `category_id`, `sort`)
SELECT t.n, t.s, c.id, t.o
FROM (
            SELECT 'Vue'          AS n, 'vue'            AS s, '前端'   AS cn, 10 AS o
  UNION ALL SELECT 'Vite',            'vite',             '前端',   20
  UNION ALL SELECT '组合式API',       'composition-api',  '前端',   30
  UNION ALL SELECT '构建工具',        'bundler',          '前端',   40
  UNION ALL SELECT 'Spring Boot',     'spring-boot',      '后端',   10
  UNION ALL SELECT 'MyBatis-Plus',    'mybatis-plus',     '后端',   20
  UNION ALL SELECT 'JWT',             'jwt',              '后端',   30
  UNION ALL SELECT '认证',            'auth',             '后端',   40
  UNION ALL SELECT '安全',            'security',         '后端',   50
  UNION ALL SELECT '设计灵感',        'design-inspire',   '设计',   10
  UNION ALL SELECT '玻璃拟态',        'glassmorphism',    '设计',   20
  UNION ALL SELECT '配色',            'color',            '设计',   30
  UNION ALL SELECT '深色主题',        'dark-mode',        '设计',   40
  UNION ALL SELECT 'UI',              'ui',               '设计',   50
  UNION ALL SELECT '极光',            'aurora',           '设计',   60
  UNION ALL SELECT '星海',            'starfield',        '设计',   70
  UNION ALL SELECT 'MySQL',           'mysql',            '数据库', 10
  UNION ALL SELECT '索引',            'index',            '数据库', 20
  UNION ALL SELECT '性能优化',        'performance',      '数据库', 30
  UNION ALL SELECT '工程化',          'engineering',      '工程',   10
  UNION ALL SELECT '前后端分离',      'frontend-backend', '工程',   20
) AS t
JOIN `category` c ON c.name = t.cn
ON DUPLICATE KEY UPDATE `slug`        = VALUES(`slug`),
                        `category_id` = VALUES(`category_id`),
                        `sort`        = VALUES(`sort`);

-- ---------- 6. 回填 article.category_id（按已有 category 名匹配） ----------
UPDATE `article` a
JOIN `category` c ON c.name = a.category
SET a.category_id = c.id
WHERE a.category_id IS NULL AND a.category <> '';

-- ---------- 7. 回填 article_tag（按 article.tags 逗号串拆分后匹配 tag.name） ----------
-- 依赖 MySQL 8.0 的 JSON_TABLE 做字符串拆分
INSERT IGNORE INTO `article_tag` (`article_id`, `tag_id`)
SELECT DISTINCT a.id, t.id
FROM `article` a
JOIN JSON_TABLE(
       CONCAT('["', REPLACE(TRIM(a.tags), ',', '","'), '"]'),
       '$[*]' COLUMNS (tag_name VARCHAR(64) PATH '$')
     ) AS jt
JOIN `tag` t ON t.name = TRIM(jt.tag_name)
WHERE a.tags <> '';

-- ---------- 8. 校验 ----------
SELECT
  (SELECT COUNT(*) FROM `category`)                                                AS category_cnt,
  (SELECT COUNT(*) FROM `tag`)                                                     AS tag_cnt,
  (SELECT COUNT(*) FROM `article`)                                                 AS article_cnt,
  (SELECT COUNT(*) FROM `article` WHERE category_id IS NULL AND category <> '')     AS article_category_unbound,
  (SELECT COUNT(*) FROM `article_tag`)                                             AS article_tag_cnt;
