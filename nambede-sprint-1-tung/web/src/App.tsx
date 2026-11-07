import { lazy } from "react"
import { BrowserRouter, Route, Routes } from "react-router"

import { AuthProvider, HomeRedirect, RequireAuth } from "@/components/AuthProvider"
import { PortalShell } from "@/components/layout/PortalShell"
import { ThemeProvider } from "@/components/layout/ThemeProvider"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { useTheme } from "@/lib/theme"
import { ComingSoonPage } from "@/pages/ComingSoonPage"
import { LoginPage } from "@/pages/LoginPage"
import { UnavailablePage } from "@/pages/UnavailablePage"
import { NotFoundPage } from "@/pages/NotFoundPage"
import { RegisterPage } from "@/pages/RegisterPage"

// Pages behind sign-in load when first opened (the shells show a skeleton meanwhile)
const ProfilePage = lazy(() => import("@/pages/ProfilePage").then((module) => ({ default: module.ProfilePage })))
const TeacherDashboardPage = lazy(() => import("@/pages/teacher/TeacherDashboardPage").then((module) => ({ default: module.TeacherDashboardPage })))

/** Toasts follow the light / dark mode of the app. */
function AppToaster() {
  const { resolvedTheme } = useTheme()
  return <Toaster theme={resolvedTheme} position="top-right" richColors closeButton />
}

export default function App() {
  return (
    <ThemeProvider>
      <TooltipProvider delay={200}>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/" element={<HomeRedirect />} />
              <Route element={<RequireAuth roles={["TEACHER"]} />}>
                <Route path="/teacher" element={<PortalShell />}>
                  <Route index element={<TeacherDashboardPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                  <Route path="classes" element={<ComingSoonPage title="Lớp học" />} />
                  <Route path="exercises" element={<ComingSoonPage title="Bài tập" />} />
                </Route>
              </Route>
              <Route element={<RequireAuth />}><Route path="/unavailable" element={<UnavailablePage />} /></Route>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
          <AppToaster />
        </AuthProvider>
      </TooltipProvider>
    </ThemeProvider>
  )
}
