# Tài liệu API NamBeDe

Tài liệu này mô tả API của **sprint 1** (nền tảng và xác thực) mà Web dùng; các sprint sau thêm lớp học, bài tập, luyện tập... vào đây (xem [`docs/scrum/ROADMAP.md`](scrum/ROADMAP.md)). Mọi lời gọi đều đi qua **API Gateway**; client không bao giờ gọi thẳng một service.

| Cách gọi | Địa chỉ gốc |
|---|---|
| Trình duyệt, cùng domain với web | `http://nambede.dev.localhost/api` |
| Gọi thẳng gateway trên máy chạy Docker | `http://localhost:8000/api` |

Trong tài liệu, đường dẫn được viết **sau** `/api`. Ví dụ `GET /users/me` nghĩa là `GET http://nambede.dev.localhost/api/users/me`.

Thử trực tiếp: **Swagger chung** ở `http://localhost:8000/docs`. Menu phía trên chọn gateway hoặc Auth Service; đăng nhập bằng `POST /auth/login` ở mục Auth Service, chép `accessToken`, bấm **Authorize**. Mọi lần gọi thử đi qua gateway như web. Swagger được sinh từ code nên luôn khớp API thật; tài liệu này bổ sung ví dụ JSON và quy tắc nghiệp vụ.

## Mục lục

1. [Quy ước chung](#1-quy-ước-chung)
2. [Gateway: sức khỏe, định tuyến](#2-gateway)
3. [Auth Service: đăng nhập, người dùng](#3-auth-service)
4. [Lời gọi giữa các thành phần](#4-lời-gọi-giữa-các-thành-phần)
5. [Ví dụ một phiên làm việc bằng curl](#5-ví-dụ-một-phiên-làm-việc-bằng-curl)

---

## 1. Quy ước chung

### Xác thực bằng JWT

- Đăng nhập bằng `POST /auth/login`, nhận `accessToken`. Các lời gọi sau gửi header `Authorization: Bearer <accessToken>`.
- Token do Auth Service ký bằng **RS256** (header có `kid`), hết hạn sau `JWT_EXPIRATION_MINUTES` phút (mặc định 1440, tức 24 giờ). Khóa công khai để kiểm tra ở `GET http://localhost:8081/.well-known/jwks.json` (JWK Set; gọi thẳng Auth Service, không qua gateway).
- Token có claim `ver`. Admin khóa tài khoản hoặc đổi vai trò thì mọi token đã phát của người đó bị gateway từ chối trong vòng 10 giây: 401 với `message` là `"Account is locked"`, `"Session ended, sign in again"` hoặc `"This account no longer exists"`. Gateway hỏi `GET /auth/session` để biết (mục 3).
- Nội dung token: `sub` (id người dùng, dạng chuỗi), `email`, `name`, `role`, `iss` = `nambede-auth`, `iat`, `exp`.
- Gateway kiểm tra token cho mọi đường dẫn trừ `/auth/*` và `/health`. Auth Service kiểm tra lại token, nên gọi thẳng service mà không có token cũng bị từ chối.

### Vai trò

| Vai trò | Ý nghĩa | Tạo bằng cách nào |
|---|---|---|
| `ADMIN` | Quản trị viên | Tài khoản đầu tiên tạo từ `ADMIN_EMAIL`, `ADMIN_PASSWORD`; các admin khác do admin tạo |
| `TEACHER` | Giảng viên | Admin tạo (`POST /users`) |
| `STUDENT` | Sinh viên | Tự đăng ký (`POST /auth/register`) hoặc admin tạo. Ở Sprint 1 web chưa có trang sinh viên: sinh viên đăng nhập xong chỉ thấy trang `/coming-soon`; API không đổi |

Mọi tài khoản có gói `FREE` hoặc `VIP` (admin đổi; gói được dùng ở các sprint sau). Tài khoản có trạng thái `ACTIVE` hoặc `LOCKED` (bị khóa thì không đăng nhập được).

### Dữ liệu

- Body và kết quả là JSON, tên trường kiểu camelCase (`fullName`, `correctOption`).
- Thời gian theo chuẩn ISO 8601, múi giờ UTC, ví dụ `2026-10-08T23:59:00Z`.
- 
### Lỗi

Auth Service và gateway trả lỗi cùng một dạng:

```json
{"status": 403, "error": "Forbidden", "message": "You do not have permission to perform this action", "path": "/users"}
```

| Mã | Khi nào |
|---|---|
| 400 | Dữ liệu gửi lên sai, `message` ghi trường bị sai, ví dụ `"newPassword: must be at most 72 bytes; an accented letter takes 2 to 3 bytes"` |
| 401 | Thiếu token, token sai hoặc hết hạn; đăng nhập sai email hoặc mật khẩu |
| 403 | Đúng người nhưng không đủ quyền: sai vai trò, tài khoản bị khóa |
| 404 | Không có dữ liệu với id đó |
| 409 | Xung đột: email đã dùng |
| 429 | Đăng nhập sai quá nhiều lần với một email; header `Retry-After` ghi số giây phải chờ |
| 502 | Gateway không gọi được service phía sau, ví dụ `"auth-service is unavailable"` |
| 500 | Lỗi không lường trước, `message` luôn là `"Internal server error"` |

---

## 2. Gateway

FastAPI, cổng `8000`, không có database. Swagger chung: `http://localhost:8000/docs` (cũng mở được ở `/api/docs`); bản mô tả API của gateway: `/api/openapi.json`, của Auth Service qua gateway: `/api/docs/<tên service>.json`.

### Định tuyến

Gateway chuyển nguyên request (method, đường dẫn, query, body, header `Authorization`) tới service phụ trách và trả nguyên kết quả về.

| Tiền tố | Service | Cần token ở gateway |
|---|---|---|
| `/auth/*` | auth-service | không |
| `/users/*` | auth-service | có |

### Mã request: `X-Request-ID`

Mỗi request qua gateway có một mã. Client gửi kèm header `X-Request-ID` (chữ, số, `.`, `_`, `-`, tối đa 64 ký tự) thì gateway dùng mã đó, không thì tự tạo (32 ký tự hex). Mã được trả lại trong header `X-Request-ID` của câu trả lời (kể cả lỗi), được chuyển sang Auth Service. Gateway và Auth Service ghi mã này trên từng dòng log, nên khi gặp lỗi chỉ cần mã đó là tìm được toàn bộ đường đi: `docker compose logs | grep <mã>`.

### `GET /health`

Công khai. Gateway còn sống.

```json
{"status": "UP", "service": "gateway"}
```

---

## 3. Auth Service

Spring Boot, cổng `8081`, database `auth_db` (bảng `users`, `signing_keys`).

| Method | Path | Ai được gọi | Mô tả |
|---|---|---|---|
| POST | `/auth/login` | công khai | Đăng nhập |
| POST | `/auth/register` | công khai | Sinh viên tự đăng ký |
| GET | `/users/me` | đã đăng nhập | Thông tin của mình |
| PUT | `/users/me` | đã đăng nhập | Đổi họ tên của mình |
| PUT | `/users/me/password` | đã đăng nhập | Đổi mật khẩu của mình |
| GET | `/users?role=` | ADMIN | Danh sách người dùng, lọc theo vai trò |
| POST | `/users` | ADMIN | Tạo tài khoản với vai trò bất kỳ |
| GET | `/users/{id}` | đã đăng nhập | Một người dùng |
| GET | `/users/lookup?ids=1,2,3` | đã đăng nhập | Nhiều người dùng một lần (tối đa 500 id) |
| GET | `/users/{id}/account-type` | đã đăng nhập | Gói FREE / VIP |
| PUT | `/users/{id}` | ADMIN | Đổi họ tên |
| PUT | `/users/{id}/role` | ADMIN | Đổi vai trò (không đổi được của chính mình) |
| PUT | `/users/{id}/status` | ADMIN | Khóa, mở khóa (không tự khóa mình) |
| PUT | `/users/{id}/account-type` | ADMIN | Chuyển FREE / VIP |
| DELETE | `/users/{id}` | ADMIN | Xóa tài khoản (không tự xóa mình), trả 204 |

### `POST /auth/login`

```json
{"email": "student1@nambede.local", "password": "Student@123"}
```

Kết quả:

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ind…",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {"id": 2, "email": "student1@nambede.local", "fullName": "Nguyễn Văn A", "role": "STUDENT",
           "status": "ACTIVE", "accountType": "FREE", "createdAt": "2026-10-01T05:34:26.669320Z"}
}
```

Lỗi: 401 `"Invalid email or password"`, 403 `"Account is locked"`. Sai mật khẩu 5 lần trong 5 phút với cùng một email thì email đó bị chặn đăng nhập (kể cả khi nhập đúng) tới khi lần sai đầu tiên cũ hơn 5 phút: **429** `"Too many failed sign-ins, try again in 240 seconds"` kèm header `Retry-After: 240`. Đăng nhập đúng thì đếm lại từ đầu. Đếm theo email, không theo IP, vì sau nginx và Docker gần như mọi client tới từ cùng một địa chỉ.

### `GET /auth/session`

Header `Authorization: Bearer <token>`. Trả **204** khi token còn dùng được: chữ ký đúng, chưa hết hạn, người dùng còn tồn tại, không bị khóa, và `ver` trong token bằng `token_version` hiện tại. Không thì **401** kèm lý do: `"Invalid or expired token"`, `"This account no longer exists"`, `"Account is locked"`, `"Session ended, sign in again"`. Gateway gọi API này cho mỗi request có token và giữ câu trả lời 204 trong 10 giây cho mỗi người dùng.

### `GET /.well-known/jwks.json`

Công khai, không cần token, gọi thẳng Auth Service (cổng 8081). Trả khóa công khai RS256 dạng JWK Set, gateway dùng để kiểm tra chữ ký:

```json
{"keys": [{"kty": "RSA", "use": "sig", "alg": "RS256", "kid": "wR3s0Qd8y5Zx1kLm", "n": "u1Sx...", "e": "AQAB"}]}
```

### `POST /auth/register`

```json
{"email": "hs1@nambede.local", "password": "Student@123", "fullName": "Nguyễn Văn A"}
```

Trả 201, cùng dạng với đăng nhập. Tài khoản tạo ra luôn là `STUDENT`. Mật khẩu từ 6 ký tự đến 72 **byte** (giới hạn của bcrypt; một chữ có dấu chiếm 2 đến 3 byte, nên mật khẩu tiếng Việt có dấu ngắn hơn 72 ký tự vẫn có thể bị từ chối với 400). Email đã có thì 409.

### Thông tin người dùng

`GET /users/me`, `GET /users/{id}` và các lệnh của admin trả về:

```json
{"id": 2, "email": "student1@nambede.local", "fullName": "Nguyễn Văn A", "role": "STUDENT",
 "status": "ACTIVE", "accountType": "FREE", "createdAt": "2026-10-01T05:34:26.669320Z"}
```

`GET /users/{id}/account-type`:

```json
{"userId": 2, "accountType": "FREE"}
```

### Tự đổi họ tên và mật khẩu

- `PUT /users/me` với `{"fullName": "Nguyễn Văn An"}` trả về thông tin mới.
- `PUT /users/me/password` với `{"currentPassword": "Student@123", "newPassword": "Moi@2026"}` trả 204. Sai mật khẩu hiện tại thì **403** (không phải 401, để phiên đăng nhập không bị xóa). Mật khẩu mới trùng mật khẩu cũ thì 400.

### Lệnh của admin

| Lệnh | Body |
|---|---|
| `POST /users` | `{"email", "password", "fullName", "role": "TEACHER"}` |
| `PUT /users/{id}` | `{"fullName"}` |
| `PUT /users/{id}/role` | `{"role": "ADMIN" \| "TEACHER" \| "STUDENT"}` |
| `PUT /users/{id}/status` | `{"status": "ACTIVE" \| "LOCKED"}` |
| `PUT /users/{id}/account-type` | `{"accountType": "FREE" \| "VIP"}` |

---

## 4. Lời gọi giữa các thành phần

Service không đọc database của nhau; cần dữ liệu thì gọi REST API của service sở hữu.

| Từ | Tới | Lời gọi | Để làm gì | Khi Auth Service lỗi |
|---|---|---|---|---|
| Gateway | Auth Service | `/auth/*`, `/users/*` | Chuyển request của client (xem mục 2) | 502 |
| Gateway | Auth Service | `GET /auth/session` | Phiên còn mở không; câu trả lời 204 được giữ 10 giây mỗi người dùng | Cho qua theo chữ ký |
| Gateway | Auth Service | `GET /.well-known/jwks.json` | Khóa công khai RS256; đọc một lần, đọc lại khi gặp `kid` mới | Dùng khóa đã có; chưa có khóa nào thì 502 |

---

## 5. Ví dụ một phiên làm việc bằng curl

```bash
G=http://localhost:8000/api

# Khóa công khai RS256 (gọi thẳng Auth Service)
curl -s http://localhost:8081/.well-known/jwks.json

# Đăng ký rồi đăng nhập
curl -s -X POST $G/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"hs1@nambede.local","password":"Student@123","fullName":"Nguyễn Văn A"}'
TOKEN=$(curl -s -X POST $G/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"student1@nambede.local","password":"Student@123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

curl -s -i $G/users/me                                          # không có token: 401
curl -s $G/users/me -H "Authorization: Bearer $TOKEN"           # có token: 200 và thông tin của mình
curl -s -D - -o /dev/null $G/users/me -H "Authorization: Bearer $TOKEN" | grep -i x-request-id  # mã để tìm trong log
```

(Tài khoản `student1@nambede.local` do `make seed` tạo; xem [`docs/HUONG-DAN.md`](HUONG-DAN.md).)

