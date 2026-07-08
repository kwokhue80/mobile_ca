from datetime import datetime
import os
import logging
from typing import Any

from mcp.server.fastmcp import FastMCP
try:
    from .spring_boot_client import call_spring_boot, post_spring_boot
except ImportError:
    from spring_boot_client import call_spring_boot, post_spring_boot
from ddgs import DDGS

## ----------------------------------------------------------------- ##
#   AUTHOR(S): Kwok Heng, Amelia, Chai Lee
#   PURPOSE: Configure MCP server used by the wellness agent
#
#   SIMPLE ROLE OF THIS FILE:
#   - Exposes wellness MCP tools over stdio for the agent.
#   - Owns tool rules and data semantics (server-side source of truth).
#
#   What this file handles:
#   1) Validation + normalization before writes:
#      - required fields (get_missing_required_fields)
#      - enum normalization for meal/exercise types
#      - range checks for numeric values
#   2) Read tools via Spring Boot:
#      - get_daily_summary
#      - get_exercise_history
#      - get_latest_recommendation
#   3) Recommendation fallback logic:
#      - use summary + goals first
#      - if no goals, use profile-based wellness web search
#      - if upstream calls fail, return safe general guidance
#   4) Logging tool (log_wellness_entry):
#      - validate payload
#      - build one backend record payload
#      - persist via authenticated Spring endpoint
#      - return compact logging result
#
#   Boundary:
#   - this file = tool behavior and validation rules
#   - agent file = conversation flow and turn-by-turn orchestration
## ----------------------------------------------------------------- ##

logging.basicConfig(
    level=getattr(logging, os.getenv("LOG_LEVEL", "INFO").upper(), logging.INFO),
    format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
)
logger = logging.getLogger("mcp_server_wellness")

mcp = FastMCP("Wellness App Database")

MEAL_TYPES = {
    "BREAKFAST", "BRUNCH", "MORNING_SNACK", "MORNING_TEA", "TEA_BREAK",
    "LUNCH", "AFTERNOON_SNACK", "AFTERNOON_TEA", "DINNER", "SUPPER",
    "SNACK", "DESSERT", "PRE_WORKOUT", "POST_WORKOUT", "MIDNIGHT_MEAL",
    "BEVERAGE", "OTHER",
}

EXERCISE_TYPES = {
    "RUNNING", "WALKING", "SWIMMING", "HIKING", "CYCLING", "JOGGING",
    "STRENGTH_TRAINING", "WEIGHTLIFTING", "BODYWEIGHT_TRAINING", "HIIT", "CROSSFIT",
    "YOGA", "PILATES", "STRETCHING", "ROWING", "JUMP_ROPE", "DANCING",
    "BASKETBALL", "FOOTBALL", "BADMINTON", "TENNIS", "VOLLEYBALL",
    "MARTIAL_ARTS", "CLIMBING", "OTHER",
}


def get_missing_required_fields(payload: dict[str, Any]) -> list[str]:
    """Server-side source of truth for required fields by wellness category.
    Agent orchestrators can use this to drive follow-up prompts consistently.
    """
    missing: list[str] = []

    if any(k in payload for k in ["meal_type", "meal_calories_kcal", "meal_description"]):
        meal_type = payload.get("meal_type")
        meal_description = payload.get("meal_description")
        meal_calories_kcal = payload.get("meal_calories_kcal")

        if meal_type is None or str(meal_type).strip() == "":
            missing.append("meal_type")
        if meal_description is None or str(meal_description).strip() == "":
            missing.append("meal_description")
        if meal_calories_kcal is None:
            missing.append("meal_calories_kcal")

    if any(k in payload for k in ["exercise_type", "exercise_duration_minutes", "exercise_distance_km", "exercise_calories_burned_kcal"]):
        if "exercise_type" not in payload:
            missing.append("exercise_type")
        if "exercise_duration_minutes" not in payload:
            missing.append("exercise_duration_minutes")

    return missing


def _validation_error(message: str) -> None:
    logger.warning("log_wellness_entry validation failed: %s", message)
    raise ValueError(message)


def _record_date_or_now(record_date: str | None) -> str:
    if record_date:
        return record_date
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def _general_recommendation_text() -> str:
    return (
        "Personalized recommendation unavailable right now. "
        "General wellness tips: drink a glass of water, take a 10-15 minute walk, "
        "include one protein-rich meal, and keep a consistent sleep schedule tonight."
    )


def _normalize_goal_type(goal_type: str | None) -> str:
    if not goal_type:
        return ""
    return goal_type.strip().upper().replace("-", "_").replace(" ", "_")


def _summary_number(summary: dict[str, Any], *keys: str, default: float = 0) -> float:
    for key in keys:
        value = summary.get(key)
        if value is not None:
            try:
                return float(value)
            except (TypeError, ValueError):
                continue
    return default


def _profile_age(profile: dict[str, Any]) -> int | None:
    date_of_birth = profile.get("dateOfBirth") or profile.get("date_of_birth")
    if not date_of_birth:
        return None

    try:
        birth_date = datetime.fromisoformat(str(date_of_birth)).date()
        today = datetime.now().date()
        years = today.year - birth_date.year
        if (today.month, today.day) < (birth_date.month, birth_date.day):
            years -= 1
        return years
    except ValueError:
        return None


def _profile_search_query(profile: dict[str, Any]) -> str:
    context_parts: list[str] = []
    age = _profile_age(profile)
    if age is not None:
        context_parts.append(f"{age}-year-old")

    # Keep profile context lightweight to avoid overly demographic search drift.
    if not context_parts:
        context_parts.append("adult")

    context = " ".join(context_parts)
    return (
        f"wellness recommendations for {context} "
        "daily nutrition hydration sleep exercise healthy habits balanced meals"
    )


def _is_wellness_nutrition_result(result: dict[str, Any]) -> bool:
    text = f"{result.get('title', '')} {result.get('body', '')}".lower()
    keywords = [
        "wellness",
        "nutrition",
        "diet",
        "hydration",
        "sleep",
        "exercise",
        "fitness",
        "healthy",
        "meal",
        "protein",
        "calorie",
        "water",
        "habit",
    ]
    return any(keyword in text for keyword in keywords)


def _to_web_snippets(results: list[dict[str, Any]]) -> list[str]:
    return [
        f"- {result['title']}: {result['body']}"
        for result in results
        if (result.get("title") or result.get("body")) and _is_wellness_nutrition_result(result)
    ]


def _format_profile_web_snippets(snippets: list[str]) -> list[str]:
    """Expand compact web snippets into a cleaner multi-line list."""
    if not snippets:
        return ["- No relevant wellness web results were found."]

    formatted: list[str] = []
    for index, snippet in enumerate(snippets, start=1):
        cleaned = snippet.strip()
        if cleaned.startswith("- "):
            cleaned = cleaned[2:].strip()

        if ":" in cleaned:
            title, body = cleaned.split(":", 1)
            title = title.strip()
            body = body.strip()
            formatted.append(f"{index}. {title}")
            if body:
                formatted.append(f"   {body}")
        else:
            formatted.append(f"{index}. {cleaned}")

        # Add spacing between recommendations for narrow/mobile screens.
        formatted.append("")

    if formatted and not formatted[-1].strip():
        formatted.pop()

    return formatted


def _summary_header(summary: dict[str, Any], include_title: bool = True) -> list[str]:
    water_ml = int(round(_summary_number(summary, "totalWaterMl", "total_water_ml")))
    calories_intake = int(round(_summary_number(summary, "totalCaloriesIntake", "total_calories_intake")))
    calories_burned = int(round(_summary_number(summary, "totalCaloriesBurned", "total_calories_burned")))
    exercise_minutes = int(round(_summary_number(summary, "totalExerciseMinutes", "total_exercise_minutes")))
    sleep_minutes = int(round(_summary_number(summary, "sleepMinutes", "sleep_minutes")))
    mood_score = summary.get("moodScore") or summary.get("mood_score")
    weight_kg = summary.get("weightKg") or summary.get("weight_kg")

    lines: list[str] = []
    if include_title:
        lines.append("Today's logged values:")

    lines.extend(
        [
            f"- Water: {water_ml} ml",
            f"- Calories intake: {calories_intake} kcal",
            f"- Calories burned: {calories_burned} kcal",
            f"- Exercise: {exercise_minutes} minutes",
        ]
    )

    if sleep_minutes:
        lines.append(f"- Sleep: {sleep_minutes} minutes")
    if mood_score is not None:
        lines.append(f"- Mood: {mood_score}/10")
    if weight_kg is not None:
        lines.append(f"- Weight: {weight_kg} kg")

    return lines


def _goal_recommendation_lines(goal: dict[str, Any], summary: dict[str, Any]) -> list[str]:
    goal_type = _normalize_goal_type(str(goal.get("goalType") or goal.get("goal_type") or ""))
    target_value = goal.get("targetValue") if goal.get("targetValue") is not None else goal.get("target_value")
    try:
        target = float(target_value) if target_value is not None else None
    except (TypeError, ValueError):
        target = None

    if not goal_type:
        return []

    if goal_type in {"HYDRATION", "WATER_ML"}:
        actual = _summary_number(summary, "totalWaterMl", "total_water_ml")
        if target is None:
            return [f"Hydration: you logged {int(round(actual))} ml today."]
        if actual >= target:
            return [
                f"Hydration: {int(round(actual))}/{int(round(target))} ml today.",
                "You have met your water goal. Keep a bottle nearby and maintain the pace.",
            ]
        deficit = max(target - actual, 0)
        return [
            f"Hydration: {int(round(actual))}/{int(round(target))} ml today.",
            f"You still need about {int(round(deficit))} ml. Aim for 2 to 3 more glasses of water.",
        ]

    if goal_type in {"SLEEP", "SLEEP_MINUTES"}:
        actual_minutes = _summary_number(summary, "sleepMinutes", "sleep_minutes")
        if target is None:
            return [f"Sleep: you logged {int(round(actual_minutes))} minutes today."]
        if goal_type == "SLEEP_MINUTES":
            target_minutes = target
        else:
            target_minutes = target * 60
        if actual_minutes >= target_minutes:
            return [
                f"sleep: {int(round(actual_minutes))}/{int(round(target_minutes))} minutes today.",
                "Your sleep target is on track. Keep the bedtime routine consistent tonight.",
            ]
        deficit = max(target_minutes - actual_minutes, 0)
        return [
            f"sleep: {int(round(actual_minutes))}/{int(round(target_minutes))} minutes today.",
            f"You are short by about {int(round(deficit))} minutes. Try winding down earlier and protecting your sleep window.",
        ]

    if goal_type in {"CALORIES", "CALORIES_BURNED"}:
        actual = _summary_number(summary, "totalCaloriesBurned", "total_calories_burned")
        if target is None:
            return [f"Calories burned: you logged {int(round(actual))} kcal today."]
        if actual >= target:
            return [
                f"Calories burned: {int(round(actual))}/{int(round(target))} kcal today.",
                "You hit your activity calorie target. Keep the momentum with a balanced recovery meal.",
            ]
        deficit = max(target - actual, 0)
        return [
            f"Calories burned: {int(round(actual))}/{int(round(target))} kcal today.",
            f"You still need about {int(round(deficit))} kcal of activity. A short walk or light session can help.",
        ]

    if goal_type in {"EXERCISE", "EXERCISE_DAYS"}:
        exercise_minutes = _summary_number(summary, "totalExerciseMinutes", "total_exercise_minutes")
        has_exercise_today = exercise_minutes > 0
        if target is None:
            return [f"Exercise: you logged {int(round(exercise_minutes))} minutes today."]
        if has_exercise_today:
            return [
                f"Exercise: {int(has_exercise_today)}/{int(round(target))} days toward your weekly goal so far.",
                f"You also logged {int(round(exercise_minutes))} minutes today. Keep the weekly streak going with another active day.",
            ]
        return [
            f"Exercise: 0/{int(round(target))} days toward your weekly goal so far.",
            "A 10 to 15 minute walk or light workout today would move you closer to the goal.",
        ]

    if goal_type in {"WEIGHT"}:
        weight = summary.get("weightKg") or summary.get("weight_kg")
        if target is None:
            return [f"Weight: latest logged value is {weight if weight is not None else 'unavailable'} kg."]
        if weight is None:
            return [f"Weight: goal target is {target} kg, but no weight was logged today."]
        return [
            f"Weight: latest logged value is {weight} kg, target is {target} kg.",
            "Keep logging consistently so the trend is easier to track over time.",
        ]

    if goal_type == "STEPS":
        exercise_minutes = int(round(_summary_number(summary, "totalExerciseMinutes", "total_exercise_minutes")))
        return [
            f"Steps goal: {int(round(target)) if target is not None else 'target unavailable'} steps today.",
            f"Your summary currently shows {exercise_minutes} minutes of exercise, so a brisk walk or extra activity break would help you move closer.",
        ]

    label = goal_type.replace("_", " ").title()
    if target is None:
        return [f"{label}: no target value was provided."]
    return [f"{label}: target is {target}."]


def _build_goal_based_recommendation(goals: list[dict[str, Any]], summary: dict[str, Any]) -> str:
    lines = _summary_header(summary)
    lines.append("Recommendations:")

    recommendation_lines: list[str] = []
    for goal in goals:
        recommendation_lines.extend(_goal_recommendation_lines(goal, summary))

    if not recommendation_lines:
        recommendation_lines.append("No recognized goals were found. Keep logging your daily habits for a clearer recommendation next time.")

    lines.extend(f"- {line}" if not line.startswith("-") else line for line in recommendation_lines)
    return "\n".join(lines)


def _build_profile_web_recommendation(
    profile: dict[str, Any],
    summary: dict[str, Any],
    search_query: str,
    snippets: list[str],
) -> str:
    age = _profile_age(profile)
    profile_context = f"{age}-year-old" if age is not None else "adult"

    lines = [
        "Recommendation Summary",
        "",
        "Daily Summary",
        *_summary_header(summary, include_title=False),
        "",
        "Profile Basis",
        f"- Profile context: {profile_context}",
        "- No active goals are set yet.",
        "- I looked up general guidance tailored to your profile.",
        "",
        "Search Details",
        f"- Query used: {search_query}",
        "",
        "Web Recommendations",
        *_format_profile_web_snippets(snippets),
    ]

    return "\n".join(lines)


def _normalized_enum(value: str) -> str:
    return value.strip().upper().replace("-", "_").replace(" ", "_")


@mcp.tool()
async def get_exercise_history(days: int = 7):
    """Fetches the current user's exercise history from the backend.
    The backend endpoint returns exercise logs and supports a `days` window."""
    logger.info("Tool called: get_exercise_history days=%s", days)
    return await call_spring_boot(f"/api/wellness/exercise-logs?days={days}")


@mcp.tool()
async def get_daily_summary():
    """Fetches today's wellness summary for the current user."""
    logger.info("Tool called: get_daily_summary")
    return await call_spring_boot("/api/wellness/daily-summary")


@mcp.tool()
async def get_latest_recommendation():
    """Fetches the latest wellness recommendation for the current user."""
    logger.info("Tool called: get_latest_recommendation")
    try:
        summary = await call_spring_boot("/api/wellness/daily-summary")
    except Exception:
        logger.exception("get_latest_recommendation failed while fetching daily summary")
        return _general_recommendation_text()

    try:
        goals = await call_spring_boot("/api/user/goals/raw")
        if isinstance(goals, list) and goals:
            return _build_goal_based_recommendation(goals, summary if isinstance(summary, dict) else {})
    except Exception:
        logger.exception("get_latest_recommendation failed while fetching raw goals")

    try:
        profile = await call_spring_boot("/api/user/profile")
        if not isinstance(profile, dict):
            profile = {}

        search_query = _profile_search_query(profile)
        with DDGS() as search_engine:
            results = search_engine.text(search_query, max_results=3)

        snippets = _to_web_snippets(results)
        if not snippets:
            with DDGS() as search_engine:
                broad_results = search_engine.text(
                    "wellness nutrition recommendations daily hydration sleep exercise balanced diet",
                    max_results=3,
                )
            snippets = _to_web_snippets(broad_results)

        if snippets:
            return _build_profile_web_recommendation(
                profile,
                summary if isinstance(summary, dict) else {},
                search_query,
                snippets,
            )
    except Exception:
        logger.exception("get_latest_recommendation profile web search fallback failed")

    return _general_recommendation_text()


@mcp.tool()
async def log_wellness_entry(
    record_date: str | None = None,
    water_intake_ml: int | None = None,
    weight_kg: float | None = None,
    mood_rating: int | None = None,
    sleep_minutes: int | None = None,
    sleep_quality_rating: int | None = None,
    meal_type: str | None = None,
    meal_description: str | None = None,
    meal_calories_kcal: int | None = None,
    exercise_type: str | None = None,
    exercise_duration_minutes: int | None = None,
    exercise_distance_km: float | None = None,
    exercise_calories_burned_kcal: int | None = None,
):
    """Logs one combined wellness entry in a single backend call.
    Any subset of hydration, weight, mood, sleep, food, and exercise can be provided.
    record_date format: YYYY-MM-DD HH:MM:SS (optional; defaults to now)."""
    # Schema/range checks before any DB write
    logger.info(
        "Tool called: log_wellness_entry water=%s weight=%s mood=%s sleep=%s meal=%s exercise=%s",
        water_intake_ml is not None,
        weight_kg is not None,
        mood_rating is not None,
        sleep_minutes is not None or sleep_quality_rating is not None,
        meal_type is not None or meal_calories_kcal is not None,
        exercise_type is not None or exercise_duration_minutes is not None,
    )
    payload = {"recordDate": _record_date_or_now(record_date)}
    categories_logged: list[str] = []

    if water_intake_ml is not None:
        if water_intake_ml <= 0:
            _validation_error("water_intake_ml must be greater than 0")
        payload["waterIntakeMl"] = water_intake_ml
        categories_logged.append("hydration")

    if weight_kg is not None:
        if weight_kg <= 0:
            _validation_error("weight_kg must be greater than 0")
        payload["weightKg"] = weight_kg
        categories_logged.append("weight")

    if mood_rating is not None:
        if mood_rating < 1 or mood_rating > 10:
            _validation_error("mood_rating must be between 1 and 10")
        payload["moodRating"] = mood_rating
        categories_logged.append("mood")

    if sleep_minutes is not None:
        if sleep_minutes <= 0:
            _validation_error("sleep_minutes must be greater than 0")
        payload["sleepMinutes"] = sleep_minutes
        categories_logged.append("sleep")

    if sleep_quality_rating is not None:
        if sleep_quality_rating < 1 or sleep_quality_rating > 10:
            _validation_error("sleep_quality_rating must be between 1 and 10")
        payload["sleepQualityRating"] = sleep_quality_rating
        if "sleep" not in categories_logged:
            categories_logged.append("sleep")

    if meal_type is not None or meal_calories_kcal is not None or meal_description is not None:
        if not meal_type or not str(meal_description or "").strip() or meal_calories_kcal is None:
            _validation_error("For food logging, meal_type, meal_description, and meal_calories_kcal are required")
        if meal_calories_kcal <= 0:
            _validation_error("meal_calories_kcal must be greater than 0")

        normalized_meal_type = _normalized_enum(meal_type)
        if normalized_meal_type not in MEAL_TYPES:
            _validation_error(f"Unsupported meal_type: {meal_type}")

        payload["mealType"] = normalized_meal_type
        payload["mealCaloriesKcal"] = meal_calories_kcal
        payload["mealDescription"] = str(meal_description).strip()
        categories_logged.append("food")

    if (
        exercise_type is not None
        or exercise_duration_minutes is not None
        or exercise_distance_km is not None
        or exercise_calories_burned_kcal is not None
    ):
        if not exercise_type or exercise_duration_minutes is None:
            _validation_error("For exercise logging, both exercise_type and exercise_duration_minutes are required")
        if exercise_duration_minutes <= 0:
            _validation_error("exercise_duration_minutes must be greater than 0")

        normalized_exercise_type = _normalized_enum(exercise_type)
        if normalized_exercise_type not in EXERCISE_TYPES:
            _validation_error(f"Unsupported exercise_type: {exercise_type}")

        if exercise_distance_km is not None and exercise_distance_km < 0:
            _validation_error("exercise_distance_km cannot be negative")
        if exercise_calories_burned_kcal is not None and exercise_calories_burned_kcal < 0:
            _validation_error("exercise_calories_burned_kcal cannot be negative")

        payload["exerciseType"] = normalized_exercise_type
        payload["exerciseDurationMinutes"] = exercise_duration_minutes
        if exercise_distance_km is not None:
            payload["exerciseDistanceKm"] = exercise_distance_km
        if exercise_calories_burned_kcal is not None:
            payload["exerciseCaloriesBurnedKcal"] = exercise_calories_burned_kcal
        categories_logged.append("exercise")

    if len(payload) == 1:
        _validation_error("At least one wellness field must be provided")

    try:
        # Spring endpoint performs authenticated storage for the current user
        await post_spring_boot("/api/wellness/records", payload)
    except Exception:
        logger.exception(
            "log_wellness_entry failed while posting to Spring. payloadFields=%s",
            sorted(k for k in payload.keys() if k != "recordDate"),
        )
        raise

    return {
        "status": "logged",
        "recordDate": payload["recordDate"],
        "categoriesLogged": sorted(set(categories_logged)),
        "submittedFields": sorted(k for k in payload.keys() if k != "recordDate"),
    }


@mcp.tool()
def web_search(query: str):
    """Searches the web for information not available in the local
    database, such as nutrition facts for dishes."""
    logger.info("Tool called: web_search query=%s", query)
    with DDGS() as search_engine:
        results = search_engine.text(query, max_results=3)

    # Combine the top results into a single block of readable text
    combined_text = "\n\n".join(
        f"{result['title']}: {result['body']}" for result in results
    )
    return combined_text


if __name__ == "__main__":
    mcp.run(transport="stdio")