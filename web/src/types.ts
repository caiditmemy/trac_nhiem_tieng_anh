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
