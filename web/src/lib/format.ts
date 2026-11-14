export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("vi-VN")
}

export function formatNumber(value: number): string {
  return value.toLocaleString("vi-VN")
}

/** Initials for an avatar from the last two words of a Vietnamese name: "Nguyễn Văn An" gives "VA". */
export function initialsOf(fullName: string | null | undefined): string {
  const words = (fullName ?? "").trim().split(/\s+/).filter(Boolean)
  if (words.length === 0) return "?"
  return words
    .slice(-2)
    .map((word) => word.charAt(0).toUpperCase())
    .join("")
}
