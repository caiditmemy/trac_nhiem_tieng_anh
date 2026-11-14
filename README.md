# NamBeDe — Sprint 1 (Tuần 6 + Tuần 7)

Sprint 1 gồm hai tuần:

- **Tuần 6**: giao diện, migration và API. Giảng viên tạo lớp (mã mời 6 ký tự), soạn ngân hàng câu hỏi (nhập từ Excel, CSV hoặc JSON), giao bài tập; sinh viên vào lớp bằng mã, làm bài, nộp và xem điểm; điểm hoạt động và chuỗi ngày học được cập nhật khi nộp bài.
- **Tuần 7**: unit test, docker compose, smoke test end-to-end, demo và báo cáo kiểm thử.

Kiến trúc: React → FastAPI Gateway → Auth / Class / Exercise (Spring Boot) và Learning (FastAPI) → PostgreSQL (mỗi service một database). Chưa dùng message broker: Exercise Service gọi thẳng Learning Service sau khi nộp bài.

## Phân công

| Người | Tuần 6 | Tuần 7 |
| --- | --- | --- |
| **Nam** | Auth Service và Gateway ổn định (`services/auth-service`, `services/gateway`). Class Service: tạo lớp, mã mời 6 ký tự `ClassCodeGenerator`, `joinClass` (`services/class-service`). Web: `web/src/pages/ClassesPage.tsx`, `ClassDetailPage.tsx`, `web/src/components/ClassFormDialog.tsx` | `ClassServiceTest`, `ClassCodeGeneratorTest`, `ClassControllerTest` (`services/class-service/src/test`); Gateway `tests/test_gateway.py`, `tests/test_jwks.py` (thêm test route cho các service mới); web `AuthProvider.test.tsx`, `ClassesPage.test.tsx`; `docker-compose.yml` với `auth_db`, `class_db` |
| **Tùng** | Learning Service: chủ đề, từ vựng, CRUD câu hỏi trắc nghiệm; nhập câu hỏi từ Excel/CSV/JSON (`services/learning-service/src/learning_service/question_import.py`). Exercise Service: `ExerciseController` (tạo, giao bài). Web: `QuestionBankPage.tsx`, `QuestionImportDialog.tsx`, `AssignExercisePage.tsx` | Learning `tests/test_question_bank.py`, `tests/test_import.py` (file hỏng và thiếu cột cho xlsx và json); `ExerciseServiceTest`, `ExerciseControllerTest`; dữ liệu mẫu `scripts/seed_data.json` + `scripts/demo.py` (dùng bởi `make demo`) |
| **Tấn** | Exercise Service: nộp bài và chấm điểm (`SubmissionController`, `Grading.java`, `SubmissionService.java`); Learning Service: API cập nhật streak và điểm (`routers/activities.py`, `progress.py`). Web: `TakeExercisePage.tsx`, `ExerciseResultsPage.tsx`, `SubmissionReview.tsx`. Android: `TakeExercise.kt` | `GradingTest`, `SubmissionServiceTest`; web `TakeExercisePage.test.tsx`; Android `ScreenshotTest` (`android/app/src/test`); `scripts/smoke.py` E2E; gói báo cáo test/coverage trong `reports/` (`scripts/report.py`) |

## Cổng và tài khoản demo

| Thành phần | Cổng | Database |
| --- | --- | --- |
| PostgreSQL (cluster riêng) | 55434 | `auth_db`, `class_db`, `exercise_db`, `learning_db` |
| Auth Service | 8104 | `auth_db` |
| Class Service | 8114 | `class_db` |
| Exercise Service | 8124 | `exercise_db` |
| Learning Service | 8134 | `learning_db` |
| Gateway | 8204 | |
| Web | 5174 | |

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | admin@sprint1.local | LocalAdmin@123 |
| TEACHER | teacher@sprint1.local | DemoUser@123 |
| STUDENT | student@sprint1.local | DemoUser@123 |

Cấu hình native duy nhất: `scripts/app.json`. Mọi cổng chỉ nghe trên 127.0.0.1.

## Chạy native (Linux)

Cần JDK 17+, Node 24+, Python 3.12+, uv, PostgreSQL 17 và `make`.

```bash
export JAVA_HOME=/home/box/opt/jdk
export PG_BIN=/workspace/pg17/bin
make build     # gateway, learning (uv build), 3 service Spring (mvnw verify), web
make test      # launcher, pytest gateway + learning, Vitest, lint (Spring: make test-java)
make up        # postgres → auth → class/learning → exercise → gateway → web
make demo      # tài khoản, ngân hàng câu hỏi + từ vựng, lớp demo có mã mời, bài tập đã giao
make smoke     # smoke test auth + E2E tuần 7
make report    # reports/: JaCoCo, pytest-cov, Vitest, README.md, sprint1-reports.tar.gz
make down      # dừng đúng các tiến trình đã khởi động, giữ dữ liệu
```

Mở http://127.0.0.1:5174. Launcher từ chối chạy nếu bất kỳ cổng nào đang bận, chỉ dừng PID có thời điểm tạo khớp bản ghi trong `.runtime`. Learning Service chạy `alembic upgrade head` trước khi khởi động, giống Dockerfile.

## Chạy bằng Docker (khuyên dùng trên Mac/Windows)

Chỉ cần **Docker Desktop** (Mac Intel hoặc Apple Silicon, Windows) hoặc Docker Engine + compose plugin (Linux). Không cần cài Java, Node, Python hay PostgreSQL. Nên để Docker Desktop dùng ít nhất 4 GB RAM (Settings → Resources).

```bash
docker compose up -d --build --wait   # lần đầu build mất vài phút (tải Maven, npm, uv)
docker compose wait demo-seed         # đợi tạo xong tài khoản và dữ liệu demo
```

Có `make` thì gõ gọn `make docker-up`. Xong mở **http://127.0.0.1:5174** (hoặc http://localhost:5174) và đăng nhập bằng tài khoản ở bảng trên (admin, giáo viên, học sinh đều dùng được ngay).

- Service `demo-seed` chạy `scripts/demo.py` một lần sau mỗi lần `up` rồi tự thoát: tạo giáo viên, học sinh, ngân hàng câu hỏi, lớp demo có mã mời và một bài tập đã giao. Chạy lại nhiều lần không tạo trùng.
- Khoá JWT RS256 do auth-service tự sinh ở lần chạy đầu và lưu trong `auth_db`, không cần tạo hay khai báo khoá. Mật khẩu trong `docker-compose.yml` chỉ dành cho demo local, mọi cổng chỉ mở trên 127.0.0.1.
- Cùng cổng với bản native, nên chỉ chạy một trong hai.

| Việc | Lệnh | Có `make` |
| --- | --- | --- |
| Smoke test E2E tuần 7 | `docker compose run --rm --no-deps demo-seed python smoke.py --api http://web/api --auth http://auth-service:8081` | `make docker-smoke` |
| Xem trạng thái | `docker compose ps -a` | |
| Xem log | `docker compose logs -f auth-service` (bỏ tên service để xem tất cả) | `make docker-logs` |
| Dừng, giữ dữ liệu | `docker compose down` | `make docker-down` |
| Dừng và xoá dữ liệu | `docker compose down -v` | `make docker-reset` |

Build bỏ qua unit test Spring cho nhanh: `SKIP_TESTS=true docker compose up -d --build --wait`. Nếu báo cổng đang bận, tắt chương trình đang dùng cổng đó (ví dụ bản native: `make down`).

## Luồng E2E tuần 7 (`scripts/smoke.py`)

Giảng viên tạo chủ đề, câu hỏi (kể cả nhập từ JSON) và lớp → lấy mã 6 ký tự → giao bài → sinh viên vào lớp bằng mã (không phân biệt hoa thường) → làm bài, nộp → điểm đúng (1/3 câu đúng = 3,33) → chuỗi ngày học = 1, điểm hoạt động = 21 (+20 và +1 mỗi câu đúng); nộp lại bị từ chối và không cộng thêm; sinh viên ngoài lớp bị chặn; `/api/activities` không được gateway mở. Phần đầu của smoke giữ nguyên các kiểm tra auth (RS256/JWKS, 401/403/409/429, khóa tài khoản...).

## Kiểm thử và báo cáo

`make report` ghi vào `reports/` (trong git chỉ giữ `reports/README.md` và `reports/sprint1-reports.tar.gz`): kết quả JUnit/pytest/Vitest, độ phủ JaCoCo cho ba service Spring, pytest-cov cho gateway và learning, `reports/README.md` tóm tắt và `reports/sprint1-reports.tar.gz`.

## Ảnh demo

`reports/screenshots/`: ảnh chụp từ web đang chạy native (giảng viên: lớp, chi tiết lớp có mã mời, ngân hàng câu hỏi, giao bài, kết quả bài; sinh viên: nhập mã vào lớp, làm bài, kết quả 6,67/10, dashboard có streak 1 ngày và 24 điểm; admin: quản lý người dùng).

## Android (`android/`)

Ứng dụng sinh viên tối giản: đăng nhập, trang chủ, danh sách bài tập, làm bài (`TakeExercise.kt`), kết quả, hồ sơ. Cần Android SDK (`android/local.properties` với `sdk.dir`, đã gitignore). Gateway mặc định `10.0.2.2:8204` (emulator).

```bash
cd android && ./gradlew --no-daemon testDebugUnitTest verifyPaparazziDebug assembleDebug
./gradlew recordPaparazziDebug     # ghi lại ảnh chụp màn hình
./gradlew verifyPaparazziDebug     # so với ảnh đã ghi
```

## Giới hạn

- Không có RabbitMQ, thông báo, luyện tập, xếp hạng (chỉ API `/ranking/weekly` và `/streak` phục vụ dashboard), xuất bảng điểm: thuộc các sprint sau. Learning Service vẫn có API `/vocabularies` (sổ từ của sinh viên) nhưng web chưa có trang cho nó.
- Điểm và streak được cộng bằng một lời gọi REST sau khi nộp bài; Learning Service tắt thì bài vẫn nộp được nhưng lần nộp đó không được cộng điểm (không có hàng đợi thử lại).
- Bản Docker đã chạy thật trên Linux amd64 (Docker Engine 29, Compose v5): đủ service healthy, `demo-seed` và smoke đều đạt. Cả sáu image cũng build được cho arm64 (Apple Silicon) bằng giả lập QEMU, nhưng chưa chạy thử trên máy Mac thật.
- Native dùng PostgreSQL trust trên loopback, chỉ cho demo local.
