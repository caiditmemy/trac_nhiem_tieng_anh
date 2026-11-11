import { ClipboardList, Flame, School, Star } from "lucide-react"
import { Link } from "react-router"

import { ClassListCard, StatsSkeleton, TodoExercisesCard } from "@/components/DashboardParts"
import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { buttonVariants } from "@/components/ui/button"
import { useLoad } from "@/hooks/use-load"
import { api } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { classApi } from "@/lib/classes"
import { exerciseApi, pendingExercises } from "@/lib/exercises"
import { formatNumber } from "@/lib/format"
import { ACCOUNT_TYPE_META, ROLE_META } from "@/lib/labels"
import type { Streak, WeeklyRanking } from "@/types"

// Loaders live outside the components so their identity never changes between renders
const loadMyClasses = () => classApi.list()
const loadMyExercises = () => exerciseApi.list()
// Streak and points come from Learning Service; when it is down the rest of the dashboard still shows
const loadProgress = () =>
  Promise.all([
    api<Streak>("/streak").catch(() => null),
    api<WeeklyRanking>("/ranking/weekly?limit=0").catch(() => null),
  ])

/** /student: classes, pending exercises, learning streak and activity points. */
export function StudentDashboardPage() {
  const { user } = useAuth()
  const classes = useLoad(loadMyClasses)
  const exercises = useLoad(loadMyExercises)
  const progress = useLoad(loadProgress)
  if (!user) return null

  const streak = progress.data?.[0] ?? null
  const me = progress.data?.[1]?.me ?? null
  const missing = "Chưa có dữ liệu"
  const pending = exercises.data ? pendingExercises(exercises.data).length : null

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

      {progress.error ? (
        <ErrorState title="Không tải được tiến độ học tập" message={progress.error} onRetry={progress.reload} />
      ) : !progress.data || !classes.data || !exercises.data ? (
        classes.error || exercises.error ? null : <StatsSkeleton count={4} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatCard title="Lớp học" value={classes.data.length} icon={<School />} accent="blue" hint="Lớp bạn đã tham gia" />
          <StatCard
            title="Bài tập chưa làm"
            value={pending ?? "—"}
            icon={<ClipboardList />}
            accent="amber"
            hint={
              <Link to="/student/exercises" className="font-medium text-foreground hover:underline">
                Xem bài tập
              </Link>
            }
          />
          <StatCard
            title="Chuỗi ngày học"
            value={streak ? `${streak.currentStreak} ngày` : "—"}
            icon={<Flame />}
            accent="rose"
            hint={streak ? (streak.activeToday ? "Hôm nay bạn đã học" : "Làm một bài tập để giữ chuỗi") : missing}
          />
          <StatCard
            title="Điểm hoạt động"
            value={me ? formatNumber(me.totalPoints) : "—"}
            icon={<Star />}
            accent="emerald"
            hint={me ? "+20 mỗi bài nộp, +1 mỗi câu đúng" : missing}
          />
        </div>
      )}

      <div className="grid gap-4 xl:grid-cols-2">
        {exercises.error ? (
          <ErrorState title="Không tải được bài tập" message={exercises.error} onRetry={exercises.reload} />
        ) : exercises.data ? (
          <TodoExercisesCard exercises={exercises.data} />
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
