# Hướng dẫn NamBeDe (Sprint 1)

NamBeDe là hệ thống học tập cho sinh viên Trường Đại học Quy Nhơn, xây dựng theo kiến trúc **SOA (Service-Oriented Architecture)**. Dự án được thực hiện theo Scrum, 4 sprint ([`docs/scrum/ROADMAP.md`](scrum/ROADMAP.md)). Nhánh này là kết quả của **Sprint 1: nền tảng và xác thực** ([`docs/scrum/SPRINT-1.md`](scrum/SPRINT-1.md)): đăng ký, đăng nhập, JWT RS256 với JWKS, API Gateway, giao diện khung và hạ tầng Docker. Lớp học, bài tập, luyện tập... được thêm ở các sprint sau.

Tài liệu này hướng dẫn cài đặt, chạy, demo và kiểm thử những gì Sprint 1 đã có. Danh sách API nằm ở [`docs/API.md`](API.md). Nhóm có 2 người: Nam giữ toàn bộ backend (Auth Service, gateway, hạ tầng, CI) và trang Admin; Tùng làm toàn bộ web còn lại, test và tài liệu (hướng dẫn này, `docs/API.md`). **Sprint 1 chưa có trang sinh viên**: sinh viên đăng ký, đăng nhập được nhưng chỉ thấy trang thông báo `/coming-soon`; trang sinh viên được làm ở Sprint 3.

## Mục lục

1. [Chức năng](#1-chức-năng)
2. [Kiến trúc](#2-kiến-trúc)
3. [Cài đặt và chạy](#3-cài-đặt-và-chạy)
4. [Vai trò và quyền](#4-vai-trò-và-quyền)
5. [Kịch bản demo trên web](#5-kịch-bản-demo-trên-web)
6. [Kiểm thử](#6-kiểm-thử)
7. [Cấu trúc thư mục](#7-cấu-trúc-thư-mục)
8. [Xử lý sự cố](#8-xử-lý-sự-cố)

---

## 1. Chức năng

| Vai trò | Làm được gì ở Sprint 1 |
|---|---|
| Quản trị viên | Đăng nhập, vào `/admin`; xem số người dùng trên trang Tổng quan; `/admin/users`: tạo tài khoản giảng viên, admin; khóa, mở khóa, xóa tài khoản; đổi vai trò, chuyển gói FREE / VIP |
| Giảng viên | Đăng nhập bằng tài khoản do admin tạo, vào `/teacher`; xem trang Tổng quan khung, hồ sơ; Lớp học, Bài tập là placeholder "Sắp có ở Sprint 2" |
| Sinh viên | Tự đăng ký; đăng nhập, vào `/coming-soon`: thấy "Xin chào <tên>", thông báo "Trang dành cho sinh viên sẽ có ở sprint sau. Tài khoản của bạn đã được tạo thành công." và nút Đăng xuất. Chưa có trang tổng quan, hồ sơ, lớp học hay bài tập |
| Mọi người | Chuyển hướng theo vai trò sau đăng nhập (`/admin`, `/teacher`, `/coming-soon` cho sinh viên), mở trang của vai trò khác thì bị đưa về trang của mình; admin và giảng viên đổi họ tên, đổi mật khẩu của mình; giao diện sáng / tối, dùng tốt trên điện thoại |

---

## 2. Kiến trúc

```text
          Web (React)
   http://nambede.dev.localhost
               │
               ▼
     ┌─────────────────────┐
     │   FastAPI Gateway   │  :8000  kiểm tra JWT (JWKS), hỏi phiên còn mở không, định tuyến, X-Request-ID
     └──────────┬──────────┘
                ▼
          Auth Service            Spring Boot :8081
                │
             auth_db              PostgreSQL 17 :5432
```

| Thành phần | Công nghệ | Cổng | Database | Phụ trách |
|---|---|---|---|---|
| Web | React 19, Vite, Tailwind CSS 4, shadcn/ui; nginx | 80 | | Giao diện cho mọi vai trò |
| Gateway | FastAPI (Python 3.12) | 8000 | | Điểm vào duy nhất: kiểm tra JWT, định tuyến |
| Auth Service | Spring Boot 4 (Java 17) | 8081 | `auth_db` | Người dùng, đăng nhập, JWT RS256 và JWKS, vai trò, trạng thái, gói FREE / VIP |
| PostgreSQL | PostgreSQL 17 | 5432 | `auth_db` | Một máy chủ, mỗi service một database riêng |

### Nguyên tắc

- **Client chỉ gọi gateway.** Web gọi `/api/...`; gateway chuyển tới service phụ trách.
- **Mỗi service một database.** Không service nào đọc bảng của service khác. Cần dữ liệu thì gọi REST API của service sở hữu.
- **Một JWT, chỉ một nơi ký.** Auth Service ký token RS256 bằng khóa riêng (tự tạo lần đầu, lưu trong bảng `signing_keys` của `auth_db`). Gateway kiểm tra bằng khóa công khai lấy ở `GET /.well-known/jwks.json`. Token mang claim `ver`; gateway hỏi Auth Service `GET /auth/session` phiên còn mở không (lưu đệm 10 giây), nên khóa tài khoản có hiệu lực sau vài giây.
- **Mật khẩu** băm bằng BCrypt; đăng nhập sai 5 lần trong 5 phút thì bị chặn một lúc (429).
- **Một dạng lỗi:** `{"status", "error", "message", "path"}`. Service không truy cập được thì gateway trả 502.
- **Lần theo một request:** gateway gán mã `X-Request-ID`, mã đi sang Auth Service và có trên từng dòng log: `docker compose logs | grep <mã>`.

---

## 3. Cài đặt và chạy

### Yêu cầu

Lệnh `install` ở mục ngay dưới tự cài những gì còn thiếu. Để biết máy cần gì:

- **Docker**: Docker Desktop trên Windows và macOS; Docker Engine có `docker compose` trên Linux. Java, Maven, Node đều chạy bên trong container. Nên dành cho Docker từ 4 GB RAM.
- **Python 3.9 trở lên** cho các lệnh cài đặt và script tài khoản demo. macOS có sẵn `python3`, Linux thường có sẵn, Windows được `scripts\install.cmd` cài.
- Không bắt buộc, để sửa code: JDK 17 (chạy Auth Service ngoài Docker), Node 24 (giao diện web có hot reload), `uv` (chạy gateway và test Python ngoài Docker).

Tài khoản demo (`make seed` hoặc `make deploy` tạo; admin có sẵn):

| Vai trò | Tài khoản | Mật khẩu | Sau đăng nhập |
|---|---|---|---|
| Quản trị viên | `admin@nambede.local` | `Admin@123` | `/admin` |
| Giảng viên | `teacher1@nambede.local` | `Teacher@123` | `/teacher` |
| Sinh viên | `student1@nambede.local` | `Student@123` | `/coming-soon` |

---

## 4. Vai trò và quyền

| Vai trò | Khu vực web | Trang chủ | Ghi chú |
|---|---|---|---|
| `ADMIN` | `/admin`, `/admin/users`, `/admin/profile` | `/admin` | Quản lý người dùng; API `/users` chỉ admin gọi được |
| `TEACHER` | `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises` | `/teacher` | Lớp học, Bài tập là placeholder "Sắp có ở Sprint 2" |
| `STUDENT` | `/coming-soon` | `/coming-soon` | Chỉ có trang thông báo và nút Đăng xuất; không có hồ sơ |

- Chưa đăng nhập mở trang cần đăng nhập thì về `/login`.
- Đăng nhập rồi mà mở khu vực của vai trò khác thì được đưa về trang chủ của mình: sinh viên mở `/admin/users` hoặc `/teacher` về `/coming-soon`; giảng viên mở `/coming-soon` về `/teacher`; admin mở `/coming-soon` về `/admin`.
- Đường dẫn không tồn tại (kể cả `/student`, chưa có ở Sprint 1) hiện trang 404 với nút về đúng trang chủ.
- API: sinh viên gọi `GET /users` bị 403.

---

## 5. Kịch bản demo trên web

Mở http://nambede.dev.localhost và làm lần lượt:

1. **Sinh viên đăng ký:** `/register`, nhập họ tên, email, mật khẩu. Đăng ký xong vào luôn `/coming-soon`: lời chào, thông báo trang sinh viên sẽ có ở sprint sau, nút Đăng xuất. Thử mở `/admin/users` hoặc `/teacher`: bị đưa về `/coming-soon`. Bấm Đăng xuất để về `/login`.
2. **Admin:** đăng nhập `admin@nambede.local`, vào `/admin` xem số người dùng; `/admin/users`: tạo một giảng viên mới, khóa tài khoản sinh viên vừa đăng ký (đăng nhập tài khoản đó lại sẽ bị từ chối).
3. **Giảng viên:** đăng nhập `teacher1@nambede.local`, vào `/teacher`, xem ba thẻ Sprint 2, mở Lớp học và Bài tập (placeholder), "Tài khoản của tôi" để đổi họ tên hoặc mật khẩu. Mở `/coming-soon` sẽ về `/teacher`.
4. **Token và gateway:** chạy các lệnh `curl` ở [`docs/API.md`](API.md) hoặc xem README: không token 401, có token 200, sinh viên gọi `GET /users` 403, tắt Auth Service thì 502.

---

## 6. Kiểm thử

| Việc | Lệnh |
|---|---|
| Test Auth Service | `cd services/auth-service && ./mvnw verify` |
| Test gateway | `cd services/gateway && uv run pytest` |
| Test web | `cd web && npm test`, `npm run lint`, `npm run build` |
| Toàn hệ thống (đang chạy) | `python3 scripts/system_test.py` |

Test web gồm chuyển hướng theo vai trò (STUDENT về `/coming-soon`, TEACHER về `/teacher`, ADMIN về `/admin`, không vòng lặp) và trang `/coming-soon` (thông báo, nút Đăng xuất).

---

## 7. Cấu trúc thư mục

| Thư mục | Nội dung |
|---|---|
| `services/auth-service` | Auth Service (Spring Boot) |
| `services/gateway` | API Gateway (FastAPI) |
| `web` | Giao diện React (xem `web/readme.md`) |
| `infra/postgres/init` | Tạo database khi PostgreSQL khởi động lần đầu |
| `scripts` | `deploy.py`, `install.cmd`, `demo_data.py`, `system_test.py` |
| `docs` | Tài liệu: `API.md`, `HUONG-DAN.md`, `scrum/` (Sprint 1 và lộ trình) |

---

## 8. Xử lý sự cố

| Triệu chứng | Cách xử lý |
|---|---|
| Cổng 80 bận | Đổi `WEB_PORT` trong `.env` rồi chạy lại `docker compose up -d` |
| Container không "healthy" | `docker compose logs <service>`; Docker nên có từ 4 GB RAM |
| Đăng nhập báo 429 | Sai mật khẩu 5 lần trong 5 phút; chờ theo `Retry-After` rồi thử lại |
| Đã đăng nhập nhưng bị đưa về trang khác | Đúng thiết kế: mỗi vai trò chỉ mở khu vực của mình (mục 4) |
| Đăng ký xong chỉ thấy trang thông báo | Đúng thiết kế ở Sprint 1: sinh viên chưa có trang riêng |
| Muốn xóa sạch dữ liệu | `docker compose down -v` |
