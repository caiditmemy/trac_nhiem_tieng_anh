import { GraduationCap, School, UserCheck, Users } from "lucide-react"
import { useEffect, useState } from "react"
import { Link } from "react-router"

import { ErrorState } from "@/components/common/ErrorState"
import { PageHeader } from "@/components/common/PageHeader"
import { StatCard } from "@/components/common/StatCard"
import { StatusBadge } from "@/components/common/StatusBadge"
import { buttonVariants } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { api, errorText } from "@/lib/api"
import { useAuth } from "@/lib/auth"
import { classApi } from "@/lib/classes"
import { formatNumber } from "@/lib/format"
import { ROLE_META } from "@/lib/labels"
import type { ClassInfo, User } from "@/types"

/** /admin: greeting, role, the user counts from Auth Service and the classes from Class Service. */
export function AdminDashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Chào ${user.fullName}`}
        documentTitle="Tổng quan quản trị"
        description={
          <span className="inline-flex flex-wrap items-center gap-2">
            Trường Đại học Quy Nhơn
            <StatusBadge meta={ROLE_META[user.role]} />
          </span>
        }
        actions={
          <>
            <Link to="/admin/classes" className={buttonVariants({ variant: "outline", size: "sm" })}>
              <School />
              Lớp học
            </Link>
            <Link to="/admin/users" className={buttonVariants({ variant: "outline", size: "sm" })}>
              <Users />
              Quản lý người dùng
            </Link>
          </>
        }
      />

      <UserStats />
    </div>
  )
}

// Defined outside the component so its identity never changes between renders
// Classes are optional here: when Class Service is down the user counts still show
const loadUsers = () => Promise.all([api<User[]>("/users"), classApi.list().catch(() => null)])

/** Admin: counts of the users and classes, through the gateway. */
function UserStats() {
  const [data, setData] = useState<[User[], ClassInfo[] | null] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    loadUsers()
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err))
      })
    return () => {
      cancelled = true
    }
  }, [version])

  if (error) return <ErrorState title="Không tải được số liệu người dùng" message={error} onRetry={() => setVersion((value) => value + 1)} />
  if (!data) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-busy="true">
        {[0, 1, 2, 3].map((index) => (
          <Skeleton key={index} className="h-28 rounded-xl" />
        ))}
      </div>
    )
  }
  const [users, classes] = data
  const count = (predicate: (user: User) => boolean) => users.filter(predicate).length
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="Người dùng"
        value={formatNumber(users.length)}
        icon={<Users />}
        accent="violet"
        hint={`${formatNumber(count((u) => u.status === "LOCKED"))} tài khoản đang bị khóa`}
      />
      <StatCard title="Giảng viên" value={formatNumber(count((u) => u.role === "TEACHER"))} icon={<UserCheck />} accent="blue" hint="Do quản trị viên tạo tài khoản" />
      <StatCard title="Sinh viên" value={formatNumber(count((u) => u.role === "STUDENT"))} icon={<GraduationCap />} accent="teal" hint="Tự đăng ký" />
      <StatCard
        title="Lớp học"
        value={classes ? formatNumber(classes.length) : "—"}
        icon={<School />}
        accent="amber"
        hint={classes ? `${formatNumber(classes.reduce((sum, item) => sum + item.studentCount, 0))} lượt sinh viên tham gia` : "Chưa có dữ liệu"}
      />
    </div>
  )
}
