import { BookOpenCheck, ClipboardList, LayoutDashboard, School, Users, type LucideIcon } from "lucide-react"

import { roleHome } from "@/lib/roles"
import type { Role } from "@/types"

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
}

export interface NavGroup {
  label: string
  items: NavItem[]
}

export interface Crumb {
  label: string
  to?: string
}

/** Admin sidebar, grouped. */
export const ADMIN_NAV_GROUPS: NavGroup[] = [
  { label: "Tổng quan", items: [{ to: "/admin", label: "Bảng điều khiển", icon: LayoutDashboard }] },
  {
    label: "Quản lý",
    items: [
      { to: "/admin/users", label: "Người dùng", icon: Users },
      { to: "/admin/classes", label: "Lớp học", icon: School },
    ],
  },
  {
    label: "Học tập",
    items: [
      { to: "/admin/questions", label: "Ngân hàng câu hỏi", icon: BookOpenCheck },
      { to: "/admin/exercises", label: "Bài tập", icon: ClipboardList },
    ],
  },
]

/** Teacher top navigation. */
export const TEACHER_NAV_ITEMS: NavItem[] = [
  { to: "/teacher", label: "Tổng quan", icon: LayoutDashboard },
  { to: "/teacher/classes", label: "Lớp học", icon: School },
  { to: "/teacher/questions", label: "Ngân hàng câu hỏi", icon: BookOpenCheck },
  { to: "/teacher/exercises", label: "Bài tập", icon: ClipboardList },
]

/** Student top navigation. */
export const STUDENT_NAV_ITEMS: NavItem[] = [
  { to: "/student", label: "Tổng quan", icon: LayoutDashboard },
  { to: "/student/classes", label: "Lớp của tôi", icon: School },
  { to: "/student/exercises", label: "Bài tập", icon: ClipboardList },
]

/** Top navigation of the teacher and student areas; the admin area uses the sidebar. */
export function portalNavItems(role: Role): NavItem[] {
  if (role === "TEACHER") return TEACHER_NAV_ITEMS
  if (role === "STUDENT") return STUDENT_NAV_ITEMS
  return []
}

const ROLE_HOMES = ["/admin", "/teacher", "/student"]

/** /admin/users/4 still highlights "Người dùng"; a role home (/admin) is active only on an exact match. */
export function isNavItemActive(pathname: string, to: string): boolean {
  if (pathname === to) return true
  return !ROLE_HOMES.includes(to) && pathname.startsWith(`${to}/`)
}

const SEGMENT_LABELS: Record<string, string> = {
  users: "Người dùng",
  classes: "Lớp học",
  questions: "Ngân hàng câu hỏi",
  exercises: "Bài tập",
  new: "Giao bài tập",
  profile: "Tài khoản của tôi",
}

/** Breadcrumb for the admin header, e.g. /admin/users/4 gives "Bảng điều khiển / Người dùng / Chi tiết". */
export function buildBreadcrumbs(pathname: string): Crumb[] {
  const home = roleHome("ADMIN")
  const segments = pathname.split("/").filter(Boolean)
  const root: Crumb = { label: "Bảng điều khiển", to: home }
  const rest = segments[0] === "admin" ? segments.slice(1) : segments
  if (rest.length === 0) return [{ label: root.label }]

  const crumbs: Crumb[] = [root]
  let prefix = home
  rest.forEach((segment, index) => {
    prefix = `${prefix}/${segment}`
    const label = /^\d+$/.test(segment) ? "Chi tiết" : (SEGMENT_LABELS[segment] ?? segment)
    crumbs.push({ label, to: index === rest.length - 1 ? undefined : prefix })
  })
  return crumbs
}
