# Lộ trình NamBeDe: 4 sprint

NamBeDe là hệ thống học tập SOA cho sinh viên Trường Đại học Quy Nhơn (gateway FastAPI, các service Spring Boot và FastAPI, PostgreSQL riêng cho từng service, RabbitMQ, web React, ứng dụng Android). Dự án được chia thành **4 sprint, mỗi sprint 2 tuần** (tổng 8 tuần). Sau mỗi sprint hệ thống **chạy được và demo được**; sprint sau chỉ thêm, không làm hỏng sprint trước.

## Nhóm

| Thành viên | Vai trò Scrum | Phụ trách chính |
|---|---|---|
| Nam | Team lead / Scrum Master, đồng thời lập trình | **Trưởng nhóm backend và Admin**: toàn bộ Auth Service (đăng ký / đăng nhập, JWT RS256 và JWKS, quản lý người dùng và hồ sơ), API Gateway, hạ tầng (Docker Compose, Makefile, `.env`, `deploy.py`), CI, RabbitMQ và outbox, các service mới (Class, Exercise, Learning, Notification: khung và hợp đồng API), tích hợp giữa các service, review mọi pull request; trang **Admin** (`/admin`) |
| Tùng | Developer | **Giáo viên và web**: trang Giáo viên (`/teacher`: tạo lớp, ngân hàng câu hỏi, giao bài, xem kết quả), khung web, đăng nhập / đăng ký / hồ sơ, route guard, test web, tài liệu; từ sprint 2 cài đặt các endpoint mà trang giáo viên cần (theo thiết kế của Nam, Nam review); từ sprint 3 làm cổng sinh viên và từ sprint 4 làm ứng dụng Android |

Nhóm chỉ có **hai người**, nên các tính năng của sinh viên (cổng `/student`, vào lớp, làm và nộp bài, luyện tập, từ vựng, xếp hạng, thông báo, ứng dụng Android) **không có ở Sprint 1 và Sprint 2** và được chia cho cả hai người ở Sprint 3 và 4. Trong thời gian đó sinh viên vẫn tự đăng ký và đăng nhập được, nhưng chỉ thấy trang thông báo `/coming-soon` ("Trang dành cho sinh viên sẽ có ở sprint sau"). Đến Sprint 3, `roleHome("STUDENT")` đổi thành `/student` và trang `/coming-soon` được gỡ.

Cách chia: Nam dẫn dắt backend, Tùng dẫn dắt web. Ở Sprint 1, Nam viết toàn bộ Auth Service, Gateway, hạ tầng và trang Admin; Tùng làm toàn bộ web còn lại, test và tài liệu. Từ sprint 2, mỗi service mới do Nam chốt thiết kế (cấu trúc dự án, hợp đồng API, migration đầu tiên), Tùng có thể cài đặt các endpoint mà trang của mình cần trong service đó, mỗi pull request đều do Nam review và phần dùng chung (gateway, hạ tầng, sự kiện, Auth Service) vẫn do Nam giữ. Các trang chung (đăng nhập, đăng ký, hồ sơ, route guard) do Tùng giữ. Địa chỉ web: `/admin`, `/teacher`, `/coming-soon` (Sprint 1 và 2), `/student` (từ Sprint 3); sau đăng nhập người dùng được chuyển theo vai trò.

Nghi thức Scrum mỗi sprint: Sprint Planning (ngày 1), Daily Scrum 15 phút, Sprint Review và Retrospective (ngày 10). Mỗi story có Definition of Done chung: code đã được người còn lại trong nhóm review, test tự động chạy qua, chạy được bằng `docker compose`, cập nhật tài liệu.

## Tổng quan

| Sprint | Chủ đề | Sản phẩm demo |
|---|---|---|
| 1 | Nền tảng và xác thực | Đăng ký, đăng nhập, token RS256 kiểm tra qua JWKS, gateway, web khung với `/admin`, `/teacher` và trang thông báo `/coming-soon` cho sinh viên |
| 2 | Class Service và Exercise Service (kèm ngân hàng câu hỏi) | Giảng viên tạo lớp, soạn câu hỏi, giao bài trên web; vào lớp, làm và nộp bài thử qua API (`curl` / `system_test.py`); admin xem lớp |
| 3 | Cổng sinh viên, Learning Service, Notification Service, sự kiện RabbitMQ | Sinh viên vào lớp, làm và nộp bài trên web (`/student`); luyện tập, từ vựng, xếp hạng tuần; điểm và thông báo đến qua sự kiện |
| 4 | Android cho sinh viên, CI đầy đủ, hoàn thiện | Ứng dụng Android (đăng nhập, lớp, bài tập, làm bài); pipeline CI và kiểm thử toàn hệ thống; tài liệu và giáo trình |

## Sprint 1: Nền tảng và xác thực (tuần 1 đến 2)

Chi tiết: [`SPRINT-1.md`](SPRINT-1.md). Đây là nhánh `sprint-1`.

- **Mục tiêu:** có một hệ thống nhỏ nhưng chạy thật từ đầu đến cuối: người dùng đăng ký, đăng nhập trên web và gọi được một API cần đăng nhập qua gateway.
- **Phạm vi:**
  - Hạ tầng: `docker-compose.yml` (PostgreSQL với `auth_db`, Auth Service, gateway, web), Makefile, `scripts/deploy.py`, `.env.example`, CI cho Auth Service, gateway, web.
  - Auth Service (Spring Boot): người dùng, đăng ký, đăng nhập, BCrypt, JWT RS256 và JWKS, khóa ký lưu trong database, `token_version` (`ver`) và `GET /auth/session`, tài khoản admin tạo sẵn, quản lý người dùng của admin, hồ sơ cá nhân.
  - Gateway (FastAPI): định tuyến `/api/auth`, `/api/users`, kiểm tra JWT bằng JWKS, kiểm tra phiên, `X-Request-ID`, Swagger chung.
  - Web (React): đăng nhập, đăng ký, chuyển hướng và bảo vệ route theo vai trò; `/admin` (Tổng quan, Người dùng, Tài khoản), `/teacher` (Tổng quan khung, Tài khoản, placeholder Lớp học / Bài tập), `/coming-soon` cho sinh viên (lời chào, thông báo "Trang dành cho sinh viên sẽ có ở sprint sau", Đăng xuất), khung giao diện sáng / tối. **Không có trang `/student`.**
- **Phân công chính:** Nam (64 giờ) giữ toàn bộ backend: Auth Service (đăng ký / đăng nhập, BCrypt, chặn đăng nhập sai, JWT RS256 + JWKS, `/auth/session`, API người dùng và hồ sơ, `AdminSeeder`, test, Dockerfile), Gateway và test gateway, hạ tầng, CI, `system_test.py`, cộng trang Admin, tích hợp và review. Tùng (63 giờ bắt buộc, 4 giờ tùy chọn): khung web, `AuthProvider`, `lib/api.ts`, route guard và test Vitest, `LoginPage` / `RegisterPage` / `ProfilePage`, trang Giáo viên, trang `/coming-soon` cho sinh viên, `demo_data.py`, `docs/HUONG-DAN.md`, `docs/API.md`, `web/readme.md`, lint và job `web` của CI.
- **Sản phẩm demo:** `make deploy`, đăng nhập ba tài khoản demo và thấy chuyển hướng `/admin`, `/teacher`, `/coming-soon`; đăng ký sinh viên mới rồi thấy trang thông báo `/coming-soon`; admin tạo giảng viên, khóa tài khoản và token cũ bị từ chối trong 10 giây; `curl` JWKS và endpoint có / không có token.

## Sprint 2: Class Service và Exercise Service (tuần 3 đến 4)

- **Mục tiêu:** hoàn thành backend của luồng lớp học và bài tập (từ lúc giảng viên tạo lớp đến lúc sinh viên nhận điểm) và toàn bộ phía giáo viên trên web. Phía sinh viên của luồng này chỉ có API ở sprint 2; trang web của sinh viên làm ở sprint 3.
- **Phạm vi:**
  - Class Service (Spring Boot, `class_db`): tạo lớp, mã lớp 6 ký tự, sinh viên tham gia và rời lớp, giảng viên xóa sinh viên khỏi lớp; lấy họ tên từ Auth Service qua `GET /users/lookup`.
  - Exercise Service (Spring Boot, `exercise_db`): giao bài cho lớp với **bản sao** câu hỏi, làm bài, lưu từng đáp án, nộp bài, chấm điểm thang 10, kết quả cho giảng viên (điểm từng sinh viên, tỉ lệ đúng từng câu); kiểm tra sinh viên thuộc lớp bằng REST sang Class Service.
  - Ngân hàng câu hỏi: Learning Service (FastAPI, `learning_db`) bắt đầu từ phần chủ đề và câu hỏi trắc nghiệm dùng chung (tạo, sửa, xóa, nhập từ Excel / CSV); Exercise Service lấy câu hỏi từ đây qua `GET /questions/{id}` để sao chép khi giao bài. Luyện tập, từ vựng, xếp hạng của Learning Service làm ở sprint 3.
  - Gateway: thêm tiền tố `/api/classes`, `/api/exercises`, `/api/submissions`, `/api/topics`, `/api/questions`, dashboard gộp dữ liệu (BFF) cho sinh viên; thêm database `class_db`, `exercise_db`, `learning_db` vào Compose.
  - Web: giáo viên (`/teacher/classes`, `/teacher/questions`, `/teacher/exercises`, kết quả); admin xem và xóa mọi lớp (`/admin/classes`). Tổng quan giáo viên và admin có số lớp và bài tập. Sinh viên vẫn ở `/coming-soon`; API tham gia lớp, làm bài, nộp bài được kiểm tra bằng `curl` và `scripts/system_test.py`.
- **Phân công chính (Nam dẫn dắt backend; Tùng cài đặt endpoint cho trang giáo viên dưới sự review của Nam):** Nam: khung và hợp đồng API của Class Service, Exercise Service, Learning Service, toàn bộ API phía sinh viên (tham gia lớp, làm bài, nộp bài, chấm điểm, `GET /users/lookup`, kiểm tra thành viên lớp), admin quản lý lớp, gateway (tiền tố mới, BFF), Compose và database mới, review, test tích hợp. Tùng: trang giáo viên tạo lớp, ngân hàng câu hỏi, giao bài, xem kết quả, cùng các endpoint tương ứng (tạo lớp, phần câu hỏi của Learning Service, giao bài, kết quả). Việc nhập câu hỏi từ Excel / CSV là việc kéo dài (stretch), lùi sang sprint 3 nếu trễ.
- **Sản phẩm demo:** giảng viên tạo lớp "Tiếng Anh 10A1" và giao bài trên web; sinh viên demo vào lớp bằng mã, làm và nộp bài qua API, thấy điểm và đáp án ngay; giảng viên xem kết quả trên web; sửa câu hỏi gốc không đổi điểm đã chấm.

## Sprint 3: Cổng sinh viên, Learning Service, Notification Service và sự kiện (tuần 5 đến 6)

- **Mục tiêu:** cổng web cho sinh viên, phần học tập của sinh viên và giao tiếp bất đồng bộ giữa các service. Đây là sprint nặng nhất với hai người, nên có nhiều việc đánh dấu tùy chọn.
- **Phạm vi:**
  - Learning Service (tiếp tục từ sprint 2): luyện tập ngẫu nhiên hoặc theo chủ đề (FREE 20 câu, VIP 50 câu, hỏi Auth Service qua `GET /users/{id}/account-type`), sổ từ vựng, chuỗi ngày học (streak), điểm hoạt động, bảng xếp hạng tuần.
  - Notification Service (FastAPI, `notification_db`): thông báo bài tập mới, nhắc bài sắp hết hạn; chỉ nhận sự kiện.
  - RabbitMQ và outbox: bảng `outbox_events` ở Auth Service và Exercise Service, `OutboxPublisher`, các sự kiện `exercise.assigned`, `exercise.submitted`, `user.deleted`, dead-letter queue. Nộp bài thì Learning Service cộng điểm qua sự kiện; xóa người dùng thì các service dọn dữ liệu.
  - Web: **cổng sinh viên `/student`** thay trang `/coming-soon` (`roleHome("STUDENT")` đổi thành `/student`): Tổng quan, `/student/classes` (vào lớp bằng mã), `/student/exercises` (làm và nộp bài), `/student/profile`, chuông thông báo; `/student/practice`, `/student/leaderboard`; `/student/vocabulary` (tùy chọn, lùi sang sprint 4 nếu trễ); giáo viên và admin xem bảng xếp hạng; admin theo dõi thông báo và sự kiện lỗi (tùy chọn).
- **Phân công chính:** Tùng: cổng sinh viên trên web (khung `/student`, Tổng quan, vào lớp, làm và nộp bài, hồ sơ, chuông thông báo) cùng các endpoint còn thiếu cho các trang này; phần giáo viên trong sự kiện (`exercise.assigned`, nhắc hạn, bảng xếp hạng lớp) và outbox ở Exercise Service, dưới sự review của Nam; gỡ `/coming-soon` và cập nhật route guard cùng test. Nam: Learning Service (luyện tập, từ vựng, chuỗi ngày học, xếp hạng) và trang `/student/practice`, `/student/leaderboard` (trang từ vựng là việc tùy chọn), Notification Service, thiết kế sự kiện và dead-letter queue, RabbitMQ trong Compose, outbox ở Auth Service, gateway, `X-Request-ID` đi qua sự kiện, review. Sự kiện `user.deleted` và trang admin theo dõi sự kiện lỗi là việc tùy chọn, lùi sang sprint 4 nếu trễ.
- **Sản phẩm demo:** sinh viên luyện tập và lên bảng xếp hạng; nộp bài thì điểm đến Learning Service không cần gọi lại; tắt Learning Service rồi nộp bài, bật lại thì điểm vẫn đến (nhờ outbox); chuông báo "Bài tập mới".

## Sprint 4: Android, CI đầy đủ và hoàn thiện (tuần 7 đến 8)

- **Mục tiêu:** đưa hệ thống đến trạng thái bàn giao: ứng dụng di động, kiểm thử tự động toàn hệ thống, tài liệu đầy đủ.
- **Phạm vi:**
  - Ứng dụng Android (Kotlin, Jetpack Compose, Retrofit) cho sinh viên: bắt buộc có đăng nhập, Tổng quan, lớp, bài tập và làm bài, Tài khoản; luyện tập, từ vựng, xếp hạng, thông báo là việc tùy chọn tùy thời gian; gọi cùng gateway và JWT; test đơn vị và ảnh chụp màn hình (Paparazzi); `make android`, `make smoke`.
  - CI đầy đủ trên GitLab: job cho mọi service, web, Android, và `system-test` chạy cả hệ thống bằng Docker-in-Docker (`scripts/system_test.py`, `scripts/demo_data.py` dữ liệu demo đầy đủ).
  - Hardening: giới hạn tần suất (rate limit) ở gateway, `X-Request-ID` đi xuyên cả sự kiện RabbitMQ, rà soát cổng và cấu hình, kiểm tra lỗi 502 khi service tắt; xem lại phần chặn đăng nhập sai và giới hạn mật khẩu 72 byte đã có từ sprint 1.
  - Tài liệu: `docs/API.md`, `docs/HUONG-DAN.md`, README từng service, giáo trình học SOA qua dự án (`docs/giao-trinh/`).
- **Phân công chính:** Tùng: ứng dụng Android cho sinh viên (đăng nhập, lớp, bài tập và làm bài, Tài khoản; sau đó luyện tập, từ vựng, xếp hạng, thông báo nếu còn thời gian) và hoàn thiện web giáo viên (`/teacher`). Nam: CI, system test, rate limit ở gateway, hardening Auth Service và các endpoint, trang admin hoàn thiện, màn hình luyện tập / xếp hạng trên Android nếu Tùng cần hỗ trợ, tài liệu, giáo trình, review.
- **Sản phẩm demo:** `make deploy` dựng cả hệ thống và mở app trên máy ảo; sinh viên làm bài trên Android, điểm hiện trên web; pipeline CI xanh; bảo vệ đồ án với kịch bản demo trong `docs/HUONG-DAN.md`.

## Rủi ro chung

| Rủi ro | Cách giảm |
|---|---|
| Sprint 3 nhiều việc nhất (cổng sinh viên, hai service mới và sự kiện) với chỉ hai người | Làm cổng sinh viên và Learning Service trước, Notification Service sau; trang từ vựng, sự kiện `user.deleted` và trang admin theo dõi có thể lùi sang sprint 4 |
| Nhóm chỉ có hai người, một người nghỉ thì sprint trễ | Mỗi sprint ghi rõ việc tùy chọn để cắt; Nam và Tùng review chéo để người kia nắm được phần của mình |
| Android phụ thuộc API ổn định | API sinh viên đã chốt từ sprint 2 và 3; Android dùng chung gateway với web |
| Build chậm trên máy yếu | Đặt `SKIP_TESTS=true` khi build Docker, CI chạy test riêng |
