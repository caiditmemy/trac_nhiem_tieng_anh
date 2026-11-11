# Class Service (Spring Boot)

Lớp học, mã lớp, giảng viên của lớp, sinh viên tham gia hoặc rời lớp.

- Cổng `8082`, database `class_db` (bảng `classes`, `class_students`). Schema do Flyway quản lý trong `src/main/resources/db/migration`.
- Swagger riêng: http://localhost:8082/swagger-ui.html (bản mô tả: `/v3/api-docs`, sinh bằng springdoc). Gọi qua gateway thì dùng Swagger chung ở http://localhost:8000/docs.
- Token được kiểm tra bằng khóa công khai của Auth Service (RS256, `JWKS_URL`, đọc một lần rồi giữ trong bộ nhớ); service không giữ bí mật nào nên không tự tạo được token. Chưa lấy được khóa: 502.
- Mỗi request mang mã `X-Request-ID` từ gateway (không có thì tự tạo): mã được ghi trên mọi dòng log, trả lại trong header, chuyển tiếp khi gọi service khác.
- Mã lớp gồm 6 ký tự, không dùng các ký tự dễ nhầm như 0/O hay 1/I/L. Sinh viên nhập không phân biệt hoa thường.
- Chỉ giảng viên của lớp hoặc admin được sửa, xóa lớp và quản lý sinh viên.
- Họ tên giảng viên và sinh viên lấy từ Auth Service qua `GET /users/lookup`, kèm token của người gọi. Service không đọc database của Auth Service. Nếu Auth Service lỗi, dữ liệu lớp vẫn trả về, chỉ thiếu họ tên.

## API

| Method | Path | Ai được gọi | Mô tả |
|---|---|---|---|
| GET | `/classes` | mọi vai trò | Giảng viên: lớp mình dạy. Sinh viên: lớp đã tham gia. Admin: tất cả |
| POST | `/classes` | giảng viên | Tạo lớp `{name, description}`, sinh mã lớp |
| GET | `/classes/{id}` | giảng viên của lớp, sinh viên trong lớp, admin | Chi tiết lớp |
| PUT | `/classes/{id}` | giảng viên của lớp, admin | Sửa tên và mô tả |
| DELETE | `/classes/{id}` | giảng viên của lớp, admin | Xóa lớp |
| POST | `/classes/join` | sinh viên | Tham gia bằng mã `{code}` |
| POST | `/classes/{id}/leave` | sinh viên | Rời lớp |
| GET | `/classes/{id}/students` | giảng viên của lớp, admin | Sinh viên trong lớp kèm họ tên |
| DELETE | `/classes/{id}/students/{studentId}` | giảng viên của lớp, admin | Xóa sinh viên khỏi lớp |
| GET | `/classes/{id}/students/{studentId}/exists` | chính sinh viên đó, giảng viên của lớp, admin | `{"exists": true}`, dùng cho Exercise Service |

## Chạy và test

```bash
docker compose up -d --build class-service        # từ thư mục gốc
cd services/class-service
docker run --rm -v "$PWD":/workspace -v nambede-m2:/root/.m2 -w /workspace \
  eclipse-temurin:17-jdk-noble ./mvnw -B -ntp verify
```
