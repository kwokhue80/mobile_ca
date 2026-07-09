# ================================================================= #
#   AUTHOR(S): Amelia (AI-assisted; Reason: Some unfamiliar concepts)
#   PURPOSE: Separation of concern from main code
# ================================================================= #

from datetime import datetime
import json
import os
import logging
from typing import Any, Awaitable, Callable

from langchain_openai import ChatOpenAI

logging.basicConfig(
    level=getattr(logging, os.getenv("LOG_LEVEL", "INFO").upper(), logging.INFO),
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("mcp_server_wellness")

_RECOMMENDATION_SYSTEM_PROMPT = """
You generate concise mobile wellness recommendations.
Return ONLY a JSON object with exactly these keys:
- type: goal_based | profile_based | general
- title: short title
- message: single line, max 140 chars
- category: short snake_case category
- priority: low | medium | high
- actionable: boolean

No markdown, no explanations, no extra keys.
""".strip()


# ================================================================= #
#   RECOMMENDATION PAYLOAD HELPERS
# ================================================================= #


def recommendation_text_from_tool_result(value: Any) -> str:
    if isinstance(value, dict):
        for key in ["recommendation", "message", "text"]:
            candidate = value.get(key)
            if candidate is not None:
                return str(candidate)
        return json.dumps(value)
    if value is None:
        return ""
    return str(value)


async def _safe_recommendation_fetch(
    request_id: str,
    label: str,
    fetcher: Callable[[], Awaitable[Any]],
    default: Any,
) -> Any:
    try:
        return await fetcher()
    except Exception:
        logger.exception("[%s] Failed to fetch %s for recommendation endpoint", request_id, label)
        return default


async def collect_recommendation_context(
    *,
    request_id: str,
    fetch_summary: Callable[[], Awaitable[Any]],
    fetch_exercise_history: Callable[[], Awaitable[Any]],
    fetch_latest_recommendation: Callable[[], Awaitable[Any]],
) -> tuple[dict[str, Any], Any, Any]:
    raw_summary = await _safe_recommendation_fetch(
        request_id=request_id,
        label="summary",
        fetcher=fetch_summary,
        default={},
    )
    summary = raw_summary if isinstance(raw_summary, dict) else {}

    exercise_history = await _safe_recommendation_fetch(
        request_id=request_id,
        label="exercise history",
        fetcher=fetch_exercise_history,
        default=[],
    )

    latest_recommendation_raw = await _safe_recommendation_fetch(
        request_id=request_id,
        label="latest recommendation fallback text",
        fetcher=fetch_latest_recommendation,
        default=None,
    )

    return summary, exercise_history, latest_recommendation_raw


async def build_personalized_recommendation_payload(
    *,
    request_id: str,
    llm: ChatOpenAI,
    fetch_summary: Callable[[], Awaitable[Any]],
    fetch_exercise_history: Callable[[], Awaitable[Any]],
    fetch_latest_recommendation: Callable[[], Awaitable[Any]],
) -> dict[str, Any]:
    summary, exercise_history, latest_recommendation_raw = await collect_recommendation_context(
        request_id=request_id,
        fetch_summary=fetch_summary,
        fetch_exercise_history=fetch_exercise_history,
        fetch_latest_recommendation=fetch_latest_recommendation,
    )

    return await generate_agentic_recommendation(
        llm=llm,
        summary=summary,
        exercise_history=exercise_history,
        latest_recommendation=recommendation_text_from_tool_result(latest_recommendation_raw),
    )


def _extract_json_object(text: str) -> dict[str, Any] | None:
    payload = text.strip()
    if not payload:
        return None

    try:
        parsed = json.loads(payload)
        if isinstance(parsed, dict):
            return parsed
    except Exception:
        pass

    start = payload.find("{")
    end = payload.rfind("}")
    if start == -1 or end == -1 or end <= start:
        return None

    try:
        parsed = json.loads(payload[start : end + 1])
        if isinstance(parsed, dict):
            return parsed
    except Exception:
        return None

    return None


def normalize_recommendation_payload(candidate: dict[str, Any]) -> dict[str, Any]:
    allowed_types = {"goal_based", "profile_based", "general"}
    allowed_priorities = {"low", "medium", "high"}

    rec_type = str(candidate.get("type") or "general").strip().lower()
    if rec_type not in allowed_types:
        rec_type = "general"

    title = str(candidate.get("title") or "Wellness Reminder").strip() or "Wellness Reminder"

    raw_message = str(candidate.get("message") or "Drink water, move a bit, and get some rest today.")
    message = " ".join(raw_message.split()).strip() or "Drink water, move a bit, and get some rest today."
    if len(message) > 140:
        message = message[:137].rstrip() + "..."

    category = str(candidate.get("category") or "general_wellness").strip().lower() or "general_wellness"

    priority = str(candidate.get("priority") or "low").strip().lower()
    if priority not in allowed_priorities:
        priority = "low"

    actionable_value = candidate.get("actionable")
    actionable = actionable_value if isinstance(actionable_value, bool) else rec_type != "general"

    return {
        "type": rec_type,
        "title": title,
        "message": message,
        "category": category,
        "priority": priority,
        "actionable": actionable,
        "generated_at": datetime.now().isoformat(),
    }


def fallback_recommendation(summary: dict[str, Any] | None, latest_recommendation_text: str | None) -> dict[str, Any]:
    summary = summary or {}
    water = int(float(summary.get("totalWaterMl") or 0)) if summary.get("totalWaterMl") is not None else 0
    exercise = int(float(summary.get("totalExerciseMinutes") or 0)) if summary.get("totalExerciseMinutes") is not None else 0

    if latest_recommendation_text:
        msg = " ".join(str(latest_recommendation_text).split()).strip()
        if msg:
            if len(msg) > 140:
                msg = msg[:137].rstrip() + "..."
            return {
                "type": "profile_based",
                "title": "Daily Tip",
                "message": msg,
                "category": "general_wellness",
                "priority": "low",
                "actionable": True,
                "generated_at": datetime.now().isoformat(),
            }

    if water > 0 or exercise > 0:
        return {
            "type": "goal_based",
            "title": "Wellness Update",
            "message": f"Today: {water}ml water and {exercise} minutes of exercise logged.",
            "category": "general_wellness",
            "priority": "low",
            "actionable": True,
            "generated_at": datetime.now().isoformat(),
        }

    return {
        "type": "general",
        "title": "Wellness Reminder",
        "message": "Drink water, move a bit, and get some rest today.",
        "category": "general_wellness",
        "priority": "low",
        "actionable": False,
        "generated_at": datetime.now().isoformat(),
    }


async def generate_agentic_recommendation(
    *,
    llm: ChatOpenAI,
    summary: dict[str, Any] | None,
    exercise_history: Any,
    latest_recommendation: str | dict[str, Any] | None,
) -> dict[str, Any]:
    summary = summary or {}

    prompt = (
        f"{_RECOMMENDATION_SYSTEM_PROMPT}\n\n"
        f"summary={json.dumps(summary, ensure_ascii=True)}\n"
        f"exercise_history={json.dumps(exercise_history, ensure_ascii=True, default=str)}\n"
        f"latest_recommendation={json.dumps(latest_recommendation, ensure_ascii=True, default=str)}"
    )

    try:
        response = await llm.ainvoke(prompt)
        text = str(getattr(response, "content", "") or "")
        parsed = _extract_json_object(text)
        if parsed is None:
            logger.warning("Agentic recommendation returned non-JSON payload")
            return fallback_recommendation(summary, str(latest_recommendation or ""))
        return normalize_recommendation_payload(parsed)
    except Exception:
        logger.exception("Agentic recommendation generation failed")
        return fallback_recommendation(summary, str(latest_recommendation or ""))
