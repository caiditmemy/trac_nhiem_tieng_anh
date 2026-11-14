"""Week 6: activity points and streak from submitted exercises, weekly ranking, vocabulary."""

from datetime import UTC, datetime, timedelta

import httpx
import pytest

from learning_service import auth_client, clock, exercise_client
from learning_service.errors import ApiError

STUDENT = ("2", "STUDENT")
OTHER_STUDENT = ("6", "STUDENT")
THIRD_STUDENT = ("5", "STUDENT")
TEACHER = ("3", "TEACHER")

STUDENT_NAMES = {2: "Nguyễn Văn A", 5: "Phạm Thị D", 6: "Hoàng Văn E"}


class FakeClock:
    """Starts on Thursday 1 October 2026 at 10:00 in Vietnam (03:00 UTC)."""

    def __init__(self) -> None:
        self.value = datetime(2026, 10, 1, 3, 0, tzinfo=UTC)

    def advance(self, **delta: float) -> None:
        self.value += timedelta(**delta)


@pytest.fixture
def now(monkeypatch):
    fake = FakeClock()
    monkeypatch.setattr(clock, "now", lambda: fake.value)
    return fake


@pytest.fixture
def submissions(monkeypatch, now):
    """What exercise-service answers to GET /submissions/{id}: id -> body. Each student only sees their own."""
    store: dict[int, dict] = {}

    def get_submission(submission_id, authorization):
        if submission_id not in store:
            raise ApiError(404, f"Submission {submission_id} not found")
        return store[submission_id]

    monkeypatch.setattr(exercise_client, "get_submission", get_submission)
    return store


@pytest.fixture
def client(bank, monkeypatch, now, submissions):
    monkeypatch.setattr(auth_client, "lookup_names", lambda ids, authorization: {i: STUDENT_NAMES[i] for i in ids if i in STUDENT_NAMES})
    return bank


@pytest.fixture
def h(auth_header):
    return lambda who: auth_header(*who)


def submitted(submissions, submission_id: int, correct: int, at: str = "2026-10-01T03:00:00.123456789Z") -> int:
    """A submission as exercise-service shows it (Java Instant: nanoseconds, Z)."""
    submissions[submission_id] = {
        "id": submission_id,
        "exerciseTitle": "Bài 2: Ôn tập thì",
        "status": "SUBMITTED",
        "correctCount": correct,
        "submittedAt": at,
    }
    return submission_id


def report(client, h, who, submission_id):
    return client.post("/activities/exercise", json={"submissionId": submission_id}, headers=h(who))


# ------------------------------------------------------------------------------------ points and streak


def test_a_submitted_exercise_gives_twenty_points_plus_one_per_correct_answer_once(client, h, submissions):
    submitted(submissions, 7, correct=2)

    first = report(client, h, STUDENT, 7)
    assert first.status_code == 200
    assert first.json() == {"pointsEarned": 22, "totalPoints": 22, "created": True}
    # A retry (the same submission reported again) adds nothing
    again = report(client, h, STUDENT, 7)
    assert again.json() == {"pointsEarned": 22, "totalPoints": 22, "created": False}

    assert client.get("/streak", headers=h(STUDENT)).json() == {
        "currentStreak": 1,
        "longestStreak": 1,
        "lastActiveDate": "2026-10-01",
        "activeToday": True,
    }
    me = client.get("/ranking/weekly", headers=h(STUDENT)).json()["me"]
    assert me["totalPoints"] == 22
    latest = me["recent"][0]
    assert (latest["points"], latest["reason"], latest["detail"]) == (22, "EXERCISE", "Bài 2: Ôn tập thì")


def test_only_a_submitted_exercise_of_the_caller_counts(client, h, submissions):
    submissions[8] = {"id": 8, "status": "IN_PROGRESS", "correctCount": None}
    assert report(client, h, STUDENT, 8).status_code == 409
    assert report(client, h, STUDENT, 99).status_code == 404  # exercise-service: not found / not yours
    assert report(client, h, TEACHER, 8).status_code == 403
    assert client.post("/activities/exercise", json={"submissionId": 8}).status_code == 401
    assert client.get("/ranking/weekly", headers=h(STUDENT)).json()["me"]["totalPoints"] == 0


def test_a_new_day_extends_the_streak_and_a_gap_restarts_it(client, h, submissions, now):
    report(client, h, STUDENT, submitted(submissions, 1, 0))
    now.advance(days=1)
    report(client, h, STUDENT, submitted(submissions, 2, 0, "2026-10-02T03:00:00Z"))
    now.advance(days=1)
    report(client, h, STUDENT, submitted(submissions, 3, 0, "2026-10-03T03:00:00Z"))
    assert client.get("/streak", headers=h(STUDENT)).json()["currentStreak"] == 3

    # A second exercise on the same day does not add a day
    report(client, h, STUDENT, submitted(submissions, 4, 1, "2026-10-03T05:00:00Z"))
    assert client.get("/streak", headers=h(STUDENT)).json()["currentStreak"] == 3

    now.advance(days=1)  # Sunday was missed... the next day is Monday
    assert client.get("/streak", headers=h(STUDENT)).json() == {"currentStreak": 3, "longestStreak": 3, "lastActiveDate": "2026-10-03", "activeToday": False}
    now.advance(days=1)
    assert client.get("/streak", headers=h(STUDENT)).json()["currentStreak"] == 0
    report(client, h, STUDENT, submitted(submissions, 5, 0, "2026-10-05T03:00:00Z"))
    assert client.get("/streak", headers=h(STUDENT)).json() == {"currentStreak": 1, "longestStreak": 3, "lastActiveDate": "2026-10-05", "activeToday": True}


def test_streak_is_for_students_only_and_starts_at_zero(client, h):
    assert client.get("/streak", headers=h(STUDENT)).json() == {"currentStreak": 0, "longestStreak": 0, "lastActiveDate": None, "activeToday": False}
    assert client.get("/streak", headers=h(TEACHER)).status_code == 403


def test_weekly_ranking_counts_this_weeks_points_with_shared_ranks(client, h, submissions, now):
    now.value = datetime(2026, 9, 27, 3, 0, tzinfo=UTC)  # last Sunday: does not count this week
    report(client, h, THIRD_STUDENT, submitted(submissions, 1, 0, "2026-09-27T03:00:00Z"))
    now.value = datetime(2026, 10, 1, 3, 0, tzinfo=UTC)
    report(client, h, STUDENT, submitted(submissions, 2, 0))  # 20 points
    report(client, h, THIRD_STUDENT, submitted(submissions, 3, 0))  # 20 points
    report(client, h, OTHER_STUDENT, submitted(submissions, 4, 2))  # 22 points

    ranking = client.get("/ranking/weekly", headers=h(STUDENT)).json()
    assert (ranking["weekStart"], ranking["weekEnd"]) == ("2026-09-28", "2026-10-04")
    assert [(entry["rank"], entry["fullName"], entry["points"]) for entry in ranking["entries"]] == [
        (1, "Hoàng Văn E", 22),
        (2, "Nguyễn Văn A", 20),
        (2, "Phạm Thị D", 20),
    ]
    assert ranking["me"]["rank"] == 2 and ranking["me"]["weekPoints"] == 20 and ranking["me"]["totalPoints"] == 20

    third = client.get("/ranking/weekly", headers=h(THIRD_STUDENT)).json()["me"]
    assert (third["rank"], third["weekPoints"], third["totalPoints"]) == (2, 20, 40)

    teacher_view = client.get("/ranking/weekly?limit=1", headers=h(TEACHER)).json()
    assert teacher_view["me"] is None
    assert [entry["fullName"] for entry in teacher_view["entries"]] == ["Hoàng Văn E"]
    summary = client.get("/ranking/weekly?limit=0", headers=h(STUDENT)).json()
    assert summary["entries"] == [] and summary["me"]["rank"] == 2


# --------------------------------------------------------------------------------------------- vocabulary


def test_vocabulary_notebook_is_private_and_has_no_duplicates(client, h):
    word = client.post("/vocabularies", json={"word": "persevere", "meaning": "kiên trì", "example": "Persevere and you will win."}, headers=h(STUDENT))
    assert word.status_code == 201
    saved = word.json()
    assert (saved["word"], saved["meaning"]) == ("persevere", "kiên trì")
    url = f"/vocabularies/{saved['id']}"

    assert client.post("/vocabularies", json={"word": "PERSEVERE", "meaning": "bền bỉ"}, headers=h(STUDENT)).status_code == 409
    # Another student neither sees nor changes it, but can save the same word
    assert client.get("/vocabularies", headers=h(THIRD_STUDENT)).json() == []
    assert client.put(url, json={"word": "x", "meaning": "y"}, headers=h(THIRD_STUDENT)).status_code == 404
    assert client.delete(url, headers=h(THIRD_STUDENT)).status_code == 404
    assert client.post("/vocabularies", json={"word": "persevere", "meaning": "kiên trì"}, headers=h(THIRD_STUDENT)).status_code == 201

    assert client.post("/vocabularies", json={"word": "diligent", "meaning": "chăm chỉ"}, headers=h(STUDENT)).status_code == 201
    assert [item["word"] for item in client.get("/vocabularies?q=kiên", headers=h(STUDENT)).json()] == ["persevere"]
    assert [item["word"] for item in client.get("/vocabularies", headers=h(STUDENT)).json()] == ["diligent", "persevere"]
    edited = client.put(url, json={"word": "persevere", "meaning": "kiên trì, bền bỉ"}, headers=h(STUDENT))
    assert edited.status_code == 200 and edited.json()["meaning"] == "kiên trì, bền bỉ"
    assert client.put(url, json={"word": "Diligent", "meaning": "chăm chỉ"}, headers=h(STUDENT)).status_code == 409

    assert client.delete(url, headers=h(STUDENT)).status_code == 204
    assert [item["word"] for item in client.get("/vocabularies", headers=h(STUDENT)).json()] == ["diligent"]
    assert client.post("/vocabularies", json={"word": " ", "meaning": "trống"}, headers=h(STUDENT)).status_code == 400
    assert client.get("/vocabularies", headers=h(TEACHER)).status_code == 403


# ------------------------------------------------------------------------------- calls to other services


def reply(status: int, body: dict | None = None):
    def fake_get(url, headers, timeout):
        assert url.endswith("/submissions/7")
        assert headers == {"authorization": "Bearer t"}
        return httpx.Response(status, json=body or {}, request=httpx.Request("GET", url))

    return fake_get


def test_exercise_client_reads_the_submission_with_the_students_token(monkeypatch):
    monkeypatch.setattr(exercise_client.httpx, "get", reply(200, {"status": "SUBMITTED"}))
    assert exercise_client.get_submission(7, "Bearer t") == {"status": "SUBMITTED"}


@pytest.mark.parametrize(("status", "expected"), [(404, 404), (403, 403), (500, 502)])
def test_exercise_client_maps_errors(monkeypatch, status, expected):
    monkeypatch.setattr(exercise_client.httpx, "get", reply(status))
    with pytest.raises(ApiError) as caught:
        exercise_client.get_submission(7, "Bearer t")
    assert caught.value.status == expected


def test_exercise_service_being_down_is_a_502(monkeypatch):
    def broken_get(url, headers, timeout):
        raise httpx.ConnectError("connection refused")

    monkeypatch.setattr(exercise_client.httpx, "get", broken_get)
    with pytest.raises(ApiError) as caught:
        exercise_client.get_submission(7, "Bearer t")
    assert caught.value.status == 502


def test_lookup_names_survives_auth_service_errors(monkeypatch):
    def fake_get(url, params, headers, timeout):
        assert url.endswith("/users/lookup") and params == {"ids": "2,5"}
        return httpx.Response(200, json=[{"id": 2, "fullName": "A"}, {"id": 5, "fullName": "D"}], request=httpx.Request("GET", url))

    monkeypatch.setattr(auth_client.httpx, "get", fake_get)
    assert auth_client.lookup_names([5, 2, 2], "Bearer t") == {2: "A", 5: "D"}

    def broken_get(url, params, headers, timeout):
        raise httpx.ConnectError("connection refused")

    monkeypatch.setattr(auth_client.httpx, "get", broken_get)
    assert auth_client.lookup_names([2], "Bearer t") == {}
