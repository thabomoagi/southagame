from datetime import datetime
from sqlalchemy import DateTime, JSON, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class AiInsightCache(Base):
    __tablename__ = "ai_insight_cache"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    insight_data: Mapped[dict] = mapped_column(JSON(), nullable=False)
    generated_at: Mapped[datetime] = mapped_column(
        DateTime(),
        server_default=func.now(),
        nullable=False,
    )
