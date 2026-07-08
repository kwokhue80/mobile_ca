import os
import sys
import uuid
import logging
import re
import json
import hashlib
import time
import asyncio
from datetime import datetime, timedelta
from dataclasses import dataclass, field
from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel, Field
from typing import Any, List
from dotenv import load_dotenv, dotenv_values
from openai import AuthenticationError as OpenAIAuthenticationError
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_mcp_adapters.client import MultiServerMCPClient
try:
    from .jwt_context import set_current_token, clear_current_token, get_current_token
except ImportError:
    from jwt_context import set_current_token, clear_current_token, get_current_token
try:
    from .mcp_server_wellness import log_wellness_entry as direct_log_wellness_entry
except ImportError:
    from mcp_server_wellness import log_wellness_entry as direct_log_wellness_entry
try:
    from .mcp_server_wellness import (
        get_daily_summary as direct_get_daily_summary,
        get_activity_history as direct_get_activity_history,
        get_latest_recommendation as direct_get_latest_recommendation,
        web_search as direct_web_search,
        get_missing_required_fields,
    )
except ImportError:
    from mcp_server_wellness import (
        get_daily_summary as direct_get_daily_summary,
        get_activity_history as direct_get_activity_history,
        get_latest_recommendation as direct_get_latest_recommendation,
        web_search as direct_web_search,
        get_missing_required_fields,
    )
    
## ----------------------------------------------------------------- ##
#   AUTHOR(S): Kwok Heng, Amelia, Chai Lee
#   PURPOSE: Configure MCP agent
#
#   FULL FLOW + CONSIDERATIONS:
#   1) /api/chat receives user query, auth token is stored in request context.
#   2) Deterministic orchestration runs first (no LLM):
#      - logging state machine (_handle_logging_orchestration)
#      - read tools (_handle_read_orchestration)
#      - wellness web-search routing (_handle_wellness_web_search_orchestration)
#   3) Draft state (DRAFT_STORE) tracks partial logs across turns:
#      - awaiting_log_offer: asks whether user wants to log
#      - missing_field: prompts for required values from server-side rules
#      - awaiting_confirmation: confirms before final write when needed
#   4) If deterministic handlers do not resolve, fallback to LangChain agent + MCP tools.
#   5) Post-processing safeguards:
#      - normalize/format tool outputs for readability
#      - suppress model echo responses
#      - replace false refusal text when tool output shows success
#      - recover recommendations with safe fallback guidance
#   6) Source-of-truth split:
#      - mcp_server_wellness.py owns validation/normalization and tool semantics
#      - this file owns conversation routing, draft lifecycle, and response shaping
## ----------------------------------------------------------------- ##

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_PATH = os.path.join(BASE_DIR, ".env")
load_dotenv(ENV_PATH, override=True)

# Prefer an explicit key in server/mcp_server/.env when present.
_dotenv_values = dotenv_values(ENV_PATH)
if _dotenv_values.get("OPENROUTER_API_KEY"):
    os.environ["OPENROUTER_API_KEY"] = str(_dotenv_values["OPENROUTER_API_KEY"])
    OPENROUTER_KEY_SOURCE = ".env"
else:
    OPENROUTER_KEY_SOURCE = "process-environment"

logging.basicConfig(
    level=getattr(logging, os.getenv("LOG_LEVEL", "INFO").upper(), logging.INFO),
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("mcp_agent_wellness")

app = FastAPI(title="Wellness Chatbot API")


@dataclass
class LoggingDraft:
    payload: dict[str, Any] = field(default_factory=dict)
    missing_field: str | None = None
    awaiting_log_offer: bool = False
    awaiting_confirmation: bool = False
    updated_at_epoch: float = field(default_factory=lambda: time.time())


LOGGING_DRAFT_TTL_SECONDS = 20 * 60


class DraftStore:
    """Storage abstraction so draft state can later move to Redis cleanly."""

    def get(self, key: str) -> LoggingDraft | None:
        raise NotImplementedError()

    def set(self, key: str, draft: LoggingDraft) -> None:
        raise NotImplementedError()

    def delete(self, key: str) -> None:
        raise NotImplementedError()

    def cleanup_expired(self, ttl_seconds: int) -> None:
        raise NotImplementedError()


class InMemoryDraftStore(DraftStore):
    def __init__(self) -> None:
        self._drafts: dict[str, LoggingDraft] = {}

    def get(self, key: str) -> LoggingDraft | None:
        return self._drafts.get(key)

    def set(self, key: str, draft: LoggingDraft) -> None:
        self._drafts[key] = draft

    def delete(self, key: str) -> None:
        self._drafts.pop(key, None)

    def cleanup_expired(self, ttl_seconds: int) -> None:
        now = time.time()
        expired_keys = [k for k, v in self._drafts.items() if now - v.updated_at_epoch > ttl_seconds]
        for key in expired_keys:
            self.delete(key)


DRAFT_STORE: DraftStore = InMemoryDraftStore()


def _get_openrouter_api_key() -> str | None:
    key = os.getenv("OPENROUTER_API_KEY")
    if key is None:
        return None
    return key.strip()


def _mask_key(key: str | None) -> str:
    if not key:
        return "<missing>"
    if len(key) <= 10:
        return "<redacted>"
    return f"{key[:6]}...{key[-4:]}"


logger.info(
    "OpenRouter key source=%s envFileExists=%s key=%s",
    OPENROUTER_KEY_SOURCE,
    os.path.exists(ENV_PATH),
    _mask_key(_get_openrouter_api_key()),
)

# Points to the MCP server script, launched automatically through
# the stdio transport rather than a separate network address
mcp_client = MultiServerMCPClient(
    {
        "wellness_db": {
            "command": sys.executable,
            "args": [os.path.join(BASE_DIR, "mcp_server_wellness.py")],
            "transport": "stdio",
        }
    }
)


# System prompt: ensure good coverage of our database schema, intentions, rules
SYSTEM_PROMPT = """
You are a wellness assistant.
Use MCP tools for user-specific wellness data before answering.
If tools fail, explain briefly and continue with safe, practical guidance.
If the user asks to log any wellness data, treat it as a structured logging task.
Use the combined tool log_wellness_entry to persist records and then confirm what was logged.

Supported categories and expected fields:
- Hydration: water_intake_ml (>0)
- Weight: weight_kg (>0)
- Mood: mood_rating (1-10)
- Sleep: sleep_minutes (>0) and/or sleep_quality_rating (1-10)
- Food: meal_type + meal_description (food name) + meal_calories_kcal required
- Exercise: exercise_type + exercise_duration_minutes required; exercise_distance_km and exercise_calories_burned_kcal optional

Enum normalization rules before tool call:
- Convert meal_type and exercise_type to uppercase with underscores.
- Accept natural variants like "strength training" -> "STRENGTH_TRAINING".

Important behavior rules:
- If the user intent is logging but required fields are missing, ask concise follow-up questions to collect missing fields.
- Do not switch to generic nutrition coaching when the user is clearly trying to log data.
- If a value is invalid (for example mood 15), explain the valid range and ask for correction.
- If user provided multiple categories in one message, log all provided categories in one log_wellness_entry call when possible.
- Use record_date only when the user provides a date/time; otherwise let the tool default to now.
- Backdated logging is supported when record_date is provided in valid format.
- Never invent unsupported limitations (for example, "can only log within 24 hours") if the tool can handle the request.
"""

# Represents one message already exchanged in the conversation
class ChatMessage(BaseModel):
    text: str
    isUser: bool

# Represents the request payload sent from the Android application
class ChatRequest(BaseModel):
    query: str
    recentMessages: List[ChatMessage] = Field(default_factory=list)
    relevantPastMessages: List[ChatMessage] = Field(default_factory=list)

# Represents the final answer sent back to the Android application
class ChatResponse(BaseModel):
    answer: str


# Extract tool calls from the result
def _extract_tool_calls(result: dict) -> list[str]:
    tool_calls: list[str] = []
    messages = result.get("messages", [])

    for msg in messages:
        if isinstance(msg, dict):
            candidate_calls = msg.get("tool_calls") or []
            for call in candidate_calls:
                if isinstance(call, dict):
                    name = call.get("name") or call.get("tool")
                    if name:
                        tool_calls.append(str(name))
            continue

        candidate_calls = getattr(msg, "tool_calls", None) or []
        for call in candidate_calls:
            name = None
            if isinstance(call, dict):
                name = call.get("name") or call.get("tool")
            else:
                name = getattr(call, "name", None)
                if not name and hasattr(call, "get"):
                    name = call.get("name")
            if name:
                tool_calls.append(str(name))

    return tool_calls


def _is_affirmative(text: str) -> bool:
    return bool(re.search(r"\b(yes|yep|yeah|ok|okay|sure|please do|go ahead|confirm|proceed)\b", text, re.IGNORECASE))


def _is_negative(text: str) -> bool:
    return bool(re.search(r"\b(no|nope|don't|do not|cancel|not now|change|edit)\b", text, re.IGNORECASE))


def _draft_key_from_current_token() -> str | None:
    token = get_current_token()
    if not token:
        return None
    digest = hashlib.sha256(token.encode("utf-8")).hexdigest()
    return digest[:24]


def _cleanup_expired_drafts() -> None:
    DRAFT_STORE.cleanup_expired(LOGGING_DRAFT_TTL_SECONDS)


def _touch_draft(draft: LoggingDraft) -> None:
    draft.updated_at_epoch = time.time()


def _tool_payload_from_draft_payload(payload: dict[str, Any]) -> dict[str, Any]:
    allowed_fields = {
        "record_date",
        "water_intake_ml",
        "weight_kg",
        "mood_rating",
        "sleep_minutes",
        "sleep_quality_rating",
        "meal_type",
        "meal_description",
        "meal_calories_kcal",
        "exercise_type",
        "exercise_duration_minutes",
        "exercise_distance_km",
        "exercise_calories_burned_kcal",
    }
    return {k: v for k, v in payload.items() if k in allowed_fields}


# Convert content to text
def _content_to_text(content: Any) -> str:
    if content is None:
        return ""

    if isinstance(content, str):
        return content.strip()

    if isinstance(content, list):
        parts: list[str] = []
        for item in content:
            if isinstance(item, str):
                text = item.strip()
                if text:
                    parts.append(text)
                continue

            if isinstance(item, dict):
                text = str(item.get("text") or "").strip()
                if text:
                    parts.append(text)
                continue

            text = str(item).strip()
            if text:
                parts.append(text)

        return "\n".join(parts).strip()

    return str(content).strip()


# Get final answer from agent
def _extract_final_answer(result: dict, called_tools: list[str], request_id: str) -> str:
    messages = result.get("messages", [])

    for msg in reversed(messages):
        if isinstance(msg, dict):
            text = _content_to_text(msg.get("content"))
        else:
            text = _content_to_text(getattr(msg, "content", None))

        if text:
            return text

    logger.warning("[%s] Agent produced empty final content. toolsCalled=%s", request_id, called_tools)
    if "log_wellness_entry" in called_tools:
        return "I attempted to process your wellness log, but I could not produce a confirmation message. Please check your latest records and try once more if needed."

    return "I could not generate a response just now. Please try again."


# Regex for detecting user intent to log entry
def _looks_like_logging_intent(text: str) -> bool:
    lowered = text.lower()

    # Information-seeking calorie questions should not auto-log.
    is_question_like = (
        "?" in text
        or bool(re.search(r"\b(how many|how much|what|estimate|can you|could you|tell me)\b", lowered))
    )
    if _looks_like_exercise_calorie_query(text) and is_question_like:
        return False

    # Explicit logging verbs with a wellness category.
    explicit_pattern = re.compile(
        r"\b(log|record|track|add|save|update)\b.*\b(food|meal|hydration|water|weight|mood|sleep|exercise|workout|calories|kcal)\b",
        re.IGNORECASE,
    )
    if explicit_pattern.search(text):
        return True

    # Natural first-person activity statements that imply logging intent.
    natural_pattern = re.compile(
        r"\b(i\s+(?:ate|had|drank|slept|ran|walked|worked\s*out|exercised|did\s+(?:a\s+)?(?:workout|run|walk|jog|swim|cycle|bike|hiit|yoga|pilates)))\b",
        re.IGNORECASE,
    )
    if natural_pattern.search(text):
        return True

    # Short direct statements like "hiit for 30 minutes" or "water 500 ml".
    if any(keyword in lowered for keyword in ["workout", "exercise", "hiit", "water", "hydration", "weight", "mood", "sleep", "meal", "calories", "kcal"]):
        if any(token in lowered for token in [" minute", " minutes", " min", " hour", " hours", " ml", " liter", " litre", "kg", "/10"]):
            return True

    return False


# Regex for detecting hallucination
def _looks_like_false_logging_refusal(text: str) -> bool:
    lowered = text.lower()
    return (
        "cannot fulfill" in lowered
        or "cannot process" in lowered
        or "unable to log" in lowered
        or "unable to save" in lowered
        or "unable to access your wellness data" in lowered
        or "unable to access" in lowered
        or "cannot access" in lowered
        or "issue with my systems" in lowered
        or "please try again" in lowered
        or "try again in a little while" in lowered
        or "older than 24 hours" in lowered
        or "older than 24 hour" in lowered
        or "lack the functionality" in lowered
    )


def _maybe_parse_json_object(text: str) -> dict[str, Any] | None:
    text = text.strip()
    if not text:
        return None

    parsed = None
    try:
        parsed = json.loads(text)
    except Exception:
        pass

    if isinstance(parsed, dict):
        return parsed

    object_match = re.search(r"\{.*\}", text, re.DOTALL)
    if not object_match:
        return None

    try:
        candidate = json.loads(object_match.group(0))
        if isinstance(candidate, dict):
            return candidate
    except Exception:
        return None

    return None


def _extract_log_wellness_tool_result(result: dict) -> dict[str, Any] | None:
    """Return the latest log_wellness_entry tool payload when available."""
    messages = result.get("messages", [])

    for msg in reversed(messages):
        role = None
        name = None
        content = None

        if isinstance(msg, dict):
            role = msg.get("role")
            name = msg.get("name")
            content = msg.get("content")
        else:
            role = getattr(msg, "role", None)
            name = getattr(msg, "name", None)
            content = getattr(msg, "content", None)

        if role not in ("tool", "function"):
            continue
        if name != "log_wellness_entry":
            continue

        content_text = _content_to_text(content)
        parsed = _maybe_parse_json_object(content_text)
        if not parsed:
            continue

        if parsed.get("status") == "logged" or "categoriesLogged" in parsed or "submittedFields" in parsed:
            return parsed

    return None


def _extract_named_tool_result(result: dict, tool_name: str) -> Any | None:
    """Return latest parsed content for a specific tool message, when available."""
    messages = result.get("messages", [])

    for msg in reversed(messages):
        role = None
        name = None
        content = None

        if isinstance(msg, dict):
            role = msg.get("role")
            name = msg.get("name")
            content = msg.get("content")
        else:
            role = getattr(msg, "role", None)
            name = getattr(msg, "name", None)
            content = getattr(msg, "content", None)

        if role not in ("tool", "function"):
            continue
        if name != tool_name:
            continue

        text = _content_to_text(content)
        parsed = _maybe_parse_json_object(text)
        if parsed is not None:
            return parsed

        if text:
            return text

    return None


def _humanize_field_name(name: str) -> str:
    # Convert camelCase/snake_case to readable labels.
    text = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name)
    text = text.replace("_", " ").strip().title()
    return text


def _should_skip_tool_result_field(name: str) -> bool:
    normalized = re.sub(r"[^a-z0-9]", "", name.lower())
    return normalized in {"id", "createdat", "updatedat", "timestamp"}


def _format_tool_result_text(value: Any, indent: int = 0) -> str:
    prefix = " " * indent
    if isinstance(value, dict):
        if not value:
            return f"{prefix}No data available."
        parts: list[str] = []
        for key, item in value.items():
            if _should_skip_tool_result_field(str(key)):
                continue
            label = _humanize_field_name(str(key))
            if isinstance(item, (dict, list)):
                child_text = _format_tool_result_text(item, indent + 2)
                parts.append(f"{prefix}{label}:\n{child_text}")
            else:
                parts.append(f"{prefix}{label}: {item}")
        return "\n".join(parts) if parts else f"{prefix}No data available."

    if isinstance(value, list):
        if not value:
            return f"{prefix}none"
        lines: list[str] = []
        for item in value:
            if isinstance(item, (dict, list)):
                lines.append(_format_tool_result_text(item, indent + 2))
            else:
                lines.append(f"{prefix}- {item}")
        return "\n".join(lines)

    return f"{prefix}{value}"


def _format_read_tool_fallback(tool_name: str, tool_result: Any) -> str:
    formatted = _format_tool_result_text(tool_result)
    if tool_name == "get_daily_summary":
        return f"I fetched your daily summary data:\n{formatted}"
    if tool_name == "get_activity_history":
        return f"I fetched your recent activity history:\n{formatted}"
    if tool_name == "get_latest_recommendation":
        return f"I fetched your latest recommendation:\n{formatted}"
    return f"I fetched data from {tool_name}:\n{formatted}"


def _tool_result_looks_like_error(tool_result: Any) -> bool:
    if not isinstance(tool_result, dict):
        return False

    lowered_keys = {str(k).lower() for k in tool_result.keys()}
    if {"error", "exception", "traceback"} & lowered_keys:
        return True

    status_value = str(tool_result.get("status", "")).lower()
    if status_value in {"error", "failed", "failure"}:
        return True

    detail = str(tool_result.get("detail", "")).lower()
    if detail and any(word in detail for word in ["error", "failed", "exception", "timeout", "unauthorized"]):
        return True

    return False


def _format_tool_fallback(tool_name: str, tool_result: Any) -> str:
    if tool_name in {"get_daily_summary", "get_activity_history", "get_latest_recommendation"}:
        return _format_read_tool_fallback(tool_name, tool_result)
    if tool_name == "web_search":
        return f"I found this information from web search:\n{_format_tool_result_text(tool_result)}"
    if tool_name == "log_wellness_entry" and isinstance(tool_result, dict):
        return _format_logging_response(tool_result)
    return f"I successfully used {tool_name} and got:\n{_format_tool_result_text(tool_result)}"


def _detect_read_tool_name(query: str) -> str | None:
    lowered = query.lower()

    if any(phrase in lowered for phrase in [
        "daily summary",
        "today summary",
        "today's summary",
        "how am i doing today",
        "summary today",
    ]):
        return "get_daily_summary"

    if any(phrase in lowered for phrase in [
        "activity history",
        "exercise history",
        "recent activity",
        "my workouts",
    ]):
        return "get_activity_history"

    if any(phrase in lowered for phrase in [
        "latest recommendation",
        "my recommendation",
        "my latest recommendation",
        "latest advice",
        "any recommendation",
        "any recommendations",
        "recommendations",
        "recommendation for me",
    ]):
        return "get_latest_recommendation"

    return None


def _normalized_text_for_compare(text: str) -> str:
    lowered = text.lower().strip()
    lowered = re.sub(r"[\?\!\.,;:\"'\(\)]", "", lowered)
    lowered = re.sub(r"\s+", " ", lowered)
    return lowered


def _looks_like_echo_response(answer: str, user_query: str) -> bool:
    normalized_answer = _normalized_text_for_compare(answer)
    normalized_query = _normalized_text_for_compare(user_query)
    if not normalized_answer or not normalized_query:
        return False
    return normalized_answer == normalized_query


def _general_recommendation_fallback_text() -> str:
    return (
        "I couldn't access your personalized recommendation right now. "
        "Here are safe general wellness suggestions for today: "
        "drink a glass of water now, take a 10-15 minute walk, "
        "include one protein-rich meal, and aim for a consistent sleep time tonight."
    )


async def _handle_read_orchestration(request: ChatRequest, request_id: str) -> tuple[bool, str | None]:
    """Deterministic handler for wellness read tools to avoid LLM/tool mismatch."""
    tool_name = _detect_read_tool_name(request.query)
    if not tool_name:
        return False, None

    try:
        if tool_name == "get_daily_summary":
            tool_result = await direct_get_daily_summary()
        elif tool_name == "get_activity_history":
            tool_result = await direct_get_activity_history()
        else:
            tool_result = await direct_get_latest_recommendation()

        logger.info("[%s] Deterministic read tool %s succeeded", request_id, tool_name)
        return True, _format_read_tool_fallback(tool_name, tool_result)
    except Exception:
        logger.exception("[%s] Deterministic read tool %s failed", request_id, tool_name)
        if tool_name == "get_latest_recommendation":
            try:
                search_query = "general daily wellness recommendations healthy habits"
                search_result = str(await asyncio.to_thread(direct_web_search, search_query) or "").strip()
                if search_result:
                    return True, (
                        "I couldn't access your personalized recommendation right now, "
                        "but I found these general wellness tips from web search: "
                        f"{search_result}"
                    )
            except Exception:
                logger.exception("[%s] Recommendation fallback web search failed", request_id)
            return True, _general_recommendation_fallback_text()
        return True, "I couldn't access that wellness data right now. Please try again in a moment."


# Detects if user is uncertain about something - for agent to further probe
def _has_uncertainty_language(text: str) -> bool:
    return bool(re.search(r"\b(not sure|unsure|don't know|do not know|maybe)\b", text, re.IGNORECASE))


def _looks_like_exercise_calorie_uncertainty(text: str) -> bool:
    lowered = text.lower()
    if not _has_uncertainty_language(text):
        return False
    mentions_calories = any(token in lowered for token in ["calorie", "calories", "kcal", "burn", "burned", "burnt"])
    mentions_exercise = any(token in lowered for token in ["exercise", "workout", "run", "running", "walk", "walking", "cycle", "cycling"])
    return mentions_calories and mentions_exercise


def _exercise_calorie_backend_estimate_prompt(payload: dict[str, Any]) -> str:
    exercise_type = str(payload.get("exercise_type") or "exercise").replace("_", " ").strip()
    duration = payload.get("exercise_duration_minutes")
    duration_text = f" for {duration} minutes" if duration is not None else ""
    return (
        f"No worries. I can estimate calories burned for your {exercise_type}{duration_text} and log it for you. "
        "The estimate uses your latest weight and profile details on the backend. "
        "Would you like me to proceed?"
    )


# Extract ml from user input
def _extract_water_ml(text: str) -> int | None:
    ml_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:ml|millilit(?:er|re)s?)\b", text, re.IGNORECASE)
    if ml_match:
        return int(round(float(ml_match.group(1))))

    l_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:l|lit(?:er|re)s?)\b", text, re.IGNORECASE)
    if l_match:
        return int(round(float(l_match.group(1)) * 1000))

    return None


# Extract weight from user input
def _extract_weight_kg(text: str) -> float | None:
    kg_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:kg|kgs|kilograms?)\b", text, re.IGNORECASE)
    if not kg_match:
        return None
    return float(kg_match.group(1))


# Extract mood rating from user input
def _extract_mood_rating(text: str) -> int | None:
    out_of_ten_match = re.search(r"\b(\d{1,2})\s*/\s*10\b", text)
    if out_of_ten_match:
        return int(out_of_ten_match.group(1))

    mood_match = re.search(r"\bmood\b[^\d]{0,20}(\d{1,2})\b", text, re.IGNORECASE)
    if mood_match:
        return int(mood_match.group(1))

    rating_match = re.search(r"\brating\b[^\d]{0,20}(\d{1,2})\b", text, re.IGNORECASE)
    if rating_match and "mood" in text.lower():
        return int(rating_match.group(1))

    return None


# Extract sleep minutes/hours from user input - convert hours to minutes
def _extract_sleep_minutes(text: str) -> int | None:
    minute_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:min|mins|minute|minutes)\b", text, re.IGNORECASE)
    if minute_match:
        return int(round(float(minute_match.group(1))))

    hour_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours)\b", text, re.IGNORECASE)
    if hour_match:
        return int(round(float(hour_match.group(1)) * 60))

    return None


# Extract sleep quality rating from user input - convert plain text if required
def _extract_sleep_quality_rating(text: str) -> int | None:
    quality_out_of_ten = re.search(r"\b(?:sleep\s+quality|quality)\b[^\d]{0,20}(\d{1,2})\s*/\s*10\b", text, re.IGNORECASE)
    if quality_out_of_ten:
        return int(quality_out_of_ten.group(1))

    quality_plain = re.search(r"\b(?:sleep\s+quality|quality)\b[^\d]{0,20}(\d{1,2})\b", text, re.IGNORECASE)
    if quality_plain and "sleep" in text.lower():
        return int(quality_plain.group(1))

    return None


# Extract exercise duration from user input
def _extract_exercise_duration_minutes(text: str) -> int | None:
    minute_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:min|mins|minute|minutes)\b", text, re.IGNORECASE)
    if minute_match:
        return int(round(float(minute_match.group(1))))

    hour_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:h|hr|hrs|hour|hours)\b", text, re.IGNORECASE)
    if hour_match:
        return int(round(float(hour_match.group(1)) * 60))

    return None


# Extract exercise type from uuser input
def _extract_exercise_type(text: str) -> str | None:
    patterns = [
        (r"\b(run|ran|running|jog|jogging)\b", "running"),
        (r"\b(walk|walked|walking)\b", "walking"),
        (r"\b(swim|swam|swimming)\b", "swimming"),
        (r"\b(hike|hiking)\b", "hiking"),
        (r"\b(cycle|cycling|bike|biking)\b", "cycling"),
        (r"\bstrength training\b", "strength training"),
        (r"\b(weightlifting|weights)\b", "weightlifting"),
        (r"\bbodyweight training\b", "bodyweight training"),
        (r"\bhiit\b", "hiit"),
        (r"\bcrossfit\b", "crossfit"),
        (r"\b(yoga|pilates|stretching|rowing|jump rope|dancing|basketball|football|badminton|tennis|volleyball|martial arts|climbing)\b", None),
    ]

    for pattern, mapped in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if not match:
            continue
        if mapped is not None:
            return mapped
        return match.group(1).lower()

    return None


def _extract_meal_type(text: str) -> str | None:
    lowered = text.lower()
    for candidate in [
        "breakfast", "brunch", "lunch", "dinner", "snack", "dessert", "beverage", "other"
    ]:
        if candidate in lowered:
            return candidate
    return None


# Set prompts for missing fields before logging entry
def _missing_field_prompt(missing_field: str) -> str:
    prompts = {
        "water_intake_ml": "Please share approximately how much water you drank, for example in ml or liters.",
        "weight_kg": "Please share your weight in kg so I can log it accurately.",
        "mood_rating": "Please rate your mood from 1 to 10.",
        "sleep_minutes": "Please share how long you slept, for example in hours or minutes.",
        "sleep_quality_rating": "If you'd like, you can also rate your sleep quality from 1 to 10.",
        "meal_type": "What meal type was this, for example breakfast, lunch, dinner, or snack?",
        "meal_description": "What food did you have? Please share the food name so I can log it accurately.",
        "meal_calories_kcal": "Please share a rough calorie estimate for the meal, or the portion size so I can help estimate it.",
        "exercise_type": "What type of exercise was it?",
        "exercise_duration_minutes": "How long was the exercise session, in minutes or hours?",
    }
    return prompts.get(missing_field, "What details should I log for this wellness entry?")


# Pre-entry confirmation
def _confirmation_prompt_from_payload(payload: dict[str, Any]) -> str:
    details: list[str] = []
    if "water_intake_ml" in payload:
        details.append(f"water {payload['water_intake_ml']} ml")
    if "weight_kg" in payload:
        details.append(f"weight {payload['weight_kg']} kg")
    if "mood_rating" in payload:
        details.append(f"mood {payload['mood_rating']}/10")
    if "sleep_minutes" in payload:
        details.append(f"sleep {payload['sleep_minutes']} minutes")
    if "sleep_quality_rating" in payload:
        details.append(f"sleep quality {payload['sleep_quality_rating']}/10")
    if "meal_type" in payload or "meal_calories_kcal" in payload:
        meal_desc = payload.get("meal_description", "meal")
        details.append(f"food {meal_desc} ({payload.get('meal_calories_kcal', '?')} kcal)")
    if "exercise_type" in payload or "exercise_duration_minutes" in payload:
        details.append(
            f"exercise {payload.get('exercise_type', 'session')} for {payload.get('exercise_duration_minutes', '?')} minutes"
        )

    detail_text = ", ".join(details) if details else "this entry"
    return f"No problem, we can keep it flexible. I understood: {detail_text}. Would you like me to log this now, or would you like to adjust anything first?"


def _logging_clarification_prompt() -> str:
    return (
        "I can help log that. Could you share the key details you want saved? "
        "For example: water (ml), weight (kg), mood (1-10), sleep (hours/minutes), "
        "food (meal type + calories), or exercise (type + duration)."
    )


def _looks_like_meal_mention(text: str) -> bool:
    lowered = text.lower()
    if any(keyword in lowered for keyword in ["breakfast", "brunch", "lunch", "dinner", "snack", "meal"]):
        return True
    return bool(re.search(r"\b(i\s+(?:had|ate))\b", lowered))


def _looks_like_calorie_lookup_query(text: str) -> bool:
    lowered = text.lower()
    if not any(token in lowered for token in ["calorie", "calories", "kcal"]):
        return False
    return any(token in lowered for token in ["what", "how many", "estimate", "in", "for", "about"])


def _looks_like_portion_size_hint(text: str) -> bool:
    lowered = text.lower()

    quantity_hint = bool(re.search(r"\b(\d+(?:\.\d+)?|a|an|one|two|three|half|small|medium|large)\b", lowered))
    unit_hint = bool(
        re.search(
            r"\b(plate|bowl|serving|portion|cup|slice|piece|packet|pack|gram|grams|g|kg|kilogram|kilograms|oz|ounce|ounces|lb|lbs|pound|pounds|ml|milliliter|milliliters|l|liter|liters)\b",
            lowered,
        )
    )

    return quantity_hint and unit_hint


def _looks_like_exercise_calorie_query(text: str) -> bool:
    lowered = text.lower()
    if not any(token in lowered for token in ["calorie", "calories", "kcal"]):
        return False
    return any(
        token in lowered
        for token in ["burn", "burned", "burnt", "exercise", "workout", "run", "running", "walk", "walking", "cycle", "cycling"]
    )


def _has_meal_context(payload: dict[str, Any]) -> bool:
    return any(field in payload for field in ["meal_type", "meal_description", "meal_calories_kcal"])


def _has_exercise_context(payload: dict[str, Any]) -> bool:
    return any(field in payload for field in ["exercise_type", "exercise_duration_minutes", "exercise_calories_burned_kcal"])


def _exercise_context_from_query(query: str) -> dict[str, Any]:
    payload: dict[str, Any] = {}

    exercise_type = _extract_exercise_type(query)
    if exercise_type is not None:
        payload["exercise_type"] = exercise_type

    duration_minutes = _extract_exercise_duration_minutes(query)
    if duration_minutes is not None:
        payload["exercise_duration_minutes"] = int(round(duration_minutes))

    distance_match = re.search(r"(\d+(?:\.\d+)?)\s*km\b", query, re.IGNORECASE)
    if distance_match:
        payload["exercise_distance_km"] = float(distance_match.group(1))

    return payload


def _extract_record_date_from_text(text: str) -> str | None:
    """Parse simple natural date/time phrases into YYYY-MM-DD HH:MM:SS."""
    lowered = text.lower()

    base_date = None
    now = datetime.now()
    if "yesterday" in lowered:
        base_date = (now - timedelta(days=1)).date()
    elif "today" in lowered:
        base_date = now.date()

    if base_date is None:
        return None

    ampm_match = re.search(r"\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b", lowered, re.IGNORECASE)
    if ampm_match:
        hour = int(ampm_match.group(1))
        minute = int(ampm_match.group(2) or 0)
        ampm = ampm_match.group(3).lower()

        if hour < 1 or hour > 12 or minute < 0 or minute > 59:
            return None

        if ampm == "pm" and hour != 12:
            hour += 12
        if ampm == "am" and hour == 12:
            hour = 0

        return f"{base_date.strftime('%Y-%m-%d')} {hour:02d}:{minute:02d}:00"

    hhmm_match = re.search(r"\b([01]?\d|2[0-3]):([0-5]\d)\b", lowered)
    if hhmm_match:
        hour = int(hhmm_match.group(1))
        minute = int(hhmm_match.group(2))
        return f"{base_date.strftime('%Y-%m-%d')} {hour:02d}:{minute:02d}:00"

    return None


def _attach_record_date(payload: dict[str, Any], query: str) -> dict[str, Any]:
    record_date = _extract_record_date_from_text(query)
    if record_date:
        payload["record_date"] = record_date
    return payload


def _payload_with_only_context(payload: dict[str, Any], context: str) -> dict[str, Any]:
    base: dict[str, Any] = {}
    if "record_date" in payload:
        base["record_date"] = payload["record_date"]

    if context == "exercise":
        allowed = {
            "exercise_type",
            "exercise_duration_minutes",
            "exercise_distance_km",
            "exercise_calories_burned_kcal",
        }
    else:
        allowed = {
            "meal_type",
            "meal_description",
            "meal_calories_kcal",
        }

    for key in allowed:
        if key in payload:
            base[key] = payload[key]

    return base


def _try_apply_web_search_estimate_to_draft(query: str, search_result: str, request_id: str) -> str | None:
    estimated_kcal = _estimate_calories_from_search_text(search_result)
    if estimated_kcal is None:
        return None

    draft_key = _draft_key_from_current_token()
    if not draft_key:
        return None

    draft = DRAFT_STORE.get(draft_key)
    if not draft:
        draft = LoggingDraft()

    applied_context: str | None = None

    if _looks_like_exercise_calorie_query(query):
        # Switch to an exercise-focused draft to avoid stale meal fields
        # triggering unrelated required-field prompts.
        draft.payload = _payload_with_only_context(draft.payload, "exercise")
        parsed_exercise = _exercise_context_from_query(query)
        for key, value in parsed_exercise.items():
            draft.payload[key] = value

        if _has_exercise_context(draft.payload):
            draft.payload["exercise_calories_burned_kcal"] = int(round(estimated_kcal))
            applied_context = "exercise"
    elif _looks_like_calorie_lookup_query(query):
        # Switch to a meal-focused draft for food calorie lookups.
        draft.payload = _payload_with_only_context(draft.payload, "meal")
        if "meal_type" not in draft.payload:
            draft.payload["meal_type"] = _extract_meal_type(query) or "other"
        if "meal_description" not in draft.payload:
            draft.payload["meal_description"] = _extract_meal_description_from_calorie_query(query)
        draft.payload["meal_calories_kcal"] = int(round(estimated_kcal))
        applied_context = "meal"
    elif _has_meal_context(draft.payload) and "meal_calories_kcal" not in draft.payload:
        draft.payload["meal_calories_kcal"] = int(round(estimated_kcal))
        if "meal_type" not in draft.payload:
            draft.payload["meal_type"] = _extract_meal_type(query) or "other"
        if "meal_description" not in draft.payload:
            draft.payload["meal_description"] = _extract_meal_description(query)
        applied_context = "meal"
    elif (
        _looks_like_exercise_calorie_query(query)
        and _has_exercise_context(draft.payload)
        and "exercise_calories_burned_kcal" not in draft.payload
    ):
        draft.payload["exercise_calories_burned_kcal"] = int(round(estimated_kcal))
        applied_context = "exercise"

    if not applied_context:
        return None

    validation = _validate_or_missing(draft.payload)
    if validation and validation.get("missing"):
        draft.payload = {k: v for k, v in validation.items() if k != "missing"}
        draft.missing_field = str(validation["missing"])
        draft.awaiting_confirmation = False
        draft.awaiting_log_offer = False
        _touch_draft(draft)
        DRAFT_STORE.set(draft_key, draft)
        logger.info("[%s] Stored web-search draft with missing field=%s", request_id, draft.missing_field)
        return (
            f"I found an estimated value of about {int(round(estimated_kcal))} kcal from web search. "
            + _missing_field_prompt(draft.missing_field)
        )

    if validation:
        draft.payload = validation
    draft.missing_field = None
    draft.awaiting_log_offer = False
    draft.awaiting_confirmation = True
    _touch_draft(draft)
    DRAFT_STORE.set(draft_key, draft)
    logger.info("[%s] Stored %s draft from web-search estimate=%s", request_id, applied_context, estimated_kcal)

    if applied_context == "exercise":
        return (
            "I ran a quick web search and found an estimated calorie burn of about "
            f"{int(round(estimated_kcal))} kcal. Would you like me to log this exercise now?"
        )

    return (
        "I ran a quick web search and found an estimated calorie value of about "
        f"{int(round(estimated_kcal))} kcal per serving. Would you like me to log this meal now?"
    )


def _is_wellness_nutrition_topic(text: str) -> bool:
    lowered = text.lower()
    topic_keywords = [
        "nutrition", "nutrient", "calorie", "calories", "kcal", "protein", "carb", "fat", "fiber",
        "vitamin", "mineral", "sodium", "sugar", "cholesterol", "meal", "food", "drink", "hydration",
        "water", "exercise", "workout", "sleep", "mood", "wellness", "health", "heart rate", "steps",
        "bmi", "weight", "body fat", "recovery",
    ]
    return any(keyword in lowered for keyword in topic_keywords)


def _looks_like_wellness_web_search_request(text: str) -> bool:
    lowered = text.lower().strip()

    # Explicit search requests always trigger deterministic web search when in-topic.
    explicit_search_tokens = ["search", "look up", "lookup", "find", "google", "web", "online"]
    if any(token in lowered for token in explicit_search_tokens):
        return True

    # Factual query shapes that are better grounded with web lookup.
    factual_patterns = [
        r"\bwhat\s+(?:is|are)\b",
        r"\bhow\s+many\b",
        r"\bhow\s+much\b",
        r"\bcalories?\s+(?:in|for)\b",
        r"\bkcal\s+(?:in|for)\b",
        r"\bbenefits?\b",
        r"\bside\s*effects?\b",
        r"\bnutrition\s+facts?\b",
        r"\bmacros?\b",
    ]
    return any(re.search(pattern, lowered, re.IGNORECASE) for pattern in factual_patterns)


def _is_explicit_search_request(text: str) -> bool:
    lowered = text.lower().strip()
    explicit_search_tokens = ["search", "look up", "lookup", "find", "google", "web", "online"]
    return any(token in lowered for token in explicit_search_tokens)


def _extract_meal_description_from_calorie_query(query: str) -> str:
    text = query.strip()
    patterns = [
        r"\b(?:calories?|kcal)\s+(?:in|for)\s+(.+)$",
        r"\bhow\s+many\s+calories(?:\s+are)?(?:\s+in|\s+for)?\s+(.+)$",
    ]

    for pattern in patterns:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            description = match.group(1).strip(" .,!?:;\"'")
            if description:
                return description[:120]

    return _extract_meal_description(query)


def _format_web_search_orchestration_response(query: str, search_result: str, estimated_kcal: int | None = None) -> str:
    if _looks_like_exercise_calorie_query(query):
        estimate_line = ""
        if estimated_kcal is not None:
            estimate_line = f"\n\nEstimated calories burned: about {estimated_kcal} kcal for this session."
        return (
            "I ran a quick web search for exercise calorie-burn information and found:\n"
            f"{search_result}{estimate_line}\n\n"
            "Would you like me to log this exercise now?"
        )

    if _looks_like_calorie_lookup_query(query):
        estimate_line = ""
        if estimated_kcal is not None:
            estimate_line = f"\n\nEstimated calories: about {estimated_kcal} kcal per serving."
        return (
            "I ran a quick web search for calorie information and found:\n"
            f"{search_result}{estimate_line}\n\n"
            "Would you like me to log this meal now?"
        )

    return (
        "I ran a quick web search for your wellness/nutrition question and found:\n"
        f"{search_result}\n\n"
        "If you want, I can also help you log related wellness data."
    )


async def _handle_wellness_web_search_orchestration(request: ChatRequest, request_id: str) -> tuple[bool, str | None]:
    """Deterministic web-search path for wellness/nutrition lookup queries."""
    query = request.query.strip()
    if not query:
        return False, None

    explicit_search = _is_explicit_search_request(query)
    draft_key = _draft_key_from_current_token()
    active_draft = DRAFT_STORE.get(draft_key) if draft_key else None
    active_draft_context = bool(active_draft and (_has_meal_context(active_draft.payload) or _has_exercise_context(active_draft.payload)))

    if not _is_wellness_nutrition_topic(query):
        # Allow explicit lookup while an active wellness draft exists (for example,
        # "Search for Singapore chicken rice" during meal logging).
        if not (explicit_search and active_draft_context):
            return False, None

    if not _looks_like_wellness_web_search_request(query):
        return False, None

    # Exercise calorie-burn questions should not use web search.
    # Prepare an exercise draft and confirm backend-based estimation + logging.
    if _looks_like_exercise_calorie_query(query):
        if not draft_key:
            return False, None

        draft = active_draft or LoggingDraft()
        draft.payload = _payload_with_only_context(draft.payload, "exercise")

        parsed_exercise = _exercise_context_from_query(query)
        for key, value in parsed_exercise.items():
            draft.payload[key] = value

        if "exercise_type" not in draft.payload:
            draft.missing_field = "exercise_type"
            draft.awaiting_log_offer = False
            draft.awaiting_confirmation = False
            _touch_draft(draft)
            DRAFT_STORE.set(draft_key, draft)
            return True, _missing_field_prompt("exercise_type")

        if "exercise_duration_minutes" not in draft.payload:
            draft.missing_field = "exercise_duration_minutes"
            draft.awaiting_log_offer = False
            draft.awaiting_confirmation = False
            _touch_draft(draft)
            DRAFT_STORE.set(draft_key, draft)
            return True, _missing_field_prompt("exercise_duration_minutes")

        draft.missing_field = None
        draft.awaiting_log_offer = False
        draft.awaiting_confirmation = True
        _touch_draft(draft)
        DRAFT_STORE.set(draft_key, draft)
        logger.info("[%s] Prepared exercise draft for backend calorie estimation (no web search)", request_id)
        return True, _exercise_calorie_backend_estimate_prompt(draft.payload)

    try:
        search_result = str(await asyncio.to_thread(direct_web_search, query) or "").strip()
        if not search_result:
            logger.warning("[%s] Deterministic wellness web search returned empty result", request_id)
            return True, "I couldn't find reliable web results right now. Please try rephrasing your question."

        draft_response = _try_apply_web_search_estimate_to_draft(query, search_result, request_id)
        if draft_response:
            return True, draft_response

        estimated_kcal: int | None = None
        if _looks_like_calorie_lookup_query(query) and not _looks_like_exercise_calorie_query(query):
            estimated_kcal = _estimate_calories_from_search_text(search_result)

            draft_key = _draft_key_from_current_token()
            if draft_key and estimated_kcal is not None:
                draft = DRAFT_STORE.get(draft_key)
                if not draft:
                    draft = LoggingDraft()

                if "meal_type" not in draft.payload:
                    draft.payload["meal_type"] = _extract_meal_type(query) or "other"
                if "meal_description" not in draft.payload:
                    draft.payload["meal_description"] = _extract_meal_description_from_calorie_query(query)
                draft.payload["meal_calories_kcal"] = int(round(estimated_kcal))

                draft.awaiting_log_offer = False
                draft.awaiting_confirmation = True
                draft.missing_field = None
                _touch_draft(draft)
                DRAFT_STORE.set(draft_key, draft)
                logger.info("[%s] Stored calorie-search meal draft for confirmation", request_id)

        logger.info("[%s] Deterministic wellness web search succeeded", request_id)
        return True, _format_web_search_orchestration_response(query, search_result, estimated_kcal)
    except Exception:
        logger.exception("[%s] Deterministic wellness web search failed", request_id)
        return True, "I couldn't run a web search right now. Please try again in a moment."


def _meal_log_interest_prompt(payload: dict[str, Any]) -> str:
    meal_desc = str(payload.get("meal_description") or "that meal").strip()
    meal_type = str(payload.get("meal_type") or "").strip().lower()
    meal_text = f"{meal_desc} ({meal_type})" if meal_type else meal_desc
    return (
        f"Got it, you had {meal_text}. Are you interested in logging this meal? "
        "If yes, I can ask for calories (or estimate them via web search if you're unsure)."
    )


def _estimate_calories_from_search_text(search_text: str) -> int | None:
    """Extract a rough calorie estimate from web search snippets."""
    kcal_matches = re.findall(r"(\d{2,4})\s*(?:kcal|calories?|cals?)\b", search_text, re.IGNORECASE)
    if kcal_matches:
        values = [int(v) for v in kcal_matches]
        # Use the middle value to reduce outlier impact from snippets.
        values.sort()
        if len(values) % 2 == 0:
            upper_mid = len(values) // 2
            lower_mid = upper_mid - 1
            return int(round((values[lower_mid] + values[upper_mid]) / 2))
        return values[len(values) // 2]

    # Fallback for ranges like "450-550" without explicit unit near each number.
    range_match = re.search(r"\b(\d{2,4})\s*(?:-|to)\s*(\d{2,4})\b", search_text, re.IGNORECASE)
    if range_match:
        low = int(range_match.group(1))
        high = int(range_match.group(2))
        return int(round((low + high) / 2))

    return None


def _build_meal_calorie_search_query(payload: dict[str, Any]) -> str:
    meal_desc = str(payload.get("meal_description") or "meal").strip()
    meal_type = str(payload.get("meal_type") or "").strip().lower()
    if meal_type:
        return f"typical calories for {meal_desc} {meal_type} serving"
    return f"typical calories for {meal_desc} per serving"


def _meal_calorie_estimate_confirmation(payload: dict[str, Any], estimated_kcal: int) -> str:
    meal_desc = str(payload.get("meal_description") or "your meal").strip()
    return (
        f"No problem. I found a rough estimate of about {estimated_kcal} kcal for {meal_desc}. "
        "Would you like me to log this estimate now?"
    )


def _infer_missing_field_from_recent_assistant(
    conversation_history: List["ChatMessage"],
    current_query: str,
) -> str | None:
    """Infer which missing field was asked in the previous assistant prompt."""
    current_stripped = current_query.strip()
    skipped_current_user = False

    for msg in reversed(conversation_history[-10:]):
        if msg.isUser:
            if not skipped_current_user and msg.text.strip() == current_stripped:
                skipped_current_user = True
            continue

        text = msg.text.lower()
        if "how much water" in text or "ml or liters" in text:
            return "water_intake_ml"
        if "weight in kg" in text:
            return "weight_kg"
        if "mood" in text and "1 to 10" in text:
            return "mood_rating"
        if "how long did you sleep" in text or "hours or minutes" in text:
            return "sleep_minutes"
        if "sleep quality" in text and "1 to 10" in text:
            return "sleep_quality_rating"
        if "meal type" in text:
            return "meal_type"
        if "what food did you have" in text or "food name" in text:
            return "meal_description"
        if "calorie" in text or "portion size" in text:
            return "meal_calories_kcal"
        if "type of exercise" in text:
            return "exercise_type"
        if "exercise session" in text and ("minutes" in text or "hours" in text):
            return "exercise_duration_minutes"

    return None


def _parse_field_value_from_follow_up(missing_field: str, query: str) -> dict[str, Any] | None:
    if missing_field == "water_intake_ml":
        value = _extract_water_ml(query)
        return {"water_intake_ml": value} if value is not None else None

    if missing_field == "weight_kg":
        value = _extract_weight_kg(query)
        return {"weight_kg": value} if value is not None else None

    if missing_field == "mood_rating":
        value = _extract_mood_rating(query)
        return {"mood_rating": int(round(value))} if value is not None else None

    if missing_field == "sleep_minutes":
        value = _extract_sleep_minutes(query)
        return {"sleep_minutes": int(round(value))} if value is not None else None

    if missing_field == "sleep_quality_rating":
        value = _extract_sleep_quality_rating(query)
        return {"sleep_quality_rating": int(round(value))} if value is not None else None

    if missing_field == "meal_type":
        value = _extract_meal_type(query)
        return {"meal_type": value} if value is not None else None

    if missing_field == "meal_description":
        value = _extract_meal_description(query)
        return {"meal_description": value} if value else None

    if missing_field == "meal_calories_kcal":
        value = _extract_calorie_number(query)
        return {"meal_calories_kcal": int(round(value))} if value is not None else None

    if missing_field == "exercise_type":
        value = _extract_exercise_type(query)
        return {"exercise_type": value} if value is not None else None

    if missing_field == "exercise_duration_minutes":
        value = _extract_exercise_duration_minutes(query)
        return {"exercise_duration_minutes": int(round(value))} if value is not None else None

    return None


def _find_recent_partial_payload(conversation_history: List["ChatMessage"], current_query: str) -> dict[str, Any]:
    """Merge recent user payload fragments so follow-ups retain prior details.

    Newer messages win for overlapping fields, while older messages can still
    contribute missing fields (for example keep food name from an earlier turn).
    """
    current_stripped = current_query.strip()
    skipped_current_user = False
    merged: dict[str, Any] = {}

    for msg in reversed(conversation_history[-10:]):
        if not msg.isUser:
            continue

        if not skipped_current_user and msg.text.strip() == current_stripped:
            skipped_current_user = True
            continue

        prior = _parse_wellness_log_request(msg.text)
        if not prior:
            continue

        cleaned = {k: v for k, v in prior.items() if k != "missing"}
        for key, value in cleaned.items():
            if key not in merged:
                merged[key] = value

    return merged


# Checks validity of payload for logging
def _validate_or_missing(payload: dict[str, Any]) -> dict[str, Any] | None:
    """Return payload if valid for direct log, otherwise return a missing-field marker.
    Validation rules are sourced from server-side schema helpers.
    """
    if not payload:
        return None

    missing_fields = get_missing_required_fields(payload)
    if missing_fields:
        return {"missing": missing_fields[0], **payload}

    return payload

# Kept generic
def _resolve_logging_follow_up(
    query: str,
    conversation_history: List["ChatMessage"],
    parsed_current: dict[str, Any] | None,
) -> dict[str, Any] | None:
    """General resolver for short follow-up replies across all log types."""
    if not conversation_history:
        return parsed_current

    if parsed_current and "missing" not in parsed_current:
        return parsed_current

    missing_field = None
    if parsed_current and parsed_current.get("missing"):
        missing_field = str(parsed_current["missing"])
    else:
        missing_field = _infer_missing_field_from_recent_assistant(conversation_history, query)

    if not missing_field:
        return parsed_current

    follow_up_value = _parse_field_value_from_follow_up(missing_field, query)
    if not follow_up_value:
        return parsed_current

    merged = _find_recent_partial_payload(conversation_history, query)
    merged.update(follow_up_value)

    if "meal_type" in merged and "meal_description" not in merged:
        merged["meal_description"] = _extract_meal_description(query)

    return _validate_or_missing(merged)

# Stores user draft state -> multi-state handling -> Confirmation of log
async def _handle_logging_orchestration(request: ChatRequest, request_id: str) -> tuple[bool, str | None]:
    """Deterministic state machine for all wellness logging turns.
    Returns (handled, response)."""
    _cleanup_expired_drafts()

    draft_key = _draft_key_from_current_token()
    if not draft_key:
        if _looks_like_logging_intent(request.query):
            return True, "Please sign in again so I can safely continue your logging session."
        return False, None

    draft = DRAFT_STORE.get(draft_key)
    has_active_draft = draft is not None
    has_logging_intent = _looks_like_logging_intent(request.query)

    # If this turn is unrelated and there is no active logging draft, do not intercept.
    if not has_logging_intent and not has_active_draft:
        return False, None

    if not draft:
        draft = LoggingDraft()
        DRAFT_STORE.set(draft_key, draft)

    _touch_draft(draft)

    # Allow wellness/nutrition lookup to run independently without destroying
    # the active logging draft; user can resume logging after the lookup.
    if (
        has_active_draft
        and (
            _is_wellness_nutrition_topic(request.query)
            or (_is_explicit_search_request(request.query) and (_has_meal_context(draft.payload) or _has_exercise_context(draft.payload)))
        )
        and _looks_like_wellness_web_search_request(request.query)
        and not _is_affirmative(request.query)
        and not _is_negative(request.query)
    ):
        logger.info("[%s] Pausing active logging draft for wellness web lookup", request_id)
        return False, None

    # If the user shared a meal statement without explicit logging command,
    # ask consent first before collecting details.
    if not has_active_draft and _looks_like_meal_mention(request.query):
        parsed_meal = _parse_wellness_log_request(request.query)
        if parsed_meal and any(field in parsed_meal for field in ["meal_type", "meal_description", "meal_calories_kcal"]):
            draft.payload = {k: v for k, v in parsed_meal.items() if k != "missing"}
            draft.missing_field = str(parsed_meal.get("missing")) if parsed_meal.get("missing") else None
            draft.awaiting_log_offer = True
            _touch_draft(draft)
            return True, _meal_log_interest_prompt(draft.payload)

    if draft.awaiting_log_offer:
        if _is_negative(request.query):
            DRAFT_STORE.delete(draft_key)
            return True, "No problem. I won't log it."
        if _is_affirmative(request.query):
            draft.awaiting_log_offer = False
            validation = _validate_or_missing(draft.payload)
            if validation and validation.get("missing"):
                draft.missing_field = str(validation["missing"])
                draft.payload = {k: v for k, v in validation.items() if k != "missing"}
                _touch_draft(draft)
                # If calories are unknown, immediately pivot to web estimation flow
                # instead of repeating the missing-field prompt.
                if not (
                    draft.missing_field == "meal_calories_kcal"
                    and (
                        _has_uncertainty_language(request.query)
                        or _looks_like_calorie_lookup_query(request.query)
                        or _looks_like_portion_size_hint(request.query)
                    )
                ):
                    return True, _missing_field_prompt(draft.missing_field)
            if validation:
                draft.payload = validation
            _touch_draft(draft)
        else:
            # Keep waiting for explicit consent while preserving a deterministic flow.
            return True, "Would you like me to log that meal for you?"

    # Confirmation stage takes precedence once already asked.
    if draft.awaiting_confirmation:
        if _is_affirmative(request.query):
            # Allow confirmation turns like "Yes ... at 7AM today" to set record_date.
            parsed_record_date = _extract_record_date_from_text(request.query)
            if parsed_record_date:
                draft.payload["record_date"] = parsed_record_date

            tool_payload = _tool_payload_from_draft_payload(draft.payload)
            validation = _validate_or_missing(tool_payload)
            if not validation or validation.get("missing"):
                draft.awaiting_confirmation = False
                draft.missing_field = validation.get("missing") if validation else None
                _touch_draft(draft)
                if draft.missing_field:
                    return True, _missing_field_prompt(draft.missing_field)
                return True, _logging_clarification_prompt()

            try:
                tool_result = await direct_log_wellness_entry(**tool_payload)
                logger.info("[%s] Draft logging succeeded: %s", request_id, tool_result)
                DRAFT_STORE.delete(draft_key)
                return True, _format_logging_response(tool_result)
            except Exception:
                logger.exception("[%s] Draft logging failed", request_id)
                return True, "I ran into an issue while saving that entry. Please try again in a moment."

        if _is_negative(request.query):
            draft.awaiting_confirmation = False
            draft.missing_field = None
            _touch_draft(draft)
            return True, "No problem. Tell me what to adjust, and I will update the log draft."

        # If user provides portion details while awaiting confirmation,
        # re-estimate calories rather than falling back to a missing-field loop.
        if _looks_like_portion_size_hint(request.query) and "meal_type" in draft.payload:
            draft.awaiting_confirmation = False
            draft.missing_field = "meal_calories_kcal"
            _touch_draft(draft)

        # If user provides an explicit calorie value, treat it as an override
        # and continue with normal deterministic logging flow.
        elif _extract_calorie_number(request.query) is not None:
            calories_value = int(round(_extract_calorie_number(request.query) or 0))
            if calories_value > 0:
                if _has_meal_context(draft.payload):
                    draft.payload["meal_calories_kcal"] = calories_value
                    draft.missing_field = None
                elif _has_exercise_context(draft.payload):
                    draft.payload["exercise_calories_burned_kcal"] = calories_value
                draft.awaiting_confirmation = False
                _touch_draft(draft)

        # Treat free text here as an update to the draft and continue.
        else:
            draft.awaiting_confirmation = False

    # Special-case meal calories: if user is unsure, estimate via web search then ask for confirmation.
    if draft.missing_field == "meal_calories_kcal" and (
        _has_uncertainty_language(request.query)
        or _looks_like_calorie_lookup_query(request.query)
        or _looks_like_portion_size_hint(request.query)
    ):
        try:
            if _looks_like_calorie_lookup_query(request.query):
                search_query = request.query
            elif _looks_like_portion_size_hint(request.query):
                search_query = f"{_build_meal_calorie_search_query(draft.payload)} {request.query}"
            else:
                search_query = _build_meal_calorie_search_query(draft.payload)
            search_text = str(await asyncio.to_thread(direct_web_search, search_query) or "")
            estimated_kcal = _estimate_calories_from_search_text(search_text)
            if estimated_kcal is None:
                return True, (
                    "No worries. I couldn't find a reliable calorie estimate from web results just now. "
                    "Could you share a rough portion size, or should I try another estimate query?"
                )

            draft.payload["meal_calories_kcal"] = int(round(estimated_kcal))
            draft.awaiting_confirmation = True
            draft.missing_field = None
            _touch_draft(draft)
            logger.info("[%s] Estimated meal calories via web search: %s", request_id, estimated_kcal)
            return True, "I did a quick web search for calorie estimates. " + _meal_calorie_estimate_confirmation(draft.payload, estimated_kcal)
        except Exception:
            logger.exception("[%s] Meal calorie estimation via web search failed", request_id)
            return True, (
                "No worries. I couldn't estimate calories from web results right now. "
                "Could you share a rough calorie range or portion size instead?"
            )

    parsed = _parse_wellness_log_request(request.query)
    parsed = _resolve_logging_follow_up(request.query, request.recentMessages, parsed)

    if parsed is None:
        # Keep draft alive for short follow-up answers.
        return True, _logging_clarification_prompt()

    if parsed.get("missing"):
        for k, v in parsed.items():
            if k != "missing":
                draft.payload[k] = v
        draft.missing_field = str(parsed["missing"])
        _touch_draft(draft)
        return True, _missing_field_prompt(draft.missing_field)

    # Merge any new values into draft.
    for k, v in parsed.items():
        if k != "missing":
            # Do not overwrite an existing specific meal description with a placeholder.
            if (
                k == "meal_description"
                and str(v).strip().lower() == "logged meal"
                and str(draft.payload.get("meal_description", "")).strip()
                and str(draft.payload.get("meal_description", "")).strip().lower() != "logged meal"
            ):
                continue
            draft.payload[k] = v

    validation = _validate_or_missing(draft.payload)
    if not validation:
        draft.missing_field = None
        _touch_draft(draft)
        return True, _logging_clarification_prompt()

    if validation.get("missing"):
        draft.missing_field = str(validation["missing"])
        draft.payload = {k: v for k, v in validation.items() if k != "missing"}
        _touch_draft(draft)
        return True, _missing_field_prompt(draft.missing_field)

    draft.payload = validation
    draft.missing_field = None

    if (
        _has_exercise_context(draft.payload)
        and "exercise_calories_burned_kcal" not in draft.payload
        and _has_uncertainty_language(request.query)
    ):
        draft.awaiting_confirmation = True
        _touch_draft(draft)
        return True, _exercise_calorie_backend_estimate_prompt(draft.payload)

    if _has_uncertainty_language(request.query):
        draft.awaiting_confirmation = True
        _touch_draft(draft)
        return True, _confirmation_prompt_from_payload(draft.payload)

    tool_payload = _tool_payload_from_draft_payload(draft.payload)
    try:
        tool_result = await direct_log_wellness_entry(**tool_payload)
        logger.info("[%s] Direct orchestrated logging succeeded: %s", request_id, tool_result)
        DRAFT_STORE.delete(draft_key)
        return True, _format_logging_response(tool_result)
    except Exception:
        logger.exception("[%s] Direct orchestrated logging failed", request_id)
        return True, "I ran into an issue while saving that entry. Please try again in a moment."


# Extract calories from user input
def _extract_calorie_number(text: str) -> float | None:
    """Extract calories only when explicitly stated as calorie-related units.
    This avoids treating unrelated numbers (like times) as calories."""
    match = re.search(r"(\d+(?:\.\d+)?)\s*(?:kcal|calories?|cals?)\b", text, re.IGNORECASE)
    if not match:
        return None
    return float(match.group(1))


# Extract meal description from user input - avoid storing whole prompt
def _extract_meal_description(query: str) -> str | None:
    """Extract a concise food description instead of storing the whole user prompt."""
    text = query.strip()

    had_or_ate = re.search(
        r"\b(?:i\s+)?(?:had|ate)\s+(.+?)(?:\s+for\s+(?:breakfast|brunch|lunch|dinner|snack|meal)\b|$)",
        text,
        re.IGNORECASE,
    )
    if had_or_ate:
        text = had_or_ate.group(1).strip()

    # Remove trailing time/date fragments often included in free-form chat.
    text = re.sub(r"\s+at\s+\d{1,2}(?::\d{2})?\s*(?:am|pm)?\b.*$", "", text, flags=re.IGNORECASE)
    text = re.sub(r"\s+(?:today|yesterday|this\s+morning|this\s+afternoon|tonight)\b.*$", "", text, flags=re.IGNORECASE)

    # Remove leading uncertainty phrases from the stored meal text.
    text = re.sub(r"^(?:i\s+am\s+not\s+sure|i'm\s+not\s+sure|not\s+sure|maybe|i\s+think)\s*,?\s*", "", text, flags=re.IGNORECASE)
    text = text.strip(" .,!?:;\"'")

    # Ignore generic logging intents; they are not food names.
    generic_intent_patterns = [
        r"^help\s+me\s+log\s+(?:a\s+)?(?:food|meal)\s+entry$",
        r"^log\s+(?:a\s+)?(?:food|meal)\s+entry$",
        r"^(?:food|meal)\s+entry$",
        r"^log\s+(?:food|meal)$",
        r"^i\s+want\s+to\s+log\s+(?:a\s+)?(?:food|meal)\s+entry$",
        r"^i\s+want\s+to\s+log\s+(?:food|meal)$",
        r"^i\s+want\s+to\s+record\s+(?:a\s+)?(?:food|meal)\s+entry$",
        r"^i\s+want\s+to\s+record\s+(?:food|meal)$",
    ]
    if any(re.match(pattern, text, re.IGNORECASE) for pattern in generic_intent_patterns):
        return None

    # Calorie-only replies are numeric metadata, not food names.
    if re.match(r"^\d+(?:\.\d+)?\s*(?:kcal|calories?|cals?)$", text, re.IGNORECASE):
        return None

    # Avoid saving a plain meal type as the meal description.
    if text.lower() in {
        "breakfast",
        "brunch",
        "lunch",
        "dinner",
        "snack",
        "dessert",
        "beverage",
        "other",
    }:
        return None

    return text[:120] if text else None


# Parse log request based on corresponding details
def _parse_wellness_log_request(query: str) -> dict[str, Any] | None:
    lowered = query.lower()

    if any(keyword in lowered for keyword in ["water", "hydration", "drink", "drank"]):
        amount = _extract_water_ml(query)
        if amount is None:
            return _attach_record_date({"missing": "water_intake_ml"}, query)
        return _attach_record_date({"water_intake_ml": int(round(amount))}, query)

    if "weight" in lowered:
        amount = _extract_weight_kg(query)
        if amount is None:
            return _attach_record_date({"missing": "weight_kg"}, query)
        return _attach_record_date({"weight_kg": amount}, query)

    if "mood" in lowered:
        amount = _extract_mood_rating(query)
        if amount is None:
            return _attach_record_date({"missing": "mood_rating"}, query)
        return _attach_record_date({"mood_rating": int(round(amount))}, query)

    if any(keyword in lowered for keyword in ["sleep", "slept"]):
        minutes = _extract_sleep_minutes(query)
        quality = _extract_sleep_quality_rating(query)
        if minutes is None and quality is None:
            return _attach_record_date({"missing": "sleep_minutes"}, query)

        payload: dict[str, Any] = {}
        if minutes is not None:
            payload["sleep_minutes"] = int(round(minutes))
        if quality is not None:
            payload["sleep_quality_rating"] = int(round(quality))
        return _attach_record_date(payload, query)

    if any(keyword in lowered for keyword in ["breakfast", "lunch", "dinner", "snack", "brunch", "meal", "eat", "ate", "food", "had"]):
        calories = _extract_calorie_number(query)
        meal_type = _extract_meal_type(query)
        meal_description = _extract_meal_description(query)

        payload: dict[str, Any] = {}
        if meal_type is not None:
            payload["meal_type"] = meal_type
        if meal_description:
            payload["meal_description"] = meal_description
        if calories is not None:
            payload["meal_calories_kcal"] = int(round(calories))
        if not payload:
            return _attach_record_date({"missing": "meal_type"}, query)
        return _attach_record_date(payload, query)

    exercise_type = _extract_exercise_type(query)
    has_exercise_keyword = any(
        keyword in lowered
        for keyword in [
            "exercise",
            "workout",
            "run",
            "ran",
            "walk",
            "walked",
            "cycle",
            "cycling",
            "bike",
            "biked",
            "jog",
            "jogging",
            "swim",
            "swam",
            "worked out",
        ]
    )
    if has_exercise_keyword or exercise_type is not None:
        duration = _extract_exercise_duration_minutes(query)

        payload: dict[str, Any] = {
        }

        if exercise_type is not None:
            payload["exercise_type"] = exercise_type

        if duration is not None:
            payload["exercise_duration_minutes"] = int(round(duration))

        if not payload:
            return _attach_record_date({"missing": "exercise_type"}, query)

        distance_match = re.search(r"(\d+(?:\.\d+)?)\s*km", query, re.IGNORECASE)
        if distance_match:
            payload["exercise_distance_km"] = float(distance_match.group(1))

        calories_match = re.search(r"(\d+(?:\.\d+)?)\s*(?:kcal|calories?|cals?)\b", query, re.IGNORECASE)
        if calories_match:
            payload["exercise_calories_burned_kcal"] = int(round(float(calories_match.group(1))))

        return _attach_record_date(payload, query)

    return None


# Confirmation message after logging
def _format_logging_response(result: dict[str, Any]) -> str:
    categories = result.get("categoriesLogged") or []
    submitted = result.get("submittedFields") or []
    if categories:
        category_text = ", ".join(categories)
        return f"Logged your {category_text} entry successfully."
    if submitted:
        field_text = ", ".join(submitted)
        return f"Logged your wellness entry successfully. Fields saved: {field_text}."
    return "Logged your wellness entry successfully."


# Full function
async def ask_agent(request: ChatRequest, request_id: str) -> str:
    """
    Builds the conversation for the language model and runs the agent,
    combining the current question with recent messages and any older
    messages found to be relevant through semantic search.
    """
    handled, logging_response = await _handle_logging_orchestration(request, request_id)
    if handled:
        return logging_response or "I can help log that. Please share the details you want saved."

    read_handled, read_response = await _handle_read_orchestration(request, request_id)
    web_search_handled, web_search_response = await _handle_wellness_web_search_orchestration(request, request_id)

    deterministic_responses: list[str] = []
    if read_handled:
        deterministic_responses.append(read_response or "I fetched your requested wellness data.")
    if web_search_handled:
        deterministic_responses.append(web_search_response or "I ran a web search and found some results.")
    if deterministic_responses:
        return "\n\n".join(deterministic_responses)

    tools = await mcp_client.get_tools()
    tool_names = sorted([tool.name for tool in tools])
    logger.info("[%s] MCP tools available: %s", request_id, tool_names)

    api_key = _get_openrouter_api_key()
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="OPENROUTER_API_KEY is missing in server/mcp_server/.env",
        )

    llm = ChatOpenAI(
        model="google/gemini-2.5-flash-lite",
        temperature=0,
        base_url="https://openrouter.ai/api/v1",
        api_key=api_key,
    )

    agent = create_agent(llm, tools=tools, system_prompt=SYSTEM_PROMPT)

    conversation_messages = []

    # Older messages found to be relevant are included as background
    # information, separate from the immediate flow of the conversation
    if request.relevantPastMessages:
        relevant_text = "\n".join(
            f"Earlier, the {'user' if msg.isUser else 'assistant'} said: \"{msg.text}\""
            for msg in request.relevantPastMessages
        )
        conversation_messages.append({
            "role": "system",
            "content": f"Relevant earlier context:\n{relevant_text}"
        })

    # Recent messages are added in order to preserve natural flow of the conversation
    for msg in request.recentMessages:
        role = "user" if msg.isUser else "assistant"
        conversation_messages.append({"role": role, "content": msg.text})

    # Avoid duplicating the latest user turn when the client already
    # includes the freshly sent query in recentMessages.
    if not (
        request.recentMessages
        and request.recentMessages[-1].isUser
        and request.recentMessages[-1].text.strip() == request.query.strip()
    ):
        conversation_messages.append({"role": "user", "content": request.query})

    logger.info(
        "[%s] Processing chat query. history=%s relevant=%s",
        request_id,
        len(request.recentMessages),
        len(request.relevantPastMessages),
    )
    result = await agent.ainvoke({"messages": conversation_messages})
    # At this point the chain is:
    # FastAPI endpoint -> LangChain agent -> MCP tool(s) -> Spring backend APIs
    called_tools = _extract_tool_calls(result)
    logger.info("[%s] MCP tools called by agent: %s", request_id, called_tools)

    if _looks_like_logging_intent(request.query) and "log_wellness_entry" not in called_tools:
        logger.warning(
            "[%s] Logging intent detected but log_wellness_entry was not called. query=%s",
            request_id,
            request.query,
        )

    final_answer = _extract_final_answer(result, called_tools, request_id)

    # Prevent low-quality echo responses (model repeating user input).
    if _looks_like_echo_response(final_answer, request.query):
        logger.warning("[%s] Detected echo response; attempting deterministic fallback", request_id)
        read_tool_name = _detect_read_tool_name(request.query)
        if read_tool_name == "get_daily_summary":
            try:
                tool_result = await direct_get_daily_summary()
                return _format_read_tool_fallback("get_daily_summary", tool_result)
            except Exception:
                logger.exception("[%s] Echo fallback daily summary failed", request_id)
        elif read_tool_name == "get_activity_history":
            try:
                tool_result = await direct_get_activity_history()
                return _format_read_tool_fallback("get_activity_history", tool_result)
            except Exception:
                logger.exception("[%s] Echo fallback activity history failed", request_id)
        elif read_tool_name == "get_latest_recommendation":
            try:
                tool_result = await direct_get_latest_recommendation()
                return _format_read_tool_fallback("get_latest_recommendation", tool_result)
            except Exception:
                logger.exception("[%s] Echo fallback recommendation failed", request_id)

        return "I couldn't find a clear answer yet. If you want, I can fetch your latest wellness recommendation or look this up on the web."

    if "log_wellness_entry" in called_tools:
        tool_result = _extract_log_wellness_tool_result(result)
        if tool_result and _looks_like_false_logging_refusal(final_answer):
            logger.warning(
                "[%s] Replacing hallucinated logging refusal after successful tool result. original=%s",
                request_id,
                final_answer[:240],
            )
            final_answer = _format_logging_response(tool_result)
        elif not tool_result and _looks_like_false_logging_refusal(final_answer):
            logger.warning(
                "[%s] Logging refusal detected but no successful tool payload found; keeping model text.",
                request_id,
            )

    if _looks_like_false_logging_refusal(final_answer):
        for tool_name in called_tools:
            tool_result = _extract_named_tool_result(result, tool_name)
            if tool_result is None:
                continue

            if _tool_result_looks_like_error(tool_result):
                continue

            logger.warning(
                "[%s] Replacing false refusal after successful %s tool output.",
                request_id,
                tool_name,
            )
            final_answer = _format_tool_fallback(tool_name, tool_result)
            break

    logger.info("[%s] Final answer length=%s preview=%s", request_id, len(final_answer), final_answer[:180])
    return final_answer


# Tools endpoint
@app.get("/api/tools")
async def list_available_tools():
    """Debugging endpoint used to check which tools the agent can see."""
    try:
        tools = await mcp_client.get_tools()
        tool_names = [tool.name for tool in tools]
        logger.info("/api/tools requested. Returning %s tools.", len(tool_names))
        return tool_names
    except Exception as error:
        logger.exception("/api/tools failed")
        raise HTTPException(status_code=500, detail=str(error))


# Main endpoint
@app.post("/api/chat")
async def chat_endpoint(request: ChatRequest, authorization: str = Header(None)):
    """
    Main endpoint called by the Android application. Stores the
    authorization token for this request so tool functions can use
    it when calling the Spring Boot backend, then runs the agent.
    """
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing Authorization header")

    request_id = uuid.uuid4().hex[:8]
    logger.info("[%s] /api/chat request received", request_id)

    try:
        # Token is stored in request-local context so MCP tool calls can forward
        # the same user identity to Spring Boot endpoints
        set_current_token(authorization)
        answer_text = await ask_agent(request, request_id)
        logger.info("[%s] /api/chat completed successfully", request_id)
        return ChatResponse(answer=answer_text)
    except OpenAIAuthenticationError:
        logger.exception("[%s] OpenRouter authentication failed", request_id)
        raise HTTPException(
            status_code=502,
            detail="OpenRouter authentication failed. Check OPENROUTER_API_KEY in server/mcp_server/.env",
        )
    except Exception as error:
        logger.exception("[%s] /api/chat failed", request_id)
        raise HTTPException(status_code=500, detail=f"[{request_id}] Agent execution failed: {str(error)}")
    finally:
        clear_current_token()


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)