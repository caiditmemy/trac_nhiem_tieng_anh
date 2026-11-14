export type Role = "ADMIN" | "TEACHER" | "STUDENT"
export type UserStatus = "ACTIVE" | "LOCKED"
export type AccountType = "FREE" | "VIP"

export interface User {
  id: number
  email: string
  fullName: string
  role: Role
  status: UserStatus
  accountType: AccountType
  createdAt: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  /** Seconds */
  expiresIn: number
  user: User
}

/** A class from class-service. teacherName is null when auth-service could not be reached. */
export interface ClassInfo {
  id: number
  name: string
  code: string
  description: string | null
  teacherId: number
  teacherName: string | null
  studentCount: number
  createdAt: string
}

/** A student in a class. fullName and email are null when auth-service could not be reached. */
export interface ClassStudent {
  studentId: number
  fullName: string | null
  email: string | null
  joinedAt: string
}

export type AnswerOption = "A" | "B" | "C" | "D"

/** A topic of the shared question bank (learning-service). */
export interface Topic {
  id: number
  name: string
  description: string | null
  questionCount: number
  createdBy: number
  /** null when auth-service could not be reached */
  createdByName: string | null
  createdAt: string
}

/** A multiple-choice question with four answers and one correct answer. */
export interface Question {
  id: number
  topicId: number
  topicName: string
  content: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctOption: AnswerOption
  explanation: string | null
  createdBy: number
  createdByName: string | null
  createdAt: string
  updatedAt: string
}

export type QuestionInput = Pick<Question, "topicId" | "content" | "optionA" | "optionB" | "optionC" | "optionD" | "correctOption" | "explanation">

/** Problems a row of an imported file can have; an empty list means the row can be imported. */
export type ImportProblem =
  | "content_missing"
  | "option_a_missing"
  | "option_b_missing"
  | "option_c_missing"
  | "option_d_missing"
  | "correct_option_invalid"
  | "too_long"
  | "duplicate"

export interface ImportRow {
  /** Row number in the file (row 1 holds the column names) */
  row: number
  content: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctOption: AnswerOption | null
  explanation: string | null
  problems: ImportProblem[]
}

/** Answer of POST /questions/import: a preview (dryRun) or what was saved. */
export interface ImportResult {
  topicId: number
  topicName: string
  dryRun: boolean
  total: number
  valid: number
  imported: number
  rows: ImportRow[]
}

/** An exercise in a list. studentCount (lecturer and admin only): students in the class. */
export interface ExerciseSummary {
  id: number
  classId: number
  className: string | null
  title: string
  description: string | null
  dueAt: string
  overdue: boolean
  questionCount: number
  createdAt: string
  studentCount: number | null
}

/** A question of an exercise (a copy taken when it was assigned). */
export interface ExerciseQuestion {
  id: number
  position: number
  content: string
  optionA: string
  optionB: string
  optionC: string
  optionD: string
  correctOption: AnswerOption
  explanation: string | null
}

/** One exercise with its copied questions (lecturer who assigned it, admin). */
export interface ExerciseDetail {
  exercise: ExerciseSummary
  questions: ExerciseQuestion[]
}
