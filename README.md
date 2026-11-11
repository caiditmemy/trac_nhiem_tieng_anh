# NamBeDe — Sprint 1 — Tấn (Thành viên 3)

Nhánh này chứa phần việc **Tuần 6 + Tuần 7 của Tấn** và các phần phụ thuộc chung để chạy được độc lập (build, test, chạy native, smoke E2E). Bản ghép đủ ba thành viên nằm ở nhánh `sprint-1`.

Kiến trúc: React → FastAPI Gateway → Auth / Class / Exercise (Spring Boot) và Learning (FastAPI) → PostgreSQL (mỗi service một database). Chưa dùng message broker: Exercise Service gọi thẳng Learning Service sau khi nộp bài.

## Phần việc của Tấn

**Tuần 6**

- Exercise Service: nộp bài và chấm điểm — `services/exercise-service/.../SubmissionController`, `Grading.java`, `SubmissionService.java`.
- Learning Service: API cập nhật streak và điểm hoạt động — `services/learning-service/src/learning_service/routers/activities.py`, `progress.py`.
- Web: `web/src/pages/TakeExercisePage.tsx`, `ExerciseResultsPage.tsx`, `web/src/components/SubmissionReview.tsx`.
- Android: `android/app/src/main/java/vn/edu/qnu/st/nambede/ui/exercises/TakeExercise.kt`.

**Tuần 7**

- Unit test: `GradingTest`, `SubmissionServiceTest` (`services/exercise-service/src/test`).
- Web: `TakeExercisePage.test.tsx`; Android: `ScreenshotTest` (`android/app/src/test`, Paparazzi).
- `scripts/smoke.py`: E2E giáo viên tạo lớp → giao bài → học sinh nhập mã → nộp bài → điểm đúng.
- Báo cáo test/coverage: `scripts/report.py`, `reports/`.

## Phụ thuộc chung (giữ lại để nhánh chạy độc lập)

Luồng của Tấn cần cả chuỗi dịch vụ phía trước, nên các phần sau được giữ nguyên, không thuộc phần việc của Tấn:

- **Nam**: `services/auth-service`, `services/gateway`, `services/class-service` (tạo lớp, mã mời, `joinClass`), web shell (đăng nhập, đăng ký, hồ sơ, quản lý người dùng, layout, api client, phân quyền) và `ClassesPage`, `ClassDetailPage`, `ClassFormDialog`.
- **Tùng**: ngân hàng câu hỏi và từ vựng (`services/learning-service`), nhập câu hỏi, `ExerciseController` (tạo, giao bài), web `QuestionBankPage`, `QuestionImportDialog`, `AssignExercisePage`, dữ liệu mẫu `scripts/seed_data.json`.

## Cổng và tài khoản demo

| Thành phần | Cổng | Database |
| --- | --- | --- |
| PostgreSQL (cluster riêng) | 55432 | `auth_db`, `class_db`, `exercise_db`, `learning_db` |
| Auth Service | 8102 | `auth_db` |
| Class Service | 8112 | `class_db` |
| Exercise Service | 8122 | `exercise_db` |
| Learning Service | 8132 | `learning_db` |
| Gateway | 8202 | |
| Web | 5172 | |

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | admin@sprint1.local | LocalAdmin@123 |
| TEACHER | teacher@sprint1-tan.local | DemoUser@123 |
| STUDENT | student@sprint1-tan.local | DemoUser@123 |

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

Mở http://127.0.0.1:5172. Launcher từ chối chạy nếu bất kỳ cổng nào đang bận, chỉ dừng PID có thời điểm tạo khớp bản ghi trong `.runtime`. Learning Service chạy `alembic upgrade head` trước khi khởi động, giống Dockerfile.

## Docker Compose

```bash
docker compose up -d --build --wait
python3 scripts/demo.py
python3 scripts/smoke.py
docker compose down
```

Cùng cổng với native; chỉ chạy một trong hai. `infra/postgres/init` tạo bốn database. nginx của web chuyển `/api` đến gateway.

## Luồng E2E tuần 7 (`scripts/smoke.py`)

Giảng viên tạo chủ đề, câu hỏi (kể cả nhập từ JSON) và lớp → lấy mã 6 ký tự → giao bài → sinh viên vào lớp bằng mã (không phân biệt hoa thường) → làm bài, nộp → điểm đúng (1/3 câu đúng = 3,33) → chuỗi ngày học = 1, điểm hoạt động = 21 (+20 và +1 mỗi câu đúng); nộp lại bị từ chối và không cộng thêm; sinh viên ngoài lớp bị chặn; `/api/activities` không được gateway mở. Phần đầu của smoke giữ nguyên các kiểm tra auth (RS256/JWKS, 401/403/409/429, khóa tài khoản...).

## Kiểm thử và báo cáo

`make report` ghi vào `reports/`: kết quả JUnit/pytest/Vitest, độ phủ JaCoCo cho ba service Spring, pytest-cov cho gateway và learning; trong git giữ `reports/README.md`, `reports/sprint1-reports.tar.gz` và `reports/screenshots/`.

| Thành phần | Số test | Lỗi | Độ phủ |
| --- | ---: | ---: | --- |
| auth-service (JUnit) | 44 | 0 | 75,3 % |
| class-service (JUnit) | 40 | 0 | 65,7 % |
| exercise-service (JUnit, gồm `GradingTest`, `SubmissionServiceTest`) | 34 | 0 | 75,3 % |
| gateway (pytest) | 59 | 0 | 94,1 % |
| learning-service (pytest) | 55 | 0 | 94,8 % |
| web (Vitest, gồm `TakeExercisePage.test.tsx`) | 63 | 0 | n/a |
| android (JUnit + Paparazzi) | 24 | 0 | n/a |
| launcher (`unittest`) | đạt | 0 | n/a |

## Ảnh demo

`reports/screenshots/`: ảnh chụp từ web đang chạy native liên quan đến phần của Tấn (sinh viên: danh sách bài tập, làm bài, kết quả, dashboard sau khi nộp có streak và điểm; giảng viên: kết quả bài nộp của lớp).

## Android (`android/`)

Ứng dụng sinh viên tối giản: đăng nhập, trang chủ, danh sách bài tập, làm bài (`TakeExercise.kt`), kết quả, hồ sơ. Cần Android SDK (`android/local.properties` với `sdk.dir`, đã gitignore). Gateway mặc định `10.0.2.2:8202` (emulator).

```bash
cd android && ./gradlew --no-daemon testDebugUnitTest verifyPaparazziDebug assembleDebug
./gradlew recordPaparazziDebug     # ghi lại ảnh chụp màn hình
./gradlew verifyPaparazziDebug     # so với ảnh đã ghi
```

## Giới hạn

- Không có RabbitMQ, thông báo, luyện tập, xếp hạng (chỉ API `/ranking/weekly` và `/streak` phục vụ dashboard), xuất bảng điểm: thuộc các sprint sau. Learning Service vẫn có API `/vocabularies` (sổ từ của sinh viên) nhưng web chưa có trang cho nó.
- Điểm và streak được cộng bằng một lời gọi REST sau khi nộp bài; Learning Service tắt thì bài vẫn nộp được nhưng lần nộp đó không được cộng điểm (không có hàng đợi thử lại).
- Docker không có trên máy phát triển nên `docker-compose.yml` mới chỉ được kiểm tra cú pháp YAML.
- Native dùng PostgreSQL trust trên loopback, chỉ cho demo local.
