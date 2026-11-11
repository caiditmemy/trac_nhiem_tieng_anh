import { ArrowLeft, CalendarClock, Clock, LoaderCircle, Send } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import { Link, useParams } from "react-router"
import { toast } from "sonner"

import { AnswerOptions } from "@/components/AnswerOptions"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { ErrorState } from "@/components/common/ErrorState"
import { LoadingState } from "@/components/common/LoadingState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatusBadge } from "@/components/common/StatusBadge"
import { ReviewQuestionList, ScoreCard } from "@/components/SubmissionReview"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { ApiError, errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { EXERCISE_STATUS_META, OVERDUE_META, exerciseApi, formatDateTime, formatScore, isPast, timeLeft } from "@/lib/exercises"
import { TONE } from "@/lib/labels"
import type { AnswerOption, ExerciseQuestion, Submission } from "@/types"

const LOAD_ERRORS = { 400: "Không tìm thấy bài tập.", 403: "Bạn không ở trong lớp của bài tập này.", 404: "Không tìm thấy bài tập.", 409: "Bài tập đã quá hạn nộp, bạn không thể bắt đầu làm nữa." }
const SAVE_ERRORS = { 409: "Bài đã nộp hoặc đã quá hạn nộp." }

/** /exercises/:id for students: one attempt, each answer saved when clicked, score and explanations right after submitting. */
export function TakeExercisePage() {
  const area = useAreaPath()
  const exerciseId = Number(useParams().id)
  const [submission, setSubmission] = useState<Submission | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  // Answers are saved one after another, so the server always keeps the last click
  const saving = useRef<Promise<void>>(Promise.resolve())

  useEffect(() => {
    let cancelled = false
    // Opening the page twice at once can race on the one-attempt rule (409): the second call then just reads the attempt
    const open = () => exerciseApi.start(exerciseId)
    open()
      .catch((err: unknown) => (err instanceof ApiError && err.status === 409 ? open() : Promise.reject(err)))
      .then((result) => {
        if (cancelled) return
        setSubmission(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err, LOAD_ERRORS))
      })
    return () => {
      cancelled = true
    }
  }, [exerciseId, version])

  function pick(question: ExerciseQuestion, option: AnswerOption) {
    if (!submission) return
    const submissionId = submission.id
    setSubmission((current) =>
      current ? { ...current, questions: current.questions.map((item) => (item.id === question.id ? { ...item, chosenOption: option } : item)) } : current
    )
    saving.current = saving.current.then(() =>
      exerciseApi.answer(submissionId, question.id, option).then(
        () => undefined,
        (err: unknown) => {
          toast.error(errorText(err, SAVE_ERRORS))
          // Show what the server really saved
          setVersion((value) => value + 1)
        }
      )
    )
  }

  async function submit() {
    if (!submission) return
    setSubmitting(true)
    try {
      await saving.current
      const result = await exerciseApi.submit(submission.id)
      setSubmission(result)
      setConfirmOpen(false)
      toast.success(`Đã nộp bài: ${formatScore(result.score ?? 0)} / 10 điểm`)
      window.scrollTo({ top: 0, behavior: "smooth" })
    } catch (err) {
      toast.error(errorText(err, SAVE_ERRORS))
      setConfirmOpen(false)
      setVersion((value) => value + 1)
    } finally {
      setSubmitting(false)
    }
  }

  const back = (
    <Link to={area("/exercises")} className={buttonVariants({ variant: "ghost", size: "sm", className: "-ml-2" })}>
      <ArrowLeft />
      Danh sách bài tập
    </Link>
  )

  if (error) {
    return (
      <div className="space-y-4">
        {back}
        <ErrorState title="Không mở được bài tập" message={error} onRetry={() => setVersion((value) => value + 1)} />
      </div>
    )
  }
  if (!submission) return <LoadingState rows={6} />

  if (submission.status === "SUBMITTED") {
    return (
      <div className="space-y-6">
        {back}
        <PageHeader
          title={submission.exerciseTitle}
          description={submission.submittedAt ? `Đã nộp lúc ${formatDateTime(submission.submittedAt)}` : undefined}
        />
        <ScoreCard result={submission} />
        <ReviewQuestionList questions={submission.questions} />
      </div>
    )
  }

  const answered = submission.questions.filter((question) => question.chosenOption).length
  const blank = submission.questionCount - answered
  // The server refuses answers after the due date; the page shows the attempt read-only
  const closed = isPast(submission.dueAt)

  return (
    <div className="space-y-6 pb-24">
      {back}
      <PageHeader
        title={submission.exerciseTitle}
        description={
          <span className="inline-flex flex-wrap items-center gap-x-3 gap-y-1">
            <span className="inline-flex items-center gap-1.5">
              <CalendarClock className="size-4" aria-hidden />
              Hạn nộp {formatDateTime(submission.dueAt)} · {timeLeft(submission.dueAt)}
            </span>
            <StatusBadge meta={closed ? OVERDUE_META : EXERCISE_STATUS_META[submission.status]} />
          </span>
        }
      />
      {submission.exerciseDescription ? <p className="text-sm whitespace-pre-wrap">{submission.exerciseDescription}</p> : null}

      {closed ? (
        <div role="status" className="flex items-start gap-2.5 rounded-xl bg-rose-50 p-4 text-sm text-rose-900 ring-1 ring-rose-500/30 dark:bg-rose-500/10 dark:text-rose-200">
          <Clock className="mt-0.5 size-4 shrink-0" aria-hidden />
          Đã quá hạn nộp bài. Bài làm dở không được chấm điểm và không sửa được nữa.
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">Đáp án được lưu ngay khi bạn chọn. Bạn có thể rời trang và quay lại làm tiếp trước hạn nộp.</p>
      )}

      <div className="space-y-4">
        {submission.questions.map((question) => (
          <Card key={question.id}>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-sm text-muted-foreground">
                Câu {question.position}
                {question.chosenOption ? (
                  <Badge variant="secondary" className={TONE.blue}>
                    Đã chọn {question.chosenOption}
                  </Badge>
                ) : null}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="font-medium whitespace-pre-wrap">{question.content}</p>
              <AnswerOptions
                question={question}
                mode="pick"
                chosen={question.chosenOption}
                disabled={closed || submitting}
                onPick={(option) => pick(question, option)}
              />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Stays at the bottom of the screen while scrolling */}
      <div className="fixed inset-x-0 bottom-0 z-10 border-t bg-background/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3 md:px-6">
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium">
              Đã trả lời {answered} / {submission.questionCount} câu
            </p>
            <div className="mt-1.5 h-1.5 w-full max-w-xs overflow-hidden rounded-full bg-muted">
              <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${(answered / Math.max(1, submission.questionCount)) * 100}%` }} />
            </div>
          </div>
          {closed ? null : (
            <Button onClick={() => setConfirmOpen(true)} disabled={submitting}>
              {submitting ? <LoaderCircle className="animate-spin" /> : <Send />}
              Nộp bài
            </Button>
          )}
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Nộp bài?"
        description={
          blank > 0
            ? `Bạn còn ${blank} câu chưa trả lời, các câu này sẽ tính là sai. Mỗi bài chỉ nộp được một lần.`
            : "Mỗi bài chỉ nộp được một lần. Sau khi nộp bạn sẽ thấy điểm, đáp án đúng và giải thích."
        }
        confirmLabel="Nộp bài"
        isPending={submitting}
        onConfirm={submit}
      />
    </div>
  )
}
