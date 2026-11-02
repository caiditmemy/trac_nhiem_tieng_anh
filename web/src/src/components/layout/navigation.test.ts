import { describe, expect, it } from "vitest"

import { buildBreadcrumbs, isNavItemActive } from "./navigation"

describe("isNavItemActive", () => {
  it("keeps the role home active only on an exact match", () => {
    expect(isNavItemActive("/admin", "/admin")).toBe(true)
    expect(isNavItemActive("/admin/users", "/admin")).toBe(false)
    expect(isNavItemActive("/teacher/classes", "/teacher")).toBe(false)
  })

  it("keeps a section active on its sub-pages", () => {
    expect(isNavItemActive("/admin/users", "/admin/users")).toBe(true)
    expect(isNavItemActive("/admin/users/4", "/admin/users")).toBe(true)
    expect(isNavItemActive("/admin/usersx", "/admin/users")).toBe(false)
  })
})

describe("buildBreadcrumbs", () => {
  it("shows only the root on /admin", () => {
    expect(buildBreadcrumbs("/admin")).toEqual([{ label: "Bảng điều khiển" }])
  })

  it("builds the trail for /admin paths", () => {
    expect(buildBreadcrumbs("/admin/users/4")).toEqual([
      { label: "Bảng điều khiển", to: "/admin" },
      { label: "Người dùng", to: "/admin/users" },
      { label: "Chi tiết" },
    ])
    expect(buildBreadcrumbs("/admin/profile")).toEqual([
      { label: "Bảng điều khiển", to: "/admin" },
      { label: "Tài khoản của tôi" },
    ])
  })
})
