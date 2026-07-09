# ================================================================= #
#   AUTHOR(S): Amelia
#   PURPOSE: In memory tracking of chat
# ================================================================= #
import time
from collections import OrderedDict
from typing import Optional

try:
    from .models import LoggingDraft
except ImportError:
    from models import LoggingDraft


class InMemoryDraftStore:
    """Thread-safe enough in-memory draft store with TTL + size limit."""

    def __init__(self, maxsize: int = 100, ttl_seconds: int = 1200):
        self._drafts: OrderedDict[str, LoggingDraft] = OrderedDict()
        self.maxsize = maxsize
        self.ttl_seconds = ttl_seconds

    def get(self, key: str) -> Optional[LoggingDraft]:
        self._cleanup_expired()
        return self._drafts.get(key)

    def set(self, key: str, draft: LoggingDraft) -> None:
        self._cleanup_expired()
        self._drafts[key] = draft
        if len(self._drafts) > self.maxsize:
            self._drafts.popitem(last=False)

    def delete(self, key: str) -> None:
        self._drafts.pop(key, None)

    def _cleanup_expired(self) -> None:
        now = time.time()
        expired_keys = [
            k for k, v in list(self._drafts.items())
            if now - v.updated_at_epoch > self.ttl_seconds
        ]
        for key in expired_keys:
            self.delete(key)

    def cleanup_expired(self, ttl_seconds: Optional[int] = None) -> None:
        if ttl_seconds:
            self.ttl_seconds = ttl_seconds
        self._cleanup_expired()