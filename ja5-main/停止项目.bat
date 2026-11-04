@echo off
chcp 936 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"
title 停止 ja5-main

echo ================================================
echo   停止 ja5-main（前端 5173 / 后端 8081）
echo ================================================
echo.

call :killport 5173
call :killport 8081

rem ---------- 说明：只按端口结束进程 ----------
rem  不按进程名批量杀 java.exe / node.exe：那会连别的项目一起杀掉

rem ---------- 确认端口已释放 ----------
echo.
set "LEFT=0"
netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul && set "LEFT=1"
netstat -ano | findstr ":8081 " | findstr "LISTENING" >nul && set "LEFT=1"

if "!LEFT!"=="1" (
  echo [警告] 端口仍被占用，请手动在任务管理器结束对应进程
) else (
  echo [完成] 5173 与 8081 均已释放
)
echo.
ping -n 4 127.0.0.1 >nul
exit /b 0

rem ============ 子过程：结束占用指定端口的进程 ============
:killport
set "HIT=0"
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
  set "HIT=1"
  echo   结束端口 %~1 上的进程 PID=%%p
  taskkill /F /T /PID %%p >nul 2>nul
)
if "!HIT!"=="0" echo   端口 %~1 空闲，无需处理
exit /b 0
