package vn.edu.qnu.st.nambede.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.edu.qnu.st.nambede.data.Question
import vn.edu.qnu.st.nambede.ui.theme.Emerald
import vn.edu.qnu.st.nambede.ui.theme.Rose

private val LETTERS = listOf("A", "B", "C", "D")

/**
 * The four answers of a question. While answering, `onPick` is set and the chosen one is highlighted;
 * when reviewing, the correct answer is green and a wrong choice red.
 */
@Composable
fun AnswerOptions(question: Question, chosen: String?, onPick: ((String) -> Unit)?, enabled: Boolean = true) {
    val review = onPick == null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LETTERS.forEach { letter ->
            val isChosen = chosen == letter
            val isCorrect = review && question.correctOption == letter
            val isWrong = review && isChosen && !isCorrect
            val accent = when {
                isCorrect -> Emerald
                isWrong -> Rose
                isChosen -> MaterialTheme.colorScheme.primary
                else -> null
            }
            val shape = RoundedCornerShape(12.dp)
            val content: @Composable () -> Unit = {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = accent ?: MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (accent != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Text(letter, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(question.option(letter), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    val note = when {
                        isCorrect && isChosen -> "Bạn chọn đúng"
                        isCorrect -> "Đáp án đúng"
                        isWrong -> "Bạn chọn"
                        else -> null
                    }
                    if (note != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(note, style = MaterialTheme.typography.labelSmall, color = accent ?: Color.Unspecified, fontWeight = FontWeight.Medium)
                    }
                }
            }
            val border = BorderStroke(if (accent != null) 2.dp else 1.dp, accent ?: MaterialTheme.colorScheme.outlineVariant)
            val background = accent?.copy(alpha = 0.08f) ?: MaterialTheme.colorScheme.surface
            val description = "Đáp án $letter: ${question.option(letter)}"
            if (onPick != null) {
                Surface(
                    onClick = { onPick(letter) },
                    enabled = enabled,
                    shape = shape,
                    border = border,
                    color = background,
                    modifier = Modifier.fillMaxWidth().semantics {
                        selected = isChosen
                        contentDescription = description
                    },
                ) { content() }
            } else {
                Surface(shape = shape, border = border, color = background, modifier = Modifier.fillMaxWidth()) { content() }
            }
        }
    }
}

/** "Câu 2" card: question text, the answers, and after submitting a coloured result and the explanation. */
@Composable
fun QuestionCard(question: Question, chosen: String?, onPick: ((String) -> Unit)?, enabled: Boolean = true) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Câu ${question.position}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                when {
                    onPick != null && chosen != null -> StatusLabel("Đã chọn $chosen", MaterialTheme.colorScheme.primary)
                    onPick == null && question.correct == true -> StatusLabel("Đúng", Emerald)
                    onPick == null && chosen != null -> StatusLabel("Sai", Rose)
                    onPick == null -> StatusLabel("Bỏ trống", MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(question.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            AnswerOptions(question, chosen, onPick, enabled)
            val explanation = question.explanation
            if (onPick == null && explanation != null) {
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Text("Giải thích: $explanation", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp).fillMaxWidth())
                }
            }
        }
    }
}

/** Big score out of 10 with the number of right and wrong answers. */
@Composable
fun ScoreCard(score: Double, correctCount: Int, questionCount: Int, modifier: Modifier = Modifier, extra: (@Composable () -> Unit)? = null) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Điểm của bạn", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatScore(score), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold, color = scoreColor(score))
                Text(" / 10", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text("$correctCount câu đúng, ${questionCount - correctCount} câu sai hoặc bỏ trống", style = MaterialTheme.typography.bodyMedium)
            if (extra != null) {
                Spacer(Modifier.height(8.dp))
                extra()
            }
        }
    }
}
