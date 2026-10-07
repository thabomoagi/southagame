import asyncio
import os
from collections.abc import AsyncGenerator, Iterator
from datetime import datetime
from pathlib import Path
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.pool import NullPool

# Settings are built at import time, so the environment must be populated first.
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")
os.environ.setdefault("SECRET_KEY", "test-secret")

from app.db.base import Base  # noqa: E402
from app.db.session import get_db  # noqa: E402
from app.main import app  # noqa: E402
from app.models import (  # noqa: E402
    Attempt,
    AttemptAnswer,
    AttemptQuestion,
    Category,
    Question,
    User,
)

_EPOCH = datetime(2026, 1, 1, 12, 0, 0)


def _create_engine(db_path: Path) -> AsyncEngine:
    # NullPool keeps an aiosqlite connection from being cached in the setup
    # event loop and then reused by the loop TestClient runs the app in.
    return create_async_engine(
        f"sqlite+aiosqlite:///{db_path.as_posix()}",
        poolclass=NullPool,
    )


def _seed_rows() -> list[Base]:
    users = [
        User(
            id=uuid4(),
            username=name,
            email=f"{name}@example.com",
            password_hash="test-hash",
        )
        for name in ("player-one", "player-two")
    ]

    questions = [
        Question(
            id=index,
            external_id=external_id,
            category_id=1,
            prompt=prompt,
            difficulty="easy",
        )
        for index, (external_id, prompt) in enumerate(
            (
                ("q-world-cup", "Which country hosted the 2010 FIFA World Cup?"),
                ("q-red-planet", "Which planet is known as the Red Planet?"),
                ("q-kenya-capital", "What is the capital of Kenya?"),
            ),
            start=1,
        )
    ]

    attempt_ids = [uuid4() for _ in range(3)]
    attempts = [
        Attempt(
            id=attempt_id,
            user_id=user_id,
            difficulty="easy",
            started_at=_EPOCH,
            expires_at=_EPOCH,
            completed_at=_EPOCH,
            score=score,
            total_questions=total_questions,
            correct_count=correct_count,
        )
        for attempt_id, (user_id, score, total_questions, correct_count) in zip(
            attempt_ids,
            ((users[0].id, 20, 3, 2), (users[1].id, 10, 2, 1), (users[0].id, 0, 1, 0)),
            strict=True,
        )
    ]

    attempt_questions = [
        AttemptQuestion(attempt_id=attempt_ids[0], question_id=1, position=1),
        AttemptQuestion(attempt_id=attempt_ids[0], question_id=2, position=2),
        AttemptQuestion(attempt_id=attempt_ids[0], question_id=3, position=3),
        AttemptQuestion(attempt_id=attempt_ids[1], question_id=1, position=1),
        AttemptQuestion(attempt_id=attempt_ids[1], question_id=2, position=2),
        AttemptQuestion(attempt_id=attempt_ids[2], question_id=1, position=1),
    ]

    attempt_answers = [
        AttemptAnswer(
            attempt_id=attempt_id,
            question_id=question_id,
            is_correct=is_correct,
            time_taken_ms=time_taken_ms,
            points_earned=10 if is_correct else 0,
        )
        for attempt_id, question_id, is_correct, time_taken_ms in (
            (attempt_ids[0], 1, True, 1000),
            (attempt_ids[0], 2, False, 4000),
            (attempt_ids[0], 3, True, 2000),
            (attempt_ids[1], 1, True, 1000),
            (attempt_ids[1], 2, False, 4000),
            (attempt_ids[2], 1, False, 1000),
        )
    ]

    return [
        Category(id=1, name="World History", slug="world-history"),
        *users,
        *questions,
        *attempts,
        *attempt_questions,
        *attempt_answers,
    ]


async def _init_db(engine: AsyncEngine, *, with_data: bool) -> None:
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    if not with_data:
        return

    session_factory = async_sessionmaker(engine, expire_on_commit=False)
    async with session_factory() as session:
        session.add_all(_seed_rows())
        await session.commit()


def _build_client(engine: AsyncEngine) -> TestClient:
    session_factory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async def override_get_db() -> AsyncGenerator[AsyncSession, None]:
        async with session_factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    return TestClient(app)


@pytest.fixture(scope="session")
def seeded_engine(tmp_path_factory: pytest.TempPathFactory) -> Iterator[AsyncEngine]:
    engine = _create_engine(tmp_path_factory.mktemp("seeded") / "doxa.sqlite3")
    asyncio.run(_init_db(engine, with_data=True))
    yield engine
    asyncio.run(engine.dispose())


@pytest.fixture(scope="session")
def empty_engine(tmp_path_factory: pytest.TempPathFactory) -> Iterator[AsyncEngine]:
    engine = _create_engine(tmp_path_factory.mktemp("empty") / "doxa.sqlite3")
    asyncio.run(_init_db(engine, with_data=False))
    yield engine
    asyncio.run(engine.dispose())


@pytest.fixture
def client(seeded_engine: AsyncEngine) -> Iterator[TestClient]:
    with _build_client(seeded_engine) as test_client:
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def empty_db_client(empty_engine: AsyncEngine) -> Iterator[TestClient]:
    with _build_client(empty_engine) as test_client:
        yield test_client
    app.dependency_overrides.clear()
