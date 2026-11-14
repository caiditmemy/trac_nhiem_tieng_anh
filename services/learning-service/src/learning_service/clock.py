"""Time helpers."""

from datetime import UTC, datetime


def as_utc(moment: datetime) -> datetime:
    """SQLite (tests) gives back naive datetimes; they are stored in UTC."""
    return moment.replace(tzinfo=UTC) if moment.tzinfo is None else moment.astimezone(UTC)
