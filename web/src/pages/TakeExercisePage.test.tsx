import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { MemoryRouter, Route, Routes } from "react-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { TakeExercisePage } from "@/pages/TakeExercisePage"
import { AuthContext, fakeGateway, sessionOf, userWith } from "@/test/gateway"
import type { ExerciseQuestion, Submission } from "@/types"

const dueAt = new Date(Date.now() + 3 * 86_400_000).toISOString()

function question(id: number, position: number, content: string): ExerciseQuestion {
  return {
    id,
    position,
    content,
    optionA: "go",
    optionB: "goes",
    optionC: "gone",
    optionD: "going",
    chosenOption: null,
    correctOption: null,
    explanation: null,
    correct: null,
    correctRate: null,
  }
}

const open: Submission = {
  id: 50,
  exerciseId: 4,
  exerciseTitle: "Bài 1: Thì hiện tại đơn",
  exerciseDescription: null,
  dueAt,
  status: "IN_PROGRESS",
  startedAt: "2026-10-01T00:00:00Z",
  submittedAt: null,
  score: null,
  correctCount: null,
  questionCount: 2,
  questions: [question(101, 1, "She ___ to school."), question(102, 2, "They ___ football.")],
}

const graded: Submission = {
  ...open,
  status: "SUBMITTED",
  submittedAt: "2026-10-02T00:00:00Z",
  score: 5,
  correctCount: 1,
  questions: [
    { ...open.questions[0], chosenOption: "B", correctOption: "B", correct: true, explanation: "Ngôi thứ ba số ít." },
    { ...open.questions[1], chosenOption: null, correctOption: "A", correct: false, explanation: null },
  ],
}

function renderPage() {
  return render(
    <AuthContext value={sessionOf(userWith("STUDENT"))}>
      <MemoryRouter initialEntries={["/student/exercises/4"]}>
        <Routes>
          <Route path="/student/exercises/:id" element={<TakeExercisePage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext>
  )
}

beforeEach(() => {
  window.scrollTo = vi.fn()
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe("TakeExercisePage", () => {
  it("opens the attempt and shows its questions without answers", async () => {
    const { calls } = fakeGateway({ "POST /exercises/4/start": open })
    renderPage()

    expect(await screen.findByText("Bài 1: Thì hiện tại đơn")).toBeTruthy()
    expect(screen.getByText("She ___ to school.")).toBeTruthy()
    expect(screen.getByText("Đã trả lời 0 / 2 câu")).toBeTruthy()
    expect(calls[0]).toMatchObject({ method: "POST", path: "/exercises/4/start" })
  })

  it("saves each picked answer, then submits and shows the score with the right answers", async () => {
    const { calls } = fakeGateway({
      "POST /exercises/4/start": open,
      "POST /submissions/50/answer": open,
      "POST /submissions/50/submit": graded,
    })
    renderPage()

    const first = await screen.findAllByRole("button", { name: /^Đáp án B: goes/ })
    fireEvent.click(first[0])
    expect(await screen.findByText("Đã trả lời 1 / 2 câu")).toBeTruthy()
    await waitFor(() => expect(calls.some((call) => call.path === "/submissions/50/answer")).toBe(true))
    expect(calls.find((call) => call.path === "/submissions/50/answer")?.body).toEqual({ exerciseQuestionId: 101, chosenOption: "B" })

    fireEvent.click(screen.getByRole("button", { name: "Nộp bài" }))
    const confirm = await screen.findByRole("alertdialog")
    expect(within(confirm).getByText(/Bạn còn 1 câu chưa trả lời/)).toBeTruthy()
    fireEvent.click(within(confirm).getByRole("button", { name: "Nộp bài" }))

    expect(await screen.findByText("Điểm của bạn")).toBeTruthy()
    expect(screen.getByText(/5,00/)).toBeTruthy()
    expect(screen.getByText("1 câu đúng")).toBeTruthy()
    expect(screen.getByText("Ngôi thứ ba số ít.")).toBeTruthy()
    // The answers are saved before the submit goes out
    const order = calls.map((call) => call.path)
    expect(order.indexOf("/submissions/50/answer")).toBeLessThan(order.indexOf("/submissions/50/submit"))
    expect(screen.queryByRole("button", { name: "Nộp bài" })).toBeNull()
  })

  it("shows the result straight away for an exercise that was already submitted", async () => {
    fakeGateway({ "POST /exercises/4/start": graded })
    renderPage()

    expect(await screen.findByText("Điểm của bạn")).toBeTruthy()
    expect(screen.queryByRole("button", { name: "Nộp bài" })).toBeNull()
  })

  it("explains why an exercise of another class cannot be opened", async () => {
    const { fetcher } = fakeGateway({})
    fetcher.mockImplementation(async () => new Response(JSON.stringify({ status: 403, message: "You are not in the class of this exercise" }), { status: 403 }))
    renderPage()

    expect(await screen.findByText("Bạn không ở trong lớp của bài tập này.")).toBeTruthy()
  })
})
