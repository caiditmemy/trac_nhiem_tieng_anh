import { Award, CalendarClock, Copy, Eye, ListChecks, Send, Trash2 } from "lucide-react"
import { useEffect, useState } from "react"
import { useNavigate, useParams } from "react-router"
import { toast } from "sonner"

import { AnswerOptions } from "@/components/AnswerOptions"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { DataTable, type Column } from "@/components/common/DataTable"
import { ErrorState } from "@/components/common/ErrorState"
import { LoadingState } from "@/components/common/LoadingState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { UserAvatar } from "@/components/common/UserAvatar"
import { Explanation, ReviewQuestionList, ScoreCard } from "@/components/SubmissionReview"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { EXERCISE_STATUS_META, exerciseApi, formatDateTime, formatScore, scoreTone, timeLeft } from "@/lib/exercises"
import { TONE } from "@/lib/labels"
import { cn } from "@/lib/utils"
import type { ExerciseResults, StudentResult, Submission } from "@/types"

/** /exercises/:id for the lecturer who assigned it and for admins: who submitted, scores, and how each question went. */
export function ExerciseResultsPage() {
  const area = useAreaPath()
  const exerciseId = Number(useParams().id)
  const navigate = useNavigate()
  const [data, setData] = useState<ExerciseResults | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [viewing, setViewing] = useState<StudentResult | null>(null)

  useEffect(() => {
    let cancelled = false
    exerciseApi
      .results(exerciseId)
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(errorText(err, { 400: "Không tìm thấy bài tập.", 403: "Chỉ giảng viên đã giao bài này hoặc quản trị viên mới xem được kết quả.", 404: "Không tìm thấy bài tập." }))
        }
      })
    return () => {
      cancelled = true
    }
  }, [exerciseId, version])

  async function confirmDelete() {
    if (!data) return
    setDeleting(true)
    try {
      await exerciseApi.remove(exerciseId)
      toast.success(`Đã xóa bài tập ${data.exercise.title}`)
      navigate(area("/exercises"), { replace: true })
    } catch (err) {
      toast.error(errorText(err))
      setDeleting(false)
    }
  }

  const breadcrumbs = [{ label: "Bài tập", to: "/exercises" }, { label: data?.exercise.title ?? "Kết quả" }]

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader title="Kết quả bài tập" breadcrumbs={breadcrumbs} />
        <ErrorState title="Không mở được bài tập này" message={error} onRetry={() => setVersion((value) => value + 1)} />
      </div>
    )
  }
  if (!data) return <LoadingState cards={4} rows={5} />

  const { exercise, students, questions, averageScore } = data
  const inProgress = students.filter((student) => student.status === "IN_PROGRESS").length

  const columns: Column<StudentResult>[] = [
    {
      key: "student",
      header: "Sinh viên",
      mobile: "title",
      cell: (row) => (
        <div className="flex min-w-0 items-center gap-3">
          <UserAvatar name={row.fullName} />
          <div className="min-w-0">
            <p className="truncate font-medium text-foreground">
              {row.fullName ?? <span className="font-normal text-muted-foreground">Sinh viên #{row.studentId}</span>}
            </p>
            <p className="truncate text-xs font-normal text-muted-foreground">{row.email ?? "Đã rời lớp hoặc chưa lấy được thông tin"}</p>
          </div>
        </div>
      ),
    },
    { key: "status", header: "Trạng thái", mobile: "trailing", cell: (row) => <StatusBadge meta={EXERCISE_STATUS_META[row.status]} /> },
    {
      key: "score",
      header: "Điểm",
      align: "right",
      cell: (row) =>
        row.score === null ? (
          <span className="text-muted-foreground">—</span>
        ) : (
          <span className={cn("font-semibold tabular-nums", scoreTone(row.score))}>{formatScore(row.score)}</span>
        ),
    },
    {
      key: "correct",
      header: "Câu đúng",
      align: "right",
      cell: (row) => (row.correctCount === null ? <span className="text-muted-foreground">—</span> : <span className="tabular-nums">{row.correctCount} / {exercise.questionCount}</span>),
    },
    {
      key: "submittedAt",
      header: "Nộp lúc",
      cell: (row) => <span className="text-muted-foreground tabular-nums">{row.submittedAt ? formatDateTime(row.submittedAt) : "—"}</span>,
    },
    {
      key: "actions",
      header: "Bài làm",
      align: "right",
      cell: (row) =>
        row.submissionId === null ? (
          <span className="text-muted-foreground">—</span>
        ) : (
          <Button variant="ghost" size="sm" onClick={() => setViewing(row)}>
            <Eye />
            Xem bài làm
          </Button>
        ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title={exercise.title}
        breadcrumbs={breadcrumbs}
        description={
          <>
            {exercise.className ?? `Lớp #${exercise.classId}`} · giao ngày {formatDateTime(exercise.createdAt)}
            {exercise.description ? <span className="mt-1 block whitespace-pre-wrap text-foreground">{exercise.description}</span> : null}
          </>
        }
        actions={
          <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
            <Trash2 />
            Xóa bài tập
          </Button>
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Đã nộp"
          value={`${exercise.submittedCount ?? 0} / ${exercise.studentCount ?? "?"}`}
          icon={<Send />}
          accent="emerald"
          hint={inProgress > 0 ? `${inProgress} sinh viên đang làm` : "Sinh viên trong lớp đã nộp bài"}
        />
        <StatCard
          title="Điểm trung bình"
          value={averageScore === null ? "—" : <span className={scoreTone(averageScore)}>{formatScore(averageScore)}</span>}
          icon={<Award />}
          accent="blue"
          hint="Thang điểm 10, tính trên các bài đã nộp"
        />
        <StatCard
          title="Hạn nộp"
          value={<span className="text-lg">{formatDateTime(exercise.dueAt)}</span>}
          icon={<CalendarClock />}
          accent="amber"
          hint={exercise.overdue ? "Đã quá hạn, sinh viên không nộp được nữa" : timeLeft(exercise.dueAt)}
        />
        <StatCard title="Câu hỏi" value={exercise.questionCount} icon={<ListChecks />} accent="violet" hint="Sao chép từ ngân hàng lúc giao bài" />
      </div>

      <section className="space-y-3">
        <div>
          <h2 className="font-heading text-lg font-semibold tracking-tight">Kết quả của sinh viên</h2>
          <p className="text-sm text-muted-foreground">Danh sách sinh viên do Class Service cung cấp, điểm do Exercise Service chấm.</p>
        </div>
        <DataTable columns={columns} data={students} rowKey={(row) => row.studentId} emptyTitle="Lớp chưa có sinh viên" />
      </section>

      <section className="space-y-3">
        <div>
          <h2 className="font-heading text-lg font-semibold tracking-tight">Thống kê theo câu hỏi</h2>
          <p className="flex items-start gap-1.5 text-sm text-muted-foreground">
            <Copy className="mt-0.5 size-3.5 shrink-0" aria-hidden />
            Đây là bản sao câu hỏi lúc giao bài. Sửa ngân hàng câu hỏi sau đó không làm đổi bài này hay điểm của sinh viên.
          </p>
        </div>
        <div className="space-y-4">
          {questions.map((question) => (
            <Card key={question.id}>
              <CardHeader>
                <CardTitle className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                  Câu {question.position}
                  <RateBadge rate={question.correctRate} />
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="font-medium whitespace-pre-wrap">{question.content}</p>
                <AnswerOptions question={question} mode="key" correct={question.correctOption} />
                <Explanation text={question.explanation} />
                {question.correctRate !== null ? (
                  <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted" aria-hidden>
                    <div className={cn("h-full rounded-full", rateColor(question.correctRate))} style={{ width: `${question.correctRate}%` }} />
                  </div>
                ) : null}
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <Dialog
        open={viewing !== null}
        onOpenChange={(open) => {
          if (!open) setViewing(null)
        }}
      >
        <DialogContent className="max-h-[calc(100svh-4rem)] overflow-y-auto sm:max-w-3xl">
          {viewing?.submissionId ? <SubmissionDetail key={viewing.submissionId} student={viewing} submissionId={viewing.submissionId} /> : null}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Xóa bài tập này?"
        description={`Bài tập ${exercise.title} và toàn bộ bài làm, điểm của sinh viên sẽ bị xóa. Thao tác này không hoàn tác được.`}
        confirmLabel="Xóa bài tập"
        variant="destructive"
        isPending={deleting}
        onConfirm={confirmDelete}
      />
    </div>
  )
}

function rateColor(rate: number): string {
  if (rate >= 80) return "bg-emerald-500"
  if (rate >= 50) return "bg-amber-500"
  return "bg-rose-500"
}

function RateBadge({ rate }: { rate: number | null }) {
  if (rate === null) {
    return (
      <Badge variant="secondary" className={TONE.slate}>
        Chưa có bài nộp
      </Badge>
    )
  }
  const tone = rate >= 80 ? TONE.emerald : rate >= 50 ? TONE.amber : TONE.rose
  return (
    <Badge variant="secondary" className={tone}>
      {rate}% làm đúng
    </Badge>
  )
}

/** One student's attempt, loaded when the dialog opens. */
function SubmissionDetail({ student, submissionId }: { student: StudentResult; submissionId: number }) {
  const [submission, setSubmission] = useState<Submission | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    exerciseApi
      .submission(submissionId)
      .then((result) => {
        if (!cancelled) setSubmission(result)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err))
      })
    return () => {
      cancelled = true
    }
  }, [submissionId])

  const name = student.fullName ?? `Sinh viên #${student.studentId}`
  return (
    <div className="space-y-4">
      <DialogHeader>
        <DialogTitle>Bài làm của {name}</DialogTitle>
        <DialogDescription>
          {submission?.submittedAt ? `Nộp lúc ${formatDateTime(submission.submittedAt)}` : "Sinh viên đang làm, chưa nộp bài."}
        </DialogDescription>
      </DialogHeader>
      {error ? (
        <ErrorState title="Không tải được bài làm" message={error} />
      ) : !submission ? (
        <div className="space-y-3" aria-busy="true">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-48 w-full" />
        </div>
      ) : (
        <>
          {submission.status === "SUBMITTED" ? <ScoreCard result={submission} label="Điểm" /> : null}
          <ReviewQuestionList questions={submission.questions} chosenLabel="Sinh viên chọn" />
        </>
      )}
    </div>
  )
}
