Môn bà Mỹ

# Web (React + Vite)

Giao diện web của NamBeDe. Chỉ gọi **API Gateway** qua đường dẫn `/api` cùng domain, không gọi trực tiếp service nào.

- React 19, Vite 8, TypeScript, Tailwind CSS 4, shadcn/ui (style `base-nova`), React Router 8.
- Phase 1: đăng nhập, sinh viên tự đăng ký, trang chủ theo vai trò (`/admin`, `/teacher`; sinh viên vào `/coming-soon`, chưa có trang sinh viên), trang quản lý người dùng cho admin (tạo tài khoản, khóa / mở khóa, chuyển FREE / VIP).
- Các sprint sau thêm lớp học, bài tập, luyện tập... và **cổng sinh viên `/student`** (Sprint 3, khi đó `roleHome("STUDENT")` đổi thành `/student` và `/coming-soon` được gỡ); xem [`docs/scrum/ROADMAP.md`](../docs/scrum/ROADMAP.md).

## Chạy

**Trong Docker** (cùng toàn bộ hệ thống, từ thư mục gốc): `docker compose up -d --build`, rồi mở http://nambede.dev.localhost. Container nginx phục vụ bản build và chuyển `/api` tới gateway (xem `nginx.conf`).

**Khi phát triển giao diện** (hot reload): giữ các service chạy bằng Docker, rồi

```bash
cd web
npm install
npm run dev      # http://localhost:5173, Vite chuyển /api tới gateway ở localhost:8000
npm run build    # kiểm tra kiểu và build
npm run lint
```

## Các trang (route)

| Khu vực | Đường dẫn | Bố cục |
|---|---|---|
| Chung | `/login`, `/register`; `/` chuyển theo vai trò | không có |
| `ADMIN` | `/admin`, `/admin/users`, `/admin/profile` | `AppShell` (thanh bên) |
| `TEACHER` | `/teacher`, `/teacher/profile`, `/teacher/classes`, `/teacher/exercises` | `PortalShell` (thanh trên) |
| `STUDENT` | `/coming-soon` | kiểu `AuthShell`, không có thanh điều hướng: lời chào, thông báo "Trang dành cho sinh viên sẽ có ở sprint sau", nút Đăng xuất |

Mỗi khu vực nằm trong `RequireAuth roles={[...]}`: chưa đăng nhập thì về `/login`, sai vai trò thì về `roleHome(role)`. Sau đăng nhập, trang đã nhớ chỉ được dùng nếu thuộc khu vực của người dùng.

## Cấu trúc `src/`

| Đường dẫn | Vai trò |
|---|---|
| `lib/api.ts` | Gọi gateway, gắn JWT, chuyển lỗi thành câu tiếng Việt (`errorText`); `upload` gửi file, `download` tải file |
| `lib/document-title.ts` | Tên trang trên tab trình duyệt, `PageHeader` tự đặt |
| `lib/auth.ts`, `components/AuthProvider.tsx` | Phiên đăng nhập, `useAuth()`, `RequireAuth` (chặn route theo vai trò, sai vai trò thì về trang chủ của mình), `HomeRedirect` |
| `lib/roles.ts` | `roleHome(role)`: `/admin`, `/teacher`, `/coming-soon` (mỗi trang chủ là trang vai trò đó được mở, nên không có vòng chuyển hướng); `isInRoleArea`, `postLoginPath` |
| `lib/labels.ts` | Nhãn tiếng Việt và màu badge cho vai trò, trạng thái, gói tài khoản |
| `lib/theme.ts`, `components/layout/ThemeProvider.tsx` | Chế độ sáng / tối / theo hệ thống |
| `components/layout/` | Admin: `AppShell` (thanh bên thu gọn được, header có breadcrumb). Giảng viên: `PortalShell` + `PortalNav` (thanh trên cùng). `navigation.ts` (menu từng vai trò), `UserMenu`, `ThemeToggle` |
| `components/common/` | `Logo`, `BrandMark`, `PageHeader`, `StatCard`, `StatusBadge`, `DataTable`, `EmptyState`, `ErrorState`, `LoadingState`, `ConfirmDialog`, `FormDialogShell`, `UserAvatar` |
| `pages/` | Chung: `LoginPage`, `RegisterPage`, `ProfilePage`, `ComingSoonPage`, `NotFoundPage`. `pages/admin/`: `AdminDashboardPage`, `UsersPage`. `pages/teacher/`: `TeacherDashboardPage`. `StudentComingSoonPage` (route `/coming-soon`). Các trang sau đăng nhập được tải khi mở (`lazy` trong `App.tsx`). Trang lớp học, bài tập, luyện tập... được thêm vào thư mục của vai trò ở các sprint sau |
| `components/ui/` | Component shadcn (style `base-nova`), thêm bằng `npx shadcn add ...` |

## Giao diện

Thiết kế theo frontend của dự án `rental-room-management`:

- Huy hiệu Trường Đại học Quy Nhơn ở `public/logo.svg`, chỉ dùng qua `Logo` và `BrandMark`. Dùng nguyên trạng: không đổi màu, không bóp méo. Dưới 48px luôn đi kèm chữ, đứng một mình chỉ từ 96px (trang đăng nhập, 404).
- Màu nhận diện `#0066CA` chỉ dùng ở trang đăng nhập và đăng ký; phần còn lại giữ bảng màu shadcn.
- Thông báo dùng toast (`sonner`); xóa, rời lớp, nộp bài dùng `ConfirmDialog`; tạo, sửa dùng `FormDialogShell`.
- Bảng dùng `DataTable`: trên điện thoại mỗi dòng thành một thẻ.
