@echo off
chcp 936 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"
title ja5-main 一键启动

rem ============================================================
rem  一键启动：并行拉起前后端 -> 健康检查就绪 -> 自动打开浏览器
rem  前后端各自在独立窗口里跑，日志可见，关窗口即停止
rem ============================================================

set "TIMEOUT=240"
set "HAS_CURL=1"
where curl >nul 2>nul || set "HAS_CURL=0"

echo ================================================
echo   ja5-main 一键启动
echo ------------------------------------------------
echo   前端  aurora-vue-2    http://localhost:5173
echo   后端  springBootTest  http://localhost:8081
echo ================================================
echo.

rem ---------- 1. 环境检查 ----------
echo [1/5] 环境检查...
where java >nul 2>nul
if errorlevel 1 (
  echo   [错误] 未找到 java，请安装 JDK 17 以上并加入 PATH
  goto :fail
)
where node >nul 2>nul
if errorlevel 1 (
  echo   [错误] 未找到 node，请安装 Node.js 18 以上并加入 PATH
  goto :fail
)
netstat -ano | findstr ":3306 " | findstr "LISTENING" >nul
if errorlevel 1 (
  echo   [警告] MySQL 3306 未监听，后端连库会失败
  echo          请先启动 MySQL 服务，然后重跑本脚本
  goto :fail
)
echo       Java / Node / MySQL 均正常

rem ---------- 2. 端口清理 ----------
echo [2/5] 清理残留端口...
call :killport 5173
call :killport 8081
echo       端口已就绪

rem ---------- 3. 数据库自检（被重置则询问是否恢复） ----------
echo [3/5] 数据库自检...
set "MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not exist "%MYSQL%" (
  set "MYSQL="
  where mysql >nul 2>nul && set "MYSQL=mysql"
)
set "ART="
if defined MYSQL (
  "!MYSQL!" -uroot -p123456 -N -B -e "SELECT COUNT(*) FROM springboot_test.article" >"%TEMP%\ja5_db.txt" 2>nul
  set /p ART=<"%TEMP%\ja5_db.txt"
)
del "%TEMP%\ja5_db.txt" >nul 2>nul
if "!ART!"=="" (
  echo   [警告] 数据库 springboot_test 未就绪（库表或数据缺失）
  echo          可用幂等 SQL 脚本一键恢复，约 5 秒
  choice /c yn /t 15 /d y /n /m "          现在自动恢复数据库？(Y=恢复 / N=跳过) "
  if errorlevel 2 (
    echo          已跳过，注意登录 / 搜索可能报错
  ) else (
    echo          正在恢复...
    call "%~dp0恢复数据.bat" <nul >nul 2>nul
    if errorlevel 1 (
      echo          [错误] 恢复失败，请单独运行 恢复数据.bat 查看原因
    ) else (
      echo          数据库恢复完成
    )
  )
) else (
  echo       数据库正常，文章 !ART! 篇
)

rem ---------- 4. 并行启动前后端 ----------
echo [4/5] 启动前后端（各自独立窗口，可单独查看日志）...
start "后端 8081" /d "%~dp0" cmd /k "启动后端.bat"
start "前端 5173" /d "%~dp0" cmd /k "启动前端.bat"

rem ---------- 5. 等待就绪 ----------
echo [5/5] 等待服务就绪（最多 %TIMEOUT% 秒，首次会装依赖 / 编译）...
set /a WAIT=0

:loop
set /a WAIT+=1
call :probe

if "!BE!"=="1" if "!FE!"=="1" (
  rem 再确认一次，避免端口刚被释放时的瞬时假就绪
  ping -n 2 127.0.0.1 >nul
  call :probe
  if "!BE!"=="1" if "!FE!"=="1" goto :ready
)
if !WAIT! geq %TIMEOUT% goto :timeout

set /a M=WAIT %% 5
if !M!==0 (
  set "S=后端编译中"
  if "!BE!"=="1" set "S=后端就绪"
  set "S=!S! / 前端安装中"
  if "!FE!"=="1" set "S=!S! / 前端就绪"
  echo       !WAIT! 秒... !S!
)
ping -n 2 127.0.0.1 >nul
goto :loop

:ready
echo.
echo ================================================
echo   启动完成，用时约 !WAIT! 秒
echo ------------------------------------------------
echo   前端  http://localhost:5173
echo   后端  http://localhost:8081
echo   账号  verify用户 / abc12345
echo ------------------------------------------------
echo   两个服务窗口请勿关闭，关掉即停止服务
echo   要停止全部服务：运行 停止项目.bat
echo ================================================
start "" http://localhost:5173
echo.
echo 本窗口可关闭。
ping -n 6 127.0.0.1 >nul
exit /b 0

:timeout
echo.
echo [超时] %TIMEOUT% 秒内未全部就绪
if "!BE!"=="0" echo   后端未就绪：请看「后端 8081」窗口的报错
if "!FE!"=="0" echo   前端未就绪：请看「前端 5173」窗口的报错
echo   已就绪的服务仍可直接访问；修好后重跑本脚本即可。
echo.
echo 按任意键关闭本窗口...
pause >nul
exit /b 1

:fail
echo.
echo 启动已中止。按任意键关闭...
pause >nul
exit /b 1

rem ============ 子过程：探测两个服务是否就绪 ============
:probe
set "BE=0"
set "FE=0"
if "!HAS_CURL!"=="1" (
  for /f %%c in ('curl -s -o nul -w "%%{http_code}" --max-time 2 http://localhost:8081/api/auth/captcha 2^>nul') do if "%%c"=="200" set "BE=1"
  for /f %%c in ('curl -s -o nul -w "%%{http_code}" --max-time 2 http://localhost:5173/ 2^>nul') do if "%%c"=="200" set "FE=1"
) else (
  netstat -ano | findstr ":8081 " | findstr "LISTENING" >nul && set "BE=1"
  netstat -ano | findstr ":5173 " | findstr "LISTENING" >nul && set "FE=1"
)
exit /b 0

rem ============ 子过程：结束占用指定端口的进程 ============
:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING"') do (
  taskkill /F /T /PID %%p >nul 2>nul && echo       端口 %~1 被占用，已结束 PID=%%p
)
exit /b 0
