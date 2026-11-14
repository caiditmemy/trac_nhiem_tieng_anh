import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"

import { QuestionImportDialog } from "@/components/QuestionImportDialog"
import { ApiError } from "@/lib/api"
import { fileError } from "@/lib/import-errors"
import { fakeGateway } from "@/test/gateway"
import type { ImportResult, Topic } from "@/types"

const topic: Topic = { id: 1, name: "Thì hiện tại đơn", description: null, questionCount: 0, createdBy: 3, createdByName: "Trần Thị B", createdAt: "2026-10-01T00:00:00Z" }

const preview: ImportResult = {
  topicId: 1,
  topicName: topic.name,
  dryRun: true,
  total: 2,
  valid: 1,
  imported: 0,
  rows: [
    { row: 1, content: "She ___ to school.", optionA: "go", optionB: "goes", optionC: "gone", optionD: "going", correctOption: "B", explanation: null, problems: [] },
    { row: 2, content: "Missing C", optionA: "a", optionB: "b", optionC: "", optionD: "d", correctOption: "A", explanation: null, problems: ["option_c_missing"] },
  ],
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe("QuestionImportDialog", () => {
  it("accepts a .json file next to .xlsx and .csv", () => {
    render(<QuestionImportDialog open onOpenChange={vi.fn()} topics={[topic]} onImported={vi.fn()} />)

    const input = screen.getByLabelText("File câu hỏi") as HTMLInputElement
    expect(input.accept).toBe(".xlsx,.csv,.json")
  })

  it("previews a JSON file row by row, then imports it", async () => {
    const onImported = vi.fn()
    const { calls } = fakeGateway({
      "POST /questions/import": () => preview,
    })
    render(<QuestionImportDialog open onOpenChange={vi.fn()} topics={[topic]} defaultTopicId={1} onImported={onImported} />)

    const file = new File([JSON.stringify([{ "Câu hỏi": "She ___ to school." }])], "cau-hoi.json", { type: "application/json" })
    fireEvent.change(screen.getByLabelText("File câu hỏi"), { target: { files: [file] } })
    fireEvent.click(screen.getByRole("button", { name: /Xem trước/ }))

    expect(await screen.findByText("She ___ to school.")).toBeTruthy()
    expect(screen.getByText("Missing C")).toBeTruthy()
    const [request] = calls
    expect(request.path).toBe("/questions/import")
    expect((request.body as FormData).get("file")).toBeInstanceOf(File)
    expect(((request.body as FormData).get("file") as File).name).toBe("cau-hoi.json")

    fireEvent.click(screen.getByRole("button", { name: /Nhập 1 câu/ }))
    await waitFor(() => expect(onImported).toHaveBeenCalled())
  })

  it("asks for a file before previewing", async () => {
    fakeGateway({})
    render(<QuestionImportDialog open onOpenChange={vi.fn()} topics={[topic]} defaultTopicId={1} onImported={vi.fn()} />)

    fireEvent.click(screen.getByRole("button", { name: /Xem trước/ }))

    expect(await within(screen.getByRole("dialog")).findByText("Chọn file .xlsx, .csv hoặc .json.")).toBeTruthy()
  })
})

describe("fileError", () => {
  it("turns the reasons a file is refused into Vietnamese", () => {
    const refuse = (message: string) => new ApiError(400, message)
    expect(fileError(refuse("Only .xlsx, .csv and .json files are supported"))).toBe("Chỉ nhận file .xlsx, .csv hoặc .json.")
    expect(fileError(refuse("The file could not be read as JSON"))).toContain("JSON")
    expect(fileError(refuse("Missing column: Đáp án D, Đáp án đúng"))).toBe("File thiếu cột bắt buộc: Đáp án D, Đáp án đúng.")
    expect(fileError(refuse("The file could not be read as an Excel workbook"))).toContain("Excel")
  })
})
