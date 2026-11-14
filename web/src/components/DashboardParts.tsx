import { School } from "lucide-react"
import type { ReactNode } from "react"
import { Link } from "react-router"

import { ClassCode } from "@/components/common/ClassCode"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useAreaPath } from "@/lib/area"
import { formatDate, formatNumber } from "@/lib/format"
import type { ClassInfo } from "@/types"

// Pieces shared by the teacher and student dashboards

export function StatsSkeleton({ count }: { count: number }) {
  return (
    <div className={count === 4 ? "grid gap-4 sm:grid-cols-2 xl:grid-cols-4" : "grid gap-4 sm:grid-cols-3"} aria-busy="true">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="space-y-3 rounded-xl bg-card p-4 ring-1 ring-foreground/10">
          <div className="flex items-center justify-between">
            <Skeleton className="h-4 w-28" />
            <Skeleton className="size-9 rounded-lg" />
          </div>
          <Skeleton className="h-7 w-20" />
          <Skeleton className="h-3 w-32" />
        </div>
      ))}
    </div>
  )
}

export function ClassListCard({ title, classes, empty, showCode = false }: { title: string; classes: ClassInfo[]; empty: ReactNode; showCode?: boolean }) {
  const area = useAreaPath()
  return (
    <Card>
      <CardHeader className="border-b">
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {classes.length === 0 ? (
          empty
        ) : (
          <div className="divide-y divide-border">
            {classes.slice(0, 6).map((item) => (
              <div key={item.id} className="flex flex-wrap items-center gap-3 py-3 first:pt-0 last:pb-0">
                <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-500/15 dark:text-blue-300">
                  <School className="size-4" />
                </span>
                <div className="min-w-0 flex-1">
                  {showCode ? (
                    <Link to={area(`/classes/${item.id}`)} className="block truncate text-sm font-medium hover:underline">
                      {item.name}
                    </Link>
                  ) : (
                    <p className="truncate text-sm font-medium">{item.name}</p>
                  )}
                  <p className="truncate text-xs text-muted-foreground">
                    {showCode ? `Tạo ngày ${formatDate(item.createdAt)}` : `Giảng viên: ${item.teacherName ?? "chưa lấy được tên"}`}
                  </p>
                </div>
                {showCode ? <ClassCode code={item.code} /> : null}
                <span className="w-16 text-right text-xs text-muted-foreground tabular-nums">{formatNumber(item.studentCount)} SV</span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
