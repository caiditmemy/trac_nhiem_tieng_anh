import { Suspense } from "react"
import { Link, Outlet } from "react-router"

import { BrandMark } from "@/components/common/BrandMark"
import { LoadingState } from "@/components/common/LoadingState"
import { useAuth } from "@/lib/auth"
import { roleHome } from "@/lib/roles"

import { PortalNav } from "./PortalNav"
import { ThemeToggle } from "./ThemeToggle"
import { UserMenu } from "./UserMenu"

/** Teacher and student areas: top bar with the QNU brand, links, light / dark switch, account menu. */
export function PortalShell() {
  const { user } = useAuth()
  return (
    <div className="flex min-h-svh flex-col bg-background">
      <header className="sticky top-0 z-20 border-b border-border bg-background/85 backdrop-blur">
        <div className="mx-auto flex h-14 w-full max-w-6xl items-center gap-2 px-4">
          <Link to={user ? roleHome(user.role) : "/login"} aria-label="Về trang tổng quan" className="min-w-0 shrink-0">
            <BrandMark size={32} />
          </Link>
          <PortalNav className="ml-4 hidden md:flex" compact />
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
        {/* Small screens: links on their own row */}
        <div className="border-t border-border md:hidden">
          <PortalNav className="mx-auto w-full max-w-6xl overflow-x-auto px-3 py-1.5" />
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 p-4 md:p-6">
        <Suspense fallback={<LoadingState />}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
