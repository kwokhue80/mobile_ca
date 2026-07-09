# Author: Amelia Wong
FROM python:3.11-slim
WORKDIR /app

COPY requirements.txt requirements-dev.txt ./
RUN pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt

COPY . .

EXPOSE 8001
CMD ["python", "-m", "uvicorn", "mcp_agent_wellness:app", "--host", "0.0.0.0", "--port", "8001", "--reload"]