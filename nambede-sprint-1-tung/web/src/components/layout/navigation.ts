import { ClipboardList, LayoutDashboard, School, type LucideIcon } from "lucide-react"
import type { Role } from "@/types"
import { BRANCH_ROLE, BRANCH_AREA } from "@/lib/branch"
export interface NavItem { to: string; label: string; icon: LucideIcon }
export function portalNavItems(role: Role): NavItem[] {
  if (role !== BRANCH_ROLE) return []
  return [
    { to: BRANCH_AREA, label: "Tổng quan", icon: LayoutDashboard },
    { to: `${BRANCH_AREA}/classes`, label: "Lớp học", icon: School },
    { to: `${BRANCH_AREA}/exercises`, label: "Bài tập", icon: ClipboardList },
  ]
}
export function isNavItemActive(pathname: string, to: string): boolean {
  return pathname === to || (to !== BRANCH_AREA && pathname.startsWith(`${to}/`))
}
