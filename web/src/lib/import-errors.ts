import { ApiError, errorText } from "@/lib/api"

/** Learning Service explains why a whole file is refused; these are the Vietnamese versions. */
const FILE_ERRORS: Record<string, string> = {
  "Only .xlsx, .csv and .json files are supported": "Chỉ nhận file .xlsx, .csv hoặc .json.",
  "The file could not be read as an Excel workbook": "Không đọc được file Excel này. Hãy lưu lại thành .xlsx rồi thử lại.",
  "The file could not be read as JSON": "Không đọc được file JSON này. Hãy kiểm tra cú pháp và lưu dạng UTF-8.",
  "The JSON file must contain an array of questions": "File JSON phải là một mảng, mỗi phần tử là một câu hỏi.",
  "Every JSON item must be an object with the question's fields": "Mỗi phần tử trong file JSON phải là một đối tượng có các trường của câu hỏi.",
  "The file has no questions": "File chưa có câu hỏi nào. Excel/CSV: dòng 1 là tên cột, câu hỏi bắt đầu từ dòng 2.",
  "At most 200 questions per file": "Mỗi file tối đa 200 câu hỏi.",
  "The file is larger than 1 MB": "File lớn hơn 1 MB.",
  "CSV files must be saved as UTF-8": "File CSV phải lưu dạng UTF-8 (trong Excel chọn CSV UTF-8).",
}

export function fileError(err: unknown): string {
  if (err instanceof ApiError && err.status === 400 && err.message.startsWith("Missing column: ")) {
    return `File thiếu cột bắt buộc: ${err.message.slice("Missing column: ".length)}.`
  }
  if (err instanceof ApiError && err.status === 400 && FILE_ERRORS[err.message]) return FILE_ERRORS[err.message]
  return errorText(err, { 404: "Không tìm thấy chủ đề này." })
}
