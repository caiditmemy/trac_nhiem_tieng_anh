import { cleanup, render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router"
import { afterEach, describe, expect, it, vi } from "vitest"

import { ExercisesPage } from "@/pages/ExercisesPage"
import { AuthContext, fakeGateway, sessionOf, userWith } from "@/test/gateway"
import type { ExerciseSummary, Role } from "@/types"

const exercise: ExerciseSummary = {
  id: 4,
  classId: 7,
  className: "Tiếng Anh 10A1",
  title: "Bài 1: Thì hiện tại",
  description: null,
  dueAt: "2099-01-01T00:00:00Z",
  overdue: false,
  questionCount: 6,
  createdAt: "2026-10-01T00:00:00Z",
  studentCount: 12,
}

function renderAs(role: Role) {
  return render(
    <AuthContext value={sessionOf(userWith(role, role === "TEACHER" ? 3 : 2))}>
      <MemoryRouter>
        <ExercisesPage />
      </MemoryRouter>
    </AuthContext>
  )
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe("ExercisesPage", () => {
  it("lists the exercises a lecturer assigned, with the class and the button to assign another", async () => {
    fakeGateway({ "GET /exercises": [exercise] })
    renderAs("TEACHER")

    expect((await screen.findAllByText("Bài 1: Thì hiện tại")).length).toBeGreaterThan(0)
    expect(screen.getAllByText("Tiếng Anh 10A1").length).toBeGreaterThan(0)
    expect(screen.getAllByRole("link", { name: /Giao bài tập/ }).length).toBeGreaterThan(0)
  })

  it("shows a student the exercises of their classes without any link to open or assign", async () => {
    fakeGateway({ "GET /exercises": [{ ...exercise, studentCount: null }] })
    renderAs("STUDENT")

    expect(await screen.findByText("Bài 1: Thì hiện tại")).toBeTruthy()
    expect(screen.getByText("6 câu hỏi")).toBeTruthy()
    expect(screen.queryByRole("link", { name: /Giao bài tập/ })).toBeNull()
    expect(screen.queryByRole("link", { name: /Bài 1/ })).toBeNull()
  })

  it("tells a student with no exercise where it will appear", async () => {
    fakeGateway({ "GET /exercises": [] })
    renderAs("STUDENT")

    expect(await screen.findByText("Chưa có bài tập nào")).toBeTruthy()
  })
})
