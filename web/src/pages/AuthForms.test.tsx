import { cleanup, fireEvent, render, screen } from "@testing-library/react"
import { afterEach, expect, it, vi } from "vitest"
import App from "@/App"
function fillLogin() {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'teacher@split.local' } })
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'Password@123' } })
}
afterEach(() => { cleanup(); localStorage.clear(); vi.unstubAllGlobals() })
for (const [status, message] of [[401, 'Email hoặc mật khẩu không đúng.'], [429, 'Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút.']] as const) {
  it(`renders login ${status} and enables retry`, async () => {
    window.history.replaceState({}, '', '/login')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'refused' }), { status })))
    render(<App />); fillLogin()
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect((await screen.findByRole('alert')).textContent).toBe(message)
    expect((screen.getByRole('button', { name: 'Đăng nhập' }) as HTMLButtonElement).disabled).toBe(false)
  })
}
it('renders duplicate email from registration', async () => {
  window.history.replaceState({}, '', '/register')
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'duplicate' }), { status: 409 })))
  render(<App />)
  fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { value: 'Sinh viên' } })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'student@split.local' } })
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'Password@123' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tạo tài khoản' }))
  expect((await screen.findByRole('alert')).textContent).toBe('Email này đã được đăng ký.')
})
