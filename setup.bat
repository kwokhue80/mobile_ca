@echo off
echo ========================================
echo     MobileCA Setup
echo ========================================

if not exist .env (
    copy .env.example .env
    echo .env file created.
) else (
    echo .env already exists.
)

echo.
echo Setup done. Now run start.bat
pause