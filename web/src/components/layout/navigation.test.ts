import { describe, expect, it } from "vitest"

import { ADMIN_NAV_GROUPS, buildBreadcrumbs, isNavItemActive, portalNavItems } from "./navigation"

describe("isNavItemActive", () => {
  it("keeps the role home active only on an exact match", () => {
    expect(isNavItemActive("/admin", "/admin")).toBe(true)
    expect(isNavItemActive("/admin/users", "/admin")).toBe(false)
    expect(isNavItemActive("/teacher/classes", "/teacher")).toBe(false)
    expect(isNavItemActive("/student/exercises", "/student")).toBe(false)
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

describe("learning pages", () => {
  it("keeps Lớp học and Bài tập active on their detail pages", () => {
    expect(isNavItemActive("/teacher/classes/4", "/teacher/classes")).toBe(true)
    expect(isNavItemActive("/teacher/exercises/new", "/teacher/exercises")).toBe(true)
    expect(isNavItemActive("/student/exercises/9", "/student/exercises")).toBe(true)
  })

  it("gives each role the links of its Sprint 1 features", () => {
    expect(portalNavItems("TEACHER").map((item) => item.to)).toEqual([
      "/teacher",
      "/teacher/classes",
      "/teacher/questions",
      "/teacher/exercises",
    ])
    expect(portalNavItems("STUDENT").map((item) => item.to)).toEqual(["/student", "/student/classes", "/student/exercises"])
    expect(portalNavItems("ADMIN")).toEqual([])
    expect(ADMIN_NAV_GROUPS.flatMap((group) => group.items.map((item) => item.to))).toEqual([
      "/admin",
      "/admin/users",
      "/admin/classes",
      "/admin/questions",
      "/admin/exercises",
    ])
  })

  it("labels the trail of the learning pages", () => {
    expect(buildBreadcrumbs("/admin/classes/4")).toEqual([
      { label: "Bảng điều khiển", to: "/admin" },
      { label: "Lớp học", to: "/admin/classes" },
      { label: "Chi tiết" },
    ])
    expect(buildBreadcrumbs("/admin/exercises/new").at(-1)).toEqual({ label: "Giao bài tập" })
  })
})
