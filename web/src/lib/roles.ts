import type { Role } from "@/types"

/** Home page (and URL prefix of the area) of each role. */
const ROLE_HOME: Record<Role, string> = {
  ADMIN: "/admin",
  TEACHER: "/teacher",
  STUDENT: "/student",
}

/** Where a signed-in user lands: /admin, /teacher or /student. */
export function roleHome(role: Role): string {
  return ROLE_HOME[role]
}

/** True when `path` is the role's home or a page under it ("/admin2" is not under "/admin"). */
export function isInRoleArea(role: Role, path: string): boolean {
  const home = roleHome(role)
  return path === home || path.startsWith(`${home}/`)
}

/** Page to go back to after sign-in: the remembered one only when it is in the user's own area. */
export function postLoginPath(role: Role, from?: string | null): string {
  return from && isInRoleArea(role, from) ? from : roleHome(role)
}
