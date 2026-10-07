from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import ForeignKey, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base

if TYPE_CHECKING:
    from app.models.attempt import Attempt
    from app.models.question import Question


class AttemptQuestion(Base):
    __tablename__ = "attempt_questions"

    __table_args__ = (
        UniqueConstraint(
            "attempt_id",
            "question_id",
            name="uq_attempt_questions_attempt_question",
        ),
    )

    id: Mapped[int] = mapped_column(primary_key=True)
    attempt_id: Mapped[UUID] = mapped_column(
        ForeignKey("attempts.id", ondelete="CASCADE")
    )
    question_id: Mapped[int] = mapped_column(
        ForeignKey("questions.id", ondelete="RESTRICT")
    )
    position: Mapped[int]

    attempt: Mapped["Attempt"] = relationship(
        back_populates="attempt_questions"
    )
    question: Mapped["Question"] = relationship(
        back_populates="attempt_questions"
    )