import { GraduationCap, UserCheck, Users } from "lucide-react"
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
import { formatNumber } from "@/lib/format"
import { ROLE_META } from "@/lib/labels"
import type { User } from "@/types"

/** /admin: greeting, role and the user counts from Auth Service. */
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
          <Link to="/admin/users" className={buttonVariants({ variant: "outline", size: "sm" })}>
            <Users />
            Quản lý người dùng
          </Link>
        }
      />

      <UserStats />
    </div>
  )
}

// Defined outside the component so its identity never changes between renders
const loadUsers = () => api<User[]>("/users")

/** Admin: counts of the users, from Auth Service through the gateway. */
function UserStats() {
  const [users, setUsers] = useState<User[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    loadUsers()
      .then((result) => {
        if (cancelled) return
        setUsers(result)
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
  if (!users) {
    return (
      <div className="grid gap-4 sm:grid-cols-3" aria-busy="true">
        {[0, 1, 2].map((index) => (
          <Skeleton key={index} className="h-28 rounded-xl" />
        ))}
      </div>
    )
  }
  const count = (predicate: (user: User) => boolean) => users.filter(predicate).length
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <StatCard
        title="Người dùng"
        value={formatNumber(users.length)}
        icon={<Users />}
        accent="violet"
        hint={`${formatNumber(count((u) => u.status === "LOCKED"))} tài khoản đang bị khóa`}
      />
      <StatCard title="Giảng viên" value={formatNumber(count((u) => u.role === "TEACHER"))} icon={<UserCheck />} accent="blue" hint="Do quản trị viên tạo tài khoản" />
      <StatCard title="Sinh viên" value={formatNumber(count((u) => u.role === "STUDENT"))} icon={<GraduationCap />} accent="teal" hint="Tự đăng ký" />
    </div>
  )
}
