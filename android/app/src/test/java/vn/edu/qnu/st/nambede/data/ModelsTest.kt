package vn.edu.qnu.st.nambede.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** The models read the gateway's real JSON (copied from responses), extra fields included. */
class ModelsTest {

    private val json = ApiClient.json

    @Test
    fun readsTheStreakAndThePointsOfTheWeeklyRanking() {
        val streak = json.decodeFromString(Streak.serializer(), """{"currentStreak":1,"longestStreak":1,"lastActiveDate":"2026-10-01","activeToday":true}""")
        assertEquals(1, streak.currentStreak)
        val body = """{"weekStart":"2026-09-28","weekEnd":"2026-10-04","entries":[],"me":{"rank":1,"weekPoints":21,"totalPoints":21,"recent":[{"points":21,"reason":"EXERCISE","detail":"Bài 1","createdAt":"2026-10-01T03:00:00Z"}]}}"""
        val ranking = json.decodeFromString(WeeklyRanking.serializer(), body)
        assertEquals(21, ranking.me?.totalPoints)
    }

    @Test
    fun readsAnExerciseAttemptWithHiddenAnswers() {
        val body = """{"id":6,"exerciseId":7,"exerciseTitle":"Bài 1","exerciseDescription":null,"dueAt":"2026-10-08T08:08:25Z","status":"IN_PROGRESS",
            "startedAt":"2026-10-01T08:08:25.845548Z","submittedAt":null,"score":null,"correctCount":null,"questionCount":1,
            "questions":[{"id":17,"position":1,"content":"She ___ to school every day.","optionA":"go","optionB":"goes","optionC":"gone","optionD":"going",
            "chosenOption":"B","correctOption":null,"explanation":null,"correct":null,"correctRate":null}]}"""
        val submission = json.decodeFromString(Submission.serializer(), body)
        assertEquals("IN_PROGRESS", submission.status)
        assertEquals("goes", submission.questions.single().option("B"))
        assertNull(submission.questions.single().correctOption)
    }
}
