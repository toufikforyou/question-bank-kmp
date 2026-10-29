package dev.toufikforyou.questionbank.android.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.android.ui.components.MessageState
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.QuizRequest
import dev.toufikforyou.questionbank.presentation.subject.SubjectIntent
import dev.toufikforyou.questionbank.presentation.subject.SubjectState
import dev.toufikforyou.questionbank.presentation.subject.SubjectStore
import dev.toufikforyou.questionbank.presentation.subject.SubjectTab

@Composable
fun SubjectScreen(
    store: SubjectStore,
    onBack: () -> Unit,
    onOpenPaper: (PaperKey) -> Unit,
    onOpenChapter: (String) -> Unit,
    onPractise: (QuizRequest) -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    SubjectContent(state, store::dispatch, onBack, onOpenPaper, onOpenChapter, onPractise)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectContent(
    state: SubjectState,
    onIntent: (SubjectIntent) -> Unit,
    onBack: () -> Unit,
    onOpenPaper: (PaperKey) -> Unit,
    onOpenChapter: (String) -> Unit,
    onPractise: (QuizRequest) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title)
                        Text(state.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> MessageState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "বিষয়টি খোলা যায়নি",
                    message = state.errorMessage ?: "",
                    actionLabel = "আবার চেষ্টা করুন",
                    onAction = { onIntent(SubjectIntent.Retry) },
                )
                else -> {
                    TabRow(selectedTabIndex = state.tab.ordinal) {
                        Tab(
                            selected = state.tab == SubjectTab.PAPERS,
                            onClick = { onIntent(SubjectIntent.SelectTab(SubjectTab.PAPERS)) },
                            text = { Text("বোর্ড প্রশ্ন") },
                            modifier = Modifier.testTag("tab_papers"),
                        )
                        Tab(
                            selected = state.tab == SubjectTab.CHAPTERS,
                            onClick = { onIntent(SubjectIntent.SelectTab(SubjectTab.CHAPTERS)) },
                            text = { Text("অধ্যায়ভিত্তিক") },
                            modifier = Modifier.testTag("tab_chapters"),
                        )
                    }
                    when (state.tab) {
                        SubjectTab.PAPERS -> PapersTab(state, onIntent, onOpenPaper)
                        SubjectTab.CHAPTERS -> ChaptersTab(state, onOpenChapter, onPractise)
                    }
                }
            }
        }
    }
}

@Composable
private fun PapersTab(state: SubjectState, onIntent: (SubjectIntent) -> Unit, onOpenPaper: (PaperKey) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("paper_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.boardFilter == null,
                        onClick = { onIntent(SubjectIntent.FilterBoard(null)) },
                        label = { Text("সব বোর্ড") },
                    )
                }
                items(state.boards, key = { it.id }) { board ->
                    FilterChip(
                        selected = state.boardFilter == board.id,
                        onClick = { onIntent(SubjectIntent.FilterBoard(board.id)) },
                        label = { Text(board.name) },
                        modifier = Modifier.testTag("board_filter_${board.id}"),
                    )
                }
            }
        }
        state.yearSections.forEach { section ->
            item(key = "year-${section.year}") {
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.animateItem().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            section.note?.let { note ->
                item(key = "note-${section.year}") {
                    Card(
                        modifier = Modifier.animateItem().fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                            Text(note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            items(section.papers, key = { "${it.key.boardId}-${it.key.year}" }) { paper ->
                PaperRow(paper.title, paper.countLabel, onClick = { onOpenPaper(paper.key) }, tag = "paper_${paper.key.boardId}_${paper.key.year}", modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun PaperRow(title: String, subtitle: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag(tag)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ChaptersTab(state: SubjectState, onOpenChapter: (String) -> Unit, onPractise: (QuizRequest) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("chapter_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.practiceRequest?.let { request ->
            item(key = "practise") {
                Button(onClick = { onPractise(request) }, modifier = Modifier.fillMaxWidth().testTag("practise_subject")) {
                    Text("এই বিষয়ের বহুনির্বাচনি অনুশীলন")
                }
            }
        }
        items(state.chapters, key = { it.id }) { chapter ->
            Card(onClick = { onOpenChapter(chapter.id) }, modifier = Modifier.animateItem().fillMaxWidth().testTag("chapter_${chapter.id}")) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.chapterLabel(chapter), style = MaterialTheme.typography.titleSmall)
                        Text(state.chapterCountLabel(chapter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
