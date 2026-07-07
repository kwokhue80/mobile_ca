import asyncio
import importlib
import sys
from datetime import datetime, timedelta
from pathlib import Path

from fastapi.testclient import TestClient

#   AUTHOR(S): Amelia
#   PURPOSE: Run tests as tasks instead of manually testing
## ----------------------------------------------------------------- ##

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

mcp_agent_wellness = importlib.import_module("mcp_agent_wellness")
mcp_server_wellness = importlib.import_module("mcp_server_wellness")


def _client() -> TestClient:
    return TestClient(mcp_agent_wellness.app)


def _auth_headers() -> dict[str, str]:
    return {"Authorization": "Bearer test-token"}


def setup_function() -> None:
    # Reset deterministic draft state between tests.
    if hasattr(mcp_agent_wellness.DRAFT_STORE, "_drafts"):
        mcp_agent_wellness.DRAFT_STORE._drafts.clear()


def test_tools_endpoint_returns_registered_tools(monkeypatch):
    class _Tool:
        def __init__(self, name: str):
            self.name = name

    async def fake_get_tools():
        return [
            _Tool("get_activity_history"),
            _Tool("get_daily_summary"),
            _Tool("get_latest_recommendation"),
            _Tool("log_wellness_entry"),
            _Tool("web_search"),
        ]

    monkeypatch.setattr(mcp_agent_wellness.mcp_client, "get_tools", fake_get_tools)

    response = _client().get("/api/tools")
    assert response.status_code == 200
    assert response.json() == [
        "get_activity_history",
        "get_daily_summary",
        "get_latest_recommendation",
        "log_wellness_entry",
        "web_search",
    ]


def test_logging_complete_payload_saves_without_llm(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": ["exercise_type", "exercise_duration_minutes"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    payload = {
        "query": "i did hiit for 30 minutes",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    assert "Logged your exercise entry successfully" in response.json()["answer"]
    assert captured["exercise_type"] == "hiit"
    assert captured["exercise_duration_minutes"] == 30


def test_exercise_category_follow_up_prompts_for_type_then_duration(monkeypatch):
    first = _client().post(
        "/api/chat",
        json={
            "query": "Hello, I want to log an exercise.",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "exercise" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "Exercise",
            "recentMessages": [
                {"text": "Hello, I want to log an exercise.", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Exercise", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    second_answer = second.json()["answer"].lower()
    assert "type of exercise" in second_answer
    assert "could you share the key details" not in second_answer

    third = _client().post(
        "/api/chat",
        json={
            "query": "running",
            "recentMessages": [
                {"text": "Hello, I want to log an exercise.", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Exercise", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "running", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "how long was the exercise session" in third.json()["answer"].lower()


def test_hydration_log_with_today_time_sets_record_date(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["hydration"],
            "submittedFields": ["record_date", "water_intake_ml"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    response = _client().post(
        "/api/chat",
        json={
            "query": "I drank 500 ml water today at 7am",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    assert "logged your hydration entry successfully" in response.json()["answer"].lower()
    assert captured["water_intake_ml"] == 500
    expected_date = datetime.now().strftime("%Y-%m-%d")
    assert captured["record_date"].startswith(f"{expected_date} 07:00:00")


def test_meal_missing_field_flow_preserves_record_date(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["record_date", "meal_type", "meal_calories_kcal", "meal_description"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "I had chicken rice for dinner yesterday at 8:15 PM",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "recentMessages": [
                {"text": "I had chicken rice for dinner yesterday at 8:15 PM", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "550 kcal",
            "recentMessages": [
                {"text": "I had chicken rice for dinner yesterday at 8:15 PM", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "550 kcal", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )

    assert third.status_code == 200
    assert "logged your food entry successfully" in third.json()["answer"].lower()
    assert captured["meal_type"] == "dinner"
    assert captured["meal_calories_kcal"] == 550
    expected_date = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
    assert captured["record_date"].startswith(f"{expected_date} 20:15:00")


def test_meal_type_then_requires_food_name_before_calories(monkeypatch):
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        _ = kwargs
        call_count["log"] += 1
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_description", "meal_calories_kcal"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "Help me log a food entry",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "meal type" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "Breakfast",
            "recentMessages": [
                {"text": "Help me log a food entry", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Breakfast", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    second_answer = second.json()["answer"].lower()
    assert "what food did you have" in second_answer or "food name" in second_answer
    assert "calorie" not in second_answer
    assert call_count["log"] == 0


def test_food_entry_after_exercise_context_asks_food_name_before_calories(monkeypatch):
    first = _client().post(
        "/api/chat",
        json={
            "query": "Exercise",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200

    second = _client().post(
        "/api/chat",
        json={
            "query": "I want to log a food entry",
            "recentMessages": [
                {"text": "Exercise", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "I want to log a food entry", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "meal type" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "Lunch",
            "recentMessages": [
                {"text": "Exercise", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "I want to log a food entry", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    third_answer = third.json()["answer"].lower()
    assert "what food did you have" in third_answer or "food name" in third_answer
    assert "calorie" not in third_answer


def test_food_entry_intent_first_turn_prompts_meal_type_not_generic_details():
    response = _client().post(
        "/api/chat",
        json={
            "query": "I want to log a food entry.",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )

    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "what meal type" in answer
    assert "could you share the key details" not in answer


def test_logging_multi_turn_follow_up_merges_missing_fields(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": ["exercise_type", "exercise_duration_minutes"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first_turn = {
        "query": "i exercised for 25 minutes",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }
    first = _client().post("/api/chat", json=first_turn, headers=_auth_headers())
    assert first.status_code == 200
    assert "type of exercise" in first.json()["answer"].lower()

    second_turn = {
        "query": "HIIT",
        "conversationHistory": [
            {"text": "i exercised for 25 minutes", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "HIIT", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    second = _client().post("/api/chat", json=second_turn, headers=_auth_headers())
    assert second.status_code == 200
    assert "Logged your exercise entry successfully" in second.json()["answer"]
    assert captured["exercise_type"] == "hiit"
    assert captured["exercise_duration_minutes"] == 25


def test_exercise_uncertain_calories_uses_backend_estimation_message_and_logs(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": ["exercise_type", "exercise_duration_minutes"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "I ran for 30 minutes and I don't know calories burned",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    first_answer = first.json()["answer"].lower()
    assert "estimate calories burned" in first_answer
    assert "would you like me to proceed" in first_answer

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "recentMessages": [
                {"text": "I ran for 30 minutes and I don't know calories burned", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "logged your exercise entry successfully" in second.json()["answer"].lower()
    assert captured["exercise_type"] == "running"
    assert captured["exercise_duration_minutes"] == 30
    assert "exercise_calories_burned_kcal" not in captured


def test_exercise_calorie_question_estimates_first_then_logs_on_confirmation(monkeypatch):
    captured = {}
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        call_count["log"] += 1
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first_turn = {
        "query": "How many calories did I burn when I ran 30 minutes today?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }
    first = _client().post("/api/chat", json=first_turn, headers=_auth_headers())
    assert first.status_code == 200
    first_answer = first.json()["answer"].lower()
    assert "estimate calories burned" in first_answer
    assert "would you like me to proceed" in first_answer
    assert call_count["log"] == 0

    second_turn = {
        "query": "yes",
        "conversationHistory": [
            {"text": "How many calories did I burn when I ran 30 minutes today?", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    second = _client().post("/api/chat", json=second_turn, headers=_auth_headers())
    assert second.status_code == 200
    assert "logged your exercise entry successfully" in second.json()["answer"].lower()
    assert call_count["log"] == 1
    assert captured["exercise_type"] == "running"
    assert captured["exercise_duration_minutes"] == 30
    assert "exercise_calories_burned_kcal" not in captured


def test_exercise_confirmation_with_today_time_sets_record_date(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "record_date",
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "How many calories did I burn when I ran 30 minutes today?",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200

    second = _client().post(
        "/api/chat",
        json={
            "query": "Yes, I ran in the morning at 7AM today",
            "conversationHistory": [
                {"text": "How many calories did I burn when I ran 30 minutes today?", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yes, I ran in the morning at 7AM today", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "logged your exercise entry successfully" in second.json()["answer"].lower()

    expected_date = datetime.now().strftime("%Y-%m-%d")
    assert captured["record_date"].startswith(f"{expected_date} 07:00:00")


def test_exercise_confirmation_with_yesterday_time_sets_record_date(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "record_date",
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "How many calories did I burn when I ran 30 minutes?",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200

    second = _client().post(
        "/api/chat",
        json={
            "query": "Yes, I ran yesterday at 6:30 PM",
            "conversationHistory": [
                {"text": "How many calories did I burn when I ran 30 minutes?", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yes, I ran yesterday at 6:30 PM", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "logged your exercise entry successfully" in second.json()["answer"].lower()

    expected_date = (datetime.now() - timedelta(days=1)).strftime("%Y-%m-%d")
    assert captured["record_date"].startswith(f"{expected_date} 18:30:00")


def test_exercise_calorie_estimate_not_now_does_not_log(monkeypatch):
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        _ = kwargs
        call_count["log"] += 1
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "How many calories did I burn when I ran 30 minutes today?",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "would you like me to proceed" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "not now",
            "conversationHistory": [
                {"text": "How many calories did I burn when I ran 30 minutes today?", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "not now", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "tell me what to adjust" in second.json()["answer"].lower()
    assert call_count["log"] == 0


def test_meal_interest_prompt_decline_does_not_log(monkeypatch):
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        _ = kwargs
        call_count["log"] += 1
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "i had chicken rice for lunch",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "don't log it",
            "conversationHistory": [
                {"text": "i had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "don't log it", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "i won't log it" in second.json()["answer"].lower()
    assert call_count["log"] == 0


def test_meal_web_estimate_decline_does_not_log(monkeypatch):
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        _ = kwargs
        call_count["log"] += 1
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Chicken rice calories per serving are commonly around 500 kcal to 600 kcal."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={
            "query": "i had chicken rice for lunch",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "i had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "i don't know",
            "conversationHistory": [
                {"text": "i had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "i don't know", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "would you like me to log" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "no",
            "conversationHistory": [
                {"text": "i had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "i don't know", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "no", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "tell me what to adjust" in fourth.json()["answer"].lower()
    assert call_count["log"] == 0


def test_exercise_calorie_question_without_question_mark_still_estimates_first(monkeypatch):
    captured = {}
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        call_count["log"] += 1
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={
            "query": "how many calories did i burn when i ran 30 minutes today",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    first_answer = first.json()["answer"].lower()
    assert "estimate calories burned" in first_answer
    assert "would you like me to proceed" in first_answer
    assert call_count["log"] == 0

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "how many calories did i burn when i ran 30 minutes today", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "logged your exercise entry successfully" in second.json()["answer"].lower()
    assert call_count["log"] == 1
    assert captured["exercise_type"] == "running"
    assert captured["exercise_duration_minutes"] == 30
    assert "exercise_calories_burned_kcal" not in captured


def test_exercise_calorie_question_uses_exercise_wording_not_meal(monkeypatch):
    response = _client().post(
        "/api/chat",
        json={
            "query": "How many calories did I burn running for 30 minutes?",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "would you like me to proceed" in answer
    assert "log this meal now" not in answer


def test_explicit_exercise_logging_with_calories_logs_directly(monkeypatch):
    captured = {}
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        call_count["log"] += 1
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "exercise_type",
                "exercise_duration_minutes",
                "exercise_calories_burned_kcal",
            ],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    response = _client().post(
        "/api/chat",
        json={
            "query": "Log my run for 30 minutes, 280 kcal burned",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert response.status_code == 200
    assert "logged your exercise entry successfully" in response.json()["answer"].lower()
    assert call_count["log"] == 1
    assert captured["exercise_type"] == "running"
    assert captured["exercise_duration_minutes"] == 30
    assert captured["exercise_calories_burned_kcal"] == 280


def test_read_orchestration_calls_daily_summary_directly(monkeypatch):
    async def fake_daily_summary():
        return {"steps": 8234, "waterIntakeMl": 1500}

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_daily_summary", fake_daily_summary)

    payload = {
        "query": "show my daily summary",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"]
    assert "daily summary data" in answer.lower()
    assert "8234" in answer


def test_recommendations_query_does_not_echo_and_uses_read_orchestration(monkeypatch):
    async def fake_latest_recommendation():
        return {"title": "Hydration Boost", "advice": "Drink 500ml water this afternoon."}

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_latest_recommendation", fake_latest_recommendation)

    payload = {
        "query": "Any recommendations?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert answer != "any recommendations?"
    assert "latest recommendation" in answer
    assert "hydration boost" in answer


def test_recommendations_backend_failure_returns_general_tips(monkeypatch):
    async def fake_latest_recommendation():
        raise RuntimeError("Spring API error 400: No enum constant ... GoalType.STEPS")

    def fake_web_search(_query: str):
        return ""

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_latest_recommendation", fake_latest_recommendation)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    payload = {
        "query": "Any recommendations for me today?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "personalized recommendation" in answer
    assert "safe general wellness suggestions" in answer


def test_recommendations_fallbacks_to_web_search_when_backend_fails(monkeypatch):
    async def failing_latest_recommendation():
        raise RuntimeError("backend unavailable")

    def fake_web_search(_query: str):
        return "Take a brisk 20-minute walk, hydrate, and keep dinner balanced with protein and vegetables."

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_latest_recommendation", failing_latest_recommendation)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    payload = {
        "query": "Any recommendations for me today?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "couldn't access your personalized recommendation" in answer
    assert "web search" in answer
    assert "walk" in answer


def test_latest_recommendation_tool_uses_goals_and_today_summary(monkeypatch):
    async def fake_call_spring_boot(path, params=None):
        _ = params
        if path == "/api/wellness/daily-summary":
            return {
                "totalWaterMl": 1200,
                "totalCaloriesBurned": 180,
                "totalExerciseMinutes": 20,
                "sleepMinutes": 300,
                "moodScore": 6,
            }
        if path == "/api/user/goals/raw":
            return [
                {"goalType": "WATER_ML", "targetValue": 2200, "unit": "ml per day"},
                {"goalType": "SLEEP_MINUTES", "targetValue": 450, "unit": "minutes per day"},
                {"goalType": "EXERCISE_DAYS", "targetValue": 3, "unit": "days per week"},
            ]
        if path == "/api/user/profile":
            return {"fullName": "Alice Tan"}
        raise AssertionError(f"Unexpected path: {path}")

    def forbidden_search_engine():
        raise AssertionError("DDGS should not be used when goals are available")

    monkeypatch.setattr(mcp_server_wellness, "call_spring_boot", fake_call_spring_boot)
    monkeypatch.setattr(mcp_server_wellness, "DDGS", forbidden_search_engine)

    result = asyncio.run(mcp_server_wellness.get_latest_recommendation())

    assert isinstance(result, str)
    assert "today's logged values" in result.lower()
    assert "water: 1200 ml" in result.lower()
    assert "1200/2200" in result
    assert "sleep: 300/450" in result
    assert "exercise" in result.lower()


def test_latest_recommendation_tool_uses_profile_web_search_when_no_goals(monkeypatch):
    async def fake_call_spring_boot(path, params=None):
        _ = params
        if path == "/api/wellness/daily-summary":
            return {
                "totalWaterMl": 900,
                "totalCaloriesBurned": 0,
                "totalExerciseMinutes": 0,
            }
        if path == "/api/user/goals/raw":
            return []
        if path == "/api/user/profile":
            return {
                "fullName": "Ben Lim",
                "dateOfBirth": "1995-11-22",
                "gender": "MALE",
                "heightCm": 175.2,
            }
        raise AssertionError(f"Unexpected path: {path}")

    class FakeSearchEngine:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def text(self, query: str, max_results: int = 3):
            _ = max_results
            assert "wellness recommendations for" in query.lower()
            return [
                {
                    "title": "Wellness Tip",
                    "body": "Take a brisk 20-minute walk, hydrate, and keep dinner balanced with protein and vegetables.",
                }
            ]

    monkeypatch.setattr(mcp_server_wellness, "call_spring_boot", fake_call_spring_boot)
    monkeypatch.setattr(mcp_server_wellness, "DDGS", lambda: FakeSearchEngine())

    result = asyncio.run(mcp_server_wellness.get_latest_recommendation())

    assert isinstance(result, str)
    assert "no active goals are set yet" in result.lower()
    assert "wellness recommendations for" in result.lower()
    assert "wellness tip" in result.lower()
    assert "brisk 20-minute walk" in result.lower()


def test_latest_recommendation_profile_web_output_has_mobile_friendly_sections(monkeypatch):
    async def fake_call_spring_boot(path, params=None):
        _ = params
        if path == "/api/wellness/daily-summary":
            return {
                "totalWaterMl": 1100,
                "totalCaloriesBurned": 140,
                "totalExerciseMinutes": 18,
            }
        if path == "/api/user/goals/raw":
            return []
        if path == "/api/user/profile":
            return {"dateOfBirth": "1990-01-15"}
        raise AssertionError(f"Unexpected path: {path}")

    class FakeSearchEngine:
        def __enter__(self):
            return self

        def __exit__(self, exc_type, exc, tb):
            return False

        def text(self, query: str, max_results: int = 3):
            _ = query
            _ = max_results
            return [
                {"title": "Celebrity Story", "body": "A movie star routine."},
                {
                    "title": "Wellness Tip",
                    "body": "Hydrate in the morning and walk 20 minutes daily for consistency.",
                },
            ]

    monkeypatch.setattr(mcp_server_wellness, "call_spring_boot", fake_call_spring_boot)
    monkeypatch.setattr(mcp_server_wellness, "DDGS", lambda: FakeSearchEngine())

    result = asyncio.run(mcp_server_wellness.get_latest_recommendation())
    normalized = result.lower()

    assert "recommendation summary" in normalized
    assert "daily summary" in normalized
    assert "profile basis" in normalized
    assert "search details" in normalized
    assert "web recommendations" in normalized
    assert "\n\n" in result
    assert "1. wellness tip" in normalized
    assert "celebrity story" not in normalized


def test_meal_unknown_calories_triggers_web_estimate_then_confirms(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Chicken rice calories per serving are commonly around 500 kcal to 600 kcal."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first_turn = {
        "query": "i had chicken rice for lunch",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }
    first = _client().post("/api/chat", json=first_turn, headers=_auth_headers())
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second_turn = {
        "query": "yes",
        "conversationHistory": [
            {"text": "i had chicken rice for lunch", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    second = _client().post("/api/chat", json=second_turn, headers=_auth_headers())
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third_turn = {
        "query": "i don't know",
        "conversationHistory": [
            {"text": "i had chicken rice for lunch", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
            {"text": second.json()["answer"], "isUser": False},
            {"text": "i don't know", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    third = _client().post("/api/chat", json=third_turn, headers=_auth_headers())
    assert third.status_code == 200
    assert "web search" in third.json()["answer"].lower()
    assert "rough estimate" in third.json()["answer"].lower()
    assert "would you like me to log" in third.json()["answer"].lower()

    fourth_turn = {
        "query": "yes",
        "conversationHistory": [
            {"text": "i had chicken rice for lunch", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
            {"text": second.json()["answer"], "isUser": False},
            {"text": "i don't know", "isUser": True},
            {"text": third.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    fourth = _client().post("/api/chat", json=fourth_turn, headers=_auth_headers())
    assert fourth.status_code == 200
    assert "Logged your food entry successfully" in fourth.json()["answer"]
    assert captured["meal_type"] == "lunch"
    assert captured["meal_calories_kcal"] == 550


def test_meal_type_follow_up_does_not_replace_food_name(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)

    first = _client().post(
        "/api/chat",
        json={"query": "I had chicken alfredo", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "recentMessages": [
                {"text": "I had chicken alfredo", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "meal type" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "Lunch",
            "recentMessages": [
                {"text": "I had chicken alfredo", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "calorie" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "650 kcal",
            "recentMessages": [
                {"text": "I had chicken alfredo", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "650 kcal", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "logged your food entry successfully" in fourth.json()["answer"].lower()
    assert captured["meal_type"] == "lunch"
    assert captured["meal_calories_kcal"] == 650
    assert captured["meal_description"].lower() == "chicken alfredo"


def test_meal_portion_size_reply_triggers_web_estimate(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Chicken rice one plate is commonly around 600 kcal."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "I had chicken rice for lunch today", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "Yeah log it. I'm not sure about the calories",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch today", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yeah log it. I'm not sure about the calories", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "web search" in second.json()["answer"].lower()
    assert "would you like me to log" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "I had one plate",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch today", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yeah log it. I'm not sure about the calories", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "I had one plate", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "web search" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch today", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yeah log it. I'm not sure about the calories", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "I had one plate", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "logged your food entry successfully" in fourth.json()["answer"].lower()
    assert captured["meal_calories_kcal"] == 600


def test_meal_type_reply_does_not_become_meal_description(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Typical lunch plate around 280 to 310 kcal for 350 grams."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "I had a meal", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "I had a meal", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "meal type" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "Lunch",
            "conversationHistory": [
                {"text": "I had a meal", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "calorie" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "I don't know, portion size was about 350 grams",
            "conversationHistory": [
                {"text": "I had a meal", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "I don't know, portion size was about 350 grams", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "would you like me to log this estimate now" in fourth.json()["answer"].lower()

    fifth = _client().post(
        "/api/chat",
        json={
            "query": "Yes",
            "conversationHistory": [
                {"text": "I had a meal", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Lunch", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "I don't know, portion size was about 350 grams", "isUser": True},
                {"text": fourth.json()["answer"], "isUser": False},
                {"text": "Yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fifth.status_code == 200
    assert "logged your food entry successfully" in fifth.json()["answer"].lower()
    assert captured["meal_type"] == "lunch"
    assert captured["meal_description"].lower() != "lunch"


def test_meal_calorie_prompt_accepts_grams_phrase_without_portion_keywords(monkeypatch):
    def fake_web_search(_query: str):
        return "Pancakes are often around 220 to 260 kcal per 100 grams depending on recipe and toppings."

    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={
            "query": "Help me log a food entry",
            "recentMessages": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200

    second = _client().post(
        "/api/chat",
        json={
            "query": "Breakfast",
            "recentMessages": [
                {"text": "Help me log a food entry", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Breakfast", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "what food did you have" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "Pancakes",
            "recentMessages": [
                {"text": "Help me log a food entry", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Breakfast", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Pancakes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "calorie" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "About 300 grams of pancakes",
            "recentMessages": [
                {"text": "Help me log a food entry", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Breakfast", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "Pancakes", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "About 300 grams of pancakes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    fourth_answer = fourth.json()["answer"].lower()
    assert "would you like me to log this estimate now" in fourth_answer
    assert "could you share the key details" not in fourth_answer


def test_meal_calorie_lookup_query_uses_web_search_and_asks_to_log(monkeypatch):
    def fake_web_search(_query: str):
        return "Chicken rice typically ranges around 500 to 600 calories per serving."

    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first_turn = {
        "query": "i had chicken rice for lunch",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }
    first = _client().post("/api/chat", json=first_turn, headers=_auth_headers())
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second_turn = {
        "query": "yes",
        "conversationHistory": [
            {"text": "i had chicken rice for lunch", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    second = _client().post("/api/chat", json=second_turn, headers=_auth_headers())
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third_turn = {
        "query": "What are the calories in chicken rice?",
        "conversationHistory": [
            {"text": "i had chicken rice for lunch", "isUser": True},
            {"text": first.json()["answer"], "isUser": False},
            {"text": "yes", "isUser": True},
            {"text": second.json()["answer"], "isUser": False},
            {"text": "What are the calories in chicken rice?", "isUser": True},
        ],
        "relevantPastMessages": [],
    }
    third = _client().post("/api/chat", json=third_turn, headers=_auth_headers())
    assert third.status_code == 200
    assert "web search" in third.json()["answer"].lower()
    assert "would you like me to log" in third.json()["answer"].lower()


def test_general_wellness_query_uses_deterministic_web_search(monkeypatch):
    def fake_web_search(_query: str):
        return "Chia seeds provide fiber, omega-3 fats, and protein per serving."

    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    payload = {
        "query": "what are the nutrition facts of chia seeds?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "web search" in answer
    assert "nutrition" in answer


def test_combined_read_and_web_search_in_one_turn(monkeypatch):
    async def fake_daily_summary():
        return {"steps": 7000, "waterIntakeMl": 1200}

    def fake_web_search(_query: str):
        return "Chicken rice is often around 500 to 600 calories per serving."

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_daily_summary", fake_daily_summary)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    payload = {
        "query": "show my daily summary and what are the calories in chicken rice?",
        "conversationHistory": [],
        "relevantPastMessages": [],
    }

    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    answer = response.json()["answer"].lower()
    assert "daily summary data" in answer
    assert "web search" in answer


def test_active_meal_draft_pauses_for_wellness_lookup_and_resumes(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Salmon nutrition facts: protein-rich meal and often around 400 to 500 calories per serving."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "i had salmon for dinner", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "recentMessages": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "what are the nutrition facts of salmon?",
            "recentMessages": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "what are the nutrition facts of salmon?", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "web search" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "500 kcal",
            "recentMessages": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "what are the nutrition facts of salmon?", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "500 kcal", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "logged your food entry successfully" in fourth.json()["answer"].lower()
    assert captured["meal_type"] == "dinner"
    assert captured["meal_calories_kcal"] == 500


def test_active_meal_confirmation_allows_explicit_search_and_resumes(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(query: str):
        lowered = query.lower()
        if "singapore chicken rice" in lowered:
            return "Singapore chicken rice is often around 450 to 550 calories per serving."
        return "Chicken rice is commonly around 500 to 600 calories per serving."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "I had chicken rice for lunch", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "I don't know, portion size was about 350 grams",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "I don't know, portion size was about 350 grams", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "would you like me to log this estimate now" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "Search for Singapore chicken rice",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "I don't know, portion size was about 350 grams", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "Search for Singapore chicken rice", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    fourth_answer = fourth.json()["answer"].lower()
    assert "web search" in fourth_answer
    assert "singapore chicken rice" in fourth_answer
    assert "could you share the key details" not in fourth_answer

    fifth = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "I had chicken rice for lunch", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "I don't know, portion size was about 350 grams", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "Search for Singapore chicken rice", "isUser": True},
                {"text": fourth.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fifth.status_code == 200
    assert "logged your food entry successfully" in fifth.json()["answer"].lower()
    assert captured["meal_type"] == "lunch"


def test_calorie_web_search_confirmation_logs_meal(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return (
            "Calories in McDonald's Big Mac Burger: 550 calories. "
            "Big Mac from McDonald's: 580 calories."
        )

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "Calories in McDonald's Big Mac Burger", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "web search" in first.json()["answer"].lower()
    assert "would you like me to log" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "Yes log it",
            "conversationHistory": [
                {"text": "Calories in McDonald's Big Mac Burger", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "Yes log it", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "logged your food entry successfully" in second.json()["answer"].lower()
    assert captured["meal_type"] == "other"
    assert captured["meal_calories_kcal"] == 565
    assert "big mac" in captured["meal_description"].lower()


def test_nutrition_search_can_complete_active_meal_draft_and_log(monkeypatch):
    captured = {}

    async def fake_log_wellness_entry(**kwargs):
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["food"],
            "submittedFields": ["meal_type", "meal_calories_kcal", "meal_description"],
        }

    def fake_web_search(_query: str):
        return "Salmon nutrition facts: around 420 calories per serving with high protein content."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={"query": "i had salmon for dinner", "conversationHistory": [], "relevantPastMessages": []},
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "what are the nutrition facts of salmon?",
            "conversationHistory": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "what are the nutrition facts of salmon?", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    assert "would you like me to log this meal now" in third.json()["answer"].lower()

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "yes log it",
            "conversationHistory": [
                {"text": "i had salmon for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "what are the nutrition facts of salmon?", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "yes log it", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "logged your food entry successfully" in fourth.json()["answer"].lower()
    assert captured["meal_type"] == "dinner"
    assert captured["meal_calories_kcal"] == 420
    assert "salmon" in captured["meal_description"].lower()


def test_active_meal_draft_then_exercise_calorie_query_stays_exercise_context(monkeypatch):
    captured = {}
    call_count = {"log": 0}

    async def fake_log_wellness_entry(**kwargs):
        call_count["log"] += 1
        captured.update(kwargs)
        return {
            "status": "logged",
            "categoriesLogged": ["exercise"],
            "submittedFields": [
                "exercise_type",
                "exercise_duration_minutes",
            ],
        }

    def fake_web_search(query: str):
        return "General wellness guidance about hydration and sleep."

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log_wellness_entry)
    monkeypatch.setattr(mcp_agent_wellness, "direct_web_search", fake_web_search)

    first = _client().post(
        "/api/chat",
        json={
            "query": "i had pasta for dinner",
            "conversationHistory": [],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert first.status_code == 200
    assert "interested in logging" in first.json()["answer"].lower()

    second = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "i had pasta for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert second.status_code == 200
    assert "calorie" in second.json()["answer"].lower()

    third = _client().post(
        "/api/chat",
        json={
            "query": "What is the estimated calorie burn if I run for 30 minutes?",
            "conversationHistory": [
                {"text": "i had pasta for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "What is the estimated calorie burn if I run for 30 minutes?", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert third.status_code == 200
    third_answer = third.json()["answer"].lower()
    assert "estimate calories burned" in third_answer
    assert "would you like me to proceed" in third_answer
    assert "meal type" not in third_answer
    assert call_count["log"] == 0

    fourth = _client().post(
        "/api/chat",
        json={
            "query": "yes",
            "conversationHistory": [
                {"text": "i had pasta for dinner", "isUser": True},
                {"text": first.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
                {"text": second.json()["answer"], "isUser": False},
                {"text": "What is the estimated calorie burn if I run for 30 minutes?", "isUser": True},
                {"text": third.json()["answer"], "isUser": False},
                {"text": "yes", "isUser": True},
            ],
            "relevantPastMessages": [],
        },
        headers=_auth_headers(),
    )
    assert fourth.status_code == 200
    assert "logged your exercise entry successfully" in fourth.json()["answer"].lower()
    assert call_count["log"] == 1
    assert captured["exercise_type"] == "running"
    assert captured["exercise_duration_minutes"] == 30
    assert "exercise_calories_burned_kcal" not in captured
    assert "meal_type" not in captured
