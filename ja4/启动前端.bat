@echo off
chcp 936 >nul
title 前端 aurora-vue-2 端口 5173
cd /d "%~dp0ja3-main\aurora-vue-2"

rem ---------- 清空代理变量 - 避免继承失效代理导致 npm 卡死 ----------
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "http_proxy="
set "https_proxy="
set "NO_PROXY="
set "no_proxy="

rem ---------- 定位 node ----------
where node >nul 2>nul
if errorlevel 1 (
  if exist "D:\Program Files\nodejs\node.exe" set "PATH=D:\Program Files\nodejs;%PATH%"
)
where node >nul 2>nul
if errorlevel 1 (
  if exist "C:\Program Files\nodejs\node.exe" set "PATH=C:\Program Files\nodejs;%PATH%"
)
where node >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 Node.js, 请先安装 Node.js 18 以上版本
  timeout /t 15 >nul
  exit /b 1
)

rem ---------- node_modules 被清空则自动重装 - 不依赖它 ----------
if not exist "node_modules\.bin\vite" (
  echo [前端] node_modules 缺失, 正在从国内镜像安装依赖, 约 1-3 分钟...
  call npm install --registry=https://registry.npmmirror.com --no-audit --no-fund --loglevel=error
  if errorlevel 1 (
    echo [错误] 依赖安装失败, 请检查网络后重试
    timeout /t 15 >nul
    exit /b 1
  )
  echo [前端] 依赖安装完成
)

echo [前端] 启动开发服务器...
echo [前端] 浏览器访问: http://localhost:5173
call npm run dev
