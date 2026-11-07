import { Hammer } from "lucide-react"
import { Link } from "react-router"

import { EmptyState } from "@/components/common/EmptyState"
import { PageHeader } from "@/components/common/PageHeader"
import { buttonVariants } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { useAuth } from "@/lib/auth"
import { roleHome } from "@/lib/roles"

interface ComingSoonPageProps {
  title: string
  sprint?: string
  description?: string
}

/** Placeholder for a page whose feature is planned for a later sprint. */
export function ComingSoonPage({ title, sprint = "Sprint 2", description }: ComingSoonPageProps) {
  const { user } = useAuth()
  return (
    <div className="space-y-6">
      <PageHeader title={title} />
      <Card>
        <EmptyState
          icon={<Hammer />}
          title={`Sắp có ở ${sprint}`}
          description={description ?? "Tính năng này đang được phát triển và sẽ có trong sprint tới."}
          action={
            user ? (
              <Link to={roleHome(user.role)} className={buttonVariants({ variant: "outline", size: "sm" })}>
                Về trang tổng quan
              </Link>
            ) : undefined
          }
        />
      </Card>
    </div>
  )
}
