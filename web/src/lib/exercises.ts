import { useEffect, useState } from "react"

import { api, errorText } from "@/lib/api"
import { TONE, type EnumMeta } from "@/lib/labels"
import type { AnswerOption, ExerciseResults, ExerciseStatus, ExerciseSummary, Submission } from "@/types"

interface NewExercise {
  classId: number
  title: string
  description: string
  dueAt: string
  questionIds: number[]
}

/** Exercise Service calls, always through the gateway (/api/exercises, /api/submissions). */
export const exerciseApi = {
  list: (classId?: number) => api<ExerciseSummary[]>(classId === undefined ? "/exercises" : `/exercises?classId=${classId}`),
  create: (body: NewExercise) => api<ExerciseSummary>("/exercises", { method: "POST", body }),
  results: (id: number) => api<ExerciseResults>(`/exercises/${id}/results`),
  remove: (id: number) => api<void>(`/exercises/${id}`, { method: "DELETE" }),
  start: (id: number) => api<Submission>(`/exercises/${id}/start`, { method: "POST" }),
  answer: (submissionId: number, exerciseQuestionId: number, chosenOption: AnswerOption) =>
    api<Submission>(`/submissions/${submissionId}/answer`, { method: "POST", body: { exerciseQuestionId, chosenOption } }),
  submit: (submissionId: number) => api<Submission>(`/submissions/${submissionId}/submit`, { method: "POST" }),
  /** One attempt with its answers: the student's own, or any attempt for the lecturer and admins. */
  submission: (submissionId: number) => api<Submission>(`/submissions/${submissionId}`),
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

/** Still open for the student: not submitted and before the due date. Soonest due first. */
export function pendingExercises(exercises: ExerciseSummary[]): ExerciseSummary[] {
  return exercises.filter((item) => item.myStatus !== "SUBMITTED" && !item.overdue).sort((a, b) => a.dueAt.localeCompare(b.dueAt))
}

export const EXERCISE_STATUS_META: Record<ExerciseStatus, EnumMeta> = {
  NOT_STARTED: { label: "Chưa làm", badge: TONE.amber },
  IN_PROGRESS: { label: "Đang làm", badge: TONE.blue },
  SUBMITTED: { label: "Đã nộp", badge: TONE.emerald },
}

export const OVERDUE_META: EnumMeta = { label: "Quá hạn", badge: TONE.rose }

/** 6.666 gives "6,67". */
export function formatScore(score: number): string {
  return score.toLocaleString("vi-VN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("vi-VN", { hour: "2-digit", minute: "2-digit", day: "2-digit", month: "2-digit", year: "numeric" })
}

/** True once the moment has passed (e.g. a due date). */
export function isPast(iso: string): boolean {
  return new Date(iso).getTime() <= Date.now()
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

/** Tailwind colour of a score out of 10: green from 8, amber from 5, red below. */
export function scoreTone(score: number): string {
  if (score >= 8) return "text-emerald-600 dark:text-emerald-400"
  if (score >= 5) return "text-amber-600 dark:text-amber-400"
  return "text-rose-600 dark:text-rose-400"
}

/** Value for <input type="datetime-local"> from a Date, in local time. */
export function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
