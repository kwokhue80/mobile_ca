import httpx
from jwt_context import get_current_token

SPRING_BOOT_BASE_URL = "http://localhost:8080/api"

async def call_spring_boot(path, params=None):
    """
    Sends a GET request to the Spring Boot backend, attaching the
    current JWT token so the request can be authenticated.
    """
    token = get_current_token()
    headers = {"Authorization": token} if token else {}

    async with httpx.AsyncClient() as client:
        response = await client.get(f"{SPRING_BOOT_BASE_URL}{path}", headers=headers, params=params)
        response.raise_for_status()
        return response.json()