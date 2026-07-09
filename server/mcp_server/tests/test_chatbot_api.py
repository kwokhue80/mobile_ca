# ================================================================= #
#   AUTHOR(S): Amelia (AI-assisted)
#   PURPOSE: Comprehensive tests for refactored wellness chatbot
# ================================================================= #

import importlib
import sys
from pathlib import Path

from fastapi.testclient import TestClient
import pytest

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

mcp_agent_wellness = importlib.import_module("mcp_agent_wellness")

def _client() -> TestClient:
    return TestClient(mcp_agent_wellness.app)

def _auth_headers() -> dict[str, str]:
    return {"Authorization": "Bearer test-token"}

def setup_function():
    if hasattr(mcp_agent_wellness.DRAFT_STORE, "_drafts"):
        mcp_agent_wellness.DRAFT_STORE._drafts.clear()


# ============================================================
# 1. TOOLS
# ============================================================

def test_tools_endpoint_returns_registered_tools(monkeypatch):
    class _Tool:
        def __init__(self, name): self.name = name

    async def fake_get_tools():
        return [_Tool(n) for n in [
            "get_daily_summary", "get_exercise_history",
            "get_latest_recommendation", "log_wellness_entry", "web_search"
        ]]

    monkeypatch.setattr(mcp_agent_wellness.mcp_client, "get_tools", fake_get_tools)
    response = _client().get("/api/tools")
    assert response.status_code == 200
    assert set(response.json()) == {
        "get_daily_summary", "get_exercise_history",
        "get_latest_recommendation", "log_wellness_entry", "web_search"
    }


# ============================================================
# 2. LOGGING (Deterministic Path)
# ============================================================

def test_logging_complete_exercise_in_one_message(monkeypatch):
    captured = {}

    async def fake_log(**kwargs):
        captured.update(kwargs)
        return {"status": "logged", "categoriesLogged": ["exercise"]}

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log)

    payload = {"query": "I did strength training for 45 minutes", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert "Logged your exercise entry successfully" in response.json()["answer"]


def test_logging_multi_turn_missing_field(monkeypatch):
    captured = {}

    async def fake_log(**kwargs):
        captured.update(kwargs)
        return {"status": "logged", "categoriesLogged": ["exercise"]}

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log)

    payload1 = {"query": "I went for a run", "recentMessages": []}
    res1 = _client().post("/api/chat", json=payload1, headers=_auth_headers())
    assert res1.status_code == 200

    previous_assistant_msg = res1.json()["answer"]

    payload2 = {
        "query": "45 minutes",
        "recentMessages": [{"isUser": False, "text": previous_assistant_msg}],
    }
    res2 = _client().post("/api/chat", json=payload2, headers=_auth_headers())

    assert res2.status_code == 200
    assert "Logged your exercise entry successfully" in res2.json()["answer"]


def test_logging_cancel_draft(monkeypatch):
    payload1 = {"query": "log my weight", "recentMessages": []}
    res1 = _client().post("/api/chat", json=payload1, headers=_auth_headers())

    payload2 = {"query": "never mind", "recentMessages": [{"isUser": False, "text": res1.json()["answer"]}]}
    res2 = _client().post("/api/chat", json=payload2, headers=_auth_headers())

    assert res2.status_code == 200
    assert any(x in res2.json()["answer"].lower() for x in ["won't log", "no problem"])


def test_logging_invalid_value():
    payload = {"query": "my mood is 15", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    assert "1 to 10" in response.json()["answer"]


# ============================================================
# 3. READ & RECOMMENDATION QUERIES (LLM Path)
# ============================================================

def test_daily_summary_via_llm_path(monkeypatch):
    async def fake_summary():
        return {"totalWaterMl": 1800, "totalExerciseMinutes": 55}

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_daily_summary", fake_summary)

    payload = {"query": "how am i doing today?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert response.json()["answer"]


def test_recommendation_when_tool_fails(monkeypatch):
    async def failing_recommendation():
        raise RuntimeError("Service unavailable")

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_latest_recommendation", failing_recommendation)

    payload = {"query": "give me a wellness recommendation", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert response.json()["answer"]


# ============================================================
# 4. GREETINGS & SELF-INTRODUCTIONS
# ============================================================

def test_simple_greeting():
    payload = {"query": "Hey", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    assert len(response.json()["answer"]) > 8


def test_self_introduction_with_name():
    payload = {"query": "Hi, I'm Sarah", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    assert response.json()["answer"]


def test_greeting_plus_logging_intent(monkeypatch):
    captured = {}

    async def fake_log(**kwargs):
        captured.update(kwargs)
        return {"status": "logged", "categoriesLogged": ["hydration"]}

    monkeypatch.setattr(mcp_agent_wellness, "direct_log_wellness_entry", fake_log)

    payload = {"query": "Hey, log my water 500ml", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200


# ============================================================
# 5. LLM BEHAVIOR & GUARDRAILS
# ============================================================

def test_out_of_scope_rejection():
    payload = {"query": "What is the meaning of life?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200
    assert any(x in response.json()["answer"].lower() for x in ["wellness", "only help with"])


def test_general_wellness_question_goes_to_llm():
    payload = {"query": "What are the benefits of walking 30 minutes a day?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert len(response.json()["answer"]) > 20


def test_nutrition_question():
    payload = {"query": "How many calories are in a bowl of chicken rice?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert response.json()["answer"]


# ============================================================
# 6. EDGE CASES & ROBUSTNESS
# ============================================================

def test_empty_query():
    payload = {"query": "", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())
    assert response.status_code == 200


def test_false_refusal_repair(monkeypatch):
    async def fake_recommendation():
        return {"message": "Stay consistent with your habits"}

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_latest_recommendation", fake_recommendation)

    payload = {"query": "what's my latest recommendation?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert response.json()["answer"]


def test_active_logging_draft_with_read_query(monkeypatch):
    _client().post("/api/chat", json={"query": "log my sleep"}, headers=_auth_headers())

    async def fake_summary():
        return {"sleepMinutes": 420}

    monkeypatch.setattr(mcp_agent_wellness, "direct_get_daily_summary", fake_summary)

    payload = {"query": "how much did I sleep last night?", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200


# ============================================================
# 7. LLM SAFETY NET
# ============================================================

def test_off_topic_safety_net():
    """Basic smoke test for off-topic input"""
    payload = {"query": "Tell me about quantum physics and also my weight", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200


def test_safety_net_catches_off_topic_llm_response(monkeypatch):
    """LLM gives off-topic answer → safety check replaces it"""
    from langchain_core.messages import AIMessage

    class FakeAgent:
        async def ainvoke(self, inputs):
            return {
                "messages": [
                    AIMessage(content="I don't have information about quantum physics or stock market trends.")
                ]
            }

    def fake_create_llm_agent(tools):
        return FakeAgent()

    monkeypatch.setattr(mcp_agent_wellness, "_create_llm_agent", fake_create_llm_agent)

    payload = {"query": "Tell me about quantum computing", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert any(x in response.json()["answer"].lower() for x in ["wellness", "only help with"])


def test_safety_net_catches_long_irrelevant_response(monkeypatch):
    """Long irrelevant response on short query triggers safety net"""
    from langchain_core.messages import AIMessage

    # Make this text significantly longer than 600 characters
    long_off_topic_text = (
        "Quantum physics is a fundamental theory in physics that provides a description "
        "of the physical properties of nature at the scale of atoms and subatomic particles. "
        "It is the foundation of all quantum physics including quantum chemistry, quantum field theory, "
        "quantum technology, and quantum information science. The theory was developed in the early 20th century "
        "and has been extremely successful in explaining many phenomena that classical physics could not. "
        "It has revolutionized our understanding of the universe at its most fundamental level and continues "
        "to be an active area of research in both theoretical and experimental physics around the world today."
    )

    class FakeAgent:
        async def ainvoke(self, inputs):
            return {"messages": [AIMessage(content=long_off_topic_text)]}

    def fake_create_llm_agent(tools):
        return FakeAgent()

    monkeypatch.setattr(mcp_agent_wellness, "_create_llm_agent", fake_create_llm_agent)

    payload = {"query": "tell me something", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    assert any(x in response.json()["answer"].lower() for x in ["wellness", "only help with"])


def test_safety_net_does_not_trigger_on_valid_wellness_response(monkeypatch):
    """Good wellness answers should NOT be blocked by the safety net"""
    from langchain_core.messages import AIMessage

    # Force clear any leftover drafts before this test
    if hasattr(mcp_agent_wellness.DRAFT_STORE, "_drafts"):
        mcp_agent_wellness.DRAFT_STORE._drafts.clear()

    good_response = (
        "Walking for 30 minutes daily can significantly improve your cardiovascular health, "
        "help manage weight, reduce stress, and improve sleep quality. It's one of the most "
        "accessible and effective forms of exercise for most people."
    )

    class FakeAgent:
        async def ainvoke(self, inputs):
            return {"messages": [AIMessage(content=good_response)]}

    def fake_create_llm_agent(tools):
        return FakeAgent()

    monkeypatch.setattr(mcp_agent_wellness, "_create_llm_agent", fake_create_llm_agent)

    payload = {"query": "benefits of walking 30 minutes a day", "recentMessages": []}
    response = _client().post("/api/chat", json=payload, headers=_auth_headers())

    assert response.status_code == 200
    answer = response.json()["answer"].lower()

    print(f"\n[DEBUG] Actual answer returned: {answer[:200]}...")

    assert "only help with" not in answer.lower()
    assert any(word in answer.lower() for word in ["walking", "health", "exercise", "stress", "sleep", "cardiovascular"])


# ============================================================
# 8. DIRECT UNIT TESTS FOR SAFETY HELPER (More Reliable)
# ============================================================

def test_response_looks_off_topic_long_response():
    """Length-based safety trigger"""
    # This text is well over 600 characters
    long_text = (
        "Artificial intelligence is changing many industries including finance, healthcare, "
        "and transportation. Many companies are investing heavily in AI research and development. "
        "There are also important discussions happening around AI ethics, regulation, bias, "
        "job displacement, and the future of work. Governments around the world are trying to "
        "create frameworks to manage the rapid advancement of these technologies while ensuring "
        "they benefit society as a whole rather than just a small group of companies. "
        "This transformation is expected to continue for many decades and will reshape how "
        "we work, live, and interact with technology on a daily basis across the globe."
    )

    print(f"\n[DEBUG] Length of long_text = {len(long_text)}")
    result = mcp_agent_wellness._response_looks_off_topic(long_text, "tell me something")
    assert result is True


def test_response_looks_off_topic_normal_wellness_response():
    """Normal wellness response should NOT trigger safety net"""
    good_text = "Walking for 30 minutes daily can improve your health and reduce stress."
    result = mcp_agent_wellness._response_looks_off_topic(good_text, "benefits of walking")
    assert result is False


def test_response_looks_off_topic_with_phrase():
    """Explicit off-topic phrases should trigger"""
    bad_text = "I don't have information about quantum physics or cryptocurrency."
    result = mcp_agent_wellness._response_looks_off_topic(bad_text, "explain quantum computing")
    assert result is True