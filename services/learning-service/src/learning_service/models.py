"""Tables of learning_db. User ids come from auth-service: no foreign key across services."""

from datetime import datetime

from sqlalchemy import BigInteger, CheckConstraint, DateTime, ForeignKey, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from learning_service.db import Base

# BIGINT on PostgreSQL; INTEGER on SQLite (tests), where only INTEGER primary keys autoincrement
Id = BigInteger().with_variant(Integer(), "sqlite")


class Topic(Base):
    __tablename__ = "topics"
    __table_args__ = (UniqueConstraint("name", name="uq_topics_name"),)

    id: Mapped[int] = mapped_column(Id, primary_key=True)
    name: Mapped[str] = mapped_column(String(255))
    description: Mapped[str | None] = mapped_column(Text)
    created_by: Mapped[int] = mapped_column(BigInteger)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())

    questions: Mapped[list["Question"]] = relationship(back_populates="topic")


class Question(Base):
    """A multiple-choice question with exactly four answers A to D and one correct answer."""

    __tablename__ = "questions"
    __table_args__ = (CheckConstraint("correct_option IN ('A', 'B', 'C', 'D')", name="ck_questions_correct_option"),)

    id: Mapped[int] = mapped_column(Id, primary_key=True)
    topic_id: Mapped[int] = mapped_column(Id, ForeignKey("topics.id", ondelete="RESTRICT"), index=True)
    content: Mapped[str] = mapped_column(Text)
    option_a: Mapped[str] = mapped_column(Text)
    option_b: Mapped[str] = mapped_column(Text)
    option_c: Mapped[str] = mapped_column(Text)
    option_d: Mapped[str] = mapped_column(Text)
    correct_option: Mapped[str] = mapped_column(String(1))
    explanation: Mapped[str | None] = mapped_column(Text)
    created_by: Mapped[int] = mapped_column(BigInteger, index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    topic: Mapped[Topic] = relationship(back_populates="questions")


# ---------------------------------------------------------------- vocabulary


class Vocabulary(Base):
    """A word in a student's own vocabulary notebook."""

    __tablename__ = "vocabularies"
    __table_args__ = (UniqueConstraint("student_id", "word", name="uq_vocabularies_student_word"),)

    id: Mapped[int] = mapped_column(Id, primary_key=True)
    student_id: Mapped[int] = mapped_column(BigInteger, index=True)
    word: Mapped[str] = mapped_column(String(100))
    meaning: Mapped[str] = mapped_column(String(500))
    example: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
