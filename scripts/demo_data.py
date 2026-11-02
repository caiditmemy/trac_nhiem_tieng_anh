#!/usr/bin/env python3
"""Creates the demo accounts of NamBeDe through the API Gateway, exactly like a client would.

Sprint 1 has only accounts: one lecturer (created by the admin) and one student (who registers by themselves).
Safe to run again: whatever already exists is reused and nothing is created twice.

    python3 scripts/demo_data.py                                   # gateway at http://localhost:8000/api
    python3 scripts/demo_data.py --api http://nambede.dev.localhost/api

Only the Python standard library is used. The admin account comes from .env (ADMIN_EMAIL, ADMIN_PASSWORD)
or from --admin-email / --admin-password.
"""

from __future__ import annotations  # the type hints below also work on the Python 3.9 that ships with macOS

import argparse
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

TEACHER_PASSWORD = "Teacher@123"
STUDENT_PASSWORD = "Student@123"

TEACHERS = [("teacher1@nambede.local", "Trần Thị B")]
STUDENTS = [("student1@nambede.local", "Nguyễn Văn A")]


class Gateway:
    """JSON calls to the API Gateway with the standard library."""

    def __init__(self, base: str) -> None:
        self.base = base.rstrip("/")

    def call(self, method: str, path: str, token: str | None = None, body=None, raw: bytes | None = None, content_type: str | None = None):
        headers = {"Accept": "application/json"}
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        if raw is not None:
            data = raw
            headers["Content-Type"] = content_type or "application/octet-stream"
        if token:
            headers["Authorization"] = f"Bearer {token}"
        request = urllib.request.Request(self.base + path, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                text = response.read().decode()
                return response.status, json.loads(text) if text else None
        except urllib.error.HTTPError as error:
            text = error.read().decode()
            try:
                return error.code, json.loads(text)
            except ValueError:
                return error.code, {"message": text}
        except urllib.error.URLError as error:
            sys.exit(f"Không gọi được gateway ở {self.base} ({error.reason}). Hệ thống đã chạy chưa? docker compose up -d")

    def ok(self, method: str, path: str, token: str | None = None, **kwargs):
        status, payload = self.call(method, path, token, **kwargs)
        if status >= 400:
            message = payload.get("message") if isinstance(payload, dict) else payload
            sys.exit(f"Lỗi {status} ở {method} {path}: {message}")
        return payload


def log(text: str) -> None:
    print(f"  ✓ {text}")


def read_env_admin() -> tuple[str, str]:
    """ADMIN_EMAIL / ADMIN_PASSWORD from the repo's .env, like docker compose uses them."""
    values = {"ADMIN_EMAIL": "admin@nambede.local", "ADMIN_PASSWORD": "Admin@123"}
    env_file = Path(__file__).resolve().parent.parent / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            key, _, value = line.partition("=")
            value = re.split(r"\s+#", value.strip(), maxsplit=1)[0].strip().strip("'\"")  # drop "  # comment" and quotes
            if key.strip() in values and value:
                values[key.strip()] = value
    return values["ADMIN_EMAIL"], values["ADMIN_PASSWORD"]


def login(api: Gateway, email: str, password: str) -> str | None:
    status, payload = api.call("POST", "/auth/login", body={"email": email, "password": password})
    if status >= 500:
        sys.exit(f"Lỗi {status} khi đăng nhập: {payload.get('message')}")
    return payload["accessToken"] if status == 200 else None


def ensure_lecturer(api: Gateway, admin: str, email: str, name: str) -> str:
    status, payload = api.call("POST", "/users", admin, body={"email": email, "password": TEACHER_PASSWORD, "fullName": name, "role": "TEACHER"})
    if status not in (201, 409):
        sys.exit(f"Không tạo được giảng viên {email}: {payload}")
    token = login(api, email, TEACHER_PASSWORD)
    if token is None:
        sys.exit(f"{email} đã có nhưng mật khẩu không phải {TEACHER_PASSWORD}")
    log(f"Giảng viên {name} <{email}> ({'mới tạo' if status == 201 else 'đã có'})")
    return token


def ensure_student(api: Gateway, email: str, name: str) -> str:
    status, payload = api.call("POST", "/auth/register", body={"email": email, "password": STUDENT_PASSWORD, "fullName": name})
    if status == 201:
        log(f"Sinh viên {name} <{email}> (tự đăng ký)")
        return payload["accessToken"]
    token = login(api, email, STUDENT_PASSWORD)
    if token is None:
        sys.exit(f"{email} đã có nhưng mật khẩu không phải {STUDENT_PASSWORD}")
    log(f"Sinh viên {name} <{email}> (đã có)")
    return token


def log(text: str) -> None:
    print(f"  ✓ {text}")


def read_env_admin() -> tuple[str, str]:
    """ADMIN_EMAIL / ADMIN_PASSWORD from the repo's .env, like docker compose uses them."""
    values = {"ADMIN_EMAIL": "admin@nambede.local", "ADMIN_PASSWORD": "Admin@123"}
    env_file = Path(__file__).resolve().parent.parent / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            key, _, value = line.partition("=")
            value = re.split(r"\s+#", value.strip(), maxsplit=1)[0].strip().strip("'\"")  # drop "  # comment" and quotes
            if key.strip() in values and value:
                values[key.strip()] = value
    return values["ADMIN_EMAIL"], values["ADMIN_PASSWORD"]


def login(api: Gateway, email: str, password: str) -> str | None:
    status, payload = api.call("POST", "/auth/login", body={"email": email, "password": password})
    if status >= 500:
        sys.exit(f"Lỗi {status} khi đăng nhập: {payload.get('message')}")
    return payload["accessToken"] if status == 200 else None


def ensure_lecturer(api: Gateway, admin: str, email: str, name: str) -> str:
    status, payload = api.call("POST", "/users", admin, body={"email": email, "password": TEACHER_PASSWORD, "fullName": name, "role": "TEACHER"})
    if status not in (201, 409):
        sys.exit(f"Không tạo được giảng viên {email}: {payload}")
    token = login(api, email, TEACHER_PASSWORD)
    if token is None:
        sys.exit(f"{email} đã có nhưng mật khẩu không phải {TEACHER_PASSWORD}")
    log(f"Giảng viên {name} <{email}> ({'mới tạo' if status == 201 else 'đã có'})")
    return token


def ensure_student(api: Gateway, email: str, name: str) -> str:
    status, payload = api.call("POST", "/auth/register", body={"email": email, "password": STUDENT_PASSWORD, "fullName": name})
    if status == 201:
        log(f"Sinh viên {name} <{email}> (tự đăng ký)")
        return payload["accessToken"]
    token = login(api, email, STUDENT_PASSWORD)
    if token is None:
        sys.exit(f"{email} đã có nhưng mật khẩu không phải {STUDENT_PASSWORD}")
    log(f"Sinh viên {name} <{email}> (đã có)")
    return token


def main() -> None:
    admin_email, admin_password = read_env_admin()
    parser = argparse.ArgumentParser(description="Tạo tài khoản demo cho NamBeDe qua API Gateway.")
    parser.add_argument("--api", default="http://localhost:8000/api", help="địa chỉ gateway (mặc định http://localhost:8000/api)")
    parser.add_argument("--admin-email", default=admin_email)
    parser.add_argument("--admin-password", default=admin_password)
    args = parser.parse_args()
    api = Gateway(args.api)

    print(f"NamBeDe: tạo tài khoản demo qua {api.base}")
    admin = login(api, args.admin_email, args.admin_password)
    if admin is None:
        sys.exit(f"Không đăng nhập được admin {args.admin_email}. Dùng --admin-email và --admin-password.")

    print("Tài khoản")
    for email, name in TEACHERS:
        ensure_lecturer(api, admin, email, name)
    for email, name in STUDENTS:
        ensure_student(api, email, name)

    print()
    print("Xong. Tài khoản demo (mở web rồi đăng nhập):")
    print(f"  Quản trị viên  {args.admin_email} / {args.admin_password}")
    for email, name in TEACHERS:
        print(f"  Giảng viên     {email} / {TEACHER_PASSWORD}  ({name})")
    for email, name in STUDENTS:
        print(f"  Sinh viên      {email} / {STUDENT_PASSWORD}  ({name})")


if __name__ == "__main__":
    main()
