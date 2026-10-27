from datetime import datetime, timezone
from typing import Any
from pydantic import BaseModel, Field
from google import genai
from google.genai import types
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc

from app.models.ai_insight import AiInsightCache
from app.services.analytics import (
    get_overview_stats,
    get_hardest_questions,
    get_most_missed_questions,
    get_easiest_questions,
)


class AIInsightsContent(BaseModel):
    summary: str = Field(description="Executive summary of current player performance and trends.")
    key_findings: list[str] = Field(description="3-4 key bullet points highlighting significant gameplay patterns.")
    difficulty_analysis: str = Field(description="Analysis of question difficulty and player accuracy.")
    recommendations: list[str] = Field(description="Actionable suggestions for game masters or content creators.")


class AIInsightsResponse(BaseModel):
    summary: str
    key_findings: list[str]
    difficulty_analysis: str
    recommendations: list[str]
    commentary_available: bool
    generated_at: str | None = None
    metrics_snapshot: dict[str, Any] | None = None


async def get_or_generate_ai_insights(
    db: AsyncSession,
    client: genai.Client | None = None,
    force_refresh: bool = False,
    cache_ttl_minutes: int = 15,
) -> dict[str, Any]:
    """
    Retrieves cached AI insights from the database if within TTL,
    otherwise gathers current metrics aggregates, calls Google Gemini with structured output,
    and caches the result. Handles fallbacks gracefully if LLM calls fail.
    """
    # 1. Check existing cache in DB
    stmt = select(AiInsightCache).order_by(desc(AiInsightCache.generated_at)).limit(1)
    result = await db.execute(stmt)
    cached_record = result.scalars().first()

    now = datetime.now(timezone.utc)
    if cached_record and not force_refresh:
        cached_time = cached_record.generated_at
        if cached_time.tzinfo is None:
            cached_time = cached_time.replace(tzinfo=timezone.utc)
        
        age_minutes = (now - cached_time).total_seconds() / 60
        if age_minutes < cache_ttl_minutes:
            data = dict(cached_record.insight_data)
            data["commentary_available"] = True
            data["generated_at"] = cached_time.isoformat()
            return data

    # 2. Gather metrics aggregates
    overview = await get_overview_stats(db)
    hardest = await get_hardest_questions(db, limit=3)
    most_missed = await get_most_missed_questions(db, limit=3)
    easiest = await get_easiest_questions(db, limit=3)

    metrics_snapshot = {
        "overview": overview,
        "hardest": hardest,
        "most_missed": most_missed,
        "easiest": easiest,
    }

    # 3. Call Google Gemini with structured output request
    content_data: AIInsightsContent | None = None
    try:
        if client is None:
            # Initialize Gemini client (reads GEMINI_API_KEY from environment)
            client = genai.Client()

        prompt = f"""
You are Doxa, an expert AI analytics assistant for 'How Southa Are You?', a South African trivia and party game.
Analyze the following aggregated gameplay telemetry and provide insights matching the required JSON schema.

Telemetry Data:
- Overview Stats: {overview}
- Hardest Questions (Lowest Accuracy): {hardest}
- Most Missed Questions: {most_missed}
- Easiest Questions: {easiest}
"""

        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json",
                response_schema=AIInsightsContent,
                temperature=0.3,
            ),
        )

        response_text = response.text
        content_data = AIInsightsContent.model_validate_json(response_text)

    except Exception as e:
        # Graceful fallback: commentary unavailable, return metrics snapshot
        return {
            "summary": "AI commentary is currently unavailable due to provider connectivity or rate limits. Displaying core metric aggregates below.",
            "key_findings": [
                f"Total recorded attempts: {overview.get('total_attempts', 0)}",
                f"Active question bank size: {overview.get('total_questions', 0)}",
                "Metric tracking operational normally."
            ],
            "difficulty_analysis": "Unable to compute AI difficulty correlation at this moment.",
            "recommendations": ["Check GEMINI_API_KEY configuration or retry generating insights later."],
            "commentary_available": False,
            "generated_at": now.isoformat(),
            "metrics_snapshot": metrics_snapshot,
            "error_detail": str(e),
        }

    # 4. Save successful result to database cache
    insight_dict = content_data.model_dump()
    insight_dict["metrics_snapshot"] = metrics_snapshot

    if cached_record:
        cached_record.insight_data = insight_dict
        cached_record.generated_at = now
    else:
        new_cache = AiInsightCache(insight_data=insight_dict, generated_at=now)
        db.add(new_cache)

    await db.commit()

    insight_dict["commentary_available"] = True
    insight_dict["generated_at"] = now.isoformat()
    return insight_dict
