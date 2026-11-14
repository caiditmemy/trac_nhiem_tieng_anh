import { Check, Copy } from "lucide-react"
import { useState, type MouseEvent } from "react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

/** The class code as a chip with a copy button, so the lecturer can paste it in a message. */
export function ClassCode({ code, className }: { code: string; className?: string }) {
  const [copied, setCopied] = useState(false)

  async function copy(event: MouseEvent) {
    event.stopPropagation()
    try {
      await copyText(code)
      setCopied(true)
      toast.success(`Đã chép mã lớp ${code}`)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      toast.error("Không chép được, hãy chép mã lớp bằng tay")
    }
  }

  return (
    <span className={cn("inline-flex items-center gap-1", className)}>
      <code className="rounded-md bg-muted px-2 py-0.5 font-mono text-sm font-semibold tracking-widest text-foreground">{code}</code>
      <Button variant="ghost" size="icon-xs" aria-label={copied ? "Đã chép mã lớp" : "Chép mã lớp"} onClick={copy}>
        {copied ? <Check /> : <Copy />}
      </Button>
    </span>
  )
}

/** navigator.clipboard only exists on secure origins (https, localhost, *.localhost).
 *  When the app is opened by IP address during a demo, fall back to a hidden textarea. */
async function copyText(text: string): Promise<void> {
  if (navigator.clipboard) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement("textarea")
  textarea.value = text
  textarea.style.position = "fixed"
  textarea.style.opacity = "0"
  document.body.append(textarea)
  textarea.select()
  const ok = document.execCommand("copy")
  textarea.remove()
  if (!ok) throw new Error("copy failed")
}
