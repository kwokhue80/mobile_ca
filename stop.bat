@echo off
echo Stopping MobileCA services...

taskkill /FI "WindowTitle eq Spring Boot Backend*" /F >nul 2>&1
taskkill /FI "WindowTitle eq FastAPI Bridge*" /F >nul 2>&1

echo Services stopped.
pause