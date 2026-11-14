import { CalendarClock, Copy, ListChecks, School, Trash2, Users } from "lucide-react"
import { useEffect, useState } from "react"
import { useNavigate, useParams } from "react-router"
import { toast } from "sonner"

import { AnswerOptions } from "@/components/AnswerOptions"
import { Explanation } from "@/components/Explanation"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { ErrorState } from "@/components/common/ErrorState"
import { LoadingState } from "@/components/common/LoadingState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { exerciseApi, formatDateTime, timeLeft } from "@/lib/exercises"
import type { ExerciseDetail } from "@/types"

/** /exercises/:id for the lecturer who assigned it and for admins: the copied questions with their answer key. */
export function ExerciseDetailPage() {
  const area = useAreaPath()
  const exerciseId = Number(useParams().id)
  const navigate = useNavigate()
  const [data, setData] = useState<ExerciseDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    let cancelled = false
    exerciseApi
      .get(exerciseId)
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(errorText(err, { 400: "Không tìm thấy bài tập.", 403: "Chỉ giảng viên đã giao bài này hoặc quản trị viên mới mở được bài tập.", 404: "Không tìm thấy bài tập." }))
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

  const breadcrumbs = [{ label: "Bài tập", to: "/exercises" }, { label: data?.exercise.title ?? "Chi tiết" }]

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader title="Bài tập" breadcrumbs={breadcrumbs} />
        <ErrorState title="Không mở được bài tập này" message={error} onRetry={() => setVersion((value) => value + 1)} />
      </div>
    )
  }
  if (!data) return <LoadingState cards={3} rows={3} />

  const { exercise, questions } = data

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
        <StatCard title="Lớp" value={<span className="text-lg">{exercise.className ?? `#${exercise.classId}`}</span>} icon={<School />} accent="blue" hint="Lớp được giao bài" />
        <StatCard title="Sinh viên" value={exercise.studentCount ?? "—"} icon={<Users />} accent="teal" hint="Sinh viên trong lớp" />
        <StatCard
          title="Hạn nộp"
          value={<span className="text-lg">{formatDateTime(exercise.dueAt)}</span>}
          icon={<CalendarClock />}
          accent="amber"
          hint={exercise.overdue ? "Đã quá hạn" : timeLeft(exercise.dueAt)}
        />
        <StatCard title="Câu hỏi" value={exercise.questionCount} icon={<ListChecks />} accent="violet" hint="Sao chép từ ngân hàng lúc giao bài" />
      </div>

      <section className="space-y-3">
        <div>
          <h2 className="font-heading text-lg font-semibold tracking-tight">Câu hỏi của bài tập</h2>
          <p className="flex items-start gap-1.5 text-sm text-muted-foreground">
            <Copy className="mt-0.5 size-3.5 shrink-0" aria-hidden />
            Đây là bản sao câu hỏi lúc giao bài. Sửa ngân hàng câu hỏi sau đó không làm đổi bài này.
          </p>
        </div>
        <div className="space-y-4">
          {questions.map((question) => (
            <Card key={question.id}>
              <CardHeader>
                <CardTitle className="text-sm text-muted-foreground">Câu {question.position}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="font-medium whitespace-pre-wrap">{question.content}</p>
                <AnswerOptions question={question} correct={question.correctOption} />
                <Explanation text={question.explanation} />
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Xóa bài tập này?"
        description={`Bài tập ${exercise.title} sẽ bị xóa khỏi lớp. Thao tác này không hoàn tác được.`}
        confirmLabel="Xóa bài tập"
        variant="destructive"
        isPending={deleting}
        onConfirm={confirmDelete}
      />
    </div>
  )
}
