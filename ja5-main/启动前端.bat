@echo off
chcp 936 >nul
setlocal
title 前端 aurora-vue-2 5173
cd /d "%~dp0aurora-vue-2"

rem ============================================================
rem  前端启动：依赖按需安装 + 直接 node 起 vite（不经 npm 包装）
rem  相比 npm run dev：省掉 npm 解析 ~2 秒
rem ============================================================

rem ---------- 清空代理变量：失效代理会让 npm 卡死 ----------
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "http_proxy="
set "https_proxy="
set "NO_PROXY="
set "no_proxy="

echo ================================================
echo   前端 aurora-vue-2  http://localhost:5173
echo ================================================
echo.

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
  echo [错误] 未找到 Node.js，请安装 Node.js 18 以上版本
  goto :fail
)

rem ---------- 端口清理 ----------
call :killport 5173

rem ---------- 依赖：缺了才装，装一次约 30 秒 ----------
if not exist "node_modules\vite\bin\vite.js" (
  echo [1/2] 依赖缺失，正在安装（约 30 秒，走国内镜像 + 本地缓存）...
  call npm install --prefer-offline --registry=https://registry.npmmirror.com --no-audit --no-fund --loglevel=error
  if errorlevel 1 (
    echo [错误] 依赖安装失败，请检查网络后重试
    goto :fail
  )
  echo       依赖安装完成
) else (
  echo [1/2] 依赖已就绪，跳过安装
)

rem ---------- 启动 ----------
echo [2/2] 启动 Vite 开发服务器...
echo.
echo   地址   http://localhost:5173
echo   代理   /api -^> http://localhost:8081（vite.config.js）
echo   停止   在本窗口按 Ctrl+C，或运行 停止项目.bat
echo.
echo ------------------------------------------------
echo.

if exist "node_modules\vite\bin\vite.js" (
  node "node_modules\vite\bin\vite.js"
) else (
  call npm run dev
)

echo.
echo [前端] 进程已退出
pause
exit /b 0

rem ============ 子过程：结束占用指定端口的进程 ============
:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
  taskkill /F /T /PID %%p >nul 2>nul && echo   [提示] 端口 %~1 被占用，已结束残留进程 PID=%%p
)
exit /b 0

:fail
echo.
echo 启动已中止。按任意键关闭窗口...
pause >nul
exit /b 1
