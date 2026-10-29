package dev.toufikforyou.questionbank.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = Color(0xFF3949AB),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFF8E24AA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFCE93D8),
)

/** Answer feedback colours that stay legible in both light and dark themes. */
@Immutable
data class FeedbackColors(
    val correct: Color,
    val correctContainer: Color,
    val incorrect: Color,
    val incorrectContainer: Color,
)

private val LightFeedback = FeedbackColors(
    correct = Color(0xFF1B7F3B),
    correctContainer = Color(0xFFD7F5DF),
    incorrect = Color(0xFFB3261E),
    incorrectContainer = Color(0xFFF9DEDC),
)

private val DarkFeedback = FeedbackColors(
    correct = Color(0xFF7DDC99),
    correctContainer = Color(0xFF0F3D1F),
    incorrect = Color(0xFFF2B8B5),
    incorrectContainer = Color(0xFF5C1A16),
)

val LocalFeedbackColors = staticCompositionLocalOf { LightFeedback }

@Composable
fun QuestionBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalFeedbackColors provides if (darkTheme) DarkFeedback else LightFeedback) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
