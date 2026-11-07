import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import App from "./App"
import { tokenStore } from "@/lib/api"
import { BRANCH_AREA, BRANCH_ROLE } from "@/lib/branch"
const me = { id: 2, email: 'demo@split.local', fullName: 'Nguyễn kiểm thử', role: BRANCH_ROLE, status: 'ACTIVE', accountType: 'FREE', createdAt: '2026-10-01T00:00:00Z' }
beforeEach(() => {
  window.history.replaceState({}, '', BRANCH_AREA)
  vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() })))
  Element.prototype.scrollIntoView = vi.fn()
})
afterEach(() => { cleanup(); localStorage.clear(); vi.unstubAllGlobals() })
it('renders the owned dashboard and persists a profile edit through the full app', async () => {
  tokenStore.set('session')
  const fetcher = vi.fn(async (url: string, options?: RequestInit) => {
    const body = options?.method === 'PUT' ? { ...me, fullName: 'Tên mới' } : url.endsWith('/users/me') ? me : [me]
    return new Response(JSON.stringify(body), { status: 200 })
  })
  vi.stubGlobal('fetch', fetcher)
  render(<App />)
  await screen.findByText(`Chào ${me.fullName}`)
  expect(document.title).toContain('Tổng quan')
  window.history.pushState({}, '', `${BRANCH_AREA}/profile`); window.dispatchEvent(new PopStateEvent('popstate'))
  const name = await screen.findByLabelText('Họ và tên')
  fireEvent.change(name, { target: { value: 'Tên mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu họ và tên' }))
  await waitFor(() => expect(fetcher.mock.calls.some(([,options]) => options?.method === 'PUT' && options.body === JSON.stringify({ fullName: 'Tên mới' }))).toBe(true))
  await screen.findAllByText('Tên mới')
})
it('shows an unsupported role landing and lets the user sign out', async () => {
  tokenStore.set('other-role')
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ ...me, role: BRANCH_ROLE === 'STUDENT' ? 'TEACHER' : 'STUDENT' }))))
  render(<App />)
  await screen.findByText('Khu vực chưa có trong nhánh này')
  expect(window.location.pathname).toBe('/unavailable')
  fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất để đổi tài khoản' }))
  await screen.findByRole('button', { name: 'Đăng nhập' })
  expect(tokenStore.get()).toBeNull()
  expect(window.location.pathname).toBe('/login')
})
it('does not expose another role UI route', async () => {
  window.history.replaceState({}, '', BRANCH_ROLE === 'ADMIN' ? '/teacher' : '/admin/users')
  render(<App />)
  await screen.findByText('Không tìm thấy trang này')
})
