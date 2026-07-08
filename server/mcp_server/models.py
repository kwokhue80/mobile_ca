from dataclasses import dataclass, field
from typing import Any, List, Optional
from pydantic import BaseModel, Field
from enum import Enum
import time


class WorkflowActionType(str, Enum):
    LOGGING = "logging"
    READ = "read"
    WEB_SEARCH = "web_search"
    LLM = "llm"


@dataclass
class LoggingDraft:
    payload: dict[str, Any] = field(default_factory=dict)
    missing_field: Optional[str] = None
    awaiting_log_offer: bool = False
    awaiting_confirmation: bool = False
    updated_at_epoch: float = field(default_factory=lambda: time.time())


@dataclass(frozen=True)
class WorkflowAction:
    action_type: WorkflowActionType
    terminal_on_handle: bool = False
    accumulate_response: bool = False
    target: Optional[str] = None
    query_override: Optional[str] = None
    logging_task: Optional["LoggingTask"] = None


@dataclass(frozen=True)
class LoggingTask:
    query: str
    recent_messages: List["ChatMessage"]
    relevant_past_messages: List["ChatMessage"]
    intent_detected: bool
    active_draft_present: bool
    meal_mention_detected: bool


@dataclass
class WorkflowActionResult:
    handled: bool
    response: Optional[str] = None


@dataclass
class WorkflowState:
    request: "ChatRequest"
    request_id: str
    actions: List[WorkflowAction] = field(default_factory=list)
    deterministic_responses: List[str] = field(default_factory=list)
    observations: List[str] = field(default_factory=list)
    goal_summary: Optional[str] = None
    plan_revision_count: int = 0
    last_observation_confidence: float = 1.0
    prefer_clarification: bool = False


class ChatMessage(BaseModel):
    text: str
    isUser: bool


class ChatRequest(BaseModel):
    query: str
    recentMessages: List[ChatMessage] = Field(default_factory=list)
    relevantPastMessages: List[ChatMessage] = Field(default_factory=list)


class ChatResponse(BaseModel):
    answer: str