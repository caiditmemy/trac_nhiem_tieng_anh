"""Vocabulary notebook and the name lookup against auth-service."""

import httpx
import pytest

from learning_service import auth_client

STUDENT = ("2", "STUDENT")
THIRD_STUDENT = ("5", "STUDENT")
TEACHER = ("3", "TEACHER")


@pytest.fixture
def client(bank):
    return bank


@pytest.fixture
def h(auth_header):
    return lambda who: auth_header(*who)


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
