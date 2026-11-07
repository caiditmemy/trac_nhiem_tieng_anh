import { expect, it } from "vitest"
import { portalNavItems, isNavItemActive } from "./navigation"
import { BRANCH_AREA, BRANCH_ROLE } from "@/lib/branch"
it("contains only owned links and exact home highlighting", () => {
  expect(portalNavItems(BRANCH_ROLE).every(item => item.to.startsWith(BRANCH_AREA))).toBe(true)
  expect(isNavItemActive(`${BRANCH_AREA}/classes`, BRANCH_AREA)).toBe(false)
  expect(isNavItemActive(`${BRANCH_AREA}/classes/1`, `${BRANCH_AREA}/classes`)).toBe(true)
  expect(isNavItemActive(`${BRANCH_AREA}/classes2`, `${BRANCH_AREA}/classes`)).toBe(false)
})
