import { CircleCheck } from "lucide-react"

import { ANSWER_OPTIONS, optionText } from "@/lib/questions"
import { cn } from "@/lib/utils"
import type { AnswerOption } from "@/types"

type Options = { optionA: string; optionB: string; optionC: string; optionD: string }

interface AnswerOptionsProps {
  question: Options
  /** The correct answer, highlighted: the lecturer's answer key. */
  correct: AnswerOption | null
}

/** The four answers of a question with the correct one highlighted. */
export function AnswerOptions({ question, correct }: AnswerOptionsProps) {
  return (
    <ul className="grid gap-2 sm:grid-cols-2" aria-label="Các đáp án">
      {ANSWER_OPTIONS.map((option) => {
        const right = correct === option
        return (
          <li key={option}>
            <div
              className={cn(
                "flex w-full items-start gap-2.5 rounded-lg p-2.5 text-sm ring-1 ring-foreground/10",
                right && "bg-emerald-50 text-emerald-900 ring-emerald-500/40 dark:bg-emerald-500/10 dark:text-emerald-200"
              )}
            >
              <span
                className={cn(
                  "flex size-6 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-semibold text-foreground",
                  right && "bg-emerald-600 text-white"
                )}
              >
                {option}
              </span>
              <span className="min-w-0 flex-1 pt-0.5 text-left break-words">{optionText(question, option)}</span>
              {right ? (
                <span className="flex items-center gap-1 pt-0.5 text-xs font-medium whitespace-nowrap text-emerald-700 dark:text-emerald-300">
                  <CircleCheck className="size-4" aria-hidden />
                  Đáp án đúng
                </span>
              ) : null}
            </div>
          </li>
        )
      })}
    </ul>
  )
}
