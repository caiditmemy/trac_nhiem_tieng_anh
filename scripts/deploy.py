#!/usr/bin/env python3
"""Sets up NamBeDe (sprint 1) with one command on Windows, macOS and Linux.

    python3 scripts/deploy.py install    install what this machine is missing: Docker
    python3 scripts/deploy.py env        create .env from .env.example (the deploy does it too)
    python3 scripts/deploy.py status     is this machine ready to deploy? and what is running now
    python3 scripts/deploy.py            everything: the system in Docker and the demo accounts
    python3 scripts/deploy.py backend    only the system in Docker (PostgreSQL, Auth Service, gateway, web)
    python3 scripts/deploy.py seed       only the demo accounts
    python3 scripts/deploy.py stop       stop the containers, keep the data

On Windows type `py scripts\\deploy.py` instead of `python3 scripts/deploy.py`; on macOS and Linux `make deploy` works too.
Needs Docker (Docker Desktop on Windows and macOS) and Python 3.9 or newer; Java, Maven and Node run in containers.
Every deploy first checks the machine like `status` does and stops before changing anything when something is missing.
"""

from __future__ import annotations  # the type hints below also work on the Python 3.9 that ships with macOS

import argparse
import getpass
import json
import os
import platform
import re
import shlex
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path
from typing import NamedTuple, NoReturn

ROOT = Path(__file__).resolve().parent.parent
API = "http://localhost:8000/api"
WINDOWS = os.name == "nt"
EXE = ".exe" if WINDOWS else ""
SELF = r"py scripts\deploy.py" if WINDOWS else "python3 scripts/deploy.py"  # how to run this script, for the hints


def step(title: str) -> None:
    print(f"\n==> {title}", flush=True)


def note(text: str) -> None:
    print(f"    {text}", flush=True)


def fail(text: str) -> NoReturn:
    print(f"\n[!] {text}", file=sys.stderr, flush=True)
    sys.exit(1)


def run(cmd: list, cwd: Path = ROOT, env: dict | None = None, check: bool = True) -> int:
    """Runs a command in the foreground and shows it first; stops the script when it fails, unless check is off."""
    cmd = [str(part) for part in cmd]
    print("    $ " + " ".join(shlex.quote(part) for part in cmd), flush=True)
    try:
        code = subprocess.run(cmd, cwd=cwd, env=env).returncode
    except OSError as error:
        fail(f"Không chạy được {cmd[0]}: {error}")
    if code != 0 and check:
        fail(f"Lệnh trên dừng với mã {code}.")
    return code


def capture(cmd: list, timeout: float = 120, stderr: bool = False) -> str | None:
    """Output of a command, or None when the program is missing, fails or hangs."""
    try:
        result = subprocess.run([str(part) for part in cmd], cwd=ROOT, capture_output=True, encoding="utf-8",
                                errors="replace", timeout=timeout)
    except (OSError, subprocess.TimeoutExpired):
        return None
    if result.returncode != 0:
        return None
    return result.stderr if stderr else result.stdout


# ---------------------------------------------------------------- the system in Docker

def read_env() -> dict[str, str]:
    values = {}
    env_file = ROOT / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            key, sep, value = line.partition("=")
            if sep and not key.strip().startswith("#"):
                values[key.strip()] = re.split(r"\s+#", value.strip(), maxsplit=1)[0].strip().strip("'\"")
    return values


def ensure_env() -> dict[str, str]:
    """Creates .env from .env.example on the first run. No secret to make: auth-service makes its own signing key."""
    env_file = ROOT / ".env"
    if not env_file.exists():
        lines = (ROOT / ".env.example").read_text(encoding="utf-8").splitlines()
        with open(env_file, "w", encoding="utf-8", newline="\n") as out:  # LF on Windows too
            out.write("\n".join(lines) + "\n")
        note("Đã tạo .env từ .env.example.")
    return read_env()


def docker_ready() -> None:
    if shutil.which("docker") is None:
        fail(f"Chưa có Docker. Cài tự động: {install_hint()} (hoặc tự cài: https://docs.docker.com/get-docker/)")
    if capture(["docker", "info", "--format", "{{.ServerVersion}}"], timeout=60) is None:
        fail("Docker chưa chạy. Mở Docker Desktop rồi chạy lại. "
             "Trên Linux: sudo systemctl start docker, và thêm user vào nhóm docker để không cần sudo.")
    if capture(["docker", "compose", "version"]) is None:
        fail("Cần Docker Compose v2 (lệnh `docker compose`). Docker Desktop có sẵn; trên Linux cài gói docker-compose-plugin.")


def web_url(env: dict[str, str]) -> str:
    port = env.get("WEB_PORT", "80")
    return "http://nambede.dev.localhost" + ("" if port == "80" else f":{port}")


def backend() -> None:
    step("Hệ thống trong Docker: PostgreSQL, Auth Service, gateway, web")
    docker_ready()
    env = ensure_env()
    note("Lần đầu mất vài phút để tải image và build, các lần sau nhanh hơn nhiều.")
    if run(["docker", "compose", "up", "-d", "--build", "--wait"], check=False) != 0:
        run(["docker", "compose", "ps", "-a"], check=False)
        fail("Có service chưa chạy được. Xem log: docker compose logs <tên service>. "
             "Nếu cổng 80 hoặc 5432 đang bị chiếm, đổi WEB_PORT hoặc POSTGRES_PORT trong .env rồi chạy lại.")
    note(f"Web: {web_url(env)}   Swagger: http://localhost:8000/docs")


def gateway_up(timeout: float) -> bool:
    deadline = time.time() + timeout
    while True:
        try:
            with urllib.request.urlopen(API + "/health", timeout=5) as response:
                if response.status == 200:
                    return True
        except (urllib.error.URLError, OSError):
            pass
        if time.time() >= deadline:
            return False
        time.sleep(2)


def seed() -> None:
    step("Dữ liệu demo")
    if not gateway_up(timeout=60):
        fail(f"Gateway ở {API} chưa trả lời. Chạy hệ thống trước: {command_hint('backend')}")
    run([sys.executable, ROOT / "scripts" / "demo_data.py", "--api", API])


# ---------------------------------------------------------------- status: is this machine ready to deploy?

FIXED_PORTS = [8000, 8081]  # gateway, auth-service; WEB_PORT and POSTGRES_PORT come from .env
SERVICES = ["postgres", "auth-service", "gateway", "web"]


class Check(NamedTuple):
    ok: bool | None  # True ✓, None ! (a warning, the deploy copes), False ✗ (the deploy cannot run)
    text: str
    hint: str = ""


def install_hint() -> str:
    return r"scripts\install.cmd" if WINDOWS else command_hint("install")


def command_hint(action: str = "") -> str:
    """How to run an action on this machine: make <action> on macOS and Linux when make is there, else this script."""
    if not WINDOWS and shutil.which("make"):
        return f"make {action or 'deploy'}"
    return f"{SELF} {action}".strip()


def compose_containers() -> dict[str, dict]:
    """The containers of this compose project by service, running or not."""
    text = (capture(["docker", "compose", "ps", "-a", "--format", "json"], timeout=60) or "").strip()
    try:
        items = json.loads(text) if text.startswith("[") else [json.loads(line) for line in text.splitlines() if line.strip()]
    except ValueError:
        return {}
    return {item.get("Service", ""): item for item in items}


def port_in_use(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.settimeout(0.5)
        return sock.connect_ex(("127.0.0.1", port)) == 0


def readiness(backend: bool) -> list[Check]:
    """What a deploy needs, checked without changing anything."""
    checks = [Check(True, f"Python {platform.python_version()}")]
    docker = docker_binary()
    docker_running = False
    if backend:
        if docker is None:
            checks.append(Check(False, "Chưa có Docker", f"cài bằng {install_hint()}"))
        else:
            version = capture([docker, "info", "--format", "{{.ServerVersion}}"], timeout=60)
            docker_running = version is not None
            if not docker_running:
                checks.append(Check(False, "Docker chưa chạy", "mở Docker Desktop; Linux: sudo systemctl start docker, "
                                    "báo permission denied thì đăng nhập lại để nhận nhóm docker"))
            else:
                checks.append(Check(True, f"Docker {version.strip()} đang chạy"))
    if backend and docker_running:
        compose = capture([docker, "compose", "version", "--short"])
        checks.append(Check(True, f"Docker Compose {compose.strip()}") if compose
                      else Check(False, "Chưa có Docker Compose v2", f"cài bằng {install_hint()}"))
        memory = int((capture([docker, "info", "--format", "{{.MemTotal}}"]) or "0").strip() or 0) / 1024 ** 3
        if 0 < memory < 3.5:
            checks.append(Check(None, f"Docker chỉ có {memory:.1f} GB RAM", "nên từ 4 GB (Docker Desktop: Settings > Resources)"))
        env = read_env()
        if not (ROOT / ".env").exists():
            checks.append(Check(None, "Chưa có .env", "deploy sẽ tự tạo từ .env.example"))
        else:
            checks.append(Check(True, "Đã có .env"))
        ours = {int(publisher["PublishedPort"]) for container in compose_containers().values() if container.get("State") == "running"
                for publisher in container.get("Publishers") or [] if publisher.get("PublishedPort")}
        ports = [int(env.get("WEB_PORT", "80")), int(env.get("POSTGRES_PORT", "5432")), *FIXED_PORTS]
        taken = [port for port in ports if port not in ours and port_in_use(port)]
        if taken:
            checks.append(Check(False, f"Cổng {', '.join(map(str, taken))} đang bị chương trình khác dùng",
                                "tắt chương trình đó; riêng cổng web và PostgreSQL đổi được bằng WEB_PORT, POSTGRES_PORT trong .env"))
        else:
            checks.append(Check(True, "Các cổng " + ", ".join(map(str, ports)) + (": còn trống hoặc NamBeDe đang dùng" if ours else ": còn trống")))
    return checks


def show_checks(checks: list[Check], deploying: bool = False) -> bool:
    """Prints the checklist; True when nothing blocks the deploy."""
    for check in checks:
        mark = {True: "✓", None: "!", False: "✗"}[check.ok]
        note(f"{mark} {check.text}" + (f": {check.hint}" if check.hint else ""))
    blocking = [check for check in checks if check.ok is False]
    print(flush=True)
    if blocking:
        note(f"=> CHƯA đủ để deploy: {len(blocking)} mục ✗, xử lý theo gợi ý ở từng dòng rồi chạy lại.")
    else:
        note("=> Đủ, bắt đầu deploy." if deploying else f"=> Đủ để deploy: {command_hint()}")
    return not blocking


def preflight() -> None:
    """Stops before changing anything when the machine is not ready."""
    step("Kiểm tra môi trường trước khi deploy")
    if not show_checks(readiness(backend=True), deploying=True):
        fail("Chưa đủ để deploy nên chưa làm gì cả. Xử lý các mục ✗ ở trên rồi chạy lại.")


def data_summary() -> str:
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import demo_data  # the demo data script's small gateway client

    api = demo_data.Gateway(API)
    email, password = demo_data.read_env_admin()
    token = demo_data.login(api, email, password)
    if token is None:
        return f"Không đăng nhập được admin {email} để đếm dữ liệu."

    users = api.call("GET", "/users", token)[1] or []
    roles = {role: sum(user["role"] == role for user in users) for role in ("ADMIN", "TEACHER", "STUDENT")}
    text = f"{len(users)} tài khoản ({roles['ADMIN']} quản trị, {roles['TEACHER']} giảng viên, {roles['STUDENT']} sinh viên)"
    if not roles["STUDENT"]:
        text += ". Chưa có tài khoản demo: make seed"
    return text


def running_state() -> None:
    step("Hệ thống đang chạy")
    docker = docker_binary()
    if docker is None or capture([docker, "info", "--format", "{{.ServerVersion}}"], timeout=60) is None:
        note("Docker chưa chạy nên chưa có gì.")
    else:
        containers = compose_containers()
        for service in SERVICES:
            container = containers.get(service)
            if container is None:
                note(f"✗ {service:<17} chưa tạo")
                continue
            ok = container.get("State") == "running" and container.get("Health") in ("healthy", "", None)
            ports = sorted({int(p["PublishedPort"]) for p in container.get("Publishers") or [] if p.get("PublishedPort")})
            note(f"{'✓' if ok else '✗'} {service:<17} {container.get('Status', container.get('State'))}"
                 + (f", cổng {', '.join(map(str, ports))}" if ports else ""))
        if gateway_up(timeout=0):
            note(f"✓ Gateway trả lời ở {API}; web: {web_url(read_env())}")
            note("Dữ liệu: " + data_summary())
        else:
            note(f"✗ Gateway chưa trả lời ở {API}: {command_hint()}")


def status(args: argparse.Namespace) -> None:
    step("Môi trường: đủ để deploy chưa?")
    ready = show_checks(readiness(backend=True))
    running_state()
    if not ready:
        sys.exit(1)


# ---------------------------------------------------------------- install: the tools this machine is missing

HOMEBREW_INSTALL = '/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"'
GET_DOCKER = "https://get.docker.com"  # Docker's official script: Docker Engine with the compose plugin
LINUX_INSTALL = {
    "apt-get": ["apt-get", "install", "-y"],
    "dnf": ["dnf", "install", "-y"],
    "pacman": ["pacman", "-S", "--noconfirm", "--needed"],
    "zypper": ["zypper", "--non-interactive", "install"],
}
LINUX_DOCKER = {"apt-get": ["docker.io", "docker-compose-v2"], "dnf": ["moby-engine", "docker-compose"],
                "pacman": ["docker", "docker-compose"], "zypper": ["docker", "docker-compose"]}


def docker_binary() -> str | None:
    """The docker command, also right after Docker Desktop is installed, before PATH is refreshed."""
    candidates = [shutil.which("docker"), "/usr/local/bin/docker", "/opt/homebrew/bin/docker",
                  "/Applications/Docker.app/Contents/Resources/bin/docker"]
    if WINDOWS:
        candidates.append(str(Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Docker" / "Docker" / "resources"
                              / "bin" / "docker.exe"))
    return next((candidate for candidate in candidates if candidate and Path(candidate).is_file()), None)


class Installer:
    """Installs what this machine is missing with its own package manager: Homebrew, winget, apt-get, dnf, pacman or zypper."""

    def __init__(self, dry_run: bool) -> None:
        self.dry_run = dry_run
        self.todo: list[str] = []  # what is left for the user to do by hand
        self.installed = False
        self.system = "windows" if WINDOWS else "macos" if sys.platform == "darwin" else "linux"
        machine = platform.machine().lower()
        self.arch = {"amd64": "x86_64", "aarch64": "arm64"}.get(machine, machine)
        self.sudo = ["sudo"] if self.system == "linux" and os.geteuid() != 0 else []
        self.kind, self.manager = self.find_manager()
        self.apt_updated = False

    def find_manager(self) -> tuple[str | None, str | None]:
        if self.system == "macos":
            brew = shutil.which("brew") or next((p for p in ("/opt/homebrew/bin/brew", "/usr/local/bin/brew") if Path(p).exists()), None)
            return ("brew", brew) if brew else (None, None)
        if self.system == "windows":
            winget = shutil.which("winget")
            return ("winget", winget) if winget else (None, None)
        name = next((name for name in LINUX_INSTALL if shutil.which(name)), None)
        return name, name

    def run(self, cmd: list, sudo: bool = False) -> bool:
        cmd = (self.sudo if sudo else []) + [str(part) for part in cmd]
        if self.dry_run:
            note("(chạy thử, không cài) $ " + " ".join(shlex.quote(part) for part in cmd))
            return True
        return run(cmd, check=False) == 0

    def package(self, name: str, brew: str = "", cask: str = "", winget: str = "", linux: list | None = None) -> bool:
        """Installs one package with the machine's package manager."""
        if self.manager is None:
            self.bootstrap()
        note(f"Cài {name}")
        if self.kind == "brew":
            ok = self.run([self.manager, "install", "--cask", cask] if cask else [self.manager, "install", brew])
        elif self.kind == "winget":
            ok = self.run([self.manager, "install", "-e", "--id", winget, "--accept-source-agreements",
                           "--accept-package-agreements"])
        else:
            if self.kind == "apt-get" and not self.apt_updated:
                self.run(["apt-get", "update"], sudo=True)
                self.apt_updated = True
            ok = self.run(LINUX_INSTALL[self.kind] + (linux or []), sudo=True)
        self.installed = self.installed or ok
        return ok

    def bootstrap(self) -> None:
        """Homebrew on a Mac that has none; Windows and Linux come with their package manager."""
        if self.system == "macos":
            note("Chưa có Homebrew: cài bằng script chính thức của Homebrew (sẽ hỏi mật khẩu của máy)")
            self.run(["/bin/bash", "-c", HOMEBREW_INSTALL])
            self.kind, self.manager = self.find_manager()
            if self.manager is None and self.dry_run:
                self.kind, self.manager = "brew", "brew"
        if self.manager is None:
            fail({"macos": "Cài Homebrew không xong, xem https://brew.sh rồi chạy lại.",
                  "windows": "Máy chưa có winget: cài App Installer từ Microsoft Store rồi chạy lại.",
                  "linux": "Không nhận ra trình cài gói (apt-get, dnf, pacman, zypper) của bản Linux này."}[self.system])

    def download(self, url: str, name: str) -> Path:
        path = Path(tempfile.gettempdir()) / name
        if not self.dry_run:
            with urllib.request.urlopen(url, timeout=120) as response, open(path, "wb") as out:
                shutil.copyfileobj(response, out)
        return path

    def join_group(self, group: str, reason: str) -> bool:
        """Adds the user to a Linux group (docker, kvm); it takes effect at the next login."""
        import grp  # Unix only

        try:
            gid = grp.getgrnam(group).gr_gid
        except KeyError:
            return False
        if gid in os.getgroups():
            return False
        if self.run(["usermod", "-aG", group, getpass.getuser()], sudo=True):
            self.todo.append(f"Đăng xuất rồi đăng nhập lại để {reason} (vừa thêm bạn vào nhóm {group})")
            return True
        return False

    def docker_up(self, docker: str) -> bool:
        return capture(self.sudo + [docker, "info", "--format", "{{.ServerVersion}}"], timeout=60) is not None

    def docker(self) -> None:
        step("Docker")
        docker = docker_binary()
        if docker:
            note(f"Đã có: {docker}")
        elif self.system == "macos":
            self.package("Docker Desktop", cask="docker-desktop")
        elif self.system == "windows":
            self.package("Docker Desktop", winget="Docker.DockerDesktop")
        elif self.kind in ("apt-get", "dnf"):
            note("Cài Docker Engine bằng script chính thức của Docker")
            if not self.run(["sh", self.download(GET_DOCKER, "get-docker.sh")], sudo=True):
                self.package("Docker Engine", linux=LINUX_DOCKER[self.kind])
        else:
            self.package("Docker Engine", linux=LINUX_DOCKER.get(self.kind, []))
        docker = docker_binary()
        if self.dry_run:
            return
        if docker is None:
            self.todo.append("Cài Docker không xong: cài tay theo https://docs.docker.com/get-docker/")
            return
        if self.system == "linux" and self.sudo:
            self.join_group("docker", "dùng lệnh docker không cần sudo")
        if not self.docker_up(docker) and not self.start_docker(docker):
            self.todo.append("Khởi động Docker: sudo systemctl start docker" if self.system == "linux"
                             else "Mở Docker Desktop (lần đầu bấm đồng ý điều khoản), đợi nó báo đang chạy")
        if self.system == "linux" and capture(self.sudo + [docker, "compose", "version"]) is None:
            self.package("Docker Compose", linux=[{"apt-get": "docker-compose-v2"}.get(self.kind, "docker-compose")])

    def start_docker(self, docker: str) -> bool:
        if self.system == "macos":
            run(["open", "-a", "Docker"], check=False)
        elif self.system == "windows":
            desktop = Path(os.environ.get("ProgramFiles", r"C:\Program Files")) / "Docker" / "Docker" / "Docker Desktop.exe"
            if desktop.exists():
                subprocess.Popen([str(desktop)], creationflags=subprocess.DETACHED_PROCESS | subprocess.CREATE_NEW_PROCESS_GROUP)
        elif not self.run(["systemctl", "enable", "--now", "docker"], sudo=True):
            return False
        note("Đợi Docker chạy (lần đầu Docker Desktop hỏi đồng ý điều khoản: hãy bấm đồng ý)")
        deadline = time.time() + 300
        while time.time() < deadline:
            if self.docker_up(docker):
                return True
            time.sleep(5)
        return False

    def make(self) -> None:
        if self.system == "windows" or shutil.which("make"):
            return
        step("make")
        if self.system == "macos":
            self.todo.append("Cài Command Line Tools để có lệnh make: xcode-select --install")
        else:
            self.package("make", linux=["make"])

    def report(self) -> None:
        step("Kết quả" + (" (chạy thử, chưa cài gì)" if self.dry_run else ""))
        docker = docker_binary()
        rows = [
            ("Docker", bool(docker) and self.docker_up(docker)),
            ("Docker Compose", bool(docker) and capture(self.sudo + [docker, "compose", "version"]) is not None),
            (f"Python {platform.python_version()}", True),
        ]
        if self.system != "windows":
            rows.append(("make", shutil.which("make") is not None))
        for name, ok in rows:
            note(("✓ " if ok else "✗ ") + name)
        if self.system == "windows" and self.installed:
            self.todo.append("Mở cửa sổ dòng lệnh mới để Windows nhận các chương trình vừa cài")
        if self.todo:
            print("\n    Việc còn lại cần làm tay:", flush=True)
            for item in self.todo:
                note(f"- {item}")
        print(flush=True)
        note(f"Tiếp theo: {'make deploy' if shutil.which('make') else SELF}")


def install(args: argparse.Namespace) -> None:
    installer = Installer(args.dry_run)
    step(f"Cài môi trường cho máy này: {installer.system} {installer.arch}" + (" (chạy thử, không cài gì)" if args.dry_run else ""))
    note(f"Trình cài gói: {installer.manager or 'chưa có'}; Python {platform.python_version()}")
    installer.docker()
    installer.make()
    installer.report()


def env_file() -> None:
    step("File .env")
    existed = (ROOT / ".env").exists()
    env = ensure_env()
    if existed:
        note("Đã có .env, giữ nguyên.")
    note(f"Web: {web_url(env)}; admin: {env.get('ADMIN_EMAIL', 'admin@nambede.local')} / {env.get('ADMIN_PASSWORD', 'Admin@123')}")
    note("Sửa .env trước lần deploy đầu tiên nếu muốn đổi cổng hay mật khẩu admin.")


# ---------------------------------------------------------------- entry point

def summary(env: dict[str, str], seeded: bool) -> None:
    step("Xong")
    note(f"Web:      {web_url(env)}  (trình duyệt không mở được thì dùng http://localhost)")
    note("Swagger:  http://localhost:8000/docs")
    note(f"Admin:    {env.get('ADMIN_EMAIL', 'admin@nambede.local')} / {env.get('ADMIN_PASSWORD', 'Admin@123')}")
    if seeded:
        note("Demo:     teacher1@nambede.local / Teacher@123, student1@nambede.local / Student@123")
    note(f"Dừng:     {command_hint('stop')}   (dữ liệu vẫn giữ)")


def main() -> None:
    if sys.version_info < (3, 9):
        fail("Cần Python 3.9 trở lên.")
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(errors="replace")  # Vietnamese text on a console that cannot show it
    parser = argparse.ArgumentParser(description="Dựng NamBeDe bằng một lệnh trên Windows, macOS, Linux.")
    parser.add_argument("action", nargs="?", default="all",
                        choices=["all", "status", "install", "env", "backend", "seed", "stop"],
                        help="mặc định all: hệ thống và dữ liệu demo; install: cài các công cụ máy còn thiếu")
    parser.add_argument("--no-seed", action="store_true", help="không tạo dữ liệu demo")
    parser.add_argument("--dry-run", action="store_true", help="với install: chỉ in ra sẽ cài gì")
    args = parser.parse_args()

    if args.action == "status":
        status(args)
    elif args.action == "install":
        install(args)
    elif args.action == "env":
        env_file()
    elif args.action == "stop":
        step("Dừng các container, giữ dữ liệu")
        docker_ready()
        run(["docker", "compose", "stop"])
    elif args.action == "backend":
        preflight()
        backend()
    elif args.action == "seed":
        seed()
    else:
        preflight()
        backend()
        if not args.no_seed:
            seed()
        summary(read_env(), not args.no_seed)


if __name__ == "__main__":
    main()
