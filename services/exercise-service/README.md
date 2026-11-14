# Exercise Service (Spring Boot)

Bài tập giảng viên giao cho lớp, bản sao câu hỏi tại thời điểm giao, bài làm, chấm điểm.

- Cổng `8083`, database `exercise_db` (bảng `exercises`, `exercise_questions`, `submissions`, `submission_answers`). Schema do Flyway quản lý trong `src/main/resources/db/migration`.
- Swagger riêng: http://localhost:8083/swagger-ui.html (bản mô tả: `/v3/api-docs`, sinh bằng springdoc). Gọi qua gateway thì dùng Swagger chung ở http://localhost:8000/docs.
- Khi giao bài, service gọi Learning Service `GET /questions/{id}` cho từng câu và **chép nội dung, 4 đáp án, đáp án đúng, giải thích** vào `exercise_questions`. Chấm điểm dựa trên bản sao này, nên sửa hoặc xóa câu trong ngân hàng sau đó không làm đổi bài đã giao hay điểm.
- Lớp và sinh viên thuộc Class Service. Service gọi `GET /classes/{id}` để biết giảng viên của lớp, `GET /classes/{id}/students/{studentId}/exists` trước khi sinh viên bắt đầu làm, `GET /classes/{id}/students` cho trang kết quả. Mọi lời gọi đều kèm token của người gọi. Nếu Class Service hoặc Learning Service không truy cập được, API trả 502.
- Mỗi sinh viên làm một lần cho mỗi bài, đáp án được lưu ngay khi chọn. Bài chỉ có hạn nộp, không đếm giờ: sau hạn không bắt đầu, không sửa, không nộp được nữa.
- Điểm thang 10, làm tròn 2 chữ số: số câu đúng × 10 / số câu. Câu bỏ trống tính là sai. Nộp xong sinh viên thấy ngay đáp án đúng và giải thích.
- Nộp bài xong (sau khi transaction commit), `ActivityReporter` gọi thẳng `POST /activities/exercise` của Learning Service bằng token của sinh viên để cộng điểm (+20, +1 mỗi câu đúng) và ghi một ngày học vào chuỗi streak. Mỗi bài nộp chỉ được tính một lần. Learning Service tắt thì bài vẫn nộp thành công (chỉ ghi log cảnh báo).
- Token được kiểm tra bằng khóa công khai của Auth Service (RS256, `JWKS_URL`, đọc một lần rồi giữ trong bộ nhớ); service không giữ bí mật nào nên không tự tạo được token. Chưa lấy được khóa: 502.
- Mỗi request mang mã `X-Request-ID` từ gateway (không có thì tự tạo): mã được ghi trên mọi dòng log, trả lại trong header, chuyển tiếp khi gọi Class Service và Learning Service.

## API

| Method | Path | Ai được gọi | Mô tả |
|---|---|---|---|
| POST | `/exercises` | giảng viên của lớp | `{classId, title, description, dueAt, questionIds}`: giao bài, chép câu hỏi theo thứ tự `questionIds` (tối đa 50) |
| GET | `/exercises?classId=` | mọi vai trò | Giảng viên: bài đã giao, kèm số bài đã nộp. Sinh viên: bài của lớp mình, kèm trạng thái và điểm. Admin: tất cả |
| GET | `/exercises/{id}` | giảng viên giao bài, sinh viên trong lớp, admin | Một bài tập |
| GET | `/exercises/{id}/results` | giảng viên giao bài, admin | Mọi sinh viên của lớp với trạng thái và điểm, điểm trung bình, tỉ lệ làm đúng từng câu |
| DELETE | `/exercises/{id}` | giảng viên giao bài, admin | Xóa bài tập cùng bài làm |
| POST | `/exercises/{id}/start` | sinh viên trong lớp | Mở bài làm: tạo ở lần đầu, các lần sau trả lại bài đang làm hoặc kết quả. 409 khi đã quá hạn |
| GET | `/submissions/{id}` | sinh viên làm bài, giảng viên giao bài, admin | Bài làm. Có đáp án đúng khi đã nộp hoặc khi người xem là giảng viên, admin |
| POST | `/submissions/{id}/answer` | sinh viên làm bài | `{exerciseQuestionId, chosenOption}`: lưu đáp án chọn cho một câu |
| POST | `/submissions/{id}/submit` | sinh viên làm bài | Nộp và chấm điểm, trả về điểm, đáp án đúng và giải thích |

## Chạy và test

```bash
docker compose up -d --build exercise-service        # từ thư mục gốc
cd services/exercise-service
docker run --rm -v "$PWD":/workspace -v nambede-m2:/root/.m2 -w /workspace \
  eclipse-temurin:17-jdk-noble ./mvnw -B -ntp verify
```
