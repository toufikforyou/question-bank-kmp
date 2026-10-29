package dev.toufikforyou.questionbank.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.android.ui.components.MessageState
import dev.toufikforyou.questionbank.android.ui.components.PickerField
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.Subject
import dev.toufikforyou.questionbank.domain.model.toBanglaDigits
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.home.HomeIntent
import dev.toufikforyou.questionbank.presentation.home.HomeState
import dev.toufikforyou.questionbank.presentation.home.HomeStore

@Composable
fun HomeScreen(
    store: HomeStore,
    onOpenSubject: (String) -> Unit,
    onOpenPaper: (PaperKey) -> Unit,
    onSearch: (ExamType) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onIntent = store::dispatch,
        onOpenSubject = onOpenSubject,
        onOpenPaper = onOpenPaper,
        onSearch = onSearch,
        onOpenBookmarks = onOpenBookmarks,
        onOpenStats = onOpenStats,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onOpenSubject: (String) -> Unit,
    onOpenPaper: (PaperKey) -> Unit,
    onSearch: (ExamType) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("প্রশ্নব্যাংক") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { onSearch(state.selectedExam) }, modifier = Modifier.testTag("search_button")) {
                        Icon(Icons.Outlined.Search, contentDescription = "প্রশ্ন খুঁজুন")
                    }
                    IconButton(onClick = onOpenBookmarks, modifier = Modifier.testTag("bookmarks_button")) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "সংরক্ষিত প্রশ্ন")
                    }
                    IconButton(onClick = onOpenStats, modifier = Modifier.testTag("stats_button")) {
                        Icon(Icons.Outlined.Insights, contentDescription = "পরিসংখ্যান")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && !state.hasContent -> LoadingState(Modifier.padding(padding))
            state.errorMessage != null && !state.hasContent -> MessageState(
                icon = Icons.Outlined.CloudOff,
                title = "প্রশ্নব্যাংক খোলা যায়নি",
                message = state.errorMessage ?: "",
                actionLabel = "আবার চেষ্টা করুন",
                onAction = { onIntent(HomeIntent.Retry) },
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("home_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "exam") { ExamSwitch(state, onIntent, Modifier.animateItem()) }
                if (state.isSample) {
                    item(key = "sample") { SampleNotice(Modifier.animateItem()) }
                }
                item(key = "finder") { PaperFinder(state, onIntent, onOpenPaper, Modifier.animateItem()) }
                item(key = "summary") { Summary(state, Modifier.animateItem()) }
                state.subjectSections.forEach { section ->
                    item(key = "header-${state.selectedExam}-${section.title}") {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.animateItem().padding(top = 8.dp),
                        )
                    }
                    items(section.subjects, key = { it.id }) { subject ->
                        SubjectCard(subject, onClick = { onOpenSubject(subject.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamSwitch(state: HomeState, onIntent: (HomeIntent) -> Unit, modifier: Modifier = Modifier) {
    val exams = state.exams
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        exams.forEachIndexed { index, exam ->
            SegmentedButton(
                selected = exam == state.selectedExam,
                onClick = { onIntent(HomeIntent.SelectExam(exam)) },
                shape = SegmentedButtonDefaults.itemShape(index, exams.size),
                modifier = Modifier.testTag("exam_${exam.name}"),
            ) {
                Text(Labels.exam(exam))
            }
        }
    }
}

@Composable
private fun SampleNotice(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Text(
                "নমুনা প্রশ্ন: এগুলো অনুশীলনের জন্য তৈরি, আসল বোর্ড প্রশ্ন নয়।",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PaperFinder(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onOpenPaper: (PaperKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier.fillMaxWidth().testTag("paper_finder")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("বোর্ড প্রশ্ন খুঁজুন", style = MaterialTheme.typography.titleMedium)
            PickerField(
                label = "বিষয়",
                value = state.finderSubject?.name ?: "বিষয় বাছাই করুন",
                options = state.subjects,
                optionLabel = { it.name },
                onSelect = { onIntent(HomeIntent.SelectFinderSubject(it.id)) },
                testTag = "finder_subject",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    label = "বোর্ড",
                    value = state.finderBoard?.name ?: "বোর্ড",
                    options = state.boards,
                    optionLabel = { it.name },
                    onSelect = { onIntent(HomeIntent.SelectFinderBoard(it.id)) },
                    modifier = Modifier.weight(1f),
                    testTag = "finder_board",
                )
                PickerField(
                    label = "সাল",
                    value = state.finderYear?.toBanglaDigits() ?: "সাল",
                    options = state.years,
                    optionLabel = { it.toBanglaDigits() },
                    onSelect = { onIntent(HomeIntent.SelectFinderYear(it)) },
                    modifier = Modifier.weight(1f),
                    testTag = "finder_year",
                )
            }
            state.finderNote?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { state.finderKey?.let(onOpenPaper) },
                enabled = state.finderKey != null,
                modifier = Modifier.fillMaxWidth().testTag("finder_open"),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text("প্রশ্ন দেখুন")
            }
        }
    }
}

@Composable
private fun Summary(state: HomeState, modifier: Modifier = Modifier) {
    val stats = state.stats
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryValue(Labels.number(state.totalQuestions), "প্রশ্ন")
            SummaryValue(Labels.number(state.boards.size), "বোর্ড")
            SummaryValue(Labels.number(stats.totalAnswered), "উত্তর দিয়েছেন")
            SummaryValue(Labels.percent(stats.accuracyPercent), "সঠিকতা")
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SubjectCard(subject: Subject, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().testTag("subject_${subject.id}")) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "বিষয় কোড ${subject.code.toBanglaDigits()} · ${Labels.number(subject.chapters.size)}টি অধ্যায়",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${Labels.number(subject.questionCount)}টি প্রশ্ন",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
