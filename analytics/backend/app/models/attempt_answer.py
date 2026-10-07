from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base

if TYPE_CHECKING:
    from app.models.attempt import Attempt
    from app.models.option import Option
    from app.models.question import Question


class AttemptAnswer(Base):
    __tablename__ = "attempt_answers"

    id: Mapped[int] = mapped_column(primary_key=True)
    attempt_id: Mapped[UUID] = mapped_column(
        ForeignKey("attempts.id", ondelete="CASCADE")
    )
    question_id: Mapped[int] = mapped_column(
        ForeignKey("questions.id", ondelete="RESTRICT")
    )
    selected_option_id: Mapped[int | None] = mapped_column(
        ForeignKey("options.id", ondelete="RESTRICT")
    )
    is_correct: Mapped[bool] = mapped_column(default=False)
    time_taken_ms: Mapped[int]
    points_earned: Mapped[int]

    attempt: Mapped["Attempt"] = relationship(
        back_populates="attempt_answers"
    )
    question: Mapped["Question"] = relationship(
        back_populates="attempt_answers"
    )
    selected_option: Mapped["Option"] = relationship(
        back_populates="attempt_answers"
    )