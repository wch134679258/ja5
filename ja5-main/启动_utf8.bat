@echo off
chcp 936 >nul
title Aurora 项目一键启动
cd /d "%~dp0"

echo ================================================
echo   Aurora 登录项目 一键启动脚本
echo   前端: aurora-vue-2 (端口 5173)
echo   后端: springBootTest (端口 8081, 需 MySQL)
echo   说明: node_modules/target 会被系统删除,
echo         本脚本会自动重装依赖, 无需手动处理
echo ================================================
echo.

rem ---------- 0. 清理上次残留(端口被占时先杀掉) ----------
call :killport 5173
call :killport 8081

rem ---------- 1. 安装前端依赖(node_modules 被删则重装) ----------
if not exist "aurora-vue-2\node_modules" (
    echo [1/4] 前端依赖不存在, 正在安装(优先走本地缓存, 约 30-90 秒)...
    cd /d "%~dp0aurora-vue-2"
    call npm install --prefer-offline --no-audit --no-fund
    cd /d "%~dp0"
) else (
    echo [1/4] 前端依赖已存在, 跳过安装
)
echo.

rem ---------- 2. 初始化数据库(幂等, 已存在则跳过建表) ----------
echo [2/4] 检查数据库 springboot_test ...
set "MYSQL_BIN="
where mysql >nul 2>nul && set "MYSQL_BIN=mysql"
if not defined MYSQL_BIN if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_BIN=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if defined MYSQL_BIN (
    "%MYSQL_BIN%" -uroot -p123456 --default-character-set=utf8mb4 < "springBootTest\init-db.sql" >nul 2>nul
    if !errorlevel! equ 0 (echo       数据库已就绪) else (echo       [警告] 数据库初始化失败, 若 MySQL 未启动请先启动 MySQL 服务)
) else (
    echo       [警告] 未找到 mysql 客户端, 跳过初始化(数据库若已建过则不影响)
)
echo.

rem ---------- 3. 启动后端(新窗口显示日志, 自动重新编译) ----------
echo [3/4] 启动后端 springBootTest ...
start "后端-8081(关闭窗口即停止)" cmd /k "cd /d "%~dp0springBootTest" && mvnw.cmd spring-boot:run"

rem ---------- 4. 等待 8081 就绪(首次需下载 Maven 依赖, 可能 2-5 分钟) ----------
echo       等待后端就绪(最多 300 秒)...
set /a waited=0
:waitbackend
timeout /t 3 /nobreak >nul
set /a waited+=3
netstat -ano | findstr ":8081" | findstr "LISTENING" >nul
if %errorlevel% equ 0 goto backendok
if %waited% geq 300 (
    echo       [超时] 后端 300 秒内未就绪, 请查看"后端-8081"窗口的报错信息
    goto startfront
)
goto waitbackend
:backendok
echo       后端已就绪: http://localhost:8081

:startfront
rem ---------- 5. 启动前端并打开浏览器 ----------
echo [4/4] 启动前端 aurora-vue-2 ...
start "前端-5173(关闭窗口即停止)" cmd /k "cd /d "%~dp0aurora-vue-2" && npm run dev"

echo       等待前端就绪...
set /a waited=0
:waitfront
timeout /t 2 /nobreak >nul
set /a waited+=2
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul
if %errorlevel% equ 0 goto frontok
if %waited% geq 60 goto done
goto waitfront
:frontok
echo       前端已就绪: http://localhost:5173
start http://localhost:5173

:done
echo.
echo ================================================
echo   启动流程完成! 浏览器将自动打开登录页
echo   停止项目请运行: 停止.bat
echo ================================================
pause
exit /b 0

rem ============ 子过程: 杀掉占用指定端口的进程 ============
:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1" ^| findstr "LISTENING"') do (
    echo       清理端口 %~1 上的残留进程 PID=%%p
    taskkill /F /T /PID %%p >nul 2>nul
)
exit /b 0
