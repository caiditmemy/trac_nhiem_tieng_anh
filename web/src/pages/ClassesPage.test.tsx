import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { MemoryRouter } from "react-router"
import { afterEach, describe, expect, it, vi } from "vitest"

import { ClassesPage } from "@/pages/ClassesPage"
import { AuthContext, fakeGateway, sessionOf, userWith } from "@/test/gateway"
import type { ClassInfo, Role } from "@/types"

const classroom: ClassInfo = {
  id: 7,
  name: "Tiếng Anh 10A1",
  code: "K7M2QX",
  description: "Học thứ ba",
  teacherId: 3,
  teacherName: "Trần Thị B",
  studentCount: 12,
  createdAt: "2026-10-01T00:00:00Z",
}

function renderAs(role: Role) {
  return render(
    <AuthContext value={sessionOf(userWith(role, role === "TEACHER" ? 3 : 2))}>
      <MemoryRouter>
        <ClassesPage />
      </MemoryRouter>
    </AuthContext>
  )
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe("ClassesPage", () => {
  it("shows the classes a lecturer teaches with their invite code", async () => {
    fakeGateway({ "GET /classes": [classroom] })
    renderAs("TEACHER")

    expect(await screen.findByText("Tiếng Anh 10A1")).toBeTruthy()
    expect(screen.getByText("K7M2QX")).toBeTruthy()
    expect(screen.getByRole("button", { name: /Tạo lớp/ })).toBeTruthy()
  })

  it("lets a lecturer create a class and shows the new class with its code", async () => {
    let created = false
    const { calls } = fakeGateway({
      "GET /classes": () => (created ? [classroom] : []),
      "POST /classes": () => {
        created = true
        return classroom
      },
    })
    renderAs("TEACHER")

    fireEvent.click((await screen.findAllByRole("button", { name: /Tạo lớp/ }))[0])
    const dialog = await screen.findByRole("dialog")
    fireEvent.change(within(dialog).getByLabelText("Tên lớp"), { target: { value: "Tiếng Anh 10A1" } })
    fireEvent.click(within(dialog).getByRole("button", { name: "Tạo lớp" }))

    await waitFor(() => expect(calls.some((call) => call.method === "POST" && call.path === "/classes")).toBe(true))
    expect(calls.find((call) => call.method === "POST")?.body).toEqual({ name: "Tiếng Anh 10A1", description: "" })
    expect(await screen.findByText("K7M2QX")).toBeTruthy()
  })

  it("lets a student join with an invite code", async () => {
    let joined = false
    const { calls } = fakeGateway({
      "GET /classes": () => (joined ? [classroom] : []),
      "POST /classes/join": () => {
        joined = true
        return classroom
      },
    })
    renderAs("STUDENT")

    fireEvent.click((await screen.findAllByRole("button", { name: /Tham gia lớp/ }))[0])
    const dialog = await screen.findByRole("dialog")
    fireEvent.change(within(dialog).getByLabelText("Mã lớp"), { target: { value: "k7m2qx" } })
    fireEvent.click(within(dialog).getByRole("button", { name: "Tham gia" }))

    await waitFor(() => expect(calls.some((call) => call.path === "/classes/join")).toBe(true))
    expect(calls.find((call) => call.path === "/classes/join")?.body).toEqual({ code: "k7m2qx" })
    expect(await screen.findByText("Tiếng Anh 10A1")).toBeTruthy()
  })

  it("explains a wrong invite code", async () => {
    const { fetcher } = fakeGateway({ "GET /classes": [] })
    renderAs("STUDENT")
    fetcher.mockImplementation(async (_url: string, options?: RequestInit) =>
      options?.method === "POST"
        ? new Response(JSON.stringify({ status: 404, message: "Class not found" }), { status: 404 })
        : new Response("[]", { status: 200 })
    )

    fireEvent.click((await screen.findAllByRole("button", { name: /Tham gia lớp/ }))[0])
    const dialog = await screen.findByRole("dialog")
    fireEvent.change(within(dialog).getByLabelText("Mã lớp"), { target: { value: "ZZZZZZ" } })
    fireEvent.click(within(dialog).getByRole("button", { name: "Tham gia" }))

    expect(await screen.findByText("Không tìm thấy lớp với mã này.")).toBeTruthy()
  })
})
