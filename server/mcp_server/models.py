# ================================================================= #
#   AUTHOR(S): Kwok Heng, Amelia, Chai Lee
#   PURPOSE: Separation of concern from main code
# ================================================================= #
from dataclasses import dataclass, field
from typing import Any, List, Optional
from pydantic import BaseModel, Field
import time

@dataclass
class LoggingDraft:
    payload: dict[str, Any] = field(default_factory=dict)
    missing_field: Optional[str] = None
    updated_at_epoch: float = field(default_factory=lambda: time.time())


@dataclass
class WorkflowState:
    request: "ChatRequest"
    request_id: str


class ChatMessage(BaseModel):
    text: str
    isUser: bool


class ChatRequest(BaseModel):
    query: str
    recentMessages: List[ChatMessage] = Field(default_factory=list)
    relevantPastMessages: List[ChatMessage] = Field(default_factory=list)


class ChatResponse(BaseModel):
    answer: str