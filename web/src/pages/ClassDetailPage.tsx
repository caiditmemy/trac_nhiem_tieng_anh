import { GraduationCap, KeyRound, Pencil, Plus, Trash2, UserCheck, UserMinus } from "lucide-react"
import { useEffect, useState } from "react"
import { Link, useNavigate, useParams } from "react-router"
import { toast } from "sonner"

import { ClassFormDialog } from "@/components/ClassFormDialog"
import { ClassCode } from "@/components/common/ClassCode"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { DataTable, type Column } from "@/components/common/DataTable"
import { ErrorState } from "@/components/common/ErrorState"
import { LoadingState } from "@/components/common/LoadingState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { UserAvatar } from "@/components/common/UserAvatar"
import { Button, buttonVariants } from "@/components/ui/button"
import { errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { useAuth } from "@/lib/auth"
import { classApi } from "@/lib/classes"
import { OVERDUE_META, formatDateTime, timeLeft, useExercises } from "@/lib/exercises"
import { formatDate, formatNumber } from "@/lib/format"
import type { ClassInfo, ClassStudent, ExerciseSummary } from "@/types"

/** /classes/:id for the class's lecturer and for admins. */
export function ClassDetailPage() {
  const area = useAreaPath()
  const classId = Number(useParams().id)
  const navigate = useNavigate()
  const { user } = useAuth()
  const classExercises = useExercises(classId)
  const [info, setInfo] = useState<ClassInfo | null>(null)
  const [students, setStudents] = useState<ClassStudent[]>([])
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [removeTarget, setRemoveTarget] = useState<ClassStudent | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    let cancelled = false
    Promise.all([classApi.get(classId), classApi.students(classId)])
      .then(([classInfo, list]) => {
        if (cancelled) return
        setInfo(classInfo)
        setStudents(list)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err, { 400: "Không tìm thấy lớp.", 403: "Bạn không phải giảng viên của lớp này.", 404: "Không tìm thấy lớp." }))
      })
    return () => {
      cancelled = true
    }
  }, [classId, version])

  async function confirmRemove() {
    if (!removeTarget) return
    setPending(true)
    try {
      await classApi.removeStudent(classId, removeTarget.studentId)
      setStudents((list) => list.filter((item) => item.studentId !== removeTarget.studentId))
      setInfo((current) => (current ? { ...current, studentCount: current.studentCount - 1 } : current))
      toast.success(`Đã xóa ${removeTarget.fullName ?? "sinh viên"} khỏi lớp`)
      setRemoveTarget(null)
    } catch (err) {
      toast.error(errorText(err))
    } finally {
      setPending(false)
    }
  }

  async function confirmDelete() {
    if (!info) return
    setPending(true)
    try {
      await classApi.remove(classId)
      toast.success(`Đã xóa lớp ${info.name}`)
      navigate(area("/classes"), { replace: true })
    } catch (err) {
      toast.error(errorText(err))
      setPending(false)
    }
  }

  const breadcrumbs = [{ label: "Lớp học", to: "/classes" }, { label: info?.name ?? "Chi tiết" }]

  if (error) {
    return (
      <div className="space-y-6">
        <PageHeader title="Chi tiết lớp" breadcrumbs={breadcrumbs} />
        <ErrorState title="Không mở được lớp này" message={error} onRetry={() => setVersion((value) => value + 1)} />
      </div>
    )
  }
  if (!info) return <LoadingState cards={3} rows={4} />

  const columns: Column<ClassStudent>[] = [
    {
      key: "student",
      header: "Sinh viên",
      mobile: "title",
      cell: (row) => (
        <div className="flex min-w-0 items-center gap-3">
          <UserAvatar name={row.fullName} />
          <div className="min-w-0">
            <p className="truncate font-medium text-foreground">
              {row.fullName ?? <span className="font-normal text-muted-foreground">Chưa lấy được tên (#{row.studentId})</span>}
            </p>
            <p className="truncate text-xs text-muted-foreground">{row.email ?? "—"}</p>
          </div>
        </div>
      ),
    },
    { key: "joined", header: "Ngày tham gia", cell: (row) => <span className="text-muted-foreground tabular-nums">{formatDate(row.joinedAt)}</span> },
    {
      key: "actions",
      header: "",
      align: "right",
      mobile: "trailing",
      cell: (row) => (
        <Button variant="ghost" size="sm" className="text-muted-foreground hover:text-destructive" onClick={() => setRemoveTarget(row)}>
          <UserMinus />
          <span className="hidden sm:inline">Xóa khỏi lớp</span>
        </Button>
      ),
    },
  ]

  const exerciseColumns: Column<ExerciseSummary>[] = [
    {
      key: "title",
      header: "Bài tập",
      mobile: "title",
      cell: (row) => (
        <div className="min-w-0">
          <p className="truncate font-medium text-foreground">{row.title}</p>
          <p className="truncate text-xs font-normal text-muted-foreground">{row.questionCount} câu hỏi</p>
        </div>
      ),
    },
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
  ]
  const isOwner = user?.id === info.teacherId

  return (
    <div className="space-y-6">
      <PageHeader
        title={info.name}
        description={info.description ?? "Chưa có mô tả"}
        breadcrumbs={breadcrumbs}
        actions={
          <>
            <Button variant="outline" onClick={() => setEditOpen(true)}>
              <Pencil />
              Sửa thông tin
            </Button>
            <Button variant="destructive" onClick={() => setDeleteOpen(true)}>
              <Trash2 />
              Xóa lớp
            </Button>
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard title="Mã lớp" value={<ClassCode code={info.code} />} icon={<KeyRound />} accent="blue" hint="Sinh viên nhập mã này để tham gia" />
        <StatCard title="Sinh viên" value={formatNumber(info.studentCount)} icon={<GraduationCap />} accent="teal" hint="Đang ở trong lớp" />
        <StatCard
          title="Giảng viên"
          value={<span className="text-lg">{info.teacherName ?? `#${info.teacherId}`}</span>}
          icon={<UserCheck />}
          accent="violet"
          hint={`Tạo ngày ${formatDate(info.createdAt)}`}
        />
      </div>

      <section className="space-y-3">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="font-heading text-lg font-semibold tracking-tight">Bài tập của lớp</h2>
            <p className="text-sm text-muted-foreground">Exercise Service lưu bài tập, mỗi bài giữ bản sao câu hỏi lúc giao.</p>
          </div>
          {isOwner ? (
            <Link to={area(`/exercises/new?classId=${info.id}`)} className={buttonVariants({ size: "sm" })}>
              <Plus />
              Giao bài tập
            </Link>
          ) : null}
        </div>
        {classExercises.error ? (
          <ErrorState title="Không tải được bài tập của lớp" message={classExercises.error} onRetry={classExercises.reload} />
        ) : (
          <DataTable
            columns={exerciseColumns}
            data={classExercises.exercises}
            rowKey={(row) => row.id}
            isLoading={classExercises.loading}
            skeletonRows={2}
            onRowClick={(row) => navigate(area(`/exercises/${row.id}`))}
            emptyTitle="Lớp chưa có bài tập"
            emptyDescription={isOwner ? "Chọn câu hỏi từ ngân hàng để giao bài cho lớp." : "Giảng viên của lớp chưa giao bài tập nào."}
          />
        )}
      </section>

      <section className="space-y-3">
        <div>
          <h2 className="font-heading text-lg font-semibold tracking-tight">Sinh viên trong lớp</h2>
          <p className="text-sm text-muted-foreground">Họ tên do Class Service lấy từ Auth Service.</p>
        </div>
        <DataTable
          columns={columns}
          data={students}
          rowKey={(row) => row.studentId}
          emptyTitle="Chưa có sinh viên"
          emptyDescription={<>Gửi mã lớp <strong className="font-mono tracking-widest text-foreground">{info.code}</strong> cho sinh viên để các em tham gia.</>}
        />
      </section>

      <ClassFormDialog
        open={editOpen}
        onOpenChange={setEditOpen}
        initial={info}
        onSaved={(saved) => {
          setInfo(saved)
          toast.success("Đã lưu thông tin lớp")
        }}
      />

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Xóa lớp này?"
        description={`Lớp ${info.name} và danh sách ${info.studentCount} sinh viên sẽ bị xóa. Thao tác này không hoàn tác được.`}
        confirmLabel="Xóa lớp"
        variant="destructive"
        isPending={pending}
        onConfirm={confirmDelete}
      />

      <ConfirmDialog
        open={removeTarget !== null}
        onOpenChange={(open) => {
          if (!open) setRemoveTarget(null)
        }}
        title="Xóa sinh viên khỏi lớp?"
        description={removeTarget ? `${removeTarget.fullName ?? `Sinh viên #${removeTarget.studentId}`} sẽ không còn ở lớp ${info.name}.` : undefined}
        confirmLabel="Xóa khỏi lớp"
        variant="destructive"
        isPending={pending}
        onConfirm={confirmRemove}
      />
    </div>
  )
}
