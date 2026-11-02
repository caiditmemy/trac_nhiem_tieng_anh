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
