@echo off
echo ========================================
echo     MobileCA - Starting Services
echo ========================================

echo.
echo Starting Spring Boot Backend...
start "Spring Boot Backend" cmd /k "cd server && .\mvnw.cmd spring-boot:run"

echo.
echo Starting FastAPI Chatbot Bridge...
start "FastAPI Bridge" cmd /k "cd server\mcp_server && python -m uvicorn mcp_agent_wellness:app --app-dir . --host 0.0.0.0 --port 8001 --reload"

echo.
echo Services started in two windows.
echo Spring Boot  → http://localhost:8000
echo FastAPI      → http://localhost:8001
echo.
echo Close the windows to stop.
pause