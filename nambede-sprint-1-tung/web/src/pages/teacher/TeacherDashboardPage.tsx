import { Link } from "react-router"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/card"
import { ClipboardList, Library, School } from "lucide-react"

import { ComingSoonCards, type ComingSoonItem } from "@/components/common/ComingSoonCards"
import { PageHeader } from "@/components/common/PageHeader"
import { StatusBadge } from "@/components/common/StatusBadge"
import { useAuth } from "@/lib/auth"
import { ROLE_META } from "@/lib/labels"

const COMING_SOON: ComingSoonItem[] = [
  { title: "Lớp học", sprint: "Sprint 2", description: "Tạo lớp, chia sẻ mã lớp cho sinh viên.", icon: <School /> },
  { title: "Bài tập", sprint: "Sprint 2", description: "Giao bài, xem bài nộp và chấm điểm.", icon: <ClipboardList /> },
  { title: "Ngân hàng câu hỏi", sprint: "Sprint 2", description: "Soạn và tái sử dụng câu hỏi cho các bài tập.", icon: <Library /> },
]

/** /teacher: greeting and role; the teaching features arrive in Sprint 2. */
export function TeacherDashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Chào ${user.fullName}`}
        documentTitle="Tổng quan giáo viên"
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            Trường Đại học Quy Nhơn
            <StatusBadge meta={ROLE_META[user.role]} />
          </span>
        }
      />
      <Card>
        <CardHeader>
          <CardTitle>Tài khoản giảng dạy</CardTitle>
          <CardDescription>Thông tin tài khoản đang đăng nhập từ hệ thống xác thực.</CardDescription>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-3 sm:grid-cols-2">
            <div><dt className="text-sm text-muted-foreground">Email</dt><dd className="break-all">{user.email}</dd></div>
            <div><dt className="text-sm text-muted-foreground">Trạng thái</dt><dd>{user.status === "ACTIVE" ? "Đang hoạt động" : "Đã khóa"}</dd></div>
          </dl>
        </CardContent>
        <CardFooter>
          <Link to="/teacher/profile" className={buttonVariants({ variant: "outline" })}>Cập nhật hồ sơ</Link>
        </CardFooter>
      </Card>
      <ComingSoonCards items={COMING_SOON} />
    </div>
  )
}
