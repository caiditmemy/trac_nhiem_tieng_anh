# API Gateway (FastAPI)

Điểm vào duy nhất cho Web. Client không gọi trực tiếp các service phía sau. (Sprint 1 chỉ có Auth Service phía sau; các service khác được thêm ở các sprint sau.)

- Cổng `8000`, không có database. Swagger chung: http://localhost:8000/docs (và `/api/docs`). `src/gateway/docs.py` lấy bản mô tả OpenAPI của từng service (`/v3/api-docs` với Spring), chỉ giữ các đường dẫn gateway có định tuyến và đặt server là `/api`, nên mọi lần gọi thử đều đi qua gateway.
- Kiểm tra JWT (RS256, bằng khóa công khai đọc một lần từ `JWKS_URL` của Auth Service, `src/gateway/jwks.py`) rồi hỏi Auth Service phiên còn mở không (`GET /auth/session`, giữ câu trả lời 10 giây mỗi người dùng, `src/gateway/sessions.py`; Auth Service không trả lời thì cho qua theo chữ ký). Sau đó chuyển tiếp request tới service theo tiền tố đường dẫn. Header `Authorization`, query và body được chuyển nguyên vẹn. Không lấy được khóa công khai: 502, không phải 401.
- Phản hồi của service (kể cả lỗi) được trả về nguyên vẹn. Gateway chỉ tự sinh lỗi 400 (request tới API của chính gateway sai dạng), 401, 403, 404, 500 và **502 khi service không truy cập được**.
- Mỗi request có một mã `X-Request-ID` (giữ mã client gửi nếu đúng dạng, không thì tự tạo). Mã được chuyển sang service, trả lại trong header và ghi trên mọi dòng log (`src/gateway/requestid.py`), mỗi request một dòng log `METHOD path status thời gian`. Trình duyệt đọc được header này nhờ `expose_headers` của CORS.
- Response header được giữ lại theo danh sách cho phép: `Content-Disposition`, `Cache-Control`, `Location`, `Retry-After`.

| Tiền tố công khai | Service | Cần token |
|---|---|---|
| `/api/auth/*` | auth-service | không |
| `/api/users/*` | auth-service | có |
| `/api/health` | gateway | không |
| `/docs`, `/api/docs`, `/api/openapi.json`, `/api/docs/<service>.json` | gateway: Swagger chung và các bản mô tả API | không |

Thêm tiền tố mới: sửa bảng `ROUTES` trong `src/gateway/routes.py`.

## Chạy và test

```bash
uv sync
uv run pytest
uv run uvicorn gateway.main:app --reload --port 8000   # đọc cấu hình từ ../../.env
```

Trong Docker: `docker compose up -d --build gateway` từ thư mục gốc.
