"""Learning activity reported by other services. Not exposed by the gateway: exercise-service calls it directly."""

from datetime import datetime

from fastapi import APIRouter

from learning_service import clock, exercise_client, progress
from learning_service.errors import ApiError
from learning_service.routers.common import Db
from learning_service.schemas import ActivityOut, ExerciseActivityIn
from learning_service.security import Student

router = APIRouter(prefix="/activities", tags=["Nội bộ"])


@router.post("/exercise", summary="(Nội bộ) Exercise Service báo đã nộp bài để cộng điểm; gateway không mở đường này", response_model=ActivityOut)
def exercise_completed(body: ExerciseActivityIn, user: Student, db: Db) -> ActivityOut:
    """exercise-service calls this with the student's token after a submit: +20 points, +1 per correct answer.

    The submission is read back from exercise-service (which only shows students their own attempts),
    so nobody gets points for an exercise they did not submit. Each submission counts once.
    """
    submission = exercise_client.get_submission(body.submission_id, user.authorization)
    if submission.get("status") != "SUBMITTED":
        raise ApiError(409, "This exercise has not been submitted yet")
    correct = int(submission.get("correctCount") or 0)
    points = progress.EXERCISE_POINTS + correct * progress.POINTS_PER_CORRECT_ANSWER
    entry, created = progress.award(db, user.id, "EXERCISE", f"EXERCISE:{body.submission_id}", points, submission.get("exerciseTitle"))
    if created:
        submitted_at = submission.get("submittedAt")
        moment = datetime.fromisoformat(submitted_at) if isinstance(submitted_at, str) else clock.now()
        progress.record_active_day(db, user.id, clock.local_date(moment))
    db.commit()
    return ActivityOut(points_earned=entry.points, total_points=progress.total_points(db, user.id), created=created)
