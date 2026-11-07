import { describe, expect, it } from "vitest"
import { roleHome, isInRoleArea, postLoginPath } from "./roles"
import { BRANCH_ROLE, BRANCH_AREA } from "./branch"
describe("branch role paths", () => {
  it("routes every role to a real landing", () => {
    for (const role of ["ADMIN", "TEACHER", "STUDENT"] as const) {
      expect(roleHome(role)).toBe(role === BRANCH_ROLE ? BRANCH_AREA : "/unavailable")
    }
  })
  it("rejects another role, lookalike prefix and external return URL", () => {
    expect(isInRoleArea(BRANCH_ROLE, `${BRANCH_AREA}2`)).toBe(false)
    expect(postLoginPath(BRANCH_ROLE, "https://example.com")).toBe(BRANCH_AREA)
    expect(postLoginPath(BRANCH_ROLE, `${BRANCH_AREA}/profile`)).toBe(`${BRANCH_AREA}/profile`)
    expect(postLoginPath(BRANCH_ROLE, "/admin2")).toBe(BRANCH_AREA)
  })
})
