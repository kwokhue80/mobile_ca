from mcp.server.fastmcp import FastMCP
from spring_boot_client import call_spring_boot
from ddgs import DDGS

mcp = FastMCP("Wellness App Database")


@mcp.tool()
async def get_activity_history(days: int = 7):
    """Fetches the logged wellness activities for the current user,
    covering meals, exercise, sleep, mood, weight, and hydration,
    over a given number of past days."""
    return await call_spring_boot("/wellness/activity", params={"days": days})


@mcp.tool()
def web_search(query: str):
    """Searches the web for information not available in the local
    database, such as nutrition facts for dishes."""
    with DDGS() as search_engine:
        results = search_engine.text(query, max_results=3)

    # Combine the top results into a single block of readable text
    combined_text = "\n\n".join(
        f"{result['title']}: {result['body']}" for result in results
    )
    return combined_text


if __name__ == "__main__":
    mcp.run(transport="stdio")