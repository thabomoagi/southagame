from typing import TYPE_CHECKING

from sqlalchemy import ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base

if TYPE_CHECKING:
    from app.models.attempt_answer import AttemptAnswer
    from app.models.attempt_question import AttemptQuestion
    from app.models.category import Category
    from app.models.option import Option


class Question(Base):
    __tablename__ = "questions"

    id: Mapped[int] = mapped_column(primary_key=True)
    external_id: Mapped[str | None] = mapped_column(String(50), unique=True)
    category_id: Mapped[int | None] = mapped_column(
        ForeignKey("categories.id", ondelete="CASCADE")
    )
    prompt: Mapped[str] = mapped_column(Text)
    difficulty: Mapped[str] = mapped_column(String(20))
    era: Mapped[str | None] = mapped_column(String(20))
    explanation: Mapped[str | None] = mapped_column(Text)
    is_active: Mapped[bool] = mapped_column(default=True)

    category: Mapped["Category | None"] = relationship(
        back_populates="questions"
    )
    options: Mapped[list["Option"]] = relationship(
        back_populates="question",
        cascade="all, delete-orphan",
    )
    attempt_questions: Mapped[list["AttemptQuestion"]] = relationship(
        back_populates="question"
    )
    attempt_answers: Mapped[list["AttemptAnswer"]] = relationship(
        back_populates="question"
    )