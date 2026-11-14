/** "3 / 25" with a small bar: how many students of the class have submitted. */
export function SubmittedProgress({ submitted, total }: { submitted: number; total: number | null }) {
  const percent = total ? Math.min(100, Math.round((submitted / total) * 100)) : 0
  return (
    <span className="inline-flex items-center gap-2">
      <span className="tabular-nums">
        {submitted} / {total ?? "?"}
      </span>
      <span className="hidden h-1.5 w-16 overflow-hidden rounded-full bg-muted sm:block" aria-hidden>
        <span className="block h-full rounded-full bg-emerald-500" style={{ width: `${percent}%` }} />
      </span>
    </span>
  )
}
