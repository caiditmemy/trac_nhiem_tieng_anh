import { cleanup, render, screen } from "@testing-library/react"
import { MemoryRouter, Route, Routes, useLocation } from "react-router"
import { afterEach, describe, expect, it, vi } from "vitest"

import { HomeRedirect, RequireAuth } from "@/components/AuthProvider"
import { AuthContext, type AuthContextValue } from "@/lib/auth"
import type { Role, User } from "@/types"

function userWith(role: Role): User {
  return {
    id: 2,
    email: "user2@nambede.local",
    fullName: "Nguyễn Văn A",
    role,
    status: "ACTIVE",
    accountType: "FREE",
    createdAt: "2026-10-01T00:00:00Z",
  }
}

function session(overrides: Partial<AuthContextValue>): AuthContextValue {
  return {
    user: null,
    loading: false,
    signedOut: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
    ...overrides,
  }
}

/** Shows where the guard sent the user, and the page it remembered for after sign-in. */
function Landed({ name }: { name: string }) {
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from
  return <p>{from ? `${name} from ${from}` : name}</p>
}

/** The three role areas, like in App.tsx. */
function renderAt(path: string, value: AuthContextValue) {
  return render(
    <AuthContext value={value}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<Landed name="login" />} />
          <Route path="/" element={<HomeRedirect />} />
          <Route element={<RequireAuth roles={["ADMIN"]} />}>
            <Route path="/admin" element={<Landed name="admin" />} />
            <Route path="/admin/users" element={<Landed name="admin users" />} />
          </Route>
          <Route element={<RequireAuth roles={["TEACHER"]} />}>
            <Route path="/teacher" element={<Landed name="teacher" />} />
          </Route>
          <Route element={<RequireAuth roles={["STUDENT"]} />}>
            <Route path="/student" element={<Landed name="student" />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthContext>
  )
}

describe("HomeRedirect", () => {
  afterEach(cleanup)

  it("sends a visitor who is not signed in to /login", () => {
    renderAt("/", session({ user: null }))
    expect(screen.getByText("login")).toBeTruthy()
  })

  it("sends an ADMIN to /admin", () => {
    renderAt("/", session({ user: userWith("ADMIN") }))
    expect(screen.getByText("admin")).toBeTruthy()
  })

  it("sends a TEACHER to /teacher", () => {
    renderAt("/", session({ user: userWith("TEACHER") }))
    expect(screen.getByText("teacher")).toBeTruthy()
  })

  it("sends a STUDENT to /student", () => {
    renderAt("/", session({ user: userWith("STUDENT") }))
    expect(screen.getByText("student")).toBeTruthy()
  })
})

describe("RequireAuth", () => {
  afterEach(cleanup)

  it("sends a visitor who is not signed in to /login and remembers the page", () => {
    renderAt("/admin/users", session({ user: null }))

    expect(screen.getByText("login from /admin/users")).toBeTruthy()
  })

  it("does not remember the page after the user signed out", () => {
    renderAt("/admin/users", session({ user: null, signedOut: true }))

    expect(screen.getByText("login")).toBeTruthy()
  })

  it("sends a STUDENT opening /admin/users to /student", () => {
    renderAt("/admin/users", session({ user: userWith("STUDENT") }))

    expect(screen.getByText("student")).toBeTruthy()
  })

  it("sends a TEACHER opening /student to /teacher", () => {
    renderAt("/student", session({ user: userWith("TEACHER") }))

    expect(screen.getByText("teacher")).toBeTruthy()
  })

  it("sends an ADMIN opening /teacher to /admin", () => {
    renderAt("/teacher", session({ user: userWith("ADMIN") }))

    expect(screen.getByText("admin")).toBeTruthy()
  })

  it("shows each area to its own role", () => {
    renderAt("/admin/users", session({ user: userWith("ADMIN") }))
    expect(screen.getByText("admin users")).toBeTruthy()
    cleanup()

    renderAt("/teacher", session({ user: userWith("TEACHER") }))
    expect(screen.getByText("teacher")).toBeTruthy()
    cleanup()

    renderAt("/student", session({ user: userWith("STUDENT") }))
    expect(screen.getByText("student")).toBeTruthy()
  })

  it("waits while the saved session is being restored", () => {
    renderAt("/admin/users", session({ loading: true }))

    expect(screen.queryByText(/login|admin|teacher|student/)).toBeNull()
  })
})
