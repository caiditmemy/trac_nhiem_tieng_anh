import { lazy } from "react"
import { BrowserRouter, Route, Routes } from "react-router"

import { AuthProvider, HomeRedirect, RequireAuth } from "@/components/AuthProvider"
import { AppShell } from "@/components/layout/AppShell"
import { PortalShell } from "@/components/layout/PortalShell"
import { ThemeProvider } from "@/components/layout/ThemeProvider"
import { Toaster } from "@/components/ui/sonner"
import { TooltipProvider } from "@/components/ui/tooltip"
import { useTheme } from "@/lib/theme"
import { LoginPage } from "@/pages/LoginPage"
import { NotFoundPage } from "@/pages/NotFoundPage"
import { RegisterPage } from "@/pages/RegisterPage"

// Pages behind sign-in load when first opened (the shells show a skeleton meanwhile)
const ProfilePage = lazy(() => import("@/pages/ProfilePage").then((module) => ({ default: module.ProfilePage })))
const AdminDashboardPage = lazy(() => import("@/pages/admin/AdminDashboardPage").then((module) => ({ default: module.AdminDashboardPage })))
const UsersPage = lazy(() => import("@/pages/admin/UsersPage").then((module) => ({ default: module.UsersPage })))
const TeacherDashboardPage = lazy(() => import("@/pages/teacher/TeacherDashboardPage").then((module) => ({ default: module.TeacherDashboardPage })))
const ClassesPage = lazy(() => import("@/pages/ClassesPage").then((module) => ({ default: module.ClassesPage })))
const ClassDetailPage = lazy(() => import("@/pages/ClassDetailPage").then((module) => ({ default: module.ClassDetailPage })))
const QuestionBankPage = lazy(() => import("@/pages/QuestionBankPage").then((module) => ({ default: module.QuestionBankPage })))
const ExercisesPage = lazy(() => import("@/pages/ExercisesPage").then((module) => ({ default: module.ExercisesPage })))
const ExerciseDetailPage = lazy(() => import("@/pages/ExerciseDetailPage").then((module) => ({ default: module.ExerciseDetailPage })))
const AssignExercisePage = lazy(() => import("@/pages/AssignExercisePage").then((module) => ({ default: module.AssignExercisePage })))
const StudentDashboardPage = lazy(() => import("@/pages/student/StudentDashboardPage").then((module) => ({ default: module.StudentDashboardPage })))

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
              <Route element={<RequireAuth roles={["ADMIN"]} />}>
                <Route path="/admin" element={<AppShell />}>
                  <Route index element={<AdminDashboardPage />} />
                  <Route path="users" element={<UsersPage />} />
                  <Route path="classes" element={<ClassesPage />} />
                  <Route path="classes/:id" element={<ClassDetailPage />} />
                  <Route path="questions" element={<QuestionBankPage />} />
                  <Route path="exercises" element={<ExercisesPage />} />
                  <Route path="exercises/:id" element={<ExerciseDetailPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                </Route>
              </Route>
              <Route element={<RequireAuth roles={["TEACHER"]} />}>
                <Route path="/teacher" element={<PortalShell />}>
                  <Route index element={<TeacherDashboardPage />} />
                  <Route path="classes" element={<ClassesPage />} />
                  <Route path="classes/:id" element={<ClassDetailPage />} />
                  <Route path="questions" element={<QuestionBankPage />} />
                  <Route path="exercises" element={<ExercisesPage />} />
                  <Route path="exercises/new" element={<AssignExercisePage />} />
                  <Route path="exercises/:id" element={<ExerciseDetailPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                </Route>
              </Route>
              <Route element={<RequireAuth roles={["STUDENT"]} />}>
                <Route path="/student" element={<PortalShell />}>
                  <Route index element={<StudentDashboardPage />} />
                  <Route path="classes" element={<ClassesPage />} />
                  <Route path="exercises" element={<ExercisesPage />} />
                  <Route path="exercises/:id" element={<ExerciseDetailPage />} />
                  <Route path="profile" element={<ProfilePage />} />
                </Route>
              </Route>
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </BrowserRouter>
          <AppToaster />
        </AuthProvider>
      </TooltipProvider>
    </ThemeProvider>
  )
}
