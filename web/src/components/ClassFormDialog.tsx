import { useState, type FormEvent } from "react"

import { FormDialogShell } from "@/components/common/FormDialogShell"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { errorText } from "@/lib/api"
import { classApi } from "@/lib/classes"
import type { ClassInfo } from "@/types"

interface ClassFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Present when editing; absent when creating a class. */
  initial?: ClassInfo
  onSaved: (saved: ClassInfo) => void
}

/** Create or edit a class (name and description). */
export function ClassFormDialog({ open, onOpenChange, initial, onSaved }: ClassFormDialogProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const body = { name: String(form.get("name")).trim(), description: String(form.get("description")).trim() }
    setSubmitting(true)
    setError(null)
    try {
      onSaved(initial ? await classApi.update(initial.id, body) : await classApi.create(body))
      onOpenChange(false)
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <FormDialogShell
      open={open}
      onOpenChange={(next) => {
        if (next) setError(null)
        onOpenChange(next)
      }}
      title={initial ? "Sửa thông tin lớp" : "Tạo lớp mới"}
      description={initial ? undefined : "Hệ thống sẽ sinh mã lớp để bạn gửi cho sinh viên."}
      onSubmit={handleSubmit}
      isSubmitting={submitting}
      submitLabel={initial ? "Lưu thay đổi" : "Tạo lớp"}
      error={error}
    >
      {/* key: reset the fields each time the dialog opens for another class */}
      <div key={`${initial?.id ?? "new"}-${open}`} className="space-y-4">
        <div className="grid gap-2">
          <Label htmlFor="class-name">Tên lớp</Label>
          <Input id="class-name" name="name" defaultValue={initial?.name} placeholder="VD: Lập trình Web - 46K1" maxLength={255} required />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="class-description">Mô tả</Label>
          <Textarea
            id="class-description"
            name="description"
            defaultValue={initial?.description ?? ""}
            placeholder="Không bắt buộc, VD: học thứ 3, phòng A5.201"
            maxLength={2000}
            rows={3}
          />
        </div>
      </div>
    </FormDialogShell>
  )
}
