# Chatbot QA Test Matrix

## Scope
This checklist validates the current chatbot workflow:
- Deterministic logging orchestration for wellness logs
- Deterministic read orchestration for summary/history/recommendations
- LLM fallback for non-deterministic conversational queries
- Refusal-replacement safeguards when tool output succeeds

## Preconditions
1. Spring backend is running on port 8000.
2. FastAPI chatbot bridge is running on port 8001.
3. Android client is authenticated (valid Authorization header).
4. Logs are visible for both services.

## Quick Health Checks
| ID | Test | Input | Expected Result |
|---|---|---|---|
| HC-01 | Tool list endpoint | GET /api/tools | Returns: get_activity_history, get_daily_summary, get_latest_recommendation, log_wellness_entry, web_search |
| HC-02 | Chat bridge health via app | Pull-to-refresh in chat | Status turns green and text indicates online |

## Deterministic Read Tool Flows
| ID | Scenario | Example User Input | Expected Result |
|---|---|---|---|
| RD-01 | Daily summary | show my daily summary | Response contains daily summary data, no generic apology |
| RD-02 | Activity history | show my activity history | Response contains recent activity data |
| RD-03 | Latest recommendation | what is my latest recommendation | Response contains recommendation data |
| RD-04 | Read failure path | stop backend then ask for summary | Response says data cannot be accessed now (graceful failure) |

## Logging Flows: Single-turn Complete Inputs
| ID | Scenario | Example User Input | Expected Result |
|---|---|---|---|
| LG-01 | Hydration full | log 600 ml water | Entry logged successfully |
| LG-02 | Weight full | record weight 68 kg | Entry logged successfully |
| LG-03 | Mood full | log mood 7/10 | Entry logged successfully |
| LG-04 | Sleep full | i slept 7 hours | Entry logged successfully |
| LG-05 | Food full | i had lunch chicken rice 650 kcal | Entry logged successfully |
| LG-06 | Exercise full | i did hiit for 45 minutes | Entry logged successfully |

## Logging Flows: Multi-turn Missing-field Recovery
| ID | Scenario | Conversation | Expected Result |
|---|---|---|---|
| MF-01 | Food calories missing | User: i had chicken rice for lunch -> Bot asks calories -> User: around 650 kcal | Logs food successfully with meal_type lunch |
| MF-02 | Exercise type missing | User: i exercised for 30 minutes -> Bot asks type -> User: HIIT session | Logs exercise successfully with type and duration |
| MF-03 | Exercise duration missing | User: i did running -> Bot asks duration -> User: 25 mins | Logs exercise successfully |
| MF-04 | Sleep duration missing | User: log sleep -> Bot asks duration -> User: 6.5 hours | Logs sleep successfully |
| MF-05 | Hydration unit clarification | User: i drank water -> Bot asks amount -> User: 1.2 liters | Logs hydration as about 1200 ml |
| MF-06 | Mood clarification | User: log my mood -> Bot asks rating -> User: 8 | Logs mood as 8/10 |

## Uncertainty and Confirmation Behavior
| ID | Scenario | Conversation | Expected Result |
|---|---|---|---|
| CF-01 | Uncertain food log | User: not sure, i had lunch maybe 500 kcal | Bot asks confirmation before logging |
| CF-02 | Confirm yes | Bot asks confirmation -> User: yes | Entry is logged |
| CF-03 | Confirm no | Bot asks confirmation -> User: no, change to 650 kcal | Bot asks for/update fields and logs only after confirmation or complete valid input |

## Robustness and Hallucination Guardrails
| ID | Scenario | Input | Expected Result |
|---|---|---|---|
| RG-01 | Tool succeeded but apology risk | any read/log query where tool output exists | Final user response should not be generic failure/apology |
| RG-02 | Time number not treated as calories | i had chicken rice for lunch at 2 pm | Bot should ask calories, should not log 2 kcal |
| RG-03 | Short follow-up answer | Bot asks exercise type -> User: HIIT | Should be merged with prior partial draft, not treated as unrelated query |
| RG-04 | No auth logging | force missing token then log request | Prompt asks user to sign in again for safe logging session |

## Web Search Tool Flow
| ID | Scenario | Input | Expected Result |
|---|---|---|---|
| WS-01 | Nutrition fact query | find calories for nasi lemak | Response includes sourced web_search content, no tool-apology contradiction |

## Regression Cases from Previous Bugs
| ID | Bug Repro | Input | Expected Result |
|---|---|---|---|
| BG-01 | Meal prompt stored as full text | not sure, i had chicken rice for lunch at 2 pm today | Meal description should be concise, not full raw prompt |
| BG-02 | Wrong calorie inferred from time | same as BG-01 | Should not log calorie=2 |
| BG-03 | Tool-called but refusal text | any query where logs show tool called | Final response should align with successful tool output |

## Pass Criteria
1. All HC, RD, LG, MF, CF, RG, WS, BG tests pass.
2. No contradictory response where tool succeeds but assistant apologizes.
3. No invalid write with obviously wrong units or inferred unrelated numbers.

## Suggested Test Run Order
1. Health checks
2. Read flows
3. Single-turn logs
4. Multi-turn logs
5. Uncertainty confirmation
6. Regression bug cases
7. Failure-path checks

## Notes for Testers
- Capture request ID from FastAPI logs for failed cases.
- Keep one test conversation per category to avoid cross-draft confusion.
- If a test fails, include both user-visible response and server log snippet in bug report.
