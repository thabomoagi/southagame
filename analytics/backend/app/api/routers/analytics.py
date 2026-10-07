from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_db
from app.services.analytics import (
    get_easiest_questions,
    get_hardest_questions,
    get_most_missed_questions,
    get_most_played_questions,
    get_overview_stats,
    get_question_response_times,
)

router = APIRouter(prefix="/api/analytics", tags=["analytics"])


@router.get("/overview")
async def overview(db: AsyncSession = Depends(get_db)):
    return await get_overview_stats(db)


@router.get("/questions/most-played")
async def most_played_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_most_played_questions(db, limit=limit)


@router.get("/questions/hardest")
async def hardest_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_hardest_questions(db, limit=limit)


@router.get("/questions/easiest")
async def easiest_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_easiest_questions(db, limit=limit)


@router.get("/questions/most-missed")
async def most_missed_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_most_missed_questions(db, limit=limit)


@router.get("/questions/fastest")
async def fastest_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_question_response_times(db, fastest=True, limit=limit)


@router.get("/questions/slowest")
async def slowest_questions(
    limit: int = Query(5, ge=1, le=50), db: AsyncSession = Depends(get_db)
):
    return await get_question_response_times(db, fastest=False, limit=limit)