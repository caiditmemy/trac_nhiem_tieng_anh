import { cleanup, fireEvent, render, screen } from "@testing-library/react"
import { MemoryRouter } from "react-router"
import { afterEach, describe, expect, it, vi } from "vitest"

import { ThemeProvider } from "@/components/layout/ThemeProvider"
import { AuthContext, type AuthContextValue } from "@/lib/auth"
import { StudentComingSoonPage } from "@/pages/StudentComingSoonPage"

function renderPage(logout = vi.fn()) {
  const value: AuthContextValue = {
    user: {
      id: 3,
      email: "student1@nambede.local",
      fullName: "Sinh Viên Một",
      role: "STUDENT",
      status: "ACTIVE",
      accountType: "FREE",
      createdAt: "2026-10-01T00:00:00Z",
    },
    loading: false,
    signedOut: false,
    login: vi.fn(),
    register: vi.fn(),
    logout,
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
  }
  render(
    <ThemeProvider>
      <AuthContext value={value}>
        <MemoryRouter>
          <StudentComingSoonPage />
        </MemoryRouter>
      </AuthContext>
    </ThemeProvider>
  )
  return logout
}

describe("StudentComingSoonPage", () => {
  afterEach(cleanup)

  it("greets the student and says the student pages come later", () => {
    renderPage()

    expect(screen.getByText("Xin chào Sinh Viên Một")).toBeTruthy()
    expect(screen.getByText("Trang dành cho sinh viên sẽ có ở sprint sau. Tài khoản của bạn đã được tạo thành công.")).toBeTruthy()
  })

  it("signs out with the logout button", () => {
    const logout = renderPage()

    fireEvent.click(screen.getByRole("button", { name: "Đăng xuất" }))

    expect(logout).toHaveBeenCalledOnce()
  })
})
