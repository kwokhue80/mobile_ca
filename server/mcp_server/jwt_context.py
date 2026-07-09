# ================================================================= #
#   AUTHOR(S): Kwok Heng
#   PURPOSE: Configure JWT token
# ================================================================= #
"""Request-local JWT token context for FastAPI -> MCP tool calls."""

from contextvars import ContextVar
from typing import Optional

_current_jwt_token: ContextVar[Optional[str]] = ContextVar("current_jwt_token", default=None)


def set_current_token(token: Optional[str]) -> None:
    _current_jwt_token.set(token)


def get_current_token() -> Optional[str]:
    return _current_jwt_token.get()


def clear_current_token() -> None:
    _current_jwt_token.set(None)