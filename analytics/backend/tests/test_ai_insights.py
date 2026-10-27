from unittest.mock import MagicMock
from fastapi.testclient import TestClient
import pytest
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker

from app.services.ai_insights import get_or_generate_ai_insights


@pytest.fixture
async def db_session(seeded_engine):
    session_factory = async_sessionmaker(seeded_engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as session:
        yield session


def test_ai_insights_endpoint_returns_data(client: TestClient) -> None:
    response = client.get("/api/analytics/ai-insights")
    assert response.status_code == 200
    data = response.json()
    assert "summary" in data
    assert "key_findings" in data
    assert "difficulty_analysis" in data
    assert "recommendations" in data
    assert "commentary_available" in data


@pytest.mark.anyio
async def test_ai_insights_service_success(seeded_engine) -> None:
    session_factory = async_sessionmaker(seeded_engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as db_session:
        mock_gemini = MagicMock()
        mock_response = MagicMock()
        mock_response.text = (
            '{"summary": "Test summary", "key_findings": ["F1"], "difficulty_analysis": "Test diff", "recommendations": ["R1"]}'
        )
        mock_gemini.models.generate_content.return_value = mock_response

        result = await get_or_generate_ai_insights(db_session, client=mock_gemini, force_refresh=True)
        assert result["commentary_available"] is True
        assert result["summary"] == "Test summary"
        assert result["key_findings"] == ["F1"]


@pytest.mark.anyio
async def test_ai_insights_service_fallback_on_error(seeded_engine) -> None:
    session_factory = async_sessionmaker(seeded_engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as db_session:
        mock_gemini = MagicMock()
        mock_gemini.models.generate_content.side_effect = Exception("Gemini quota exceeded")

        result = await get_or_generate_ai_insights(db_session, client=mock_gemini, force_refresh=True)
        assert result["commentary_available"] is False
        assert "unavailable" in result["summary"].lower()
        assert "metrics_snapshot" in result
