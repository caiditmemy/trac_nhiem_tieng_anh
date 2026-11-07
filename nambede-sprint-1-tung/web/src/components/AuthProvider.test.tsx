import { cleanup, render, screen } from "@testing-library/react"
import { MemoryRouter, Route, Routes, useLocation } from "react-router"
import { afterEach, expect, it, vi } from "vitest"
import { HomeRedirect, RequireAuth } from "./AuthProvider"
import { AuthContext, type AuthContextValue } from "@/lib/auth"
import { BRANCH_AREA, BRANCH_ROLE } from "@/lib/branch"
import type { Role, User } from "@/types"
const user = (role: Role): User => ({ id: 2, email: "user@test.local", fullName: "Nguyễn A", role, status: "ACTIVE", accountType: "FREE", createdAt: "2026-10-01T00:00:00Z" })
function Landed() { const location = useLocation(); return <p>{location.pathname}:{location.state?.from}</p> }
function renderAt(path: string, overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = { user: null, loading: false, signedOut: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), updateProfile: vi.fn(), changePassword: vi.fn(), ...overrides }
  return render(<AuthContext value={value}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/" element={<HomeRedirect />} />
    <Route path="/login" element={<Landed />} />
    <Route element={<RequireAuth roles={[BRANCH_ROLE]} />}><Route path={`${BRANCH_AREA}/*`} element={<Landed />} /></Route>
    <Route element={<RequireAuth />}><Route path="/unavailable" element={<Landed />} /></Route>
  </Routes></MemoryRouter></AuthContext>)
}
afterEach(cleanup)
it("restores the intended page only for expired sessions", () => {
  renderAt(`${BRANCH_AREA}/profile`)
  expect(screen.getByText(`/login:${BRANCH_AREA}/profile`)).toBeTruthy(); cleanup()
  renderAt(`${BRANCH_AREA}/profile`, { signedOut: true })
  expect(screen.getByText('/login:')).toBeTruthy()
})
it("sends every authenticated role to a supported landing without redirect loops", () => {
  for (const role of ["ADMIN", "TEACHER", "STUDENT"] as const) {
    renderAt('/', { user: user(role) })
    expect(screen.getByText(`${role === BRANCH_ROLE ? BRANCH_AREA : '/unavailable'}:`)).toBeTruthy(); cleanup()
    renderAt(`${BRANCH_AREA}/profile`, { user: user(role) })
    expect(screen.getByText(`${role === BRANCH_ROLE ? `${BRANCH_AREA}/profile` : '/unavailable'}:`)).toBeTruthy(); cleanup()
  }
})
it("does not flash protected content during restoration", () => {
  renderAt(`${BRANCH_AREA}/profile`, { loading: true })
  expect(screen.queryByText(/\/login:|profile:/)).toBeNull()
})
