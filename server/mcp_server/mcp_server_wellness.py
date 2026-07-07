from mcp.server.fastmcp import FastMCP
import httpx
from jwt_context import get_current_token
from spring_boot_client import call_spring_boot

mcp = FastMCP("Wellness App Database")

SPRING_BOOT_BASE_URL = "http://localhost:8080/api"

def call_spring_boot(path: str, params: dict = None):
    """Sends a request to Spring Boot, attaching the current JWT token
    so the request can be authenticated on the server side."""
    token = get_current_token()
    headers = {"Authorization": token} if token else {}

    response = httpx.get(f"{SPRING_BOOT_BASE_URL}{path}", headers=headers, params=params)
    response.raise_for_status()
    return response.json()

@mcp.tool()
def get_user_meal_history(days: int = 7):
    """Fetches the logged meals for the current user over a given number of past days."""
    return call_spring_boot("/meals", params={"days": days})

@mcp.tool()
def get_user_activity_summary(days: int = 7):
    """Fetches the logged activity and exercise records for the current user."""
    return call_spring_boot("/activity", params={"days": days})

if __name__ == "__main__":
    mcp.run(transport="stdio")