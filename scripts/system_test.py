#!/usr/bin/env python3
"""System test (sprint 1): drives the running system through the API Gateway, like a client, and checks that the
gateway and Auth Service work together: health, JWKS, register, login, protected endpoint, locked accounts.

    python3 scripts/system_test.py                                 # gateway at http://localhost:8000/api
    python3 scripts/system_test.py --api http://nambede.dev.localhost/api --auth http://localhost:8081

Every run registers its own users (emails end with a random tag), so it can run any number of times next to the
demo data. Exit code 0 when every check passed. Only the Python standard library is used.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
import uuid

from demo_data import Gateway, login, read_env_admin

PASSWORD = "System@123"
#: The gateway keeps an "open session" answer 10 s
SESSION_TIMEOUT = 20.0

failures: list[str] = []


def check(ok: bool, text: str) -> bool:
    print(f"  {'✓' if ok else '✗'} {text}")
    if not ok:
        failures.append(text)
    return ok


def eventually(probe, timeout: float = 10.0):
    """Calls `probe` until it returns something truthy or the time is up; returns the last result."""
    deadline = time.monotonic() + timeout
    while True:
        result = probe()
        if result or time.monotonic() > deadline:
            return result
        time.sleep(0.5)


def fetch_json(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=30) as response:
        return json.load(response)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api", default="http://localhost:8000/api", help="gateway address (default %(default)s)")
    parser.add_argument("--auth", default="http://localhost:8081", help="Auth Service address, for the JWKS; empty to skip (default %(default)s)")
    parser.add_argument("--admin-email")
    parser.add_argument("--admin-password")
    args = parser.parse_args()
    api = Gateway(args.api)
    tag = uuid.uuid4().hex[:8]
    print(f"Kiểm thử hệ thống ở {args.api} (mã lần chạy {tag})")

    print("Sức khỏe")
    try:
        check(fetch_json(f"{args.api}/health").get("status") == "UP", "gateway: /api/health UP")
        if args.auth:
            check(fetch_json(f"{args.auth}/actuator/health").get("status") == "UP", "auth-service: /actuator/health UP")
    except (urllib.error.URLError, OSError) as error:
        sys.exit(f"Không gọi được hệ thống ({error}). Đã chạy chưa? make backend")

    if args.auth:
        print("RS256 + JWKS: khóa công khai của Auth Service")
        keys = fetch_json(f"{args.auth}/.well-known/jwks.json").get("keys", [])
        check(bool(keys) and all(key.get("kty") == "RSA" and key.get("kid") and key.get("n") for key in keys), f"JWKS có {len(keys)} khóa RSA")
        check(all("d" not in key for key in keys), "không có thành phần khóa riêng (d)")

    print("Đăng ký và đăng nhập")
    email = f"sv-{tag}@test.local"
    registered = api.ok("POST", "/auth/register", body={"email": email, "password": PASSWORD, "fullName": f"SV {tag}"})
    check(registered["user"]["role"] == "STUDENT", "đăng ký luôn tạo tài khoản STUDENT")
    user_id = registered["user"]["id"]
    status, _ = api.call("POST", "/auth/register", body={"email": email, "password": PASSWORD, "fullName": f"SV {tag}"})
    check(status == 409, "đăng ký trùng email bị từ chối (409)")
    token = login(api, email, PASSWORD)
    check(token is not None, "đăng nhập đúng mật khẩu nhận accessToken")
    check(login(api, email, "Sai@12345") is None, "đăng nhập sai mật khẩu bị từ chối")

    print("Endpoint cần đăng nhập: GET /api/users/me")
    check(api.call("GET", "/users/me")[0] == 401, "không có token: 401")
    check(api.call("GET", "/users/me", "token.khong.hop.le")[0] == 401, "token sai: 401")
    status, me = api.call("GET", "/users/me", token)
    check(status == 200 and me["email"] == email, "có token: 200 và đúng người dùng")

    print("X-Request-ID: gateway trả lại mã của request")
    request = urllib.request.Request(f"{args.api}/users/me", headers={"Authorization": f"Bearer {token}", "X-Request-ID": f"system-test-{tag}"})
    with urllib.request.urlopen(request, timeout=30) as response:
        check(response.headers.get("X-Request-ID") == f"system-test-{tag}", f"giữ mã system-test-{tag}")

    print("Phân quyền: chỉ admin xem được danh sách người dùng")
    check(api.call("GET", "/users", token)[0] == 403, "sinh viên gọi GET /users: 403")
    env_email, env_password = read_env_admin()
    admin = login(api, args.admin_email or env_email, args.admin_password or env_password)
    if admin is None:
        sys.exit("Không đăng nhập được admin: kiểm tra ADMIN_EMAIL, ADMIN_PASSWORD")
    check(api.call("GET", "/users", admin)[0] == 200, "admin gọi GET /users: 200")

    print("Phiên đăng nhập: khóa tài khoản thì token đang dùng bị từ chối, không phải chờ hết hạn")
    check(api.call("GET", "/users/me", token)[0] == 200, "token dùng bình thường")
    api.ok("PUT", f"/users/{user_id}/status", admin, body={"status": "LOCKED"})

    def refused_now():
        status, body = api.call("GET", "/users/me", token)
        return body if status == 401 else None

    refused = eventually(refused_now, SESSION_TIMEOUT)
    check(bool(refused) and refused.get("message") == "Account is locked", "admin khóa tài khoản: token cũ bị 401 \"Account is locked\" trong vòng 10 giây")
    api.ok("PUT", f"/users/{user_id}/status", admin, body={"status": "ACTIVE"})
    again = login(api, email, PASSWORD)
    check(again is not None and api.call("GET", "/users/me", again)[0] == 200, "mở khóa, đăng nhập lại được")

    print()
    if failures:
        sys.exit(f"{len(failures)} kiểm tra không đạt: " + "; ".join(failures))
    print("Mọi kiểm tra đều đạt.")


if __name__ == "__main__":
    main()
