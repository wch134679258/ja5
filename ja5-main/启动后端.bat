@echo off
chcp 936 >nul
setlocal
title 后端 springBootTest 8081
cd /d "%~dp0springBootTest"

rem ============================================================
rem  后端启动：Maven 离线增量编译 + 直接 java 启动（不经 mvn 插件）
rem  相比 mvnw spring-boot:run：
rem    - 少一次 Maven 全量启动（约 7 秒）
rem    - 不依赖 spring-boot-maven-plugin（该插件离线时常缺失，会直接失败）
rem ============================================================

rem ---------- 清空干扰变量（代理 / 沙盒注入的端口变量） ----------
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "http_proxy="
set "https_proxy="
set "NO_PROXY="
set "no_proxy="
set "SERVER_PORT="
set "SERVER_ADDRESS="
set "SERVER__PORT="
set "SERVER__ADDRESS="
set "SERVER__HOST="
set "MAVEN_OPTS="

echo ================================================
echo   后端 springBootTest  http://localhost:8081
echo ================================================
echo.

where java >nul 2>nul
if errorlevel 1 (
  echo [错误] 未找到 java，请安装 JDK 17 以上并加入 PATH
  echo        当前 PATH 中没有 java.exe
  goto :fail
)

rem ---------- 端口清理：避免二次启动直接崩溃 ----------
call :killport 8081

rem ---------- MySQL 检查（只警告，不阻塞） ----------
set "DBOK=1"
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul || set "DBOK=0"
if "%DBOK%"=="0" (
  echo [警告] 3306 端口未监听：MySQL 可能没启动，后端会在连库时失败
  echo        请先启动 MySQL 服务（或运行 恢复数据.bat）
  echo.
)

rem ---------- 1. 运行时 classpath（首次 / pom 变更后生成） ----------
set "CPFILE=target\classpath.txt"
set "NEEDCP=0"
if not exist "%CPFILE%" set "NEEDCP=1"
if "%NEEDCP%"=="0" (
  call :isNewer "pom.xml" "%CPFILE%"
  if errorlevel 1 set "NEEDCP=1"
)

if "%NEEDCP%"=="1" (
  echo [1/3] 解析依赖 classpath（首次或 pom.xml 有变更）...
  call :run_cp offline
  if errorlevel 1 (
    echo       离线不可用，切换联网解析...
    call :run_cp online
  )
  if not exist "%CPFILE%" (
    echo [错误] 依赖解析失败，请检查网络或 Maven 配置
    goto :fail
  )
) else (
  echo [1/3] 依赖 classpath 已就绪，跳过
)

rem ---------- 2. 增量编译 ----------
echo [2/3] 编译源码（增量，无改动约 4 秒）...
call mvnw.cmd -B -o -q -DskipTests compile
if errorlevel 1 (
  echo       离线编译失败，切换联网重试...
  call mvnw.cmd -B -q -DskipTests compile
  if errorlevel 1 (
    echo [错误] 编译失败，请查看上方 Maven 报错
    goto :fail
  )
)
if not exist "target\classes\com\fenglin\springboottest\SpringBootTestApplication.class" (
  echo [错误] 编译产物缺失，请手动执行：mvnw.cmd clean compile
  goto :fail
)

rem ---------- 3. 直接启动 JVM ----------
echo [3/3] 启动应用...
echo.
echo   地址   http://localhost:8081
echo   接口   GET /api/auth/captcha
echo   就绪  日志出现 "Started SpringBootTestApplication" 即可
echo   停止  在本窗口按 Ctrl+C，或运行 停止项目.bat
echo   说明  改完 Java 代码重启本脚本即可（会自动增量编译）
echo.
echo ------------------------------------------------
echo.

rem 依赖列表写在 classpath.txt 里（单行，约 7KB）
rem 注意：不能用 set /p 读取，它只读前 1024 个字符，会静默截断导致 NoClassDefFoundError
set "DEPCP="
for /f "usebackq delims=" %%L in ("target\classpath.txt") do set "DEPCP=%%L"
if not defined DEPCP (
  echo [错误] target\classpath.txt 读取失败，请删除该文件后重跑本脚本
  goto :fail
)

rem -XX:TieredStopAtLevel=1：只做 C1 编译，牺牲运行期峰值换启动速度（开发环境）
rem -Xss512k：默认栈偏大，开发场景用不到
java -XX:TieredStopAtLevel=1 -Xss512k -Dspring.jmx.enabled=false -Dserver.port=8081 -cp "target\classes;%DEPCP%" com.fenglin.springboottest.SpringBootTestApplication

echo.
echo [后端] 进程已退出（退出码 %errorlevel%）
pause
exit /b 0

rem ============ 子过程：离线 / 联网解析 classpath ============
:run_cp
if /i "%~1"=="offline" (
  call mvnw.cmd -B -q -o org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=%CPFILE% -DincludeScope=runtime
) else (
  call mvnw.cmd -B -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath -Dmdep.outputFile=%CPFILE% -DincludeScope=runtime
)
exit /b %errorlevel%

rem ============ 子过程：%1 是否比 %2 新（返回 1 = 更新） ============
:isNewer
for %%a in ("%~1") do set "T1=%%~ta"
for %%a in ("%~2") do set "T2=%%~ta"
if "%T1%" GTR "%T2%" exit /b 1
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
