package dev.toufikforyou.questionbank.android.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.AnswerOptionCard
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.android.ui.components.MessageState
import dev.toufikforyou.questionbank.android.ui.components.rememberHaptics
import dev.toufikforyou.questionbank.android.ui.theme.LocalFeedbackColors
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.presentation.quiz.QuizEffect
import dev.toufikforyou.questionbank.presentation.quiz.QuizIntent
import dev.toufikforyou.questionbank.presentation.quiz.QuizState
import dev.toufikforyou.questionbank.presentation.quiz.QuizStatus
import dev.toufikforyou.questionbank.presentation.quiz.QuizStore
import kotlinx.coroutines.launch

@Composable
fun QuizScreen(store: QuizStore, request: QuizRequest, onBack: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(store) {
        if (store.currentState.status == QuizStatus.IDLE) store.dispatch(QuizIntent.Start(request))
        store.effects.collect { effect ->
            when (effect) {
                is QuizEffect.Haptic -> haptics.perform(effect.kind)
                is QuizEffect.ShowMessage -> launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    QuizContent(
        state = state,
        title = if (request.timed) "পরীক্ষা" else "অনুশীলন",
        onIntent = store::dispatch,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    )
}

/** Stateless quiz UI: everything it renders comes from [state], everything it does goes through [onIntent]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    state: QuizState,
    title: String,
    onIntent: (QuizIntent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান") }
                },
                actions = {
                    state.timerLabel?.takeIf { state.status == QuizStatus.IN_PROGRESS }?.let { clock ->
                        val warn = state.isTimeRunningOut
                        val color by animateColorAsState(
                            if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                            label = "timer",
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(clock, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = "বাকি সময়") },
                            colors = AssistChipDefaults.assistChipColors(containerColor = color),
                            modifier = Modifier.testTag("quiz_timer"),
                        )
                    }
                    val question = state.currentQuestion
                    if (question != null) {
                        IconButton(onClick = { onIntent(QuizIntent.ToggleBookmark) }, modifier = Modifier.testTag("bookmark_button")) {
                            Icon(
                                if (question.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (question.isBookmarked) "সংরক্ষণ বাতিল" else "সংরক্ষণ করুন",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.status) {
                QuizStatus.IDLE, QuizStatus.LOADING -> LoadingState()
                QuizStatus.EMPTY -> MessageState(
                    icon = Icons.Outlined.Inbox,
                    title = "অনুশীলনের প্রশ্ন নেই",
                    message = state.errorMessage ?: "এখানে কোনো বহুনির্বাচনি প্রশ্ন নেই।",
                    actionLabel = "ফিরে যান",
                    onAction = onBack,
                )
                QuizStatus.ERROR -> MessageState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "কিছু একটা ভুল হয়েছে",
                    message = state.errorMessage ?: "আবার চেষ্টা করুন।",
                    actionLabel = "আবার চেষ্টা করুন",
                    onAction = { onIntent(QuizIntent.Restart) },
                )
                QuizStatus.IN_PROGRESS -> QuestionFlow(state, onIntent)
                QuizStatus.FINISHED -> ResultContent(state, onRestart = { onIntent(QuizIntent.Restart) }, onDone = onBack)
            }
        }
    }
}

@Composable
private fun QuestionFlow(state: QuizState, onIntent: (QuizIntent) -> Unit) {
    val progress by animateFloatAsState(state.progress, animationSpec = tween(400), label = "progress")

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().testTag("quiz_progress"))
        Text(
            state.progressLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp).testTag("quiz_counter"),
        )

        AnimatedContent(
            targetState = state.currentQuestion,
            contentKey = { it?.id },
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 2 } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200))) using
                    SizeTransform(clip = false)
            },
            modifier = Modifier.weight(1f),
            label = "question",
        ) { question ->
            if (question != null) QuestionCard(question, state, onIntent)
        }

        AnimatedVisibility(visible = state.isAnswerRevealed, enter = slideInVertically { it } + fadeIn(), exit = fadeOut(tween(120))) {
            Button(
                onClick = { onIntent(QuizIntent.Next) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("next_button"),
            ) {
                Text(if (state.isLastQuestion) "ফলাফল দেখুন" else "পরের প্রশ্ন")
            }
        }
    }
}

@Composable
private fun QuestionCard(question: Question, state: QuizState, onIntent: (QuizIntent) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().testTag("quiz_question_card")) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(question.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(4.dp))
            question.options.forEachIndexed { index, option ->
                AnswerOptionCard(
                    label = option.id,
                    text = option.text,
                    optionState = state.optionState(option.id),
                    onClick = { onIntent(QuizIntent.SelectOption(option.id)) },
                    modifier = Modifier.testTag("option_$index"),
                )
            }
            val answer = state.lastAnswer
            AnimatedVisibility(visible = answer != null, enter = expandVertically() + fadeIn()) {
                if (answer != null) {
                    val colors = LocalFeedbackColors.current
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (answer.isCorrect) "সঠিক উত্তর!" else "ভুল হয়েছে",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (answer.isCorrect) colors.correct else colors.incorrect,
                            modifier = Modifier.testTag("answer_feedback"),
                        )
                        answer.explanation?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(state: QuizState, onRestart: () -> Unit, onDone: () -> Unit) {
    val score = state.score
    val animatedProgress by animateFloatAsState(score.percent / 100f, animationSpec = tween(900), label = "ring")

    Column(
        Modifier.fillMaxSize().padding(32.dp).testTag("quiz_result"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.timeUp) {
            Text("সময় শেষ!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        }
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(160.dp),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(state.percentLabel, style = MaterialTheme.typography.displaySmall)
        }
        Text(if (score.isPassing) "চমৎকার!" else "আরও অনুশীলন করুন", style = MaterialTheme.typography.headlineSmall)
        Text(
            state.scoreLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("result_summary"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f).testTag("done_button")) { Text("ফিরে যান") }
            Button(onClick = onRestart, modifier = Modifier.weight(1f).testTag("restart_button")) { Text("আবার দিন") }
        }
    }
}
