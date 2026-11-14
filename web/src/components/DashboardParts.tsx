import { ChevronRight, ClipboardList, School } from "lucide-react"
import type { ReactNode } from "react"
import { Link } from "react-router"

import { ClassCode } from "@/components/common/ClassCode"
import { EmptyState } from "@/components/common/EmptyState"
import { buttonVariants } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAreaPath } from "@/lib/area"
import { formatDateTime, timeLeft } from "@/lib/exercises"
import { formatDate, formatNumber } from "@/lib/format"
import type { ClassInfo, ExerciseSummary } from "@/types"

// Pieces shared by the three dashboards (admin, teacher, student)

export function StatsSkeleton({ count }: { count: number }) {
  return (
    <div className={count === 4 ? "grid gap-4 sm:grid-cols-2 xl:grid-cols-4" : "grid gap-4 sm:grid-cols-3"} aria-busy="true">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10">
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-28" />
            <Skeleton className="size-9 rounded-lg" />
          </div>
          <Skeleton className="h-7 w-20" />
          <Skeleton className="h-3 w-32" />
        </div>
      ))}
    </div>
  )
}

/** Lecturer: exercises still open, soonest due first. */
export function OpenExercisesCard({ exercises }: { exercises: ExerciseSummary[] | null }) {
  const area = useAreaPath()
  const open = (exercises ?? []).filter((item) => !item.overdue).sort((a, b) => a.dueAt.localeCompare(b.dueAt))
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>Bài tập đang mở</CardTitle>
        <CardAction>
          <Link to={area("/exercises")} className={buttonVariants({ variant: "ghost", size: "xs" })}>
            Xem tất cả
          </Link>
        </CardAction>
      </CardHeader>
      <CardContent>
        {exercises === null ? (
          <p className="text-sm text-muted-foreground">Không tải được bài tập, Exercise Service có thể đang tạm dừng.</p>
        ) : open.length === 0 ? (
          <EmptyState
            icon={<ClipboardList />}
            title="Không có bài tập đang mở"
            description="Chọn câu hỏi trong ngân hàng và giao bài cho lớp."
            action={
              <Link to={area("/exercises/new")} className={buttonVariants({ size: "sm" })}>
                Giao bài tập
              </Link>
            }
          />
        ) : (
          <div className="divide-y divide-border">
            {open.slice(0, 5).map((item) => (
              <Link
                key={item.id}
                to={area(`/exercises/${item.id}`)}
                className="-mx-2 flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-muted/60"
              >
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{item.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {item.className ?? `Lớp #${item.classId}`} · {timeLeft(item.dueAt)}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground tabular-nums">{item.questionCount} câu</span>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** Student: exercises of their classes that are still open, soonest due first. */
export function OpenAssignedExercisesCard({ exercises }: { exercises: ExerciseSummary[] }) {
  const area = useAreaPath()
  const open = exercises.filter((item) => !item.overdue).sort((a, b) => a.dueAt.localeCompare(b.dueAt))
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>Bài tập được giao</CardTitle>
        <CardAction>
          <Link to={area("/exercises")} className={buttonVariants({ variant: "ghost", size: "xs" })}>
            Xem tất cả
          </Link>
        </CardAction>
      </CardHeader>
      <CardContent>
        {open.length === 0 ? (
          <EmptyState
            icon={<ClipboardList />}
            title="Không có bài tập đang mở"
            description="Bài tập giảng viên giao cho lớp của bạn sẽ hiện ở đây."
          />
        ) : (
          <div className="divide-y divide-border">
            {open.slice(0, 5).map((item) => (
              <div key={item.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0 last:pb-0">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300">
                  <ClipboardList className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium">{item.title}</p>
                  <p className="truncate text-xs text-muted-foreground">
                    {item.className ?? `Lớp #${item.classId}`} · hạn {formatDateTime(item.dueAt)} · {timeLeft(item.dueAt)}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground tabular-nums">{item.questionCount} câu</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export function ClassListCard({ title, classes, empty, showCode = false }: { title: string; classes: ClassInfo[]; empty: ReactNode; showCode?: boolean }) {
  const area = useAreaPath()
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {classes.length === 0 ? (
          empty
        ) : (
          <div className="divide-y divide-border">
            {classes.slice(0, 6).map((item) => (
              <div key={item.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0 last:pb-0">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300">
                  <School className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  {showCode ? (
                    <Link to={area(`/classes/${item.id}`)} className="block truncate text-sm font-medium hover:underline">
                      {item.name}
                    </Link>
                  ) : (
                    <p className="truncate text-sm font-medium">{item.name}</p>
                  )}
                  <p className="truncate text-xs text-muted-foreground">
                    {showCode ? `Tạo ngày ${formatDate(item.createdAt)}` : `Giảng viên: ${item.teacherName ?? "chưa lấy được tên"}`}
                  </p>
                </div>
                {showCode ? <ClassCode code={item.code} /> : null}
                <span className="w-16 text-right text-xs text-muted-foreground tabular-nums">{formatNumber(item.studentCount)} SV</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
