import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, expect, it, vi } from "vitest"
import App from "./App"
import { tokenStore } from "@/lib/api"
import { roleHome } from "@/lib/roles"
import type { Role } from "@/types"
const roles: Role[] = ['ADMIN', 'TEACHER', 'STUDENT']
const me = { id: 2, email: 'demo@sprint1.local', fullName: 'Nguyễn kiểm thử', status: 'ACTIVE', accountType: 'FREE', createdAt: '2026-10-01T00:00:00Z' }
/** What the gateway answers: the profile, the user list, and empty lists for everything else. */
function answer(role: Role, url: string, options?: RequestInit) {
  const user = { ...me, role }
  const path = url.split('?')[0]
  if (options?.method === 'PUT') return { ...user, fullName: 'Tên mới' }
  if (path.endsWith('/users/me')) return user
  if (path.endsWith('/streak')) return { currentStreak: 2, longestStreak: 3, lastActiveDate: '2026-10-01', activeToday: true }
  if (path.endsWith('/ranking/weekly')) return { weekStart: '2026-09-28', weekEnd: '2026-10-04', entries: [], me: { rank: 1, weekPoints: 42, totalPoints: 42, recent: [] } }
  return path.endsWith('/users') ? [user] : []
}
beforeEach(() => {
  vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() })))
  Element.prototype.scrollIntoView = vi.fn()
})
afterEach(() => { cleanup(); localStorage.clear(); vi.unstubAllGlobals() })
it.each(roles)('restores %s dashboard and saves its shared profile through the API', async role => {
  window.history.replaceState({}, '', '/')
  tokenStore.set('session')
  const fetcher = vi.fn(async (url: string, options?: RequestInit) => new Response(JSON.stringify(answer(role, url, options)), { status: 200 }))
  vi.stubGlobal('fetch', fetcher)
  render(<App />)
  await screen.findByText(`Chào ${me.fullName}`)
  expect(window.location.pathname).toBe(roleHome(role))
  expect(document.title).toContain('Tổng quan')
  window.history.pushState({}, '', `${roleHome(role)}/profile`); window.dispatchEvent(new PopStateEvent('popstate'))
  const name = await screen.findByLabelText('Họ và tên')
  fireEvent.change(name, { target: { value: 'Tên mới' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu họ và tên' }))
  await waitFor(() => expect(fetcher.mock.calls.some(([,options]) => options?.method === 'PUT' && options.body === JSON.stringify({ fullName: 'Tên mới' }))).toBe(true))
  await screen.findAllByText('Tên mới')
})
for (const role of roles) {
  it.each(roles.filter(other => other !== role))(`redirects ${role} away from %s UI`, async other => {
    window.history.replaceState({}, '', `${roleHome(other)}/profile`)
    tokenStore.set('session')
    vi.stubGlobal('fetch', vi.fn(async (url: string) => new Response(JSON.stringify(answer(role, url)))))
    render(<App />)
    await screen.findByText(`Chào ${me.fullName}`)
    expect(window.location.pathname).toBe(roleHome(role))
    expect(screen.queryByLabelText('Họ và tên')).toBeNull()
  })
}
it.each(roles)('requires login for the %s area', async role => {
  window.history.replaceState({}, '', roleHome(role))
  render(<App />)
  await screen.findByRole('button', { name: 'Đăng nhập' })
  expect(window.location.pathname).toBe('/login')
})
