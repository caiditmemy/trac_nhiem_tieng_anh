import { useEffect, useState } from "react"

import { api, errorText } from "@/lib/api"
import type { ClassInfo, ClassStudent } from "@/types"

interface ClassForm {
  name: string
  description: string
}

/** Class Service calls, always through the gateway (/api/classes/...). */
export const classApi = {
  list: () => api<ClassInfo[]>("/classes"),
  get: (id: number) => api<ClassInfo>(`/classes/${id}`),
  create: (body: ClassForm) => api<ClassInfo>("/classes", { method: "POST", body }),
  update: (id: number, body: ClassForm) => api<ClassInfo>(`/classes/${id}`, { method: "PUT", body }),
  remove: (id: number) => api<void>(`/classes/${id}`, { method: "DELETE" }),
  join: (code: string) => api<ClassInfo>("/classes/join", { method: "POST", body: { code } }),
  leave: (id: number) => api<void>(`/classes/${id}/leave`, { method: "POST" }),
  students: (id: number) => api<ClassStudent[]>(`/classes/${id}/students`),
  removeStudent: (id: number, studentId: number) =>
    api<void>(`/classes/${id}/students/${studentId}`, { method: "DELETE" }),
}

/** The caller's classes (teacher: taught, student: joined, admin: all). Call reload() after a change. */
export function useClasses() {
  const [classes, setClasses] = useState<ClassInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    classApi
      .list()
      .then((result) => {
        if (cancelled) return
        setClasses(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [version])

  return { classes, loading, error, reload: () => setVersion((value) => value + 1) }
}
