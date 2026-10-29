package dev.toufikforyou.questionbank.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.toufikforyou.questionbank.android.ui.theme.LocalFeedbackColors
import dev.toufikforyou.questionbank.presentation.quiz.OptionState

/** One MCQ option. Colour, border, scale and icon animate as [optionState] changes. */
@Composable
fun AnswerOptionCard(
    label: String,
    text: String,
    optionState: OptionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val feedback = LocalFeedbackColors.current
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (optionState) {
            OptionState.CORRECT -> feedback.correctContainer
            OptionState.INCORRECT -> feedback.incorrectContainer
            OptionState.SELECTED -> scheme.secondaryContainer
            OptionState.IDLE, OptionState.DIMMED -> scheme.surface
        },
        label = "optionContainer",
    )
    val border by animateColorAsState(
        when (optionState) {
            OptionState.CORRECT -> feedback.correct
            OptionState.INCORRECT -> feedback.incorrect
            OptionState.SELECTED -> scheme.secondary
            OptionState.IDLE -> scheme.outlineVariant
            OptionState.DIMMED -> scheme.outlineVariant.copy(alpha = 0.4f)
        },
        label = "optionBorder",
    )
    val scale by animateFloatAsState(
        if (optionState == OptionState.SELECTED) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "optionScale",
    )

    OutlinedCard(
        onClick = onClick,
        enabled = optionState == OptionState.IDLE,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .semantics {
                selected = optionState == OptionState.SELECTED
                stateDescription = when (optionState) {
                    OptionState.CORRECT -> "সঠিক উত্তর"
                    OptionState.INCORRECT -> "ভুল উত্তর"
                    OptionState.SELECTED -> "নির্বাচিত"
                    else -> ""
                }
            },
        colors = CardDefaults.outlinedCardColors(
            containerColor = container,
            disabledContainerColor = container,
            contentColor = scheme.onSurface,
            disabledContentColor = scheme.onSurface.copy(alpha = if (optionState == OptionState.DIMMED) 0.6f else 1f),
        ),
        border = BorderStroke(if (compact) 1.dp else 1.5.dp, border),
    ) {
        Row(Modifier.padding(if (compact) 10.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = border.copy(alpha = 0.15f), modifier = Modifier.size(if (compact) 26.dp else 32.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelLarge) }
            }
            Text(
                text,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            AnimatedVisibility(
                visible = optionState == OptionState.CORRECT || optionState == OptionState.INCORRECT,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            ) {
                Icon(
                    if (optionState == OptionState.CORRECT) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = border,
                )
            }
        }
    }
}
