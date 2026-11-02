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
      <ComingSoonCards items={COMING_SOON} />
    </div>
  )
}
