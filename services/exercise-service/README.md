# Exercise Service (Spring Boot)

Bài tập giảng viên giao cho lớp và bản sao câu hỏi tại thời điểm giao.

- Cổng `8083`, database `exercise_db` (bảng `exercises`, `exercise_questions`). Schema do Flyway quản lý trong `src/main/resources/db/migration`.
- Swagger riêng: http://localhost:8083/swagger-ui.html (bản mô tả: `/v3/api-docs`, sinh bằng springdoc). Gọi qua gateway thì dùng Swagger chung ở http://localhost:8000/docs.
- Khi giao bài, service gọi Learning Service `GET /questions/{id}` cho từng câu và **chép nội dung, 4 đáp án, đáp án đúng, giải thích** vào `exercise_questions`. Sửa hoặc xóa câu trong ngân hàng sau đó không làm đổi bài đã giao.
- Lớp và sinh viên thuộc Class Service. Service gọi `GET /classes/{id}` để biết giảng viên của lớp, `GET /classes` để liệt kê lớp của người gọi. Mọi lời gọi đều kèm token của người gọi. Nếu Class Service hoặc Learning Service không truy cập được, API trả 502.
- Bài chỉ có hạn nộp, không đếm giờ; hạn nộp phải ở tương lai khi giao.
- Token được kiểm tra bằng khóa công khai của Auth Service (RS256, `JWKS_URL`, đọc một lần rồi giữ trong bộ nhớ); service không giữ bí mật nào nên không tự tạo được token. Chưa lấy được khóa: 502.
- Mỗi request mang mã `X-Request-ID` từ gateway (không có thì tự tạo): mã được ghi trên mọi dòng log, trả lại trong header, chuyển tiếp khi gọi Class Service và Learning Service.

## API

| Method | Path | Ai được gọi | Mô tả |
|---|---|---|---|
| POST | `/exercises` | giảng viên của lớp | `{classId, title, description, dueAt, questionIds}`: giao bài, chép câu hỏi theo thứ tự `questionIds` (tối đa 50) |
| GET | `/exercises?classId=` | mọi vai trò | Giảng viên: bài đã giao, kèm số sinh viên của lớp. Sinh viên: bài của lớp mình (chỉ đọc). Admin: tất cả |
| GET | `/exercises/{id}` | giảng viên giao bài, admin | Bài tập cùng các câu hỏi đã sao chép và đáp án đúng. Sinh viên: 403 |
| DELETE | `/exercises/{id}` | giảng viên giao bài, admin | Xóa bài tập |

## Chạy và test

```bash
docker compose up -d --build exercise-service        # từ thư mục gốc
cd services/exercise-service
docker run --rm -v "$PWD":/workspace -v nambede-m2:/root/.m2 -w /workspace \
  eclipse-temurin:17-jdk-noble ./mvnw -B -ntp verify
```
