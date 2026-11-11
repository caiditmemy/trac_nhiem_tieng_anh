"""vocabulary notebook, streak and activity points

Revision ID: 0003
Revises: 0002
Create Date: 2026-10-01

"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0003"
down_revision: Union[str, Sequence[str], None] = "0002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "vocabularies",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("student_id", sa.BigInteger(), nullable=False),
        sa.Column("word", sa.String(100), nullable=False),
        sa.Column("meaning", sa.String(500), nullable=False),
        sa.Column("example", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.UniqueConstraint("student_id", "word", name="uq_vocabularies_student_word"),
    )
    op.create_index("ix_vocabularies_student_id", "vocabularies", ["student_id"])

    op.create_table(
        "student_streak",
        sa.Column("student_id", sa.BigInteger(), primary_key=True, autoincrement=False),
        sa.Column("current_streak", sa.Integer(), nullable=False),
        sa.Column("longest_streak", sa.Integer(), nullable=False),
        sa.Column("last_active_date", sa.Date(), nullable=False),
    )

    op.create_table(
        "student_points",
        sa.Column("id", sa.BigInteger(), primary_key=True, autoincrement=True),
        sa.Column("student_id", sa.BigInteger(), nullable=False),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("reason", sa.String(20), nullable=False),
        sa.Column("source_key", sa.String(50), nullable=False),
        sa.Column("detail", sa.String(255), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("source_key", name="uq_student_points_source_key"),
        sa.CheckConstraint("reason IN ('EXERCISE')", name="ck_student_points_reason"),
    )
    op.create_index("ix_student_points_student_id", "student_points", ["student_id"])
    op.create_index("ix_student_points_created_at", "student_points", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_student_points_created_at", table_name="student_points")
    op.drop_index("ix_student_points_student_id", table_name="student_points")
    op.drop_table("student_points")
    op.drop_table("student_streak")
    op.drop_index("ix_vocabularies_student_id", table_name="vocabularies")
    op.drop_table("vocabularies")
