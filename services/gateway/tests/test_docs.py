import httpx
import respx


@respx.mock
def test_service_spec_is_rewritten_to_go_through_the_gateway(client):
    respx.get("http://auth.test/v3/api-docs").mock(
        return_value=httpx.Response(
            200,
            json={
                "openapi": "3.1.0",
                "servers": [{"url": "http://auth-service:8081"}],
                "paths": {"/users": {}, "/users/{id}": {}, "/usersx": {}, "/actuator/health": {}},
            },
        )
    )

    response = client.get("/api/docs/auth-service.json")

    assert response.status_code == 200
    spec = response.json()
    assert [server["url"] for server in spec["servers"]] == ["/api"]
    # only what the gateway routes to auth-service: /api/users and below
    assert set(spec["paths"]) == {"/users", "/users/{id}"}


def test_unknown_service_spec_is_404(client):
    response = client.get("/api/docs/payment-service.json")

    assert response.status_code == 404
    assert response.json()["message"] == "No API description for payment-service"


@respx.mock
def test_spec_of_a_stopped_service_is_502(client):
    respx.get("http://auth.test/v3/api-docs").mock(side_effect=httpx.ConnectError("connection refused"))

    response = client.get("/api/docs/auth-service.json")

    assert response.status_code == 502
    assert response.json()["message"] == "auth-service is unavailable"


def test_swagger_page_lists_auth_service(client):
    for path in ("/docs", "/api/docs"):
        page = client.get(path)
        assert page.status_code == 200
        assert "/api/docs/auth-service.json" in page.text
        assert "StandaloneLayout" in page.text
        assert "swagger-ui-standalone-preset.js" in page.text
        assert '"displayRequestDuration": true,\n  "presets"' in page.text  # the settings stay valid JavaScript


def test_gateway_spec_describes_its_own_endpoints(client):
    spec = client.get("/api/openapi.json").json()

    assert "/api/health" in spec["paths"]
