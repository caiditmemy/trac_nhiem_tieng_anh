import { CalendarClock, ClipboardList, Eye, ListChecks, MoreHorizontal, Plus, School, Trash2 } from "lucide-react"
import { useState, type ReactNode } from "react"
import { Link, useNavigate } from "react-router"
import { toast } from "sonner"

import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { DataTable, type Column } from "@/components/common/DataTable"
import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatusBadge } from "@/components/common/StatusBadge"
import { SubmittedProgress } from "@/components/common/SubmittedProgress"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Skeleton } from "@/components/ui/skeleton"
import { errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { useAuth } from "@/lib/auth"
import {
  EXERCISE_STATUS_META,
  OVERDUE_META,
  exerciseApi,
  formatDateTime,
  formatScore,
  pendingExercises,
  scoreTone,
  timeLeft,
  useExercises,
} from "@/lib/exercises"
import { formatDate } from "@/lib/format"
import { cn } from "@/lib/utils"
import type { ExerciseSummary } from "@/types"

/** /exercises: lecturers follow what they assigned, students do their exercises, admins see every exercise. */
export function ExercisesPage() {
  const { user } = useAuth()
  if (!user) return null
  if (user.role === "STUDENT") return <StudentExercises />
  return <StaffExercises canAssign={user.role === "TEACHER"} />
}

const ICON_TILE = "flex size-10 shrink-0 items-center justify-center rounded-lg bg-amber-100 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"

/* --------------------------------------------------------- Lecturer, admin */

function StaffExercises({ canAssign }: { canAssign: boolean }) {
  const area = useAreaPath()
  const navigate = useNavigate()
  const { exercises, loading, error, reload } = useExercises()
  const [classFilter, setClassFilter] = useState("")
  const [deleteTarget, setDeleteTarget] = useState<ExerciseSummary | null>(null)
  const [deleting, setDeleting] = useState(false)

  const classOptions = [...new Map(exercises.map((item) => [item.classId, item.className ?? `Lớp #${item.classId}`])).entries()]
  const visible = classFilter ? exercises.filter((item) => String(item.classId) === classFilter) : exercises

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await exerciseApi.remove(deleteTarget.id)
      toast.success(`Đã xóa bài tập ${deleteTarget.title}`)
      setDeleteTarget(null)
      reload()
    } catch (err) {
      toast.error(errorText(err))
    } finally {
      setDeleting(false)
    }
  }

  const assignButton = canAssign ? (
    <Link to={area("/exercises/new")} className={buttonVariants()}>
      <Plus />
      Giao bài tập
    </Link>
  ) : null

  const columns: Column<ExerciseSummary>[] = [
    {
      key: "title",
      header: "Bài tập",
      mobile: "title",
      cell: (row) => (
        <div className="min-w-0">
          <p className="truncate font-medium text-foreground">{row.title}</p>
          <p className="truncate text-xs font-normal text-muted-foreground">
            {row.questionCount} câu hỏi · Giao ngày {formatDate(row.createdAt)}
          </p>
        </div>
      ),
    },
    { key: "class", header: "Lớp", cell: (row) => row.className ?? <span className="text-muted-foreground">Lớp #{row.classId}</span> },
    {
      key: "due",
      header: "Hạn nộp",
      cell: (row) => (
        <span className="inline-flex flex-wrap items-center justify-end gap-2 md:justify-start">
          <span className="tabular-nums">{formatDateTime(row.dueAt)}</span>
          {row.overdue ? <StatusBadge meta={OVERDUE_META} /> : <span className="text-xs text-muted-foreground">{timeLeft(row.dueAt)}</span>}
        </span>
      ),
    },
    { key: "submitted", header: "Đã nộp", cell: (row) => <SubmittedProgress submitted={row.submittedCount ?? 0} total={row.studentCount} /> },
    {
      key: "actions",
      header: "",
      align: "right",
      mobile: "trailing",
      cell: (row) => (
        <span onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
          <DropdownMenu>
            <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với bài tập ${row.title}`} />}>
              <MoreHorizontal />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-44">
              <DropdownMenuItem onClick={() => navigate(area(`/exercises/${row.id}`))}>
                <Eye />
                Xem kết quả
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onClick={() => setDeleteTarget(row)}>
                <Trash2 />
                Xóa bài tập
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </span>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title={canAssign ? "Bài tập đã giao" : "Bài tập"}
        description={
          canAssign
            ? "Câu hỏi được sao chép vào bài tập lúc giao, nên sửa ngân hàng câu hỏi sau đó không làm đổi bài đã giao hay điểm của sinh viên."
            : "Tất cả bài tập giảng viên đã giao cho các lớp."
        }
        actions={assignButton}
      />

      <div className="flex flex-col gap-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10 sm:flex-row sm:items-center">
        <NativeSelect aria-label="Lọc theo lớp" value={classFilter} onChange={(event) => setClassFilter(event.target.value)} className="sm:w-64">
          <NativeSelectOption value="">Tất cả lớp</NativeSelectOption>
          {classOptions.map(([id, name]) => (
            <NativeSelectOption key={id} value={String(id)}>
              {name}
            </NativeSelectOption>
          ))}
        </NativeSelect>
        <p className="text-sm text-muted-foreground sm:ml-auto">{loading ? "Đang tải…" : `${visible.length} bài tập`}</p>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (
        <DataTable
          columns={columns}
          data={visible}
          rowKey={(row) => row.id}
          isLoading={loading}
          onRowClick={(row) => navigate(area(`/exercises/${row.id}`))}
          emptyTitle={canAssign ? "Bạn chưa giao bài tập nào" : "Chưa có bài tập nào"}
          emptyDescription={canAssign ? "Chọn câu hỏi từ ngân hàng và giao cho một lớp bạn dạy." : "Bài tập do giảng viên giao sẽ hiện ở đây."}
          emptyAction={assignButton}
        />
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="Xóa bài tập này?"
        description={deleteTarget ? `Bài tập ${deleteTarget.title} và toàn bộ bài làm, điểm của sinh viên sẽ bị xóa. Thao tác này không hoàn tác được.` : undefined}
        confirmLabel="Xóa bài tập"
        variant="destructive"
        isPending={deleting}
        onConfirm={confirmDelete}
      />
    </div>
  )
}

/* ---------------------------------------------------------------- Student */

function StudentExercises() {
  const area = useAreaPath()
  const { exercises, loading, error, reload } = useExercises()
  const todo = pendingExercises(exercises)
  const done = exercises.filter((item) => item.myStatus === "SUBMITTED")
  const missed = exercises.filter((item) => item.myStatus !== "SUBMITTED" && item.overdue)

  return (
    <div className="space-y-6">
      <PageHeader title="Bài tập" description="Bài tập giảng viên giao cho các lớp của bạn. Mỗi bài chỉ nộp được một lần, nộp xong xem được ngay đáp án và giải thích." />

      {error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : loading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-busy="true">
          {Array.from({ length: 3 }, (_, index) => (
            <Skeleton key={index} className="h-48 rounded-xl" />
          ))}
        </div>
      ) : exercises.length === 0 ? (
        <div className="rounded-xl bg-card ring-1 ring-foreground/10">
          <EmptyState
            icon={<ClipboardList />}
            title="Chưa có bài tập nào"
            description="Bài tập giảng viên giao cho lớp của bạn sẽ hiện ở đây."
            action={
              <Link to={area("/classes")} className={buttonVariants({ size: "sm", variant: "outline" })}>
                <School />
                Lớp học của tôi
              </Link>
            }
          />
        </div>
      ) : (
        <>
          <Section title="Cần làm" count={todo.length}>
            {todo.length === 0 ? (
              <p className="rounded-xl bg-card p-4 text-sm text-muted-foreground ring-1 ring-foreground/10">Bạn đã làm hết bài tập đang mở.</p>
            ) : (
              <CardGrid items={todo} />
            )}
          </Section>
          {done.length > 0 ? (
            <Section title="Đã nộp" count={done.length}>
              <CardGrid items={done} />
            </Section>
          ) : null}
          {missed.length > 0 ? (
            <Section title="Quá hạn" count={missed.length}>
              <CardGrid items={missed} />
            </Section>
          ) : null}
        </>
      )}
    </div>
  )
}

function Section({ title, count, children }: { title: string; count: number; children: ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="font-heading text-lg font-semibold tracking-tight">
        {title} <span className="text-sm font-normal text-muted-foreground tabular-nums">({count})</span>
      </h2>
      {children}
    </section>
  )
}

function CardGrid({ items }: { items: ExerciseSummary[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {items.map((item) => (
        <StudentExerciseCard key={item.id} exercise={item} />
      ))}
    </div>
  )
}

function StudentExerciseCard({ exercise }: { exercise: ExerciseSummary }) {
  const status = exercise.myStatus ?? "NOT_STARTED"
  const submitted = status === "SUBMITTED"
  const missed = !submitted && exercise.overdue
  const to = `/exercises/${exercise.id}`

  let action: ReactNode
  if (submitted) {
    action = (
      <Link to={to} className={buttonVariants({ variant: "outline", size: "sm" })}>
        Xem kết quả
      </Link>
    )
  } else if (missed) {
    action =
      status === "IN_PROGRESS" ? (
        <Link to={to} className={buttonVariants({ variant: "ghost", size: "sm" })}>
          Xem bài làm
        </Link>
      ) : (
        <Button size="sm" variant="ghost" disabled>
          Đã hết hạn
        </Button>
      )
  } else {
    action = (
      <Link to={to} className={buttonVariants({ size: "sm" })}>
        {status === "IN_PROGRESS" ? "Làm tiếp" : "Làm bài"}
      </Link>
    )
  }

  return (
    <Card className={cn(missed && "opacity-80")}>
      <CardHeader>
        <div className="flex items-start gap-3">
          <span className={ICON_TILE}>
            <ClipboardList className="size-5" />
          </span>
          <div className="min-w-0 flex-1 space-y-1">
            <CardTitle className="line-clamp-2">{exercise.title}</CardTitle>
            <CardDescription className="truncate">{exercise.className ?? `Lớp #${exercise.classId}`}</CardDescription>
          </div>
          <StatusBadge meta={missed ? OVERDUE_META : EXERCISE_STATUS_META[status]} className="shrink-0" />
        </div>
      </CardHeader>
      <CardContent className="space-y-1.5 text-sm text-muted-foreground">
        <p className="flex items-center gap-2">
          <CalendarClock className="size-4 shrink-0" aria-hidden />
          <span className="tabular-nums">Hạn nộp {formatDateTime(exercise.dueAt)}</span>
        </p>
        <p className="flex items-center gap-2">
          <ListChecks className="size-4 shrink-0" aria-hidden />
          {exercise.questionCount} câu hỏi
        </p>
      </CardContent>
      <CardFooter className="justify-between gap-2">
        {submitted && exercise.myScore !== null ? (
          <span className={cn("font-heading text-xl font-semibold tabular-nums", scoreTone(exercise.myScore))}>
            {formatScore(exercise.myScore)}
            <span className="text-sm font-normal text-muted-foreground"> / 10</span>
          </span>
        ) : (
          <span className={cn("text-xs", missed ? "text-rose-600 dark:text-rose-400" : "text-muted-foreground")}>{timeLeft(exercise.dueAt)}</span>
        )}
        {action}
      </CardFooter>
    </Card>
  )
}
