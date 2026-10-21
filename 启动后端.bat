@echo off
chcp 936 >nul
title 后端 springBootTest 端口 8081
cd /d "%~dp0ja3-main\springBootTest"

rem ---------- 清空干扰变量 - 避免代理失效卡死和端口被环境变量覆盖 ----------
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "http_proxy="
set "https_proxy="
set "SERVER_PORT="
set "SERVER_ADDRESS="
set "SERVER__PORT="
set "SERVER__HOST="

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 Java, 请先安装 JDK 17 以上版本
  timeout /t 15 >nul
  exit /b 1
)

rem ---------- 检查 MySQL 是否在运行 - 3306 端口 ----------
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul
if errorlevel 1 (
  echo [警告] MySQL 3306 端口未运行, 后端可能启动失败, 请先启动 MySQL 服务
)

rem ---------- target 被清空属正常 - mvnw 会自动重新编译 ----------
echo [后端] 编译并启动 Spring Boot, 首次约 1-3 分钟...
call mvnw.cmd spring-boot:run
