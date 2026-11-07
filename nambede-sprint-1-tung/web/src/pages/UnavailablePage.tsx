import { Navigate } from "react-router"
import { useAuth } from "@/lib/auth"
import { BRANCH_ROLE } from "@/lib/branch"
import { ROLE_META } from "@/lib/labels"
import { AuthShell } from "@/components/AuthShell"
import { Button } from "@/components/ui/button"

export function UnavailablePage() {
  const { user, logout } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return <AuthShell title="Khu vực chưa có trong nhánh này" description={`Nhánh này dành cho ${ROLE_META[BRANCH_ROLE].label}. Tài khoản của bạn vẫn hợp lệ.`}>
    <Button onClick={logout}>Đăng xuất để đổi tài khoản</Button>
  </AuthShell>
}
