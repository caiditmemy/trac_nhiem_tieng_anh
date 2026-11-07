import json

import httpx
import pytest
import respx
from conftest import OTHER_KEY


def test_health_is_public(client):
    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_missing_token_returns_401(client):
    response = client.get("/api/users/me")

    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"
    assert response.json() == {
        "status": 401,
        "error": "Unauthorized",
        "message": "Authentication required",
        "path": "/api/users/me",
    }


def test_token_signed_with_another_key_returns_401(client, auth_header):
    headers = auth_header(key=OTHER_KEY)

    assert client.get("/api/users/me", headers=headers).status_code == 401


def test_expired_token_returns_401(client, auth_header):
    assert client.get("/api/users/me", headers=auth_header(expires_in=-60)).status_code == 401


@respx.mock
def test_me_with_a_valid_token_returns_the_profile(client, auth_header):
    profile = {"id": 2, "email": "user2@nambede.local", "fullName": "User 2", "role": "STUDENT"}
    respx.get("http://auth.test/users/me").mock(return_value=httpx.Response(200, json=profile))

    response = client.get("/api/users/me", headers=auth_header())

    assert response.status_code == 200
    assert response.json() == profile


@respx.mock
def test_request_is_forwarded_with_token_query_and_body(client, auth_header):
    route = respx.put("http://auth.test/users/5/status").mock(
        return_value=httpx.Response(200, json={"id": 5, "status": "LOCKED"})
    )
    headers = auth_header(sub="1", role="ADMIN")

    response = client.put("/api/users/5/status?notify=1", json={"status": "LOCKED"}, headers=headers)

    assert response.status_code == 200
    assert response.json() == {"id": 5, "status": "LOCKED"}
    sent = route.calls.last.request
    assert sent.headers["authorization"] == headers["Authorization"]
    assert sent.url.params["notify"] == "1"
    assert json.loads(sent.content) == {"status": "LOCKED"}


@respx.mock
def test_login_needs_no_token(client):
    respx.post("http://auth.test/auth/login").mock(return_value=httpx.Response(200, json={"accessToken": "t"}))

    response = client.post("/api/auth/login", json={"email": "a@nambede.local", "password": "secret"})

    assert response.status_code == 200
    assert response.json() == {"accessToken": "t"}


@respx.mock
def test_blocked_login_keeps_retry_after(client):
    body = {"status": 429, "error": "Too Many Requests", "message": "Too many failed sign-ins, try again in 240 seconds", "path": "/auth/login"}
    respx.post("http://auth.test/auth/login").mock(return_value=httpx.Response(429, json=body, headers={"retry-after": "240"}))

    response = client.post("/api/auth/login", json={"email": "a@nambede.local", "password": "secret"})

    assert response.status_code == 429
    assert response.headers["retry-after"] == "240"
    assert response.json() == body


@respx.mock
def test_service_errors_pass_through_unchanged(client, auth_header):
    body = {"status": 403, "error": "Forbidden", "message": "You do not have permission", "path": "/users"}
    respx.get("http://auth.test/users").mock(return_value=httpx.Response(403, json=body))

    response = client.get("/api/users", headers=auth_header())

    assert response.status_code == 403
    assert response.json() == body


@respx.mock
def test_unreachable_service_returns_502(client, auth_header):
    respx.get("http://auth.test/users/ping").mock(side_effect=httpx.ConnectError("connection refused"))

    response = client.get("/api/users/ping", headers=auth_header())

    assert response.status_code == 502
    assert response.json()["message"] == "auth-service is unavailable"
    assert response.json()["path"] == "/api/users/ping"


@respx.mock
def test_every_request_gets_an_id_that_the_services_receive_too(client, auth_header):
    route = respx.get("http://auth.test/users/me").mock(return_value=httpx.Response(200, json=[]))

    response = client.get("/api/users/me", headers=auth_header())

    request_id = response.headers["x-request-id"]
    assert len(request_id) == 32
    assert route.calls.last.request.headers["x-request-id"] == request_id


@respx.mock
def test_a_well_formed_request_id_from_the_client_is_kept(client, auth_header):
    route = respx.get("http://auth.test/users/me").mock(return_value=httpx.Response(200, json=[]))

    kept = client.get("/api/users/me", headers={**auth_header(), "X-Request-ID": "web-7f3a"})
    replaced = client.get("/api/users/me", headers={**auth_header(), "X-Request-ID": "x" * 65})

    assert kept.headers["x-request-id"] == "web-7f3a"
    assert route.calls[0].request.headers["x-request-id"] == "web-7f3a"
    assert replaced.headers["x-request-id"] != "x" * 65
    # Errors made by the gateway itself carry one as well
    assert len(client.get("/api/users/me").headers["x-request-id"]) == 32


@pytest.mark.real_sessions
@respx.mock
def test_a_session_ended_by_auth_service_is_refused_with_its_reason(client, auth_header):
    respx.get("http://auth.test/auth/session").mock(
        return_value=httpx.Response(401, json={"status": 401, "error": "Unauthorized", "message": "Account is locked", "path": "/auth/session"})
    )

    response = client.get("/api/users/me", headers=auth_header(sub="7"))

    assert response.status_code == 401
    assert response.json()["message"] == "Account is locked"


@pytest.mark.real_sessions
@respx.mock
def test_an_open_session_is_asked_once_then_kept_a_few_seconds(client, auth_header):
    session = respx.get("http://auth.test/auth/session").mock(return_value=httpx.Response(204))
    respx.get("http://auth.test/users/me").mock(return_value=httpx.Response(200, json=[]))
    headers = auth_header(sub="8", version=2)

    for _ in range(3):
        assert client.get("/api/users/me", headers=headers).status_code == 200

    assert session.call_count == 1
    sent = session.calls.last.request
    assert sent.headers["authorization"] == headers["Authorization"]
    assert "x-request-id" in sent.headers
    # A newer token (higher version) is a new session: asked again
    assert client.get("/api/users/me", headers=auth_header(sub="8", version=3)).status_code == 200
    assert session.call_count == 2


@pytest.mark.real_sessions
@respx.mock
def test_auth_service_down_lets_signed_tokens_through(client, auth_header):
    respx.get("http://auth.test/auth/session").mock(side_effect=httpx.ConnectError("connection refused"))
    respx.get("http://auth.test/users/me").mock(return_value=httpx.Response(200, json=[]))

    assert client.get("/api/users/me", headers=auth_header(sub="9")).status_code == 200


def test_keys_that_cannot_be_read_give_502_not_401(client, auth_header, monkeypatch):
    from gateway import security
    from gateway.jwks import KeysUnavailable

    class NoKeys:
        def find(self, kid):
            raise KeysUnavailable("auth-service is unavailable")

    monkeypatch.setattr(security, "public_keys", lambda: NoKeys())

    response = client.get("/api/users/me", headers=auth_header())

    assert response.status_code == 502
    assert response.json()["message"] == "auth-service is unavailable"

