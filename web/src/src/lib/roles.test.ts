import { describe, expect, it } from "vitest"

import { isInRoleArea, postLoginPath, roleHome } from "./roles"

describe("roleHome", () => {
  it("maps every role to its own area", () => {
    expect(roleHome("ADMIN")).toBe("/admin")
    expect(roleHome("TEACHER")).toBe("/teacher")
    expect(roleHome("STUDENT")).toBe("/coming-soon")
  })

  it("sends every role to a page of its own area", () => {
    for (const role of ["ADMIN", "TEACHER", "STUDENT"] as const) {
      expect(isInRoleArea(role, roleHome(role))).toBe(true)
    }
  })
})

describe("isInRoleArea", () => {
  it("accepts the home and the pages under it only", () => {
    expect(isInRoleArea("ADMIN", "/admin")).toBe(true)
    expect(isInRoleArea("ADMIN", "/admin/users")).toBe(true)
    expect(isInRoleArea("ADMIN", "/admin2")).toBe(false)
    expect(isInRoleArea("ADMIN", "/teacher")).toBe(false)
  })
})

describe("postLoginPath", () => {
  it("goes back to a remembered page of the user's own area", () => {
    expect(postLoginPath("ADMIN", "/admin/users")).toBe("/admin/users")
  })

  it("falls back to the role home for another area or no page", () => {
    expect(postLoginPath("STUDENT", "/admin/users")).toBe("/coming-soon")
    expect(postLoginPath("STUDENT", "/teacher")).toBe("/coming-soon")
    expect(postLoginPath("TEACHER", undefined)).toBe("/teacher")
  })
})
