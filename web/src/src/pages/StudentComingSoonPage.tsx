import { LogOut } from "lucide-react"
import { useNavigate } from "react-router"
import { toast } from "sonner"

import { AuthShell } from "@/components/AuthShell"
import { ThemeToggle } from "@/components/layout/ThemeToggle"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/lib/auth"
import { useDocumentTitle } from "@/lib/document-title"

/** /coming-soon: where a STUDENT lands in Sprint 1. The student pages arrive in a later sprint. */
export function StudentComingSoonPage() {
  useDocumentTitle("Sắp có trang sinh viên")
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) return null

  const signOut = () => {
    logout()
    toast.success("Đã đăng xuất")
    navigate("/login", { replace: true })
  }

  return (
    <AuthShell title={`Xin chào ${user.fullName}`} description={user.email}>
      <p className="text-sm leading-relaxed">
        Trang dành cho sinh viên sẽ có ở sprint sau. Tài khoản của bạn đã được tạo thành công.
      </p>
      <div className="mt-6 flex items-center gap-2">
        <Button onClick={signOut} className="flex-1">
          <LogOut aria-hidden="true" />
          Đăng xuất
        </Button>
        <ThemeToggle />
      </div>
    </AuthShell>
  )
}
