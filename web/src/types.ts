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

export type ExerciseStatus = "NOT_STARTED" | "IN_PROGRESS" | "SUBMITTED"

/** An exercise in a list. Lecturer / admin fields: submittedCount, studentCount. Student fields: my*. */
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
  submittedCount: number | null
  studentCount: number | null
  myStatus: ExerciseStatus | null
  myScore: number | null
  mySubmissionId: number | null
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
  chosenOption: AnswerOption | null
  /** null while the student is still working */
  correctOption: AnswerOption | null
  explanation: string | null
  correct: boolean | null
  /** percent of submitted students who got it right (lecturer results only) */
  correctRate: number | null
}

export interface Submission {
  id: number
  exerciseId: number
  exerciseTitle: string
  exerciseDescription: string | null
  dueAt: string
  status: ExerciseStatus
  startedAt: string
  submittedAt: string | null
  score: number | null
  correctCount: number | null
  questionCount: number
  questions: ExerciseQuestion[]
}

export interface StudentResult {
  studentId: number
  fullName: string | null
  email: string | null
  status: ExerciseStatus
  score: number | null
  correctCount: number | null
  submittedAt: string | null
  submissionId: number | null
}

export interface ExerciseResults {
  exercise: ExerciseSummary
  averageScore: number | null
  students: StudentResult[]
  questions: ExerciseQuestion[]
}

/** A question shown for answering or reviewing (exercise and practice share this shape). */
export type ReviewQuestion = Omit<ExerciseQuestion, "correctRate">

export interface Streak {
  currentStreak: number
  longestStreak: number
  lastActiveDate: string | null
  activeToday: boolean
}

export interface PointEntry {
  points: number
  reason: "EXERCISE"
  /** Title of the exercise */
  detail: string | null
  createdAt: string
}

export interface RankingEntry {
  rank: number
  studentId: number
  fullName: string | null
  points: number
}

export interface WeeklyRanking {
  weekStart: string
  weekEnd: string
  entries: RankingEntry[]
  /** Only for students */
  me: { rank: number | null; weekPoints: number; totalPoints: number; recent: PointEntry[] } | null
}
