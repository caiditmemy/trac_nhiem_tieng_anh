package vn.edu.qnu.st.nambede.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = QnuBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondaryContainer = Color(0xFFE3E8F2),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
)

private val DarkColors = darkColorScheme(
    primary = QnuBlueLight,
    onPrimary = Color(0xFF003063),
    primaryContainer = Color(0xFF00458E),
    onPrimaryContainer = Color(0xFFD6E3FF),
)

/** QNU blue in light and dark mode (no wallpaper colours, so the app always looks like the university's). */
@Composable
fun NamBeDeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
