"""vocabulary notebook

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


def downgrade() -> None:
    op.drop_index("ix_vocabularies_student_id", table_name="vocabularies")
    op.drop_table("vocabularies")
