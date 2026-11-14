import { BookOpenCheck, ClipboardList, GraduationCap, School } from "lucide-react"
import { Link } from "react-router"

import { ClassListCard, OpenExercisesCard, StatsSkeleton } from "@/components/DashboardParts"
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
import { formatNumber } from "@/lib/format"
import { ROLE_META } from "@/lib/labels"
import { questionApi } from "@/lib/questions"

// Exercises and questions are optional on the dashboard: when their service is down the classes still show
const loadTeacherData = () => Promise.all([classApi.list(), questionApi.list({ mine: true }).catch(() => null), exerciseApi.list().catch(() => null)])

/** /teacher: classes, students, assigned exercises and questions written by the lecturer. */
export function TeacherDashboardPage() {
  const { user } = useAuth()
  const { data, error, reload } = useLoad(loadTeacherData)
  if (!user) return null

  const classes = data?.[0]
  const myQuestions = data?.[1] ?? null
  const exercises = data?.[2] ?? null
  const students = (classes ?? []).reduce((sum, item) => sum + item.studentCount, 0)
  const openExercises = (exercises ?? []).filter((item) => !item.overdue)

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
          <>
            <Link to="/teacher/classes" className={buttonVariants({ variant: "outline", size: "sm" })}>
              <School />
              Lớp học của tôi
            </Link>
            <Link to="/teacher/exercises/new" className={buttonVariants({ size: "sm" })}>
              <ClipboardList />
              Giao bài tập
            </Link>
          </>
        }
      />

      {error ? (
        <ErrorState title="Không tải được lớp học" message={error} onRetry={reload} />
      ) : !classes ? (
        <StatsSkeleton count={4} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard title="Lớp đang dạy" value={formatNumber(classes.length)} icon={<School />} accent="blue" hint="Lớp do bạn tạo" />
            <StatCard title="Sinh viên" value={formatNumber(students)} icon={<GraduationCap />} accent="teal" hint="Tổng số sinh viên trong các lớp" />
            <StatCard
              title="Bài tập đã giao"
              value={exercises ? formatNumber(exercises.length) : "—"}
              icon={<ClipboardList />}
              accent="amber"
              hint={exercises ? `${formatNumber(openExercises.length)} bài còn hạn nộp` : "Chưa có dữ liệu"}
            />
            <StatCard
              title="Câu hỏi đã soạn"
              value={myQuestions ? formatNumber(myQuestions.length) : "—"}
              icon={<BookOpenCheck />}
              accent="violet"
              hint={
                <Link to="/teacher/questions" className="font-medium text-foreground hover:underline">
                  Mở ngân hàng câu hỏi
                </Link>
              }
            />
          </div>
          <div className="grid gap-4 xl:grid-cols-2">
            <OpenExercisesCard exercises={exercises} />
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
          </div>
        </>
      )}
    </div>
  )
}
