import type { Role } from "@/types"
import { BRANCH_ROLE, BRANCH_AREA } from "./branch"

export function roleHome(role: Role): string {
  return role === BRANCH_ROLE ? BRANCH_AREA : "/unavailable"
}

export function isInRoleArea(role: Role, path: string): boolean {
  const home = roleHome(role)
  return path === home || (role === BRANCH_ROLE && path.startsWith(`${home}/`))
}

export function postLoginPath(role: Role, from?: string | null): string {
  return from && isInRoleArea(role, from) ? from : roleHome(role)
}
