@echo off
chcp 936 >nul
title ja3-main 一键启动
cd /d "%~dp0"

echo ================================================
echo   ja3-main 一键启动
echo   前端: aurora-vue-2   http://localhost:5173
echo   后端: springBootTest http://localhost:8081
echo ================================================
echo.
echo 说明: 每次开机 node_modules 和 target 会被清空,
echo       两个子脚本会自动重装依赖并重新编译, 请耐心等待.
echo.

start "后端-8081" cmd /c ""%~dp0启动后端.bat""
start "前端-5173" cmd /c ""%~dp0启动前端.bat""

echo 两个独立窗口已启动:
echo   - 前端依赖安装约 1-3 分钟, 就绪较快
echo   - 后端需编译, 首次约 1-3 分钟
echo 就绪后用浏览器打开: http://localhost:5173
echo.
echo 本窗口 10 秒后自动关闭...
timeout /t 10 >nul
