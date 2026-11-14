import { CalendarDays, ChevronRight, Eye, LogOut, MoreHorizontal, Plus, School, Search, Trash2 } from "lucide-react"
import { useState, type FormEvent } from "react"
import { Link, useNavigate } from "react-router"
import { toast } from "sonner"

import { ClassFormDialog } from "@/components/ClassFormDialog"
import { ClassCode } from "@/components/common/ClassCode"
import { ConfirmDialog } from "@/components/common/ConfirmDialog"
import { DataTable, type Column } from "@/components/common/DataTable"
import { EmptyState } from "@/components/common/EmptyState"
import { ErrorState } from "@/components/common/ErrorState"
import { FormDialogShell } from "@/components/common/FormDialogShell"
import { PageHeader } from "@/components/common/PageHeader"
import { UserAvatar } from "@/components/common/UserAvatar"
import { Button, buttonVariants } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { InputGroup, InputGroupAddon, InputGroupInput } from "@/components/ui/input-group"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { errorText } from "@/lib/api"
import { useAreaPath } from "@/lib/area"
import { useAuth } from "@/lib/auth"
import { classApi, useClasses } from "@/lib/classes"
import { formatDate, formatNumber } from "@/lib/format"
import type { ClassInfo } from "@/types"

/** /classes: lecturers manage their classes, students join with a code, admins see every class. */
export function ClassesPage() {
  const { user } = useAuth()
  if (!user) return null
  if (user.role === "TEACHER") return <TeacherClasses />
  if (user.role === "STUDENT") return <StudentClasses />
  return <AdminClasses />
}

const ICON_TILE = "flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300"

function CardsSkeleton() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" aria-busy="true">
      {Array.from({ length: 3 }, (_, index) => (
        <div key={index} className="space-y-4 rounded-xl bg-card p-4 ring-1 ring-foreground/10">
          <div className="flex gap-3">
            <Skeleton className="size-10 rounded-lg" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-2/3" />
              <Skeleton className="h-3 w-full" />
            </div>
          </div>
          <Skeleton className="h-6 w-1/2" />
        </div>
      ))}
    </div>
  )
}

/* --------------------------------------------------------------- Lecturer */

function TeacherClasses() {
  const area = useAreaPath()
  const { classes, loading, error, reload } = useClasses()
  const [createOpen, setCreateOpen] = useState(false)
  const createButton = (
    <Button onClick={() => setCreateOpen(true)}>
      <Plus />
      Tạo lớp
    </Button>
  )

  return (
    <div className="space-y-6">
      <PageHeader title="Lớp học của tôi" description="Tạo lớp rồi gửi mã lớp cho sinh viên để các em tham gia." actions={createButton} />

      {error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : loading ? (
        <CardsSkeleton />
      ) : classes.length === 0 ? (
        <div className="rounded-xl bg-card ring-1 ring-foreground/10">
          <EmptyState icon={<School />} title="Bạn chưa có lớp nào" description="Tạo lớp đầu tiên, hệ thống sẽ sinh mã lớp để bạn gửi cho sinh viên." action={createButton} />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {classes.map((item) => (
            <Card key={item.id} className="transition-shadow hover:shadow-md">
              <CardHeader>
                <div className="flex items-start gap-3">
                  <span className={ICON_TILE}>
                    <School className="size-5" />
                  </span>
                  <div className="min-w-0 space-y-1">
                    <CardTitle className="truncate">
                      <Link to={area(`/classes/${item.id}`)} className="hover:underline">
                        {item.name}
                      </Link>
                    </CardTitle>
                    <CardDescription className="line-clamp-2">{item.description ?? "Chưa có mô tả"}</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex flex-wrap items-center justify-between gap-2">
                <ClassCode code={item.code} />
                <span className="text-sm text-muted-foreground tabular-nums">{formatNumber(item.studentCount)} sinh viên</span>
              </CardContent>
              <CardFooter className="justify-between text-xs text-muted-foreground">
                <span className="inline-flex items-center gap-1.5">
                  <CalendarDays className="size-3.5" />
                  {formatDate(item.createdAt)}
                </span>
                <Link to={area(`/classes/${item.id}`)} className={buttonVariants({ variant: "ghost", size: "sm" })}>
                  Quản lý lớp
                  <ChevronRight />
                </Link>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <ClassFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSaved={(created) => {
          reload()
          toast.success(`Đã tạo lớp ${created.name}`, { description: `Mã lớp: ${created.code}` })
        }}
      />
    </div>
  )
}

/* ---------------------------------------------------------------- Student */

function StudentClasses() {
  const { classes, loading, error, reload } = useClasses()
  const [joinOpen, setJoinOpen] = useState(false)
  const [leaveTarget, setLeaveTarget] = useState<ClassInfo | null>(null)
  const [leaving, setLeaving] = useState(false)
  const joinButton = (
    <Button onClick={() => setJoinOpen(true)}>
      <Plus />
      Tham gia lớp
    </Button>
  )

  async function confirmLeave() {
    if (!leaveTarget) return
    setLeaving(true)
    try {
      await classApi.leave(leaveTarget.id)
      toast.success(`Đã rời lớp ${leaveTarget.name}`)
      setLeaveTarget(null)
      reload()
    } catch (err) {
      toast.error(errorText(err))
    } finally {
      setLeaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Lớp học của tôi" description="Nhập mã lớp do giảng viên gửi để tham gia lớp." actions={joinButton} />

      {error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : loading ? (
        <CardsSkeleton />
      ) : classes.length === 0 ? (
        <div className="rounded-xl bg-card ring-1 ring-foreground/10">
          <EmptyState icon={<School />} title="Bạn chưa tham gia lớp nào" description="Hỏi giảng viên mã lớp gồm 6 ký tự, rồi bấm Tham gia lớp." action={joinButton} />
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {classes.map((item) => (
            <Card key={item.id}>
              <CardHeader>
                <div className="flex items-start gap-3">
                  <span className={ICON_TILE}>
                    <School className="size-5" />
                  </span>
                  <div className="min-w-0 space-y-1">
                    <CardTitle className="truncate">{item.name}</CardTitle>
                    <CardDescription className="line-clamp-2">{item.description ?? "Chưa có mô tả"}</CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex items-center gap-2.5 text-sm">
                <UserAvatar name={item.teacherName} size="sm" />
                <span className="min-w-0 truncate">
                  <span className="text-muted-foreground">Giảng viên </span>
                  <span className="font-medium">{item.teacherName ?? "chưa lấy được tên"}</span>
                </span>
              </CardContent>
              <CardFooter className="justify-between text-xs text-muted-foreground">
                <span className="tabular-nums">{formatNumber(item.studentCount)} sinh viên trong lớp</span>
                <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" onClick={() => setLeaveTarget(item)}>
                  <LogOut />
                  Rời lớp
                </Button>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <JoinClassDialog
        open={joinOpen}
        onOpenChange={setJoinOpen}
        onJoined={(joined) => {
          reload()
          toast.success(`Đã tham gia lớp ${joined.name}`)
        }}
      />

      <ConfirmDialog
        open={leaveTarget !== null}
        onOpenChange={(open) => {
          if (!open) setLeaveTarget(null)
        }}
        title="Rời lớp này?"
        description={leaveTarget ? `Bạn sẽ không còn ở lớp ${leaveTarget.name}. Muốn vào lại thì nhập mã lớp lần nữa.` : undefined}
        confirmLabel="Rời lớp"
        variant="destructive"
        isPending={leaving}
        onConfirm={confirmLeave}
      />
    </div>
  )
}

function JoinClassDialog({ open, onOpenChange, onJoined }: { open: boolean; onOpenChange: (open: boolean) => void; onJoined: (joined: ClassInfo) => void }) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const code = String(new FormData(event.currentTarget).get("code")).trim()
    setSubmitting(true)
    setError(null)
    try {
      onJoined(await classApi.join(code))
      onOpenChange(false)
    } catch (err) {
      setError(errorText(err, { 404: "Không tìm thấy lớp với mã này.", 409: "Bạn đã ở trong lớp này rồi." }))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <FormDialogShell
      open={open}
      onOpenChange={(next) => {
        if (next) setError(null)
        onOpenChange(next)
      }}
      title="Tham gia lớp"
      description="Nhập mã lớp gồm 6 ký tự do giảng viên gửi. Không phân biệt chữ hoa, chữ thường."
      onSubmit={handleSubmit}
      isSubmitting={submitting}
      submitLabel="Tham gia"
      error={error}
      contentClassName="sm:max-w-md"
    >
      <div className="grid gap-2">
        <Label htmlFor="class-code" className="sr-only">
          Mã lớp
        </Label>
        <Input
          id="class-code"
          name="code"
          placeholder="K7M2QX"
          maxLength={12}
          autoComplete="off"
          autoFocus
          required
          className="h-14 text-center font-mono text-2xl font-semibold tracking-[0.4em] uppercase placeholder:tracking-[0.4em] md:text-2xl"
        />
      </div>
    </FormDialogShell>
  )
}

/* ------------------------------------------------------------------ Admin */

function AdminClasses() {
  const area = useAreaPath()
  const navigate = useNavigate()
  const { classes, loading, error, reload } = useClasses()
  const [search, setSearch] = useState("")
  const [deleteTarget, setDeleteTarget] = useState<ClassInfo | null>(null)
  const [deleting, setDeleting] = useState(false)

  const query = search.trim().toLowerCase()
  const visible = classes.filter(
    (item) =>
      !query ||
      item.name.toLowerCase().includes(query) ||
      item.code.toLowerCase().includes(query) ||
      (item.teacherName ?? "").toLowerCase().includes(query)
  )

  async function confirmDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await classApi.remove(deleteTarget.id)
      toast.success(`Đã xóa lớp ${deleteTarget.name}`)
      setDeleteTarget(null)
      reload()
    } catch (err) {
      toast.error(errorText(err))
    } finally {
      setDeleting(false)
    }
  }

  const columns: Column<ClassInfo>[] = [
    {
      key: "name",
      header: "Lớp học",
      mobile: "title",
      cell: (row) => (
        <div className="min-w-0">
          <p className="truncate font-medium text-foreground">{row.name}</p>
          <p className="max-w-xs truncate text-xs font-normal text-muted-foreground">{row.description ?? "Chưa có mô tả"}</p>
        </div>
      ),
    },
    {
      key: "teacher",
      header: "Giảng viên",
      cell: (row) => (
        <span className="inline-flex items-center gap-2">
          <UserAvatar name={row.teacherName} size="sm" />
          {row.teacherName ?? <span className="text-muted-foreground">#{row.teacherId}</span>}
        </span>
      ),
    },
    { key: "code", header: "Mã lớp", cell: (row) => <ClassCode code={row.code} /> },
    { key: "students", header: "Sinh viên", align: "right", cell: (row) => <span className="tabular-nums">{formatNumber(row.studentCount)}</span> },
    { key: "created", header: "Ngày tạo", cell: (row) => <span className="text-muted-foreground tabular-nums">{formatDate(row.createdAt)}</span> },
    {
      key: "actions",
      header: "",
      align: "right",
      mobile: "trailing",
      cell: (row) => (
        <span onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
          <DropdownMenu>
            <DropdownMenuTrigger render={<Button variant="ghost" size="icon-sm" aria-label={`Thao tác với lớp ${row.name}`} />}>
              <MoreHorizontal />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-44">
              <DropdownMenuItem onClick={() => navigate(area(`/classes/${row.id}`))}>
                <Eye />
                Xem chi tiết
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onClick={() => setDeleteTarget(row)}>
                <Trash2 />
                Xóa lớp
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </span>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader title="Lớp học" description="Tất cả lớp trong hệ thống. Lớp do giảng viên tạo, sinh viên tham gia bằng mã lớp." />

      <div className="flex flex-col gap-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10 sm:flex-row sm:items-center">
        <InputGroup className="sm:max-w-sm">
          <InputGroupAddon>
            <Search />
          </InputGroupAddon>
          <InputGroupInput placeholder="Tìm theo tên lớp, mã lớp, giảng viên…" value={search} onChange={(event) => setSearch(event.target.value)} />
        </InputGroup>
        <p className="text-sm text-muted-foreground sm:ml-auto">{loading ? "Đang tải…" : `${visible.length} / ${classes.length} lớp`}</p>
      </div>

      {error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (
        <DataTable
          columns={columns}
          data={visible}
          rowKey={(row) => row.id}
          isLoading={loading}
          onRowClick={(row) => navigate(area(`/classes/${row.id}`))}
          emptyTitle={classes.length === 0 ? "Chưa có lớp nào" : "Không có lớp khớp từ khóa"}
        />
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteTarget(null)
        }}
        title="Xóa lớp này?"
        description={deleteTarget ? `Lớp ${deleteTarget.name} và danh sách ${deleteTarget.studentCount} sinh viên trong lớp sẽ bị xóa.` : undefined}
        confirmLabel="Xóa lớp"
        variant="destructive"
        isPending={deleting}
        onConfirm={confirmDelete}
      />
    </div>
  )
}
