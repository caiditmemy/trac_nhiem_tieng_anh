import { useAuth } from "@/lib/auth"
import { ExerciseResultsPage } from "@/pages/ExerciseResultsPage"
import { TakeExercisePage } from "@/pages/TakeExercisePage"

/** /exercises/:id: students do the exercise (or see their result), lecturers and admins see the class results. */
export function ExerciseDetailPage() {
  const { user } = useAuth()
  if (!user) return null
  return user.role === "STUDENT" ? <TakeExercisePage /> : <ExerciseResultsPage />
}
