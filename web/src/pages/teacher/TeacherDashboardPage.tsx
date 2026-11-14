import { GraduationCap, School, Sparkles } from "lucide-react"
import { Link } from "react-router"

import { ClassListCard, StatsSkeleton } from "@/components/DashboardParts"
import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { buttonVariants } from "@/components/ui/button"
import { useLoad } from "@/hooks/use-load"
import { useAuth } from "@/lib/auth"
import { classApi } from "@/lib/classes"
import { formatNumber } from "@/lib/format"
import { ROLE_META } from "@/lib/labels"
import type { ClassInfo } from "@/types"

// Loaders live outside the components so their identity never changes between renders
const loadTeacherClasses = () => classApi.list()

/** /teacher: the classes of the lecturer, their students and invite codes. */
export function TeacherDashboardPage() {
  const { user } = useAuth()
  const { data: classes, error, reload } = useLoad(loadTeacherClasses)
  if (!user) return null

  const students = (classes ?? []).reduce((sum, item) => sum + item.studentCount, 0)
  const newest = (classes ?? []).reduce<ClassInfo | null>((latest, item) => (!latest || item.createdAt > latest.createdAt ? item : latest), null)

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
        actions={
          <Link to="/teacher/classes" className={buttonVariants({ size: "sm" })}>
            <School />
            Lớp học của tôi
          </Link>
        }
      />

      {error ? (
        <ErrorState title="Không tải được lớp học" message={error} onRetry={reload} />
      ) : !classes ? (
        <StatsSkeleton count={3} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard title="Lớp đang dạy" value={formatNumber(classes.length)} icon={<School />} accent="blue" hint="Lớp do bạn tạo" />
            <StatCard title="Sinh viên" value={formatNumber(students)} icon={<GraduationCap />} accent="teal" hint="Tổng số sinh viên trong các lớp" />
            <StatCard
              title="Lớp mới nhất"
              value={<span className="text-lg">{newest ? newest.name : "—"}</span>}
              icon={<Sparkles />}
              accent="violet"
              hint={newest ? `Mã lớp ${newest.code}` : "Chưa có lớp nào"}
            />
          </div>
          <ClassListCard
            title="Lớp học của tôi"
            classes={classes}
            empty={
              <EmptyState
                icon={<School />}
                title="Bạn chưa có lớp nào"
                description="Tạo lớp rồi gửi mã lớp cho sinh viên để các em tham gia."
                action={
                  <Link to="/teacher/classes" className={buttonVariants({ size: "sm" })}>
                    Tạo lớp đầu tiên
                  </Link>
                }
              />
            }
            showCode
          />
        </>
      )}
    </div>
  )
}
