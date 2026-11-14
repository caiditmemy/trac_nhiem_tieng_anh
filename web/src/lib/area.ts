import { useCallback } from "react"

import { useAuth } from "@/lib/auth"
import { roleHome } from "@/lib/roles"

/** Pages are shared by the roles but live under each role's area: area("/classes/4") is "/teacher/classes/4" for a lecturer. */
export function useAreaPath(): (path: string) => string {
  const { user } = useAuth()
  const home = user ? roleHome(user.role) : ""
  return useCallback((path: string) => `${home}${path}`, [home])
}
