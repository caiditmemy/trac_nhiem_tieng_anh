import { GraduationCap, KeyRound, School, Users } from "lucide-react"
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
import { ACCOUNT_TYPE_META, ROLE_META } from "@/lib/labels"

// Loaders live outside the components so their identity never changes between renders
const loadMyClasses = () => classApi.list()

/** /student: the classes the student joined and a shortcut to join one with an invite code. */
export function StudentDashboardPage() {
  const { user } = useAuth()
  const classes = useLoad(loadMyClasses)
  if (!user) return null

  const teachers = new Set((classes.data ?? []).map((item) => item.teacherId)).size

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Chào ${user.fullName}`}
        documentTitle="Tổng quan sinh viên"
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            Trường Đại học Quy Nhơn
            <StatusBadge meta={ROLE_META[user.role]} />
            <StatusBadge meta={ACCOUNT_TYPE_META[user.accountType]} />
          </span>
        }
        actions={
          <Link to="/student/classes" className={buttonVariants({ size: "sm" })}>
            <KeyRound />
            Tham gia lớp
          </Link>
        }
      />

      {classes.error ? (
        <ErrorState title="Không tải được lớp học" message={classes.error} onRetry={classes.reload} />
      ) : !classes.data ? (
        <StatsSkeleton count={3} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard title="Lớp học" value={formatNumber(classes.data.length)} icon={<School />} accent="blue" hint="Lớp bạn đã tham gia" />
            <StatCard title="Giảng viên" value={formatNumber(teachers)} icon={<GraduationCap />} accent="teal" hint="Giảng viên các lớp của bạn" />
            <StatCard
              title="Sinh viên cùng lớp"
              value={formatNumber(classes.data.reduce((sum, item) => sum + item.studentCount, 0))}
              icon={<Users />}
              accent="amber"
              hint="Tính cả bạn, theo từng lớp"
            />
          </div>
          <ClassListCard
            title="Lớp của tôi"
            classes={classes.data}
            empty={
              <EmptyState
                icon={<School />}
                title="Bạn chưa tham gia lớp nào"
                description="Nhập mã lớp do giảng viên gửi để tham gia."
                action={
                  <Link to="/student/classes" className={buttonVariants({ size: "sm" })}>
                    Tham gia lớp
                  </Link>
                }
              />
            }
          />
        </>
      )}
    </div>
  )
}
