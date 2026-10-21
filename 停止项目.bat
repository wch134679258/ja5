@echo off
chcp 936 >nul
title 停止 ja3-main
echo 正在停止 前端(5173) 与 后端(8081)...
call :killport 5173
call :killport 8081
echo 已停止.
timeout /t 3 >nul
exit /b 0

:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
  echo   结束端口 %~1 上的进程 PID=%%p
  taskkill /F /PID %%p >nul 2>nul
)
goto :eof
