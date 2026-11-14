import { useEffect, useState } from "react"

import { api, errorText } from "@/lib/api"
import { TONE, type EnumMeta } from "@/lib/labels"
import type { ExerciseDetail, ExerciseSummary } from "@/types"

interface NewExercise {
  classId: number
  title: string
  description: string
  dueAt: string
  questionIds: number[]
}

/** Exercise Service calls, always through the gateway (/api/exercises). */
export const exerciseApi = {
  list: (classId?: number) => api<ExerciseSummary[]>(classId === undefined ? "/exercises" : `/exercises?classId=${classId}`),
  create: (body: NewExercise) => api<ExerciseSummary>("/exercises", { method: "POST", body }),
  get: (id: number) => api<ExerciseDetail>(`/exercises/${id}`),
  remove: (id: number) => api<void>(`/exercises/${id}`, { method: "DELETE" }),
}

/** Exercises the caller can see (lecturer: assigned, student: of joined classes, admin: all), optionally of one class. */
export function useExercises(classId?: number) {
  const [exercises, setExercises] = useState<ExerciseSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    exerciseApi
      .list(classId)
      .then((result) => {
        if (cancelled) return
        setExercises(result)
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
  }, [classId, version])

  return { exercises, loading, error, reload: () => setVersion((value) => value + 1) }
}

export const OVERDUE_META: EnumMeta = { label: "Quá hạn", badge: TONE.rose }

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" })
}

/** "Còn 3 ngày", "Còn 5 giờ", "Đã quá hạn". */
export function timeLeft(dueAt: string): string {
  const ms = new Date(dueAt).getTime() - Date.now()
  if (ms <= 0) return "Đã quá hạn"
  const hours = Math.floor(ms / 3_600_000)
  if (hours >= 24) return `Còn ${Math.floor(hours / 24)} ngày`
  if (hours >= 1) return `Còn ${hours} giờ`
  return `Còn ${Math.max(1, Math.floor(ms / 60_000))} phút`
}

/** Value for <input type="datetime-local"> from a Date, in local time. */
export function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
