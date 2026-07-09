# ================================================================= #
#   AUTHOR(S): Kwok Heng, Chai Lee
#   PURPOSE: Set up client to connect to spring boot backend
# ================================================================= #
import os
import httpx
try:
    from .jwt_context import get_current_token
except ImportError:
    from jwt_context import get_current_token

SPRING_BOOT_BASE_URL = os.getenv("SPRING_BOOT_BASE_URL", "http://localhost:8000").rstrip("/")

def _build_headers():
    token = get_current_token()
    if not token:
        return {}

    # Normalize header value if caller passes only the raw JWT.
    if not token.lower().startswith("bearer "):
        token = f"Bearer {token}"
    return {"Authorization": token}


def _parse_response(response: httpx.Response):
    if response.status_code >= 400:
        body = response.text.strip()
        body_preview = body[:400] if body else "<empty body>"
        raise RuntimeError(
            f"Spring API error {response.status_code}: {body_preview}"
        )

    if not response.content:
        return {"status": "ok"}

    content_type = response.headers.get("content-type", "")
    if "application/json" in content_type:
        return response.json()
    return {"status": "ok", "raw": response.text}

async def call_spring_boot(path, params=None):
    """
    Sends a GET request to the Spring Boot backend, attaching the
    current JWT token so the request can be authenticated.
    """
    headers = _build_headers()

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get(f"{SPRING_BOOT_BASE_URL}{path}", headers=headers, params=params)
        return _parse_response(response)


async def post_spring_boot(path, payload):
    """Sends a POST request with JSON payload to Spring Boot backend."""
    headers = _build_headers()

    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.post(f"{SPRING_BOOT_BASE_URL}{path}", headers=headers, json=payload)
        return _parse_response(response)