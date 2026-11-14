# NamBeDe — Sprint 1 — Tùng (Thành viên 2)

Nhánh `sprint-1-tung` chỉ chứa phần việc Tuần 6 + Tuần 7 của Tùng cùng các phụ thuộc chung cần để chạy một mình. Bản ghép đủ ba thành viên nằm ở nhánh `sprint-1`.

Kiến trúc: React → FastAPI Gateway → Auth / Class / Exercise (Spring Boot) và Learning (FastAPI) → PostgreSQL (mỗi service một database). Exercise Service gọi Class Service (lớp của giảng viên) và Learning Service (đọc câu hỏi để sao chép) bằng REST.

## Phần việc của Tùng

| | Nội dung | Vị trí |
| --- | --- | --- |
| **Tuần 6** | Learning Service: chủ đề, từ vựng, CRUD câu hỏi trắc nghiệm | `services/learning-service/src/learning_service/routers/` (`topics.py`, `questions.py`, `vocabularies.py`), `models.py`, `schemas.py`, `alembic/versions/` |
| | Nhập câu hỏi từ Excel / CSV / JSON, file mẫu, xem trước từng dòng | `services/learning-service/src/learning_service/question_import.py` |
| | Exercise Service: tạo và giao bài (sao chép câu hỏi từ ngân hàng), danh sách, chi tiết, xóa | `services/exercise-service/.../web/ExerciseController.java`, `service/ExerciseService.java`, `clients/LearningClient.java`, `clients/ClassClient.java` |
| | Web: ngân hàng câu hỏi, nhập câu hỏi, giao bài, danh sách và chi tiết bài tập (giảng viên) | `web/src/pages/QuestionBankPage.tsx`, `web/src/components/QuestionImportDialog.tsx`, `QuestionFormDialog.tsx`, `TopicFormDialog.tsx`, `web/src/pages/AssignExercisePage.tsx`, `ExercisesPage.tsx`, `ExerciseDetailPage.tsx` |
| **Tuần 7** | Test ngân hàng câu hỏi và nhập file (file hỏng, thiếu cột với xlsx, csv và json) | `services/learning-service/tests/test_question_bank.py`, `tests/test_import.py`, `tests/test_vocabulary.py` |
| | Test Exercise Service | `ExerciseServiceTest`, `ExerciseControllerTest` trong `services/exercise-service/src/test` |
| | Test web của phần này | `web/src/components/QuestionImportDialog.test.tsx`, `web/src/pages/ExercisesPage.test.tsx` |
| | Dữ liệu mẫu: câu hỏi và từ vựng | `scripts/seed_data.json`, `scripts/demo.py` (`make demo`) |
| | Smoke test, báo cáo | `scripts/smoke.py`, `scripts/report.py`, `reports/` |

### Phụ thuộc chung (giữ lại để nhánh chạy một mình)

Các phần dưới đây do **Nam** phụ trách, không phải việc của Tùng:

- Auth Service và Gateway (`services/auth-service`, `services/gateway`): đăng nhập, đăng ký, JWT RS256/JWKS, người dùng, định tuyến `/api/...`.
- Class Service (`services/class-service`) và các trang lớp học (`web/src/pages/ClassesPage.tsx`, `ClassDetailPage.tsx`, `web/src/components/ClassFormDialog.tsx`): bài tập luôn giao cho một lớp, nên giảng viên cần tạo được lớp và sinh viên cần vào được lớp bằng mã.
- Web shell: đăng nhập, đăng ký, hồ sơ, quản trị người dùng, layout, client API, phân quyền theo vai trò.

## Cổng và tài khoản

| Thành phần | Cổng | Database |
| --- | --- | --- |
| PostgreSQL (cluster riêng) | 55433 | `auth_db`, `class_db`, `exercise_db`, `learning_db` |
| Auth Service | 8103 | `auth_db` |
| Class Service | 8113 | `class_db` |
| Exercise Service | 8123 | `exercise_db` |
| Learning Service | 8133 | `learning_db` |
| Gateway | 8203 | |
| Web | 5173 | |

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | admin@sprint1.local | LocalAdmin@123 |
| TEACHER | teacher@sprint1-tung.local | DemoUser@123 |
| STUDENT | student@sprint1-tung.local | DemoUser@123 |

Tài khoản giảng viên và sinh viên do `make demo` tạo. Cấu hình native duy nhất: `scripts/app.json`. Mọi cổng chỉ nghe trên 127.0.0.1. Token đăng nhập của web lưu ở khóa `nambede.sprint1.tung.token`.

## Chạy native (Linux)

Cần JDK 17+, Node 24+, Python 3.12+, uv, PostgreSQL 17 và `make`.

```bash
export JAVA_HOME=/home/box/opt/jdk
export PG_BIN=/workspace/pg17/bin
make build     # gateway, learning (uv build), 3 service Spring (mvnw verify), web
make test      # launcher, pytest gateway + learning, Vitest, lint (Spring: make test-java)
make up        # postgres → auth → class/learning → exercise → gateway → web
make demo      # tài khoản, ngân hàng câu hỏi (nhập từ JSON) + từ vựng, lớp demo có mã mời, bài tập đã giao
make smoke     # smoke test auth + luồng câu hỏi → lớp → giao bài
make report    # reports/: JaCoCo, pytest-cov, Vitest, README.md, sprint1-reports.tar.gz
make down      # dừng đúng các tiến trình đã khởi động, giữ dữ liệu
```

Không có `make` thì chạy từng lệnh trong `Makefile`; phần chạy là `python3 scripts/native.py up`, `python3 scripts/demo.py`, `python3 scripts/smoke.py`, `python3 scripts/native.py down`.

Mở http://127.0.0.1:5173. Launcher từ chối chạy nếu bất kỳ cổng nào đang bận, chỉ dừng PID có thời điểm tạo khớp bản ghi trong `.runtime`. Learning Service chạy `alembic upgrade head` trước khi khởi động, giống Dockerfile.

## Chạy bằng Docker (khuyên dùng trên Mac/Windows)

Chỉ cần **Docker Desktop** (Mac Intel hoặc Apple Silicon, Windows) hoặc Docker Engine + compose plugin (Linux). Không cần cài Java, Node, Python hay PostgreSQL. Nên để Docker Desktop dùng ít nhất 4 GB RAM (Settings → Resources).

```bash
docker compose up -d --build --wait   # lần đầu build mất vài phút (tải Maven, npm, uv)
docker compose wait demo-seed         # đợi tạo xong tài khoản và dữ liệu demo
```

Có `make` thì gõ gọn `make docker-up`. Xong mở **http://127.0.0.1:5173** (hoặc http://localhost:5173) và đăng nhập bằng tài khoản ở bảng trên (admin, giáo viên, học sinh đều dùng được ngay).

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

Phần đầu giữ các kiểm tra auth (RS256/JWKS, đăng ký, đăng nhập, 401/403/409/429, khóa tài khoản, đổi vai trò...). Phần cuối là luồng của Tùng, đi qua web proxy và gateway:

1. Giảng viên tạo chủ đề và 3 câu hỏi (đáp án B, C, A); sinh viên đọc được chủ đề nhưng không đọc được ngân hàng câu hỏi (403) và không tạo được chủ đề, câu hỏi (403).
2. Nhập câu hỏi từ file JSON: xem trước báo dòng sai đáp án đúng, nhập thật lưu 1 câu; file JSON thiếu cột trả 400 `Missing column`; sinh viên nhập file bị 403.
3. Giảng viên tạo lớp (mã 6 ký tự), giao bài từ ngân hàng: hạn nộp ở quá khứ hoặc danh sách câu hỏi rỗng bị 400, sinh viên giao bài bị 403.
4. Câu hỏi được sao chép: sửa câu trong ngân hàng sau khi giao không đổi bài tập; sinh viên không mở được chi tiết bài (đáp án) và không xóa được bài.
5. Sinh viên nhập mã vào lớp (không phân biệt hoa thường, mã sai 404, vào lại 409) rồi thấy bài tập đã giao; sinh viên ngoài lớp không thấy và bị 403 khi lọc theo lớp.

## Kiểm thử và báo cáo

`make report` ghi vào `reports/` (trong git giữ `reports/README.md`, `reports/sprint1-reports.tar.gz` và `reports/screenshots/`).

| Thành phần | Số test | Độ phủ |
| --- | ---: | --- |
| auth-service (JUnit) | 44 | 75,3 % |
| class-service (JUnit) | 40 | 65,7 % |
| exercise-service (JUnit) | 17 | 72,7 % |
| gateway (pytest) | 52 | 94,1 % |
| learning-service (pytest) | 45 | 93,6 % |
| web (Vitest) | 62 | |
| launcher (unittest) | 6 | |

Tất cả đều đạt, 0 lỗi; `npm run lint` và `npm run build` sạch (lint có 4 cảnh báo sẵn có của các file `components/ui` và `use-mobile`).

## Ảnh demo

`reports/screenshots/` chụp từ web đang chạy native: giảng viên (tổng quan, lớp, ngân hàng câu hỏi, giao bài, bài đã giao, chi tiết bài tập với đáp án), sinh viên (tổng quan, lớp, bài tập được giao), admin (quản lý người dùng).

## Giới hạn

- Sinh viên chỉ xem được danh sách bài tập của lớp mình (chỉ đọc), chưa làm bài, nộp bài hay xem điểm.
- Giảng viên chỉ xem được chi tiết bài tập (câu hỏi và đáp án đã sao chép), chưa có kết quả của sinh viên.
- Sổ từ vựng chỉ có API `/vocabularies` (sinh viên); web chưa có trang cho nó. Dữ liệu mẫu nạp qua `make demo`.
- Chưa có thông báo, luyện tập, xếp hạng.
- Bản Docker đã chạy thật trên Linux amd64 (Docker Engine 29, Compose v5): đủ service healthy, `demo-seed` và smoke đều đạt. Image dùng base đa kiến trúc nên build được cho arm64 (Apple Silicon), nhưng chưa chạy thử trên máy Mac thật.
- Native dùng PostgreSQL trust trên loopback, chỉ cho demo local.
- Một vài test web (`App.integration.test.tsx`) có thể quá thời gian khi máy đang bận, chạy lại sẽ qua.
