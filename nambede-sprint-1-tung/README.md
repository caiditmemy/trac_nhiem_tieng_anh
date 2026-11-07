# Sprint 1 — Tùng

Nhánh `sprint-1-tung`, tách từ `f24921d`; phân công gốc ở `7b91136`, `3a2831c`.

**Sở hữu:** Form đăng nhập/đăng ký, hồ sơ đổi tên/mật khẩu, khung Vite/React và UI giáo viên: tổng quan tài khoản thật, liên kết hồ sơ, điều hướng lớp học/bài tập.

**Phụ thuộc chung giữ để chạy độc lập:** backend Auth/Gateway/PostgreSQL của Nam; login/register/profile và khung web của Tùng; session/API/guard của Tấn. Đây là code thật, không dùng mock khi chạy. Bản sao phụ thuộc không chuyển quyền sở hữu. Test đơn vị có giả lập HTTP; `scripts/smoke.py` gọi toàn bộ hệ thống thật.

Chỉ có UI `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises`. Không giữ trang/route của vai trò khác. Đăng nhập vai trò khác đến `/unavailable`, có nút đăng xuất; đăng ký luôn tạo STUDENT, nên dùng tài khoản demo đúng vai trò để xem UI nhánh này. Backend vẫn quản lý ba vai trò để cấp tài khoản và kiểm tra quyền. Lớp/bài tập hiển thị rõ Sprint 2; chưa có API học tập hay số liệu giả.

## Build và test

Cần Linux (native launcher dùng `/proc`), JDK 17+, Maven wrapper, Node 24+, Python 3.12+, uv, PostgreSQL 17 (server + `psql`). Maven 3.9.9 cài sẵn cũng dùng được. Không cần clone hoặc service của nhánh khác.

```bash
cd /workspace/nambede-sprint1-tung
# Nếu dùng bộ công cụ có sẵn trong workspace này:
export JAVA_HOME=/home/box/opt/jdk
export PATH="$JAVA_HOME/bin:$PATH"
export PG_BIN=/workspace/pg17/bin
export UV_CACHE_DIR=/tmp/sprint-split-uv
make build
make test
```

Lệnh tương đương, từ thư mục gốc:

```bash
(cd services/auth-service && ./mvnw -B -ntp verify)
(cd services/gateway && uv run --locked pytest -q)
(cd web && npm ci && npm run build && npm test && npm run lint)
python3 -m unittest discover -s scripts -p 'test_*.py'
```

Mockito được nạp bằng `-javaagent` khi chạy test, không cần self-attach của JDK. Máy khác: đặt `JAVA_HOME` và `PG_BIN` theo vị trí cài đặt, không dùng đường dẫn workspace phía trên. Dependency khóa trong `package-lock.json`, `uv.lock`; Maven giữ cấu hình nguồn.

## Chạy native, demo, smoke

```bash
python3 scripts/native.py up
python3 scripts/smoke.py --demo
python3 scripts/smoke.py
# Khi xong: chỉ dừng tiến trình do launcher nhánh này tạo
python3 scripts/native.py down
```

Mở http://127.0.0.1:5173/teacher. Tài khoản local demo:

- ADMIN: `admin@tung.local` / `LocalAdmin@123`
- TEACHER: `teacher@tung.local` / `DemoUser@123`
- STUDENT: `student@tung.local` / `DemoUser@123`

`--demo` tạo giáo viên/sinh viên qua API admin thật và giữ dữ liệu để demo; chạy lại không tạo trùng. `smoke.py` tạo tài khoản ngẫu nhiên rồi xóa trong `finally`: đăng ký/login, RS256/JWKS, không token 401, cấm role 403, email trùng 409, giới hạn đăng nhập 429/Retry-After, đổi tên/mật khẩu, giữ phiên sau đổi mật khẩu, đổi gói, khóa và vô hiệu phiên/đổi vai trò (chờ tối đa 22 giây). Chạy smoke sau `up`, không phải sau build đơn thuần.

| Tài nguyên | Nhánh này |
|---|---|
| Web | `127.0.0.1:5173` |
| Gateway | `127.0.0.1:8203` |
| Auth | `127.0.0.1:8103` |
| PostgreSQL | `127.0.0.1:55433` |
| Database / user | `auth_tung` / `sprint_tung` |
| Compose project | `nambede-sprint1-tung` |
| Native data / logs / PID records | `.runtime/pgdata`, `.runtime/*.log`, `.runtime/*.json` trong worktree này |
| Browser token key | `nambede.tung.token` (origin riêng theo port) |

Ba nhánh dùng bộ port lần lượt Nam `5171/8201/8101/55431`, Tấn `5172/8202/8102/55432`, Tùng `5173/8203/8103/55433`; có thể chạy đồng thời. Launcher kiểm tra cả bốn port, từ chối nếu bận; không dừng listener có sẵn. Dữ liệu không bị xóa khi `down`; PID chỉ được gửi tín hiệu khi thời điểm tạo `/proc` trùng bản ghi. Native PostgreSQL chỉ bind loopback, dùng trust cho cluster demo riêng; không dùng cấu hình này để public hoặc production. Không đọc `.env` hay credential có sẵn để khởi chạy native.

## Docker (tùy chọn)

```bash
docker compose up -d --build --wait
python3 scripts/smoke.py --demo
python3 scripts/smoke.py
docker compose down
```

Compose dùng cùng cổng nhánh, volume `nambede-sprint1-tung_pgdata` riêng và mật khẩu local demo. Không chạy Docker và native của cùng nhánh đồng thời. Không dùng `down -v` nếu muốn giữ dữ liệu. CI tích hợp chạy smoke trong mạng Compose bằng DNS service nội bộ.

## Bằng chứng kiểm tra trong phiên tách nhánh

Spring `verify` đã pass **44 test mỗi nhánh (132 tổng cộng)**; bằng chứng web build/test được giữ nguyên: 71 test frontend tổng cộng trên ba nhánh, cùng 6 test launcher tổng cộng. Test UI tích hợp dùng jsdom.

Ngày 2026-11-07, shell được cho phép bên ngoài sandbox đã chạy thành công native startup, demo seed, HTTP smoke thật và toàn bộ gateway pytest trên cả ba nhánh. Mỗi gateway **23 passed**, kèm 1 `StarletteDeprecationWarning` về `httpx` trong TestClient (69 test gateway tổng cộng, đã bao gồm test JWKS). Ba stack đang chạy đồng thời tại web port 5171/5172/5173 theo kết quả xác minh này; giới hạn socket của sandbox trước đó không còn là trạng thái xác minh runtime.

Lệnh chính xác đã chạy từ thư mục gốc của từng worktree (ghi lại bằng chứng, không cần khởi động lại stack đang chạy):

```bash
export JAVA_HOME=/home/box/opt/jdk PG_BIN=/workspace/pg17/bin UV_CACHE_DIR=/tmp/sprint-split-uv; python3 scripts/native.py up && python3 scripts/smoke.py --demo && python3 scripts/smoke.py && (cd services/gateway && uv run --locked pytest -q)
```

HTTP smoke PASS qua web proxy → gateway → Spring → PostgreSQL: RS256/JWKS, đăng ký/đăng nhập, 401/403/409/429 và Retry-After, hồ sơ/mật khẩu, admin CRUD, VIP, khóa/vô hiệu phiên và đổi vai trò. Log nhánh này: `/home/box/.cursor/projects/workspace/terminals/314473.txt` (exit code 0). Theo review tĩnh độc lập, review tĩnh độc lập của Claude không phát hiện vấn đề tác động cao.

**Chưa có browser E2E trên trình duyệt thật; Docker chưa được kiểm thử.** HTTP smoke và test jsdom không thay thế browser E2E. Báo cáo tổng hợp, lệnh và bằng chứng của cả ba nhánh ở `/workspace/sprint-split-result.txt`.
