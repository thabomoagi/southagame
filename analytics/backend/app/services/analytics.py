from sqlalchemy import Integer, asc, case, desc, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Attempt, AttemptAnswer, AttemptQuestion, Category, Question, User


async def get_overview_stats(db: AsyncSession) -> dict:
    user_count = await db.scalar(select(func.count()).select_from(User))
    attempt_count = await db.scalar(select(func.count()).select_from(Attempt))
    question_count = await db.scalar(select(func.count()).select_from(Question))
    category_count = await db.scalar(select(func.count()).select_from(Category))

    return {
        "total_users": user_count or 0,
        "total_attempts": attempt_count or 0,
        "total_questions": question_count or 0,
        "total_categories": category_count or 0,
    }


async def get_most_played_questions(db: AsyncSession, limit: int = 5) -> list:
    stmt = (
        select(Question.id, Question.prompt, func.count(AttemptQuestion.id).label("play_count"))
        .join(AttemptQuestion, Question.id == AttemptQuestion.question_id)
        .group_by(Question.id, Question.prompt)
        .order_by(desc("play_count"))
        .limit(limit)
    )
    result = await db.execute(stmt)
    return [
        {"id": row.id, "prompt": row.prompt, "play_count": row.play_count}
        for row in result.all()
    ]


async def get_hardest_questions(db: AsyncSession, limit: int = 5) -> list:
    stmt = (
        select(
            Question.id,
            Question.prompt,
            func.count(AttemptAnswer.id).label("total_attempts"),
            func.sum(func.cast(AttemptAnswer.is_correct, Integer)).label("correct_count"),
            (
                func.sum(func.cast(AttemptAnswer.is_correct, Integer))
                * 100.0
                / func.count(AttemptAnswer.id)
            ).label("accuracy_percentage"),
        )
        .join(AttemptAnswer, Question.id == AttemptAnswer.question_id)
        .group_by(Question.id, Question.prompt)
        .having(func.count(AttemptAnswer.id) >= 1)
        .order_by(asc("accuracy_percentage"))
        .limit(limit)
    )
    result = await db.execute(stmt)
    return [
        {
            "id": row.id,
            "prompt": row.prompt,
            "total_attempts": row.total_attempts,
            "correct_count": row.correct_count or 0,
            "accuracy_percentage": round(row.accuracy_percentage or 0.0, 2),
        }
        for row in result.all()
    ]


async def get_easiest_questions(db: AsyncSession, limit: int = 5) -> list:
    stmt = (
        select(
            Question.id,
            Question.prompt,
            func.count(AttemptAnswer.id).label("total_attempts"),
            func.sum(func.cast(AttemptAnswer.is_correct, Integer)).label("correct_count"),
            (
                func.sum(func.cast(AttemptAnswer.is_correct, Integer))
                * 100.0
                / func.count(AttemptAnswer.id)
            ).label("accuracy_percentage"),
        )
        .join(AttemptAnswer, Question.id == AttemptAnswer.question_id)
        .group_by(Question.id, Question.prompt)
        .having(func.count(AttemptAnswer.id) >= 1)
        .order_by(desc("accuracy_percentage"))
        .limit(limit)
    )
    result = await db.execute(stmt)
    return [
        {
            "id": row.id,
            "prompt": row.prompt,
            "total_attempts": row.total_attempts,
            "correct_count": row.correct_count or 0,
            "accuracy_percentage": round(row.accuracy_percentage or 0.0, 2),
        }
        for row in result.all()
    ]


async def get_most_missed_questions(db: AsyncSession, limit: int = 5) -> list:
    incorrect_case = func.sum(case((AttemptAnswer.is_correct.is_(False), 1), else_=0))
    stmt = (
        select(
            Question.id,
            Question.prompt,
            incorrect_case.label("miss_count"),
            func.count(AttemptAnswer.id).label("total_attempts"),
        )
        .join(AttemptAnswer, Question.id == AttemptAnswer.question_id)
        .group_by(Question.id, Question.prompt)
        .order_by(desc("miss_count"))
        .limit(limit)
    )
    result = await db.execute(stmt)
    return [
        {
            "id": row.id,
            "prompt": row.prompt,
            "miss_count": row.miss_count or 0,
            "total_attempts": row.total_attempts,
        }
        for row in result.all()
    ]


async def get_question_response_times(db: AsyncSession, fastest: bool = True, limit: int = 5) -> list:
    order_dir = (
        asc(func.avg(AttemptAnswer.time_taken_ms))
        if fastest
        else desc(func.avg(AttemptAnswer.time_taken_ms))
    )
    stmt = (
        select(
            Question.id,
            Question.prompt,
            func.avg(AttemptAnswer.time_taken_ms).label("avg_time_ms"),
            func.count(AttemptAnswer.id).label("total_attempts"),
        )
        .join(AttemptAnswer, Question.id == AttemptAnswer.question_id)
        .group_by(Question.id, Question.prompt)
        .order_by(order_dir)
        .limit(limit)
    )
    result = await db.execute(stmt)
    return [
        {
            "id": row.id,
            "prompt": row.prompt,
            "avg_time_ms": round(row.avg_time_ms or 0.0, 2),
            "total_attempts": row.total_attempts,
        }
        for row in result.all()
    ]