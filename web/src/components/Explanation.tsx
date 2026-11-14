import { Lightbulb } from "lucide-react"

export function Explanation({ text }: { text: string | null }) {
  if (!text) return null
  return (
    <div className="flex gap-2.5 rounded-lg bg-muted/60 p-3 text-sm">
      <Lightbulb className="mt-0.5 size-4 shrink-0 text-amber-500" aria-hidden />
      <p className="whitespace-pre-wrap">
        <span className="font-medium">Giải thích: </span>
        {text}
      </p>
    </div>
  )
}
