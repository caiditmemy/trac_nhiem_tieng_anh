import type { ReactNode } from "react"

import { StatCard } from "@/components/common/StatCard"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export interface ComingSoonItem {
  title: string
  sprint: string
  description: string
  icon: ReactNode
}

/** Placeholder cards for the features that arrive in later sprints. */
export function ComingSoonCards({ title = "Sắp có trong các sprint sau", items }: { title?: string; items: ComingSoonItem[] }) {
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="mb-4 text-sm text-muted-foreground">
          Sprint 1 hoàn thành nền tảng và xác thực. Các tính năng học tập sẽ được thêm vào trang này ở các sprint tiếp theo.
        </p>
        <div className="grid gap-4 sm:grid-cols-2">
          {items.map((item) => (
            <StatCard key={item.title} title={item.title} value={item.sprint} icon={item.icon} hint={item.description} />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
