# NamBeDe — Sprint 1 — Nam (Thành viên 1)

Nhánh này chỉ chứa phần việc Tuần 6 + Tuần 7 của Nam và các phụ thuộc chung cần để chạy độc lập. Bản gộp ứng dụng của cả ba thành viên nằm ở nhánh `sprint-1`.

Kiến trúc: React → FastAPI Gateway → Auth Service và Class Service (Spring Boot) → PostgreSQL (mỗi service một database).

## Phần việc

**Tuần 6**: Auth Service và Gateway ổn định; Class Service cho giảng viên tạo lớp với mã mời 6 ký tự, sinh viên vào lớp bằng mã.

| Phần | Đường dẫn |
| --- | --- |
| Auth Service (đăng nhập, đăng ký, RS256/JWKS, phiên, quản lý người dùng) | `services/auth-service` |
| Gateway (chỉ chuyển tiếp `/api/auth`, `/api/users`, `/api/classes`) | `services/gateway` |
| Class Service: tạo lớp, mã mời `ClassCodeGenerator`, `joinClass` | `services/class-service` (`classes/ClassService.java`, `classes/ClassCodeGenerator.java`, `classes/ClassController.java`) |
| Web: danh sách lớp, nhập mã vào lớp | `web/src/pages/ClassesPage.tsx` |
| Web: chi tiết lớp, mã lớp, danh sách sinh viên | `web/src/pages/ClassDetailPage.tsx`, `web/src/components/common/ClassCode.tsx` |
| Web: tạo / sửa lớp | `web/src/components/ClassFormDialog.tsx` |

**Tuần 7**: unit test, docker compose, smoke test, báo cáo.

| Phần | Đường dẫn |
| --- | --- |
| `ClassServiceTest`, `ClassCodeGeneratorTest`, `ClassControllerTest` | `services/class-service/src/test` |
| Gateway test | `services/gateway/tests/test_gateway.py`, `test_jwks.py` (và `test_docs.py`) |
| Web test | `web/src/components/AuthProvider.test.tsx`, `web/src/pages/ClassesPage.test.tsx`, `web/src/components/layout/navigation.test.ts` |
| Docker Compose với `auth_db`, `class_db` | `docker-compose.yml`, `infra/postgres/init` |
| Smoke, demo, báo cáo | `scripts/smoke.py`, `scripts/demo.py`, `scripts/report.py` |

## Phụ thuộc chung

Giữ lại để nhánh chạy một mình; Auth Service và Gateway do Nam phụ trách:

- `services/auth-service`, `services/gateway`.
- Web shell: đăng nhập, đăng ký, hồ sơ, quản lý người dùng của admin, bố cục (`components/layout`), api client (`lib/api.ts`), bảo vệ route theo vai trò (`AuthProvider.tsx`), ba trang tổng quan (admin, giảng viên, sinh viên) chỉ hiển thị dữ liệu lớp.

## Cổng và tài khoản demo

| Thành phần | Cổng | Database |
| --- | --- | --- |
| PostgreSQL (cluster riêng) | 55431 | `auth_db`, `class_db` |
| Auth Service | 8101 | `auth_db` |
| Class Service | 8111 | `class_db` |
| Gateway | 8201 | |
| Web | 5171 | |

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | admin@sprint1-nam.local | LocalAdmin@123 |
| TEACHER | teacher@sprint1-nam.local | DemoUser@123 |
| STUDENT | student@sprint1-nam.local | DemoUser@123 |

Cấu hình native duy nhất: `scripts/app.json` (owner `sprint1-nam`). Mọi cổng chỉ nghe trên 127.0.0.1. Token đăng nhập của web lưu ở khóa `nambede.sprint1.nam.token`.

## Chạy native (Linux)

Cần JDK 17+, Node 24+, Python 3.12+, uv, PostgreSQL 17 và `make`.

```bash
export JAVA_HOME=/home/box/opt/jdk
export PG_BIN=/workspace/pg17/bin
make build     # gateway (uv build), auth + class (mvnw verify), web
make test      # launcher, pytest gateway, Vitest, lint (Spring: make test-java)
make up        # postgres → auth → class → gateway → web
make demo      # tài khoản demo, một lớp demo có mã mời, sinh viên demo đã vào lớp
make smoke     # smoke test auth + luồng lớp học
make report    # reports/: JaCoCo, pytest-cov, Vitest, README.md, sprint1-reports.tar.gz
make down      # dừng đúng các tiến trình đã khởi động, giữ dữ liệu
```

Mở http://127.0.0.1:5171. Launcher từ chối chạy nếu bất kỳ cổng nào đang bận, chỉ dừng PID có thời điểm tạo khớp bản ghi trong `.runtime`.

## Chạy bằng Docker (khuyên dùng trên Mac/Windows)

Chỉ cần **Docker Desktop** (Mac Intel hoặc Apple Silicon, Windows) hoặc Docker Engine + compose plugin (Linux). Không cần cài Java, Node, Python hay PostgreSQL. Nên để Docker Desktop dùng ít nhất 4 GB RAM (Settings → Resources).

```bash
docker compose up -d --build --wait   # lần đầu build mất vài phút (tải Maven, npm, uv)
docker compose wait demo-seed         # đợi tạo xong tài khoản và dữ liệu demo
```

Có `make` thì gõ gọn `make docker-up`. Xong mở **http://127.0.0.1:5171** (hoặc http://localhost:5171) và đăng nhập bằng tài khoản ở bảng trên (admin, giáo viên, học sinh đều dùng được ngay).

- Service `demo-seed` chạy `scripts/demo.py` một lần sau mỗi lần `up` rồi tự thoát, tạo tài khoản giáo viên, học sinh và dữ liệu demo. Chạy lại nhiều lần không tạo trùng.
- Khoá JWT RS256 do auth-service tự sinh ở lần chạy đầu và lưu trong `auth_db`, không cần tạo hay khai báo khoá. Mật khẩu trong `docker-compose.yml` chỉ dành cho demo local, mọi cổng chỉ mở trên 127.0.0.1.
- Cùng cổng với bản native, nên chỉ chạy một trong hai. Các nhánh sprint dùng cổng khác nhau nên có thể chạy song song.

| Việc | Lệnh | Có `make` |
| --- | --- | --- |
| Smoke test | `docker compose run --rm --no-deps demo-seed python smoke.py --api http://web/api --auth http://auth-service:8081` | `make docker-smoke` |
| Xem trạng thái | `docker compose ps -a` | |
| Xem log | `docker compose logs -f auth-service` (bỏ tên service để xem tất cả) | `make docker-logs` |
| Dừng, giữ dữ liệu | `docker compose down` | `make docker-down` |
| Dừng và xoá dữ liệu | `docker compose down -v` | `make docker-reset` |

Build bỏ qua unit test Spring cho nhanh: `SKIP_TESTS=true docker compose up -d --build --wait`. Nếu báo cổng đang bận, tắt chương trình đang dùng cổng đó (ví dụ bản native: `make down`).

## Smoke test (`scripts/smoke.py`)

- Auth: RS256/JWKS, đăng ký, đăng nhập, 401/403/409/429 kèm `Retry-After`, hồ sơ của ba vai trò, đổi mật khẩu, admin tạo/xóa người dùng, gói VIP, khóa tài khoản và thu hồi phiên, đổi vai trò.
- Lớp học: giảng viên tạo lớp → nhận mã 6 ký tự; sinh viên không tạo được lớp (403); mã sai trả 404, mã rỗng trả 400, giảng viên không vào lớp bằng mã (403); sinh viên nhập mã bằng chữ thường vẫn vào được, vào lần hai bị 409; giảng viên thấy sinh viên trong lớp; sinh viên và người ngoài lớp không xem được danh sách; sinh viên rời lớp thì danh sách trống.

## Kết quả kiểm thử

| Thành phần | Công cụ | Số test | Lỗi | Độ phủ |
| --- | --- | ---: | ---: | --- |
| auth-service | JUnit 5 + Mockito / MockMvc | 44 | 0 | 75,3 % (JaCoCo, dòng) |
| class-service | JUnit 5 + Mockito / MockMvc | 40 | 0 | 65,7 % (JaCoCo, dòng) |
| gateway | pytest | 40 | 0 | 94,1 % (pytest-cov, dòng) |
| web | Vitest + Testing Library | 55 | 0 | |
| launcher | unittest | 6 | 0 | |

Trong class-service: `ClassCodeGeneratorTest` 20, `ClassControllerTest` 6, `ClassServiceTest` 7. Web: `npm run lint` và `npm run build` đạt. Runtime: `native.py up`, `demo.py`, `smoke.py` đều đạt. `make report` ghi vào `reports/` (trong git chỉ giữ `reports/README.md`, `reports/sprint1-reports.tar.gz` và `reports/screenshots/`).

## Ảnh demo

`reports/screenshots/`, chụp từ web đang chạy native:

- `admin-0-admin.png`, `admin-1-admin_users.png`: tổng quan admin, quản lý người dùng.
- `teacher-0-teacher.png`, `teacher-1-teacher_classes.png`, `teacher-2-class-detail.png`: tổng quan giảng viên, danh sách lớp, chi tiết lớp có mã mời và sinh viên.
- `student-0-student.png`, `student-1-student_classes.png`: tổng quan sinh viên, danh sách lớp và nhập mã.

## Giới hạn

- Chỉ có lớp học và tài khoản; ngân hàng câu hỏi, bài tập, nộp bài, điểm và streak không thuộc nhánh này.
- Bản Docker đã chạy thật trên Linux amd64 (Docker Engine 29, Compose v5): đủ service healthy, `demo-seed` và smoke đều đạt. Image dùng base đa kiến trúc nên build được cho arm64 (Apple Silicon), nhưng chưa chạy thử trên máy Mac thật.
- Native dùng PostgreSQL trust trên loopback, chỉ cho demo local.
- Class Service chưa có thông báo khi sinh viên vào hoặc rời lớp.
