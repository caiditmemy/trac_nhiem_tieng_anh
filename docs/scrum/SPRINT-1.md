# Sprint 1: Nền tảng và xác thực

| | |
|---|---|
| Thời gian | 2 tuần (10 ngày làm việc) |
| Nhóm | 2 người: Nam (Team lead / Scrum Master, đồng thời lập trình) và Tùng |
| Nhánh | `sprint-1` |
| Lộ trình | [`ROADMAP.md`](ROADMAP.md) |

## Ai làm trang nào

Sprint 1 có **hai người**. **Nam (team lead / Scrum Master) giữ toàn bộ backend** (Auth Service, Gateway, hạ tầng, CI) cùng các trang Admin, tích hợp và review. **Tùng làm toàn bộ phần web còn lại**: trang Giáo viên, đăng nhập / đăng ký / hồ sơ, khung web, phiên đăng nhập và route guard, test web, tài liệu. Tùng không viết code Auth Service mà làm việc với API của Nam qua dữ liệu giả (mock) cho đến khi API sẵn sàng. Mỗi người khoảng 64 đến 67 giờ trong 10 ngày làm việc (6 đến 7 giờ mỗi ngày): nặng nhưng làm được. Các việc đánh dấu "tùy chọn" là phần dư, bỏ được nếu sprint trễ.

**Trang sinh viên không có trong Sprint 1.** Sinh viên vẫn tự đăng ký và đăng nhập được (backend không đổi, đăng ký luôn tạo `STUDENT`), nhưng chỉ thấy một trang thông báo `/coming-soon` ("Trang dành cho sinh viên sẽ có ở sprint sau. Tài khoản của bạn đã được tạo thành công.") kèm nút Đăng xuất. Cổng sinh viên `/student` nằm ở Sprint 3 (xem [`ROADMAP.md`](ROADMAP.md)).

| Thành viên | Vai trò trong nhóm | Khu vực trang | Phần chung | Giờ |
|---|---|---|---|---|
| Nam | Team lead / Scrum Master | **Admin**: `/admin`, `/admin/users`, `/admin/profile` (`AppShell` thanh bên, `AdminDashboardPage`, `UsersPage`) | **Toàn bộ Auth Service**: đăng ký / đăng nhập, BCrypt, `LoginAttempts`, RS256 + JWKS (`signing_keys`, `JwtKeys`, `JwtService`, `JwksController`), `ver` + `GET /auth/session`, API người dùng và hồ sơ (`UserController`, `UserService`), `AdminSeeder`, `GlobalExceptionHandler`, `RequestIdFilter`, `OpenApiConfig`, test đơn vị và Dockerfile của service; Gateway và test gateway (`pytest`); hạ tầng (Compose, Makefile, `.env`, `deploy.py`); CI; `scripts/system_test.py`; tích hợp, review, Scrum | 64 |
| Tùng | Developer | **Giáo viên**: `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises` (placeholder); `PortalShell` / `PortalNav`. **Sinh viên (tạm)**: `/coming-soon` | Trang chung `LoginPage`, `RegisterPage`, `AuthShell`, `PasswordInput`, `ProfilePage`; khởi tạo dự án web (Vite, Tailwind, shadcn/ui, `ThemeProvider`); `ComingSoonPage` / `ComingSoonCards` và trang `/coming-soon` cho sinh viên; `AuthProvider`, `lib/api.ts`, `lib/roles.ts`, `HomeRedirect`, `RequireAuth` và test Vitest, `NotFoundPage`; lint web và job `web` của CI; Dockerfile và `nginx.conf` của web; `demo_data.py`; `docs/HUONG-DAN.md`, `docs/API.md`, `web/readme.md` | 67 (63 + 4 tùy chọn) |

## Sprint goal

> Cuối sprint, một người dùng có thể **đăng ký, đăng nhập trên web và được chuyển đến đúng trang theo vai trò** (ADMIN về `/admin`, TEACHER về `/teacher`, STUDENT về trang thông báo `/coming-soon`), gọi một API cần đăng nhập qua gateway; token được ký RS256 bởi Auth Service và gateway kiểm tra bằng khóa công khai (JWKS). Mở trang của vai trò khác thì bị đưa về trang của mình. Cả hệ thống dựng được bằng một lệnh `docker compose` và có CI.

Ngoài phạm vi sprint này (làm ở các sprint sau): mọi trang của sinh viên (cổng `/student`, vào lớp, làm và nộp bài, luyện tập, từ vựng, xếp hạng), lớp học và bài tập của giáo viên, thông báo, RabbitMQ, Android. Các trang `classes`, `exercises` của giáo viên chỉ là placeholder "Sắp có ở Sprint 2".

## Các trang theo vai trò

| Khu vực | Đường dẫn | Bố cục | Nội dung ở Sprint 1 |
|---|---|---|---|
| Chung (công khai) | `/login`, `/register` | `AuthShell` | Đăng nhập, sinh viên tự đăng ký; sau đó chuyển theo vai trò. `/` cũng chuyển theo vai trò (chưa đăng nhập thì về `/login`) |
| Admin (`ADMIN`) | `/admin`, `/admin/users`, `/admin/profile` | `AppShell` (thanh bên) | Tổng quan có số người dùng, quản lý người dùng, hồ sơ |
| Giáo viên (`TEACHER`) | `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises` | `PortalShell` (thanh trên) | Lời chào, vai trò, thẻ "Lớp học", "Bài tập", "Ngân hàng câu hỏi" (Sprint 2); hồ sơ; hai trang placeholder |
| Sinh viên (`STUDENT`) | `/coming-soon` | Kiểu `AuthShell`, không có thanh điều hướng | "Xin chào <tên>", thông báo trang sinh viên sẽ có ở sprint sau, nút Đăng xuất, nút đổi giao diện sáng / tối. Không có hồ sơ hay trang nào khác |

Quy tắc: mỗi khu vực được bảo vệ bằng `RequireAuth roles={[...]}`. Chưa đăng nhập thì về `/login`; đăng nhập rồi mà mở khu vực của vai trò khác thì được chuyển về trang chủ của vai trò mình (`roleHome(role)`: `/admin`, `/teacher`, `/coming-soon`). Trang chủ của mỗi vai trò luôn là trang vai trò đó được phép mở nên không có vòng chuyển hướng. Sau đăng nhập, trang đã nhớ (hết phiên) chỉ được dùng lại nếu nằm trong khu vực của người dùng.

## Product backlog của sprint

Điểm story theo thang Fibonacci (1 điểm khoảng nửa ngày làm việc của một người). **Tổng 45 điểm** (5 + 5 + 8 + 8 + 8 + 5 + 3 + 3), nhóm 2 người trong 10 ngày.

| Mã | User story | Tiêu chí chấp nhận | Điểm | Người làm chính |
|---|---|---|---|---|
| US-01 | Là người dùng, tôi muốn **đăng nhập và được chuyển đến đúng trang theo vai trò** (chung) | `POST /auth/login` trả `accessToken`; sai mật khẩu 401; tài khoản khóa 403; sai 5 lần trong 5 phút thì 429 kèm `Retry-After`; trang đăng nhập hiện lỗi tiếng Việt; sau đăng nhập và ở `/`: ADMIN về `/admin`, TEACHER về `/teacher`, STUDENT về `/coming-soon` | 5 | Nam (API), Tùng (trang, `AuthProvider`, `HomeRedirect`) |
| US-02 | Là sinh viên, tôi muốn **tự đăng ký** tài khoản để có tài khoản trong hệ thống | `POST /auth/register` luôn tạo `STUDENT`; email trùng 409; mật khẩu 6 ký tự đến 72 byte, băm BCrypt; đăng ký xong đăng nhập luôn và vào `/coming-soon`, thấy "Xin chào <tên>", thông báo trang sinh viên sẽ có ở sprint sau và nút Đăng xuất; không có đường dẫn `/student` | 5 | Nam (API), Tùng (trang đăng ký, trang `/coming-soon`) |
| US-03 | Là hệ thống, tôi muốn token **ký RS256 và công bố JWKS** để service khác kiểm tra mà không giữ khóa bí mật | Khóa RSA tự tạo lần đầu, lưu bảng `signing_keys`; `GET /.well-known/jwks.json` trả khóa công khai; token có `kid`, `sub`, `role`, `ver`; không có `JWT_SECRET` | 8 | Nam |
| US-04 | Là hệ thống, tôi muốn **gateway xác thực** mọi request để service phía sau chỉ nhận request hợp lệ | Gateway đọc JWKS, từ chối 401 khi thiếu / sai / hết hạn; hỏi `GET /auth/session` (lưu đệm 10 giây) nên khóa tài khoản có hiệu lực ngay; 502 khi Auth Service tắt; `X-Request-ID`; có test `pytest` | 8 | Nam |
| US-05 | Là nhóm phát triển, chúng tôi muốn **hạ tầng docker-compose và CI** để hai người chạy cùng một hệ thống và phát hiện lỗi sớm | `docker compose up -d --build --wait` dựng PostgreSQL (`auth_db`), Auth Service, gateway, web, tất cả healthy; Makefile, `scripts/deploy.py`, `.env.example`; `.gitlab-ci.yml` có job `auth-service`, `gateway`, `web`, `system-test`; `scripts/system_test.py` qua | 8 | Nam (chính), Tùng (Dockerfile và `nginx.conf` của web, lint và job `web`) |
| US-06 | Là admin, tôi muốn **trang tổng quan và quản lý người dùng** để cấp tài khoản giáo viên và khóa tài khoản vi phạm | `/admin` có số người dùng, giáo viên, sinh viên; tài khoản admin tạo sẵn (`AdminSeeder`); `/admin/users`: tạo, khóa / mở khóa, đổi vai trò, gói FREE / VIP, xóa; khóa hay đổi vai trò thì `ver` tăng, token cũ bị từ chối; `/admin/profile` | 5 | Nam |
| US-07 | Là giáo viên, tôi muốn **trang tổng quan và hồ sơ** của mình | `/teacher` có lời chào, vai trò, thẻ "Lớp học", "Bài tập", "Ngân hàng câu hỏi" ghi Sprint 2; `/teacher/profile` đổi họ tên, đổi mật khẩu (sai mật khẩu cũ 403, không bị đăng xuất); `/teacher/classes`, `/teacher/exercises` hiện "Sắp có ở Sprint 2"; thanh trên cùng có liên kết đúng | 3 | Tùng |
| US-08 | Là người dùng, tôi muốn **bị chặn khi mở trang của vai trò khác** để mỗi vai trò chỉ thấy khu vực của mình | Chưa đăng nhập: về `/login`; STUDENT mở `/admin/users` hoặc `/teacher` thì về `/coming-soon`; TEACHER mở `/coming-soon` thì về `/teacher`; ADMIN mở `/coming-soon` hoặc `/teacher` thì về `/admin`; không có vòng chuyển hướng; đúng vai trò thì thấy trang; có test Vitest; API vẫn trả 403 khi sinh viên gọi `GET /users` | 3 | Tùng (route guard, test), Nam (API trả 403) |
| | **Tổng** | | **45** | |

(Điểm tính theo story, mỗi story một lần; những story có nhiều người ghi chung công sức API và trang. Phần API và hạ tầng của US-01 đến US-06 thuộc Nam; Tùng nhận phần giao diện, test và tài liệu của các story đó cùng US-07 và US-08.)

## Kế hoạch theo ngày

Nam viết Auth Service trong ngày 1 đến 5, rồi chuyển sang Gateway và trang Admin. Trong lúc đó Tùng dựng các trang web trên **API giả (mock)** theo đúng hợp đồng trong `docs/API.md`, rồi nối sang API thật khi Nam xong từng phần (đăng ký / đăng nhập cuối ngày 3, người dùng và hồ sơ cuối ngày 5, gateway cuối ngày 8).

| Ngày | Công việc |
|---|---|
| 1 | Sprint Planning (hai người): chốt sprint goal, chia việc (Nam backend và Admin, Tùng toàn bộ web còn lại), thống nhất hợp đồng API đăng nhập, đăng ký, hồ sơ và `docs/API.md` bản đầu. Nam: khung repo, `docker-compose.yml` với PostgreSQL, dự án Spring Boot, Flyway `V1`. Tùng: dự án Vite + React, Tailwind, shadcn/ui, `ThemeProvider`, khung `lib/api.ts` và dữ liệu mock |
| 2 đến 3 | Nam: `User`, `UserRepository`, `AuthService` (đăng ký, đăng nhập, BCrypt), `LoginAttempts`, `GlobalExceptionHandler`. Tùng: `lib/api.ts`, `lib/auth.ts`, `AuthProvider`, `lib/roles.ts`, `HomeRedirect`, `AuthShell`, `LoginPage`, `RegisterPage`, `PasswordInput` trên mock |
| 4 đến 5 | Nam: RS256, `signing_keys`, `JwtKeys`, `JwtService`, `JwksController`, `ver`, `GET /auth/session`, `SecurityConfig`, `UserController`, `UserService`, `AdminSeeder`, `RequestIdFilter`, `OpenApiConfig`, test và Dockerfile của Auth Service. Tùng: nối `LoginPage` / `RegisterPage` với API thật, `RequireAuth` theo vai trò, trang `/coming-soon` cho sinh viên, `NotFoundPage`, test Vitest chuyển hướng |
| 6 | Mid-sprint review (hai người review chéo): chạy thử đăng ký, đăng nhập với Auth Service thật, kiểm tra chuyển hướng ba vai trò; cắt các việc "tùy chọn" nếu trễ. Nam bắt đầu Gateway |
| 7 đến 8 | Nam: Gateway (`routes.py`, `proxy.py`, `jwks.py`, `security.py`, `sessions.py`, `requestid.py`, `docs.py`) và test gateway (`pytest` + `respx`), rồi `AppShell`, `/admin`, `UsersPage` trên API thật. Tùng: `PortalShell`, `PortalNav`, `/teacher`, `ProfilePage`, `ComingSoonPage`, `/teacher/*` placeholder, test component đăng nhập / đăng ký, Dockerfile và `nginx.conf` của web, lint web, `docs/API.md`. Cuối ngày 8 web đổi từ mock sang gọi qua gateway |
| 9 | Tích hợp: `docker compose` đầy đủ, CI (Nam viết, Tùng lo job `web`), `scripts/system_test.py` (Nam), `scripts/demo_data.py`, `docs/HUONG-DAN.md` kèm ảnh chụp màn hình và `web/readme.md` (Tùng), sửa lỗi, review chéo, đăng nhập thử 3 tài khoản demo |
| 10 | Sprint Review (demo) và Retrospective; Tùng làm phần giao diện sáng / tối và điện thoại nếu còn giờ (tùy chọn) |

## Phân công chi tiết

Ước lượng bằng giờ (hai người tự ước lượng ở Planning, cập nhật khi xong). **Nam gánh toàn bộ backend** (Auth Service, Gateway, hạ tầng, CI) cùng trang Admin và vai trò team lead (64 giờ). **Tùng làm toàn bộ web còn lại, test và tài liệu** (63 giờ bắt buộc cộng 4 giờ tùy chọn) và không có việc nào trong Auth Service; khi cần API mới, Tùng nêu yêu cầu với Nam ở Daily Scrum.

### Nam (Team lead / Scrum Master): toàn bộ backend, trang Admin, hạ tầng

**Trang phụ trách:** `/admin`, `/admin/users`, `/admin/profile` (`AppShell`).
**User story:** US-01 (API), US-02 (API), US-03, US-04, US-05, US-06, US-08 (API trả 403).

| Việc | Story | Giờ |
|---|---|---|
| Auth Service: dự án Spring Boot, Flyway `V1__init.sql`, entity `User`, `UserRepository`, `Role`, `UserStatus`, `AccountType`, README | US-01 | 3 |
| `AuthService`: đăng ký (luôn `STUDENT`), đăng nhập, BCrypt (giới hạn 72 byte), `LoginAttempts` (chặn đăng nhập sai) | US-01, US-02 | 5 |
| RS256: `SigningKey`, `signing_keys` (`V2__...sql`), `JwtKeys`, `JwtService`, `JwksController` | US-03 | 4 |
| `ver`, `GET /auth/session`, `JwtAuthenticationFilter`, `SecurityConfig` | US-03, US-06 | 3 |
| `UserController`, `UserService` (quản lý người dùng của admin, hồ sơ cá nhân), `AdminSeeder` | US-06 | 5 |
| `GlobalExceptionHandler`, `RequestIdFilter`, `OpenApiConfig`, Dockerfile của Auth Service | US-01, US-05 | 2 |
| Test đơn vị Auth Service: `AuthServiceTest`, `AuthControllerTest`, `JwtServiceTest`, `LoginAttemptsTest`, `UserServiceTest`, `UserControllerTest` | US-01, US-03, US-06 | 5 |
| Gateway: `config.py`, `main.py`, `routes.py`, `proxy.py`, `errors.py`, `jwks.py`, `security.py`, `sessions.py`, `requestid.py`, `docs.py` | US-04 | 8 |
| Test gateway (`pytest` + `respx`): 401, 502, phiên, `X-Request-ID`, JWKS | US-04 | 6 |
| `docker-compose.yml` (postgres, auth-service, gateway, web), `infra/postgres/init`, `.env.example` | US-05 | 3 |
| Makefile, `scripts/deploy.py`, `scripts/install.cmd` | US-05 | 2 |
| `.gitlab-ci.yml` (4 job; job `web` cùng Tùng) | US-05 | 2 |
| `scripts/system_test.py`: health, JWKS, đăng ký sinh viên (role `STUDENT`), đăng nhập, 401 / 200 / 403 | US-02, US-08 | 4 |
| Admin: `AppShell`, `AppSidebar`, `Header` (breadcrumb), `ADMIN_NAV_GROUPS`, `isNavItemActive`, `buildBreadcrumbs` | US-06 | 3 |
| Admin: `AdminDashboardPage` (số người dùng), `UsersPage`, route `/admin/*` trong `App.tsx` | US-06 | 4 |
| Review code của Tùng, tích hợp, điều phối Scrum (Planning, Daily, Review, Retro) | tất cả | 5 |
| **Tổng** | | **64** |

**Tiêu chí chấp nhận của phần Nam:** `POST /auth/register` và `/auth/login` đúng các mã 201 / 401 / 403 / 409 / 429 và `Retry-After`; `GET /.well-known/jwks.json` trả khóa RS256 và token có `kid`, `sub`, `role`, `ver`; đăng nhập admin vào `/admin`, thấy số người dùng; tạo giáo viên, khóa tài khoản thì token cũ bị từ chối trong 10 giây; sinh viên gọi `GET /users` bị 403; `./mvnw verify` và `uv run pytest` xanh; `/admin/users/4` vẫn sáng mục "Người dùng", `/admin` chỉ sáng ở đúng trang tổng quan; `docker compose up -d --build --wait` xanh; CI chạy đủ 4 job; `python3 scripts/system_test.py` qua.

### Tùng (Developer): web (trang Giáo viên, đăng nhập / đăng ký / hồ sơ, trang sinh viên tạm), route guard, test web và tài liệu

**Trang phụ trách:** `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises`, `/coming-soon`; chung: `/login`, `/register`, `ProfilePage`, `NotFoundPage`.
**User story:** US-01 (trang, `AuthProvider`, chuyển hướng), US-02 (trang, `/coming-soon`), US-05 (Dockerfile web, lint, job `web`, `demo_data.py`, tài liệu), US-07, US-08.

| Việc | Story | Giờ |
|---|---|---|
| Khởi tạo Vite + React 19 + Tailwind + shadcn/ui, `ThemeProvider`, `index.css` | US-05 | 5 |
| Dockerfile và `nginx.conf` của web | US-05 | 2 |
| `lib/api.ts` (gắn token, lỗi tiếng Việt, 401 thì đăng xuất), `lib/auth.ts`, `AuthProvider`; dữ liệu mock dùng chung cho đến khi API sẵn sàng | US-01 | 6 |
| `lib/roles.ts` (`roleHome`, `postLoginPath`), `HomeRedirect`, `RequireAuth roles={[...]}` (chuyển về trang chủ của vai trò), `NotFoundPage` | US-01, US-08 | 4 |
| `AuthShell`, `LoginPage`, `RegisterPage`, `PasswordInput` (lỗi tiếng Việt, chuyển theo vai trò sau đăng nhập và đăng ký); dựng trên API giả, nối API thật khi Nam xong | US-01, US-02 | 7 |
| `ProfilePage` dùng chung cho Admin và Giáo viên (đổi họ tên, đổi mật khẩu; sai mật khẩu cũ không bị đăng xuất) | US-07 | 3 |
| `PortalShell`, `PortalNav`, `TEACHER_NAV_ITEMS`, `UserMenu`, `ThemeToggle` | US-07 | 4 |
| `TeacherDashboardPage`, `ComingSoonPage` / `ComingSoonCards`, placeholder `/teacher/classes`, `/teacher/exercises`, route `/teacher/*` | US-07 | 4 |
| `StudentComingSoonPage` (route `/coming-soon`, `RequireAuth roles={["STUDENT"]}`, lời chào, thông báo, nút Đăng xuất, đổi giao diện) | US-02 | 3 |
| Test Vitest: `roles.test.ts`, `navigation.test.ts`, `AuthProvider.test.tsx` (HomeRedirect, RequireAuth theo vai trò, không vòng lặp), `api.test.ts`, test trang `/coming-soon`; test component form đăng nhập / đăng ký (kiểm tra dữ liệu, lỗi 401 / 409 / 429) | US-01, US-02, US-08 | 8 |
| Lint web (`npm run lint`), job `web` trong `.gitlab-ci.yml` (cùng Nam) | US-05 | 2 |
| `scripts/demo_data.py` (tài khoản giáo viên `teacher1@nambede.local`, sinh viên `student1@nambede.local` qua gateway) | US-05 | 2 |
| `docs/HUONG-DAN.md`: hướng dẫn người dùng và kịch bản demo, chụp ảnh màn hình ba vai trò | US-05 | 4 |
| `docs/API.md`: danh sách endpoint, ví dụ yêu cầu và phản hồi, dạng lỗi | US-05 | 4 |
| `web/readme.md`: cấu trúc thư mục, bảng route, lệnh chạy, cách thêm trang | US-05 | 2 |
| Kiểm thử thủ công chuyển hướng ba vai trò, ghi checklist nghiệm thu, cập nhật phần "Cách kiểm tra" theo kết quả chạy thật | US-08 | 3 |
| **Bắt buộc** | | **63** |
| *Tùy chọn:* kiểm tra giao diện sáng / tối và điện thoại, sửa lỗi giao diện sau tích hợp | US-07 | 4 |
| **Tổng (gồm việc tùy chọn)** | | **67** |

**Tiêu chí chấp nhận của phần Tùng:** đăng nhập `teacher1@nambede.local` thì vào `/teacher` và thấy ba thẻ Sprint 2; thanh trên cùng có Tổng quan, Lớp học, Bài tập, và chỉ "Tổng quan" sáng ở `/teacher`; `/teacher/classes` và `/teacher/exercises` hiện "Sắp có ở Sprint 2"; "Tài khoản của tôi" dẫn đến `/teacher/profile`; đăng ký sinh viên mới hoặc đăng nhập `student1@nambede.local` thì vào `/coming-soon`, thấy lời chào, thông báo và nút Đăng xuất hoạt động; STUDENT mở `/admin/users` hoặc `/teacher` bị đưa về `/coming-soon`, TEACHER mở `/coming-soon` về `/teacher`, ADMIN mở `/coming-soon` về `/admin`; chưa đăng nhập về `/login`; đường dẫn lạ hiện 404 với nút về đúng trang chủ vai trò; trang đăng nhập hiện lỗi tiếng Việt; `npm test`, `npm run lint`, `npm run build` xanh; web build thành image nginx chạy được; `docs/HUONG-DAN.md` có ảnh chụp ba vai trò; `docs/API.md` khớp API thật.

## Definition of Done

Một story chỉ được coi là xong khi:

1. Code đã được người còn lại trong nhóm review và merge vào nhánh `sprint-1`.
2. Test tự động của phần đó viết xong và chạy qua: `./mvnw verify` (Auth Service), `uv run pytest` (gateway), `npm test`, `npm run lint`, `npm run build` (web).
   Mỗi vai trò mở được trang của mình (`/admin`, `/teacher`, `/coming-soon`) và chuyển hướng đúng (xem "Cách kiểm tra").
3. Chạy được trong `docker compose up -d --build --wait`, các container healthy.
4. Lỗi trả về đúng dạng `{"status", "error", "message", "path"}`; không có bí mật nào trong git.
5. Tài liệu liên quan (`docs/API.md`, README của service) được cập nhật.
6. Product Owner (giảng viên hướng dẫn) hoặc cả nhóm đã xem demo và chấp nhận tiêu chí chấp nhận của story.

## Cách chạy

### Bằng Docker Compose

```bash
cp .env.example .env
docker compose up -d --build --wait     # PostgreSQL, Auth Service, gateway, web
python3 scripts/demo_data.py            # tài khoản demo (hoặc: make seed)
```

Hoặc một lệnh: `make deploy` (macOS, Linux), `py scripts\deploy.py` (Windows).

| Địa chỉ | Dùng để |
|---|---|
| http://nambede.dev.localhost (hoặc http://localhost) | Web |
| http://localhost:8000/docs | Swagger chung qua gateway |
| http://localhost:8081/swagger-ui.html | Swagger của Auth Service |
| http://localhost:8081/.well-known/jwks.json | JWKS |

### Không dùng Docker cho các service

Cần JDK 17, `uv`, Node 24 và PostgreSQL chạy sẵn trên máy. Tạo database một lần:

```bash
psql -U postgres -c "CREATE USER nambede WITH PASSWORD 'nambede' CREATEDB;"
psql -U postgres -c "CREATE DATABASE auth_db OWNER nambede;"
```

Mỗi lệnh dưới đây chạy trong một terminal riêng:

```bash
# 1. Auth Service, cổng 8081 (mặc định kết nối localhost:5432/auth_db, user nambede / nambede)
cd services/auth-service && ./mvnw spring-boot:run

# 2. Gateway, cổng 8000 (mặc định gọi Auth Service ở localhost:8081)
cd services/gateway && uv sync && uv run uvicorn gateway.main:app --reload --port 8000

# 3. Web, cổng 5173 (gọi gateway ở localhost:8000)
cd web && npm install && npm run dev
```

Mở http://localhost:5173, đăng nhập rồi web chuyển đến `/admin`, `/teacher` hoặc `/coming-soon` tùy vai trò (Vite chuyển `/api` sang gateway ở `localhost:8000`). Cấu hình qua biến môi trường: Auth Service đọc `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `JWT_EXPIRATION_MINUTES`; gateway đọc `JWKS_URL`, `AUTH_SERVICE_URL`, `CORS_ORIGINS` (từ môi trường hoặc `.env` ở thư mục gốc). Giá trị mặc định đủ để chạy như trên.

### Tài khoản demo

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Quản trị viên | `admin@nambede.local` | `Admin@123` |
| Giảng viên | `teacher1@nambede.local` | `Teacher@123` |
| Sinh viên | `student1@nambede.local` | `Student@123` |

Admin có sẵn khi Auth Service khởi động lần đầu. Giảng viên và sinh viên do `python3 scripts/demo_data.py` (hoặc `make seed`) tạo qua gateway.

## Cách kiểm tra (nghiệm thu)

```bash
G=http://localhost:8000/api

# 1. Sức khỏe
curl -s $G/health                                    # {"status":"UP","service":"gateway"}

# 2. JWKS: khóa công khai RS256 (gọi thẳng Auth Service)
curl -s http://localhost:8081/.well-known/jwks.json  # {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"...","n":"...","e":"AQAB"}]}

# 3. Đăng nhập
curl -s -X POST $G/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@nambede.local","password":"Admin@123"}'          # có accessToken
TOKEN=$(curl -s -X POST $G/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@nambede.local","password":"Admin@123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')

# 4. Endpoint cần đăng nhập: không token 401, có token 200
curl -s -i $G/users/me | head -1                                        # HTTP/1.1 401
curl -s -i $G/users/me -H "Authorization: Bearer $TOKEN" | head -1      # HTTP/1.1 200

# 5. Đăng ký sinh viên rồi đăng nhập
curl -s -X POST $G/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"hs1@nambede.local","password":"Student@123","fullName":"Nguyễn Văn A"}'   # 201, role STUDENT

# 6. Phân quyền: sinh viên không xem được danh sách người dùng
STOKEN=$(curl -s -X POST $G/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"hs1@nambede.local","password":"Student@123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["accessToken"])')
curl -s -i $G/users -H "Authorization: Bearer $STOKEN" | head -1       # HTTP/1.1 403
```

### Kiểm tra trên web: chuyển hướng theo vai trò

Mở http://nambede.dev.localhost (hoặc http://localhost:5173 khi chạy `npm run dev`) và đăng nhập lần lượt ba tài khoản demo (đăng xuất giữa các lần):

| Tài khoản | Sau đăng nhập phải thấy | Thử mở trang khác vai trò |
|---|---|---|
| `admin@nambede.local` | `/admin`, thanh bên, số người dùng; `/admin/users`, `/admin/profile` mở được | `/teacher`, `/coming-soon` thì về `/admin` |
| `teacher1@nambede.local` | `/teacher`, thanh trên cùng, thẻ Lớp học, Bài tập, Ngân hàng câu hỏi (Sprint 2); `/teacher/classes`, `/teacher/exercises` hiện "Sắp có ở Sprint 2" | `/admin/users`, `/coming-soon` thì về `/teacher` |
| `student1@nambede.local` (hoặc đăng ký mới) | `/coming-soon`: "Xin chào <tên>", "Trang dành cho sinh viên sẽ có ở sprint sau. Tài khoản của bạn đã được tạo thành công.", nút Đăng xuất (không có thanh điều hướng, không có hồ sơ) | `/admin/users`, `/teacher` thì về `/coming-soon` |

Ngoài ra: chưa đăng nhập mở `/admin` thì về `/login`; mở `/` đã đăng nhập thì về trang chủ của vai trò; "Tài khoản của tôi" ở menu người dùng của admin và giáo viên dẫn đến `/<vai trò>/profile`; sinh viên không có hồ sơ ở Sprint 1; mở `/student` (không còn tồn tại) hiện trang 404.

Kiểm tra tự động toàn bộ các bước trên khi hệ thống đang chạy: `python3 scripts/system_test.py`. Test từng thành phần: `./mvnw verify` (Auth Service), `uv run pytest` (gateway), `npm test` (web).

## Sprint Review

Mẫu điền vào ngày 10.

| Mục | Nội dung |
|---|---|
| Ngày, người dự | |
| Sprint goal có đạt không | |

| Mã story | Kết quả (xong / chưa xong) | Ghi chú từ demo |
|---|---|---|
| US-01 | | |
| US-02 | | |
| US-03 | | |
| US-04 | | |
| US-05 | | |
| US-06 | | |
| US-07 | | |
| US-08 | | |

- Điểm hoàn thành / điểm cam kết (45): …
- Góp ý của người xem demo:
- Việc đưa lại vào product backlog (và sprint dự kiến):

## Sprint Retrospective

Mẫu điền ngay sau Review, 45 phút, mỗi người nói ý kiến của mình trước khi thảo luận.

| Làm tốt, nên giữ | Chưa tốt, cần cải thiện | Thử nghiệm ở Sprint 2 |
|---|---|---|
| | | |

| Hành động | Người phụ trách | Hạn |
|---|---|---|
| | | |

Câu hỏi gợi ý: ước lượng giờ và điểm có sát thực tế không? Nam gánh toàn bộ backend (64 giờ) có thành điểm nghẽn không, và Tùng có bị chờ API quá lâu (phải dùng mock) không? Hai người, mỗi người 63 đến 67 giờ trong 10 ngày có quá tải không, và nên cắt việc "tùy chọn" nào trước? Có việc nào bị chặn vì chờ người kia và Daily Scrum đã phát hiện sớm chưa? Để Tùng tự viết endpoint cho trang giáo viên (dưới sự review của Nam) ở sprint sau có hợp lý hơn không? Việc bỏ trang sinh viên khỏi Sprint 1 có làm sprint nhẹ đi như dự tính không, và trang `/coming-soon` có đủ để demo đăng ký của sinh viên không? Review chéo giữa hai người có làm chậm không? Hạ tầng (Compose, CI) có giúp cả nhóm chạy cùng một môi trường không?
