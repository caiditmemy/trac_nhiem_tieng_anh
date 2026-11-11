package vn.edu.qnu.st.nambede.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import vn.edu.qnu.st.nambede.data.ClassInfo
import vn.edu.qnu.st.nambede.data.ExerciseSummary
import vn.edu.qnu.st.nambede.data.Question
import vn.edu.qnu.st.nambede.data.Submission
import vn.edu.qnu.st.nambede.data.User
import vn.edu.qnu.st.nambede.ui.common.Remote
import vn.edu.qnu.st.nambede.ui.exercises.ExercisesContent
import vn.edu.qnu.st.nambede.ui.exercises.TakeExerciseContent
import vn.edu.qnu.st.nambede.ui.exercises.TakeExerciseState
import vn.edu.qnu.st.nambede.ui.home.HomeContent
import vn.edu.qnu.st.nambede.ui.home.HomeData
import vn.edu.qnu.st.nambede.ui.login.LoginContent
import vn.edu.qnu.st.nambede.ui.profile.ProfileContent
import vn.edu.qnu.st.nambede.ui.theme.NamBeDeTheme

/**
 * Renders each screen with sample data (the same as the demo data) without a device:
 * ./gradlew recordPaparazziDebug writes the pictures to app/src/test/snapshots/images.
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, theme = "android:Theme.Material.Light.NoActionBar")

    private val now = Instant.parse("2026-10-01T03:00:00Z")
    private val student = User(2, "student1@nambede.local", "Nguyễn Văn A", "STUDENT", "ACTIVE", "FREE")
    private val present = Question(101, 1, "She ___ to school every day.", "go", "goes", "gone", "going")
    private val past = Question(102, 2, "Last summer we ___ to Quy Nhon beach.", "go", "goes", "went", "gone")

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        paparazzi.snapshot(name) {
            NamBeDeTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) { content() }
            }
        }
    }

    private val home = HomeData(
        classes = listOf(ClassInfo(1, "Tiếng Anh 10A1", "JHAAQ3", teacherName = "Trần Thị B", studentCount = 1)),
        pendingExercises = 1,
        streak = 3,
        points = 32,
    )

    @Test
    fun login() = shot("login") {
        LoginContent("student1@nambede.local", "secret", loading = false, error = "Email hoặc mật khẩu không đúng.", onEmailChange = {}, onPasswordChange = {}, onSubmit = {})
    }

    @Test
    fun home() = shot("home") { HomeContent(student, Remote(home, loading = false), onRefresh = {}, onOpenExercises = {}, onJoinClick = {}) }

    @Test
    fun homeDark() = shot("home-dark", dark = true) { HomeContent(student, Remote(home, loading = false), onRefresh = {}, onOpenExercises = {}, onJoinClick = {}) }

    @Test
    fun exercises() = shot("exercises") {
        val list = listOf(
            ExerciseSummary(3, 1, "Tiếng Anh 10A1", "Bài 3: Luyện thêm", null, "2026-10-04T03:00:00Z", false, 1, "NOT_STARTED"),
            ExerciseSummary(5, 1, "Tiếng Anh 10A1", "Bài 2: Ôn tập thì", null, "2026-10-08T16:59:00Z", false, 3, "SUBMITTED", 6.67),
            ExerciseSummary(2, 1, "Tiếng Anh 10A1", "Bài 1: Thì hiện tại và quá khứ", null, "2026-09-30T07:45:00Z", true, 3, "IN_PROGRESS"),
        )
        ExercisesContent(Remote(list, loading = false), onRefresh = {}, onOpen = {}, now = now)
    }

    @Test
    fun takeExercise() = shot("take-exercise") {
        val submission = Submission(9, 3, "Bài 3: Luyện thêm", null, "2026-10-04T03:00:00Z", "IN_PROGRESS", questionCount = 2,
            questions = listOf(present.copy(chosenOption = "B"), past))
        TakeExerciseContent(TakeExerciseState(submission = submission, loading = false), onPick = { _, _ -> }, onSubmit = {}, onRetry = {}, now = now)
    }

    @Test
    fun exerciseResult() = shot("exercise-result") {
        val submission = Submission(
            9, 3, "Bài 2: Ôn tập thì", null, "2026-10-08T16:59:00Z", "SUBMITTED", submittedAt = "2026-10-01T03:20:00Z", score = 5.0, correctCount = 1, questionCount = 2,
            questions = listOf(
                present.copy(chosenOption = "B", correctOption = "B", correct = true, explanation = "Chủ ngữ số ít: động từ thêm -es."),
                past.copy(chosenOption = "A", correctOption = "C", correct = false, explanation = "Last summer là mốc quá khứ nên dùng went."),
            ),
        )
        TakeExerciseContent(TakeExerciseState(submission = submission, loading = false), onPick = { _, _ -> }, onSubmit = {}, onRetry = {})
    }

    @Test
    fun profile() = shot("profile") {
        ProfileContent(student, name = student.fullName, onNameChange = {}, savingName = false, nameError = null, onSaveName = {},
            current = "Student@123", next = "12345", confirm = "12345", onCurrentChange = {}, onNextChange = {}, onConfirmChange = {},
            savingPassword = false, passwordError = "Mật khẩu mới phải có ít nhất 6 ký tự.", onChangePassword = {}, onLogout = {})
    }

}
