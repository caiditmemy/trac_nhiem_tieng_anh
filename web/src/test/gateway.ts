import { vi } from "vitest"

import { AuthContext, type AuthContextValue } from "@/lib/auth"
import type { Role, User } from "@/types"

export function userWith(role: Role, id = 2): User {
  return { id, email: `user${id}@nambede.local`, fullName: "Nguyễn Văn A", role, status: "ACTIVE", accountType: "FREE", createdAt: "2026-10-01T00:00:00Z" }
}

export function sessionOf(user: User): AuthContextValue {
  return {
    user,
    loading: false,
    signedOut: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateProfile: vi.fn(),
    changePassword: vi.fn(),
  }
}

export { AuthContext }

interface Call {
  method: string
  path: string
  body: unknown
}

/**
 * Replaces fetch with a fake gateway. `routes` maps "METHOD /path" (query string removed) to the JSON answer,
 * or to a function of the request body. Every call is recorded in `calls`.
 */
export function fakeGateway(routes: Record<string, unknown | ((body: unknown) => unknown)>) {
  const calls: Call[] = []
  const fetcher = vi.fn(async (url: string, options?: RequestInit) => {
    const method = options?.method ?? "GET"
    const path = url.replace(/^\/api/, "").split("?")[0]
    const raw = options?.body
    const body = typeof raw === "string" ? (JSON.parse(raw) as unknown) : raw
    calls.push({ method, path, body })
    const route = routes[`${method} ${path}`]
    if (route === undefined) return new Response(JSON.stringify({ message: `no fake for ${method} ${path}` }), { status: 404 })
    const answer = typeof route === "function" ? (route as (body: unknown) => unknown)(body) : route
    return new Response(JSON.stringify(answer), { status: method === "POST" && path === "/classes" ? 201 : 200 })
  })
  vi.stubGlobal("fetch", fetcher)
  return { calls, fetcher }
}
