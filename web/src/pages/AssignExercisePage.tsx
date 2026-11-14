import { BookOpenCheck, CheckCheck, Copy, LoaderCircle, RotateCcw, School, Search, Send, X } from "lucide-react"
import { useEffect, useState, type FormEvent } from "react"
import { Link, useNavigate, useSearchParams } from "react-router"
import { toast } from "sonner"

import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Label } from "@/components/ui/label"
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select"
import { Skeleton } from "@/components/ui/skeleton"
import { Textarea } from "@/components/ui/textarea"
import { ApiError, errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { useClasses } from "@/lib/classes"
import { exerciseApi, formatDateTime, toLocalInputValue } from "@/lib/exercises"
import { TONE } from "@/lib/labels"
import { ANSWER_OPTIONS, optionText, questionApi, topicApi } from "@/lib/questions"
import { cn } from "@/lib/utils"
import type { Question, Topic } from "@/types"

/** Same limit as Exercise Service. */
const MAX_QUESTIONS = 50

/** A week from now at 23:59, local time. */
function defaultDueAt(): string {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  date.setHours(23, 59, 0, 0)
  return toLocalInputValue(date)
}

function createError(err: unknown): string {
  if (err instanceof ApiError && err.status === 400) {
    if (/does not exist/i.test(err.message)) return "Có câu hỏi vừa bị xóa khỏi ngân hàng. Bấm Tải lại ở danh sách câu hỏi rồi chọn lại."
    if (/due date/i.test(err.message)) return "Hạn nộp phải sau thời điểm hiện tại."
  }
  return errorText(err, { 403: "Bạn chỉ giao bài được cho lớp mình dạy.", 404: "Không tìm thấy lớp. Có thể lớp vừa bị xóa." })
}

/** /exercises/new for lecturers: a class, a due date and questions picked from the shared bank. */
export function AssignExercisePage() {
  const area = useAreaPath()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { classes, loading: classesLoading, error: classesError, reload: reloadClasses } = useClasses()
  const [bank, setBank] = useState<{ topics: Topic[]; questions: Question[] } | null>(null)
  const [bankError, setBankError] = useState<string | null>(null)
  const [bankVersion, setBankVersion] = useState(0)

  const [classId, setClassId] = useState(params.get("classId") ?? "")
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [dueAt, setDueAt] = useState(defaultDueAt)
  const [selected, setSelected] = useState<number[]>([])
  const [topicFilter, setTopicFilter] = useState("")
  const [search, setSearch] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    Promise.all([topicApi.list(), questionApi.list()])
      .then(([topics, questions]) => {
        if (cancelled) return
        setBank({ topics, questions })
        setBankError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setBankError(errorText(err))
      })
    return () => {
      cancelled = true
    }
  }, [bankVersion])

  const chosenClass = classes.find((item) => String(item.id) === classId)
  const questions = bank?.questions ?? []
  const byId = new Map(questions.map((question) => [question.id, question]))
  const query = search.trim().toLowerCase()
  const visible = questions.filter(
    (question) =>
      (!topicFilter || String(question.topicId) === topicFilter) &&
      (!query ||
        question.content.toLowerCase().includes(query) ||
        ANSWER_OPTIONS.some((option) => optionText(question, option).toLowerCase().includes(query)))
  )
  const unselectedVisible = visible.filter((question) => !selected.includes(question.id))

  function toggle(id: number) {
    setError(null)
    setSelected((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]))
  }

  function selectVisible() {
    if (selected.length + unselectedVisible.length > MAX_QUESTIONS) {
      setError(`Mỗi bài tối đa ${MAX_QUESTIONS} câu hỏi.`)
      return
    }
    setError(null)
    setSelected((current) => [...current, ...unselectedVisible.map((question) => question.id)])
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const due = new Date(dueAt)
    let problem: string | null = null
    if (!chosenClass) problem = "Chọn lớp để giao bài."
    else if (!title.trim()) problem = "Nhập tiêu đề bài tập."
    else if (Number.isNaN(due.getTime()) || due.getTime() <= Date.now()) problem = "Hạn nộp phải sau thời điểm hiện tại."
    else if (selected.length === 0) problem = "Chọn ít nhất một câu hỏi từ ngân hàng."
    else if (selected.length > MAX_QUESTIONS) problem = `Mỗi bài tối đa ${MAX_QUESTIONS} câu hỏi.`
    if (problem || !chosenClass) {
      setError(problem)
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      const created = await exerciseApi.create({
        classId: chosenClass.id,
        title: title.trim(),
        description: description.trim(),
        dueAt: due.toISOString(),
        questionIds: selected,
      })
      toast.success(`Đã giao bài tập cho lớp ${chosenClass.name}`, {
        description: `${created.questionCount} câu hỏi, hạn nộp ${formatDateTime(created.dueAt)}`,
      })
      navigate(area(`/exercises/${created.id}`), { replace: true })
    } catch (err) {
      setError(createError(err))
      setSubmitting(false)
    }
  }

  const header = (
    <PageHeader
      title="Giao bài tập"
      description="Chọn lớp, hạn nộp và các câu hỏi trong ngân hàng. Câu hỏi được sao chép vào bài tập ngay lúc giao."
      breadcrumbs={[{ label: "Bài tập", to: "/exercises" }, { label: "Giao bài tập" }]}
    />
  )

  if (classesError) {
    return (
      <div className="space-y-6">
        {header}
        <ErrorState title="Không tải được lớp học" message={classesError} onRetry={reloadClasses} />
      </div>
    )
  }
  if (!classesLoading && classes.length === 0) {
    return (
      <div className="space-y-6">
        {header}
        <div className="rounded-xl bg-card ring-1 ring-foreground/10">
          <EmptyState
            icon={<School />}
            title="Bạn chưa có lớp nào"
            description="Tạo lớp và mời sinh viên tham gia trước, rồi mới giao bài tập."
            action={
              <Link to={area("/classes")} className={buttonVariants({ size: "sm" })}>
                Tạo lớp
              </Link>
            }
          />
        </div>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-6">
      {header}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="min-w-0 space-y-6">
          <Card>
            <CardHeader className="border-b">
              <CardTitle>Thông tin bài tập</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="exercise-class">Lớp</Label>
                <NativeSelect
                  id="exercise-class"
                  value={chosenClass ? classId : ""}
                  onChange={(event) => {
                    setClassId(event.target.value)
                    setError(null)
                  }}
                  disabled={classesLoading}
                  className="w-full"
                >
                  <NativeSelectOption value="" disabled>
                    {classesLoading ? "Đang tải lớp…" : "Chọn lớp"}
                  </NativeSelectOption>
                  {classes.map((item) => (
                    <NativeSelectOption key={item.id} value={String(item.id)}>
                      {item.name} ({item.studentCount} SV)
                    </NativeSelectOption>
                  ))}
                </NativeSelect>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="exercise-due">Hạn nộp</Label>
                <Input
                  id="exercise-due"
                  type="datetime-local"
                  value={dueAt}
                  onChange={(event) => {
                    setDueAt(event.target.value)
                    setError(null)
                  }}
                  required
                />
              </div>
              <div className="grid gap-2 sm:col-span-2">
                <Label htmlFor="exercise-title">Tiêu đề</Label>
                <Input
                  id="exercise-title"
                  value={title}
                  onChange={(event) => {
                    setTitle(event.target.value)
                    setError(null)
                  }}
                  placeholder="VD: Bài 1 - Thì hiện tại đơn"
                  maxLength={255}
                  required
                />
              </div>
              <div className="grid gap-2 sm:col-span-2">
                <Label htmlFor="exercise-description">Mô tả</Label>
                <Textarea
                  id="exercise-description"
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  placeholder="Không bắt buộc, VD: ôn tập trước buổi kiểm tra giữa kỳ"
                  maxLength={2000}
                  rows={3}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="border-b">
              <CardTitle>Chọn câu hỏi từ ngân hàng</CardTitle>
              <CardDescription>Câu hỏi trong bài theo đúng thứ tự bạn chọn.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {bankError ? (
                <ErrorState title="Không tải được ngân hàng câu hỏi" message={bankError} onRetry={() => setBankVersion((value) => value + 1)} />
              ) : !bank ? (
                <div className="space-y-2" aria-busy="true">
                  {Array.from({ length: 4 }, (_, index) => (
                    <Skeleton key={index} className="h-16 w-full" />
                  ))}
                </div>
              ) : questions.length === 0 ? (
                <EmptyState
                  icon={<BookOpenCheck />}
                  title="Ngân hàng chưa có câu hỏi"
                  description="Soạn câu hỏi trong ngân hàng trước, rồi quay lại giao bài."
                  action={
                    <Link to={area("/questions")} className={buttonVariants({ size: "sm" })}>
                      Mở ngân hàng câu hỏi
                    </Link>
                  }
                />
              ) : (
                <>
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                    <InputGroup className="sm:max-w-xs">
                      <InputGroupAddon>
                        <Search />
                      </InputGroupAddon>
                      <InputGroupInput placeholder="Tìm câu hỏi…" value={search} onChange={(event) => setSearch(event.target.value)} />
                    </InputGroup>
                    <NativeSelect aria-label="Lọc theo chủ đề" value={topicFilter} onChange={(event) => setTopicFilter(event.target.value)} className="sm:w-52">
                      <NativeSelectOption value="">Tất cả chủ đề</NativeSelectOption>
                      {bank.topics.map((topic) => (
                        <NativeSelectOption key={topic.id} value={String(topic.id)}>
                          {topic.name} ({topic.questionCount})
                        </NativeSelectOption>
                      ))}
                    </NativeSelect>
                    <div className="flex items-center gap-1 sm:ml-auto">
                      <Button type="button" variant="ghost" size="sm" onClick={selectVisible} disabled={unselectedVisible.length === 0}>
                        <CheckCheck />
                        Chọn tất cả ({unselectedVisible.length})
                      </Button>
                      <Button type="button" variant="ghost" size="icon-sm" aria-label="Tải lại câu hỏi" onClick={() => setBankVersion((value) => value + 1)}>
                        <RotateCcw />
                      </Button>
                    </div>
                  </div>

                  {visible.length === 0 ? (
                    <p className="py-6 text-center text-sm text-muted-foreground">Không có câu hỏi khớp bộ lọc.</p>
                  ) : (
                    <ul className="space-y-2">
                      {visible.map((question) => {
                        const order = selected.indexOf(question.id)
                        const checked = order >= 0
                        return (
                          <li key={question.id}>
                            <label
                              className={cn(
                                "flex cursor-pointer items-start gap-3 rounded-lg p-3 ring-1 ring-foreground/10 transition-colors hover:bg-muted/60",
                                checked && "bg-primary/5 ring-2 ring-primary hover:bg-primary/5"
                              )}
                            >
                              <input type="checkbox" className="mt-0.5 size-4 shrink-0 accent-primary" checked={checked} onChange={() => toggle(question.id)} />
                              <span className="min-w-0 flex-1 space-y-1.5">
                                <span className="block text-sm font-medium whitespace-pre-wrap text-foreground">{question.content}</span>
                                <span className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-muted-foreground">
                                  <Badge variant="secondary" className={TONE.blue}>
                                    {question.topicName}
                                  </Badge>
                                  <span className="min-w-0 break-words">
                                    Đáp án đúng {question.correctOption}: {optionText(question, question.correctOption)}
                                  </span>
                                </span>
                              </span>
                              {checked ? (
                                <span className="shrink-0 rounded-full bg-primary px-2 py-0.5 text-xs font-semibold text-primary-foreground tabular-nums">
                                  Câu {order + 1}
                                </span>
                              ) : null}
                            </label>
                          </li>
                        )
                      })}
                    </ul>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </div>

        <aside className="h-fit lg:sticky lg:top-20">
          <Card>
            <CardHeader className="border-b">
              <CardTitle>Câu đã chọn ({selected.length})</CardTitle>
              {selected.length > 0 ? (
                <CardAction>
                  <Button type="button" variant="ghost" size="xs" onClick={() => setSelected([])}>
                    Bỏ chọn hết
                  </Button>
                </CardAction>
              ) : null}
            </CardHeader>
            <CardContent className="space-y-4">
              {selected.length === 0 ? (
                <p className="text-sm text-muted-foreground">Chưa chọn câu nào. Tích vào câu hỏi trong ngân hàng để thêm vào bài.</p>
              ) : (
                <ol className="max-h-72 space-y-1.5 overflow-y-auto" aria-label="Câu hỏi đã chọn">
                  {selected.map((id, index) => (
                    <li key={id} className="flex items-start gap-2 text-sm">
                      <span className="w-6 shrink-0 pt-0.5 text-muted-foreground tabular-nums">{index + 1}.</span>
                      <span className="line-clamp-2 min-w-0 flex-1 pt-0.5">{byId.get(id)?.content ?? `Câu hỏi #${id}`}</span>
                      <Button type="button" variant="ghost" size="icon-xs" aria-label={`Bỏ câu ${index + 1}`} onClick={() => toggle(id)}>
                        <X />
                      </Button>
                    </li>
                  ))}
                </ol>
              )}

              <div className="flex gap-2 rounded-lg bg-muted/60 p-3 text-xs text-muted-foreground">
                <Copy className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                <p>Bài tập giữ bản sao của câu hỏi. Sửa ngân hàng sau này không làm đổi bài đã giao hay điểm của sinh viên.</p>
              </div>

              {error ? (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              ) : null}

              <Button type="submit" className="w-full" disabled={submitting}>
                {submitting ? <LoaderCircle className="animate-spin" /> : <Send />}
                {submitting ? "Đang giao bài…" : "Giao bài"}
              </Button>
            </CardContent>
          </Card>
        </aside>
      </div>
    </form>
  )
}
