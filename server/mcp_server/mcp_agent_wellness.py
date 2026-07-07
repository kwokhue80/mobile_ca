import os
from fastapi import FastAPI, HTTPException, Header
from pydantic import BaseModel
from typing import List
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain_mcp_adapters.client import MultiServerMCPClient
from jwt_context import set_current_token

load_dotenv()

app = FastAPI(title="Wellness Chatbot API")

# Points to the MCP server script, launched automatically through
# the stdio transport rather than a separate network address
mcp_client = MultiServerMCPClient(
    {
        "wellness_db": {
            "command": "python",
            "args": ["mcp_server_wellness.py"],
            "transport": "stdio",
        }
    }
)

SYSTEM_PROMPT = """
... (the full wellness coach system prompt stays exactly as it was) ...
"""

# Represents one message already exchanged in the conversation
class ChatMessage(BaseModel):
    text: str
    isUser: bool

# Represents the request payload sent from the Android application
class ChatRequest(BaseModel):
    query: str
    recentMessages: List[ChatMessage] = []
    relevantPastMessages: List[ChatMessage] = []

# Represents the final answer sent back to the Android application
class ChatResponse(BaseModel):
    answer: str


async def ask_agent(request: ChatRequest) -> str:
    """
    Builds the conversation for the language model and runs the agent,
    combining the current question with recent messages and any older
    messages found to be relevant through semantic search.
    """
    tools = await mcp_client.get_tools()

    llm = ChatOpenAI(
        model="google/gemini-2.5-flash-lite",
        temperature=0,
        base_url="https://openrouter.ai/api/v1",
        api_key=os.getenv("OPENROUTER_API_KEY")
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

    # Recent messages are added in order to preserve the natural
    # flow of the conversation
    for msg in request.recentMessages:
        role = "user" if msg.isUser else "assistant"
        conversation_messages.append({"role": role, "content": msg.text})

    conversation_messages.append({"role": "user", "content": request.query})

    result = await agent.ainvoke({"messages": conversation_messages})

    return result["messages"][-1].content


@app.get("/api/tools")
async def list_available_tools():
    """Debugging endpoint used to check which tools the agent can see."""
    try:
        tools = await mcp_client.get_tools()
        return [tool.name for tool in tools]
    except Exception as error:
        raise HTTPException(status_code=500, detail=str(error))


@app.post("/api/chat")
async def chat_endpoint(request: ChatRequest, authorization: str = Header(None)):
    """
    Main endpoint called by the Android application. Stores the
    authorization token for this request so tool functions can use
    it when calling the Spring Boot backend, then runs the agent.
    """
    try:
        set_current_token(authorization)
        answer_text = await ask_agent(request)
        return ChatResponse(answer=answer_text)
    except Exception as error:
        raise HTTPException(status_code=500, detail=f"Agent execution failed: {str(error)}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)