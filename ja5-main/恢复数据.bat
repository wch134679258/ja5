@echo off
chcp 936 >nul
title 恢复数据库 springboot_test
cd /d "%~dp0"

echo ================================================
echo   数据库恢复脚本（机器重置后数据被清空时用）
echo   库: springboot_test    账号: root / 123456
echo ================================================
echo.

rem ---------- 定位 mysql 客户端 ----------
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL%" (
  where mysql >nul 2>nul
  if errorlevel 1 (
    echo [错误] 未找到 mysql 客户端, 请确认 MySQL 已安装
    timeout /t 15 >nul
    exit /b 1
  )
  set "MYSQL=mysql"
)

echo [1/3] 执行建库建表 + 内容迁移脚本...
for %%f in (
  init-db.sql
  migrate-category.sql
  migrate-novel.sql
  migrate-favorite.sql
  migrate-game-score.sql
  migrate-history-like.sql
  migrate-article-content.sql
) do (
  echo   - %%f
  rem init-db.sql 负责建库, 不能传不存在的库作默认库
  if "%%f"=="init-db.sql" (
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 < "springBootTest\%%f" 2>nul
  ) else (
    "%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 springboot_test < "springBootTest\%%f" 2>nul
  )
)

echo [2/3] 重建测试账号 verify用户 / abc12345
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 springboot_test -e "INSERT INTO user (id, name, password) VALUES (2, 'verify用户', '$2a$10$5jB/x.hdAEwoToMk.Kj3ceSxrApIi/tzm8zMaWxf7YhB04gqhj7lW') ON DUPLICATE KEY UPDATE password = VALUES(password);" 2>nul

echo [3/3] 校验数据量
"%MYSQL%" -uroot -p123456 --default-character-set=utf8mb4 springboot_test -e "SELECT (SELECT COUNT(*) FROM user) AS 用户数, (SELECT COUNT(*) FROM article) AS 文章数, (SELECT COUNT(*) FROM category) AS 分类数, (SELECT COUNT(*) FROM tag) AS 标签数;" 2>nul

echo.
echo 恢复完成。文章应为 13、分类 6、标签 28、用户 1。
echo 若数据库服务未启动, 请先开启 MySQL 后重跑本脚本。
pause
