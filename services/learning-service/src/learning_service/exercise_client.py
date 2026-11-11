"""REST client for exercise-service: confirms that a student really submitted an exercise before points are added."""

from typing import Any

import httpx

from learning_service.config import get_settings
from learning_service.errors import ApiError


def get_submission(submission_id: int, authorization: str) -> dict[str, Any]:
    """GET /submissions/{id} with the student's token: exercise-service only shows a student their own attempt."""
    try:
        response = httpx.get(
            f"{get_settings().exercise_service_url}/submissions/{submission_id}",
            headers={"authorization": authorization},
            timeout=5.0,
        )
    except httpx.HTTPError:
        raise ApiError(502, "exercise-service is unavailable") from None
    if response.status_code == 404:
        raise ApiError(404, f"Submission {submission_id} not found")
    if response.status_code == 403:
        raise ApiError(403, "This is not your submission")
    if response.status_code != 200:
        raise ApiError(502, "exercise-service is unavailable")
    return response.json()
