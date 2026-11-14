import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, expect, it, vi } from "vitest"
import { AuthProvider } from "./AuthProvider"
import { useAuth } from "@/lib/auth"
import { tokenStore } from "@/lib/api"

const me = { id: 2, email: "user@test.local", fullName: "Sinh viên", role: "STUDENT", status: "ACTIVE", accountType: "FREE", createdAt: "2026-10-01" }
const response = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status })
function Probe() {
  const { user, loading, login, logout, updateProfile } = useAuth()
  return <><p>{loading ? 'restoring' : user?.fullName ?? 'guest'}</p>
    <button onClick={() => void login(me.email, 'Password@123')}>Login</button>
    <button onClick={logout}>Logout</button>
    <button onClick={() => void updateProfile('Tên mới')}>Save</button></>
}
afterEach(() => { cleanup(); localStorage.clear(); vi.unstubAllGlobals() })
it("restores a saved token and persists profile through the API client", async () => {
  tokenStore.set('saved-token')
  const fetcher = vi.fn().mockResolvedValueOnce(response(me)).mockResolvedValueOnce(response({ ...me, fullName: 'Tên mới' }))
  vi.stubGlobal('fetch', fetcher)
  render(<AuthProvider><Probe /></AuthProvider>)
  await screen.findByText(me.fullName)
  expect(fetcher.mock.calls[0][1].headers.Authorization).toBe('Bearer saved-token')
  fireEvent.click(screen.getByText('Save'))
  await screen.findByText('Tên mới')
  expect(fetcher.mock.calls[1][1].method).toBe('PUT')
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({ fullName: 'Tên mới' })
})
it("clears a rejected stored token and allows a fresh login/logout", async () => {
  tokenStore.set('expired')
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(response({ message: 'expired' }, 401)).mockResolvedValueOnce(response({ accessToken: 'fresh-token', user: me })))
  render(<AuthProvider><Probe /></AuthProvider>)
  await screen.findByText('guest'); expect(tokenStore.get()).toBeNull()
  fireEvent.click(screen.getByText('Login')); await screen.findByText(me.fullName)
  expect(tokenStore.get()).toBe('fresh-token')
  fireEvent.click(screen.getByText('Logout'))
  await waitFor(() => expect(screen.getByText('guest')).toBeTruthy())
  expect(tokenStore.get()).toBeNull()
})
