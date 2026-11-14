import { ClipboardList, School } from "lucide-react"
import { Link } from "react-router"

import { ClassListCard, OpenAssignedExercisesCard, StatsSkeleton } from "@/components/DashboardParts"
import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { buttonVariants } from "@/components/ui/button"
import { useLoad } from "@/hooks/use-load"
import { useAuth } from "@/lib/auth"
import { classApi } from "@/lib/classes"
import { exerciseApi } from "@/lib/exercises"
import { ACCOUNT_TYPE_META, ROLE_META } from "@/lib/labels"

// Loaders live outside the components so their identity never changes between renders
const loadMyClasses = () => classApi.list()
const loadMyExercises = () => exerciseApi.list()

/** /student: joined classes and the exercises assigned to them. */
export function StudentDashboardPage() {
  const { user } = useAuth()
  const classes = useLoad(loadMyClasses)
  const exercises = useLoad(loadMyExercises)
  if (!user) return null

  const open = exercises.data ? exercises.data.filter((item) => !item.overdue).length : null

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
          <Link to="/student/exercises" className={buttonVariants({ size: "sm" })}>
            <ClipboardList />
            Bài tập
          </Link>
        }
      />

      {!classes.data || !exercises.data ? (
        classes.error || exercises.error ? null : <StatsSkeleton count={2} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <StatCard title="Lớp học" value={classes.data.length} icon={<School />} accent="blue" hint="Lớp bạn đã tham gia" />
          <StatCard
            title="Bài tập đang mở"
            value={open ?? "—"}
            icon={<ClipboardList />}
            accent="amber"
            hint={
              <Link to="/student/exercises" className="font-medium text-foreground hover:underline">
                Xem bài tập
              </Link>
            }
          />
        </div>
      )}

      <div className="grid gap-4 xl:grid-cols-2">
        {exercises.error ? (
          <ErrorState title="Không tải được bài tập" message={exercises.error} onRetry={exercises.reload} />
        ) : exercises.data ? (
          <OpenAssignedExercisesCard exercises={exercises.data} />
        ) : null}

        {classes.error ? (
          <ErrorState title="Không tải được lớp học" message={classes.error} onRetry={classes.reload} />
        ) : classes.data ? (
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
        ) : null}
      </div>
    </div>
  )
}
