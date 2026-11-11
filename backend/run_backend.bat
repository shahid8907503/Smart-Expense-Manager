@echo off
title Smart Expense Manager Backend Server
echo ===================================================
echo Starting Voice Expense Manager Backend Server...
echo ===================================================

where node >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Node.js detected. Starting Node server...
    if not exist node_modules (
        echo Installing dependencies...
        call npm install
    )
    node server.js
    goto end
)

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [OK] Python detected. Starting zero-dependency Python backend server...
    python server.py
    goto end
)

echo [Notice] Neither Node.js nor Python found in PATH.
echo Please install Node.js from https://nodejs.org or run with Python.
pause

:end

