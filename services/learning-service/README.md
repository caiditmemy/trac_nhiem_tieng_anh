# Learning Service (FastAPI)

Chủ đề, ngân hàng câu hỏi (nhập từ Excel/CSV/JSON) và sổ từ vựng.

- Cổng `8084`, database `learning_db` (bảng `topics`, `questions`, `vocabularies`). Schema do Alembic quản lý trong `alembic/versions`, chạy tự động khi container khởi động.
- Swagger riêng: http://localhost:8084/docs (bản mô tả: `/openapi.json`). Gọi qua gateway thì dùng Swagger chung ở http://localhost:8000/docs.
- Ngân hàng câu hỏi dùng chung cho mọi giảng viên: câu trắc nghiệm 4 đáp án A đến D, 1 đáp án đúng. Chỉ người soạn hoặc admin được sửa, xóa. Sinh viên không xem được ngân hàng (tránh lộ đáp án) nhưng xem được danh sách chủ đề.
- Nhập câu hỏi từ file `.xlsx`, `.csv` hoặc `.json` theo file mẫu (Câu hỏi, Đáp án A đến D, Đáp án đúng, Giải thích), tối đa 200 câu và 1 MB. File JSON là một mảng các object có khóa trùng tên cột của file mẫu (`"Câu hỏi"`, `"Đáp án A"`... hoặc `content`, `optionA`..`optionD`, `correctOption`, `explanation`); Excel/CSV/JSON thiếu cột bắt buộc thì bị từ chối (400, `Missing column: ...`). Số dòng của JSON là vị trí trong mảng. Mỗi dòng được kiểm tra riêng và trả về mã lỗi (`content_missing`, `option_b_missing`, `correct_option_invalid`, `too_long`, `duplicate`). Xem trước với `dryRun=true`, nhập thật thì chỉ lưu các dòng không có lỗi và không trùng câu đã có trong chủ đề.
- Họ tên lấy từ Auth Service qua `GET /users/lookup`, kèm token của người gọi. Nếu Auth Service lỗi, dữ liệu vẫn trả về, chỉ thiếu tên.
- Token được kiểm tra bằng khóa công khai của Auth Service (RS256, `JWKS_URL`, đọc một lần rồi giữ trong bộ nhớ); service không giữ bí mật nào nên không tự tạo được token. Chưa lấy được khóa: 502.
- Mỗi request mang mã `X-Request-ID` từ gateway (không có thì tự tạo): mã được ghi trên mọi dòng log, trả lại trong header, chuyển tiếp khi gọi service khác.
- `GET /health` trả 503 khi không kết nối được database.

## API

| Method | Path | Ai được gọi | Mô tả |
|---|---|---|---|
| GET | `/topics` | mọi vai trò | Chủ đề kèm số câu hỏi |
| POST | `/topics` | giảng viên, admin | Tạo chủ đề `{name, description}`; tên không trùng |
| PUT, DELETE | `/topics/{id}` | người tạo, admin | Sửa, xóa; không xóa được khi còn câu hỏi (409) |
| GET | `/questions?topicId=&q=&mine=true` | giảng viên, admin | Lọc theo chủ đề, từ khóa, câu của tôi |
| GET | `/questions/{id}` | giảng viên, admin | Một câu hỏi; Exercise Service dùng để sao chép khi giao bài |
| POST | `/questions` | giảng viên, admin | `{topicId, content, optionA..optionD, correctOption, explanation}` |
| PUT, DELETE | `/questions/{id}` | người soạn, admin | Sửa, xóa |
| GET | `/questions/import/template` | giảng viên, admin | File Excel mẫu `mau-cau-hoi.xlsx` |
| POST | `/questions/import?topicId=&dryRun=true` | giảng viên, admin | Gửi file (multipart, trường `file`): xem trước từng dòng, hoặc nhập khi `dryRun=false` |
| GET, POST | `/vocabularies` | sinh viên | Từ vựng của sinh viên (API có sẵn, chưa có trang web): danh sách, thêm `{word, meaning, example}` (không trùng từ) |
| PUT, DELETE | `/vocabularies/{id}` | chủ sổ | Sửa, xóa |

## Chạy và test

```bash
uv sync
uv run pytest --cov=learning_service
uv run alembic upgrade head                                 # cần Postgres đang chạy (`make up` ở thư mục gốc)
uv run uvicorn learning_service.main:app --reload --port 8084
```

Thêm bảng mới: khai báo model trong `src/learning_service/models.py`, rồi chạy `uv run alembic revision --autogenerate -m "mo ta"`, kiểm tra file sinh ra, và `uv run alembic check` để chắc model khớp database. Test dùng SQLite trong bộ nhớ, không cần Postgres.

Dữ liệu mẫu (chủ đề, câu hỏi, từ vựng): `python3 scripts/demo.py` ở thư mục gốc (gọi API, không ghi thẳng vào database). Trong Docker: `docker compose up -d --build learning-service` từ thư mục gốc.
