package dev.toufikforyou.questionbank.android.ui.questions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.AnswerOptionCard
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.android.ui.components.MessageState
import dev.toufikforyou.questionbank.android.ui.components.rememberHaptics
import dev.toufikforyou.questionbank.android.ui.theme.LocalFeedbackColors
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.questions.QuestionFilter
import dev.toufikforyou.questionbank.presentation.questions.QuestionItem
import dev.toufikforyou.questionbank.presentation.questions.QuestionListEffect
import dev.toufikforyou.questionbank.presentation.questions.QuestionListIntent
import dev.toufikforyou.questionbank.presentation.questions.QuestionListState
import dev.toufikforyou.questionbank.presentation.questions.QuestionListStore
import kotlinx.coroutines.launch

@Composable
fun QuestionListScreen(store: QuestionListStore, onBack: () -> Unit, onPractise: (QuizRequest) -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is QuestionListEffect.Haptic -> haptics.perform(effect.kind)
                is QuestionListEffect.ShowMessage -> launch { snackbar.showSnackbar(effect.message) }
            }
        }
    }
    QuestionListContent(state, store::dispatch, onBack, onPractise, snackbar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListContent(
    state: QuestionListState,
    onIntent: (QuestionListIntent) -> Unit,
    onBack: () -> Unit,
    onPractise: (QuizRequest) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title, modifier = Modifier.testTag("list_title"))
                        if (state.subtitle.isNotEmpty()) {
                            Text(state.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isSearch) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onIntent(QuestionListIntent.UpdateQuery(it)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("search_field"),
                    placeholder = { Text("যেমন: নিউটন, সালোকসংশ্লেষণ, মূলধন") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> MessageState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "প্রশ্ন পাওয়া যায়নি",
                    message = state.errorMessage ?: "",
                    actionLabel = if (state.isSearch) null else "আবার চেষ্টা করুন",
                    onAction = if (state.isSearch) null else ({ onIntent(QuestionListIntent.Retry) }),
                )
                state.isEmpty -> MessageState(
                    icon = if (state.isBookmarks) Icons.Outlined.BookmarkBorder else if (state.isSearch) Icons.Outlined.Search else Icons.Outlined.Inbox,
                    title = if (state.isBookmarks) "কোনো সংরক্ষিত প্রশ্ন নেই" else if (state.isSearch) "প্রশ্ন খুঁজুন" else "কোনো প্রশ্ন নেই",
                    message = state.emptyMessage,
                )
                else -> QuestionList(state, onIntent, onPractise)
            }
        }
    }
}

@Composable
private fun QuestionList(state: QuestionListState, onIntent: (QuestionListIntent) -> Unit, onPractise: (QuizRequest) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("question_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "filters") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filter == QuestionFilter.ALL,
                    onClick = { onIntent(QuestionListIntent.SetFilter(QuestionFilter.ALL)) },
                    label = { Text("সব (${Labels.number(state.items.size)})") },
                )
                FilterChip(
                    selected = state.filter == QuestionFilter.MCQ,
                    onClick = { onIntent(QuestionListIntent.SetFilter(QuestionFilter.MCQ)) },
                    label = { Text("বহুনির্বাচনি (${Labels.number(state.mcqCount)})") },
                    modifier = Modifier.testTag("filter_mcq"),
                )
                FilterChip(
                    selected = state.filter == QuestionFilter.CQ,
                    onClick = { onIntent(QuestionListIntent.SetFilter(QuestionFilter.CQ)) },
                    label = { Text("সৃজনশীল (${Labels.number(state.cqCount)})") },
                    modifier = Modifier.testTag("filter_cq"),
                )
            }
        }
        state.practiceRequest?.let { request ->
            item(key = "practise") {
                Button(onClick = { onPractise(request) }, modifier = Modifier.fillMaxWidth().testTag("practise_button")) {
                    Text(state.practiceLabel)
                }
            }
        }
        if (state.isFilteredEmpty) {
            item(key = "filtered-empty") {
                Text(
                    state.filteredEmptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("filtered_empty"),
                )
            }
        }
        items(state.visibleItems, key = { it.question.id }) { item ->
            if (item.question.isMcq) {
                McqCard(item, state, onIntent, Modifier.animateItem())
            } else {
                CreativeCard(item, state.isRevealed(item.question.id), onIntent, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun QuestionHeader(item: QuestionItem, onIntent: (QuestionListIntent) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(item.number, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(item.chapterName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.subjectName.isNotEmpty()) {
                Text(item.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        IconButton(
            onClick = { onIntent(QuestionListIntent.ToggleBookmark(item.question.id)) },
            modifier = Modifier.testTag("bookmark_${item.question.id}"),
        ) {
            Icon(
                if (item.question.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (item.question.isBookmarked) "সংরক্ষণ বাতিল" else "সংরক্ষণ করুন",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(item: QuestionItem) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item.repeatLabel?.let { label ->
            AssistChip(
                onClick = {},
                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.padding(0.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.testTag("repeat_${item.question.id}"),
            )
        }
        item.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag, style = MaterialTheme.typography.labelSmall) }) }
    }
}

@Composable
private fun McqCard(item: QuestionItem, state: QuestionListState, onIntent: (QuestionListIntent) -> Unit, modifier: Modifier = Modifier) {
    val question = item.question
    val revealed = state.isRevealed(question.id)
    ElevatedCard(modifier.fillMaxWidth().testTag("question_${question.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestionHeader(item, onIntent)
            Text(question.prompt, style = MaterialTheme.typography.titleMedium)
            question.options.forEachIndexed { index, option ->
                AnswerOptionCard(
                    label = option.id,
                    text = option.text,
                    optionState = state.optionState(question.id, option.id),
                    onClick = { onIntent(QuestionListIntent.SelectOption(question.id, option.id)) },
                    compact = true,
                    modifier = Modifier.testTag("option_${question.id}_$index"),
                )
            }
            AnimatedVisibility(visible = revealed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val answer = state.answers[question.id]
                val colors = LocalFeedbackColors.current
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (answer != null) {
                        Text(
                            if (answer.isCorrect) "সঠিক উত্তর!" else "ভুল হয়েছে",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (answer.isCorrect) colors.correct else colors.incorrect,
                            modifier = Modifier.testTag("feedback_${question.id}"),
                        )
                    }
                    question.explanation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (state.answers[question.id] == null) {
                TextButton(onClick = { onIntent(QuestionListIntent.ToggleReveal(question.id)) }, modifier = Modifier.testTag("reveal_${question.id}")) {
                    Text(if (revealed) "উত্তর লুকান" else "উত্তর দেখুন")
                }
            }
            Tags(item)
        }
    }
}

@Composable
private fun CreativeCard(item: QuestionItem, revealed: Boolean, onIntent: (QuestionListIntent) -> Unit, modifier: Modifier = Modifier) {
    val question = item.question
    ElevatedCard(modifier.fillMaxWidth().testTag("question_${question.id}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuestionHeader(item, onIntent)
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("উদ্দীপক", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(question.prompt, style = MaterialTheme.typography.bodyLarge)
                }
            }
            question.parts.forEachIndexed { index, part ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text("${part.label}. ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(part.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(Labels.number(part.marks), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AnimatedVisibility(visible = revealed, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Surface(color = LocalFeedbackColors.current.correctContainer, shape = MaterialTheme.shapes.small) {
                            Text(part.answer, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp).fillMaxWidth())
                        }
                    }
                }
            }
            TextButton(onClick = { onIntent(QuestionListIntent.ToggleReveal(question.id)) }, modifier = Modifier.testTag("reveal_${question.id}")) {
                Text(if (revealed) "নমুনা উত্তর লুকান" else "নমুনা উত্তর দেখুন")
            }
            Tags(item)
        }
    }
}
