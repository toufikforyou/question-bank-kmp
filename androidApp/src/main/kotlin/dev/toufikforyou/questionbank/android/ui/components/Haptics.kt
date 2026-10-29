package dev.toufikforyou.questionbank.android.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind

/** Native bridge for the shared [HapticKind] effects emitted by the stores. */
class AndroidHaptics(private val view: View) {
    fun perform(kind: HapticKind) {
        val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val constant = when (kind) {
            HapticKind.CORRECT, HapticKind.COMPLETED ->
                if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            HapticKind.INCORRECT ->
                if (modern) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            HapticKind.SELECTION -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): AndroidHaptics {
    val view = LocalView.current
    return remember(view) { AndroidHaptics(view) }
}
