package dev.toufikforyou.questionbank.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.UserStats
import dev.toufikforyou.questionbank.presentation.home.HomeIntent
import dev.toufikforyou.questionbank.presentation.home.HomeState
import dev.toufikforyou.questionbank.presentation.home.HomeStore

@Composable
fun HomeScreen(
    store: HomeStore,
    onStartQuiz: (Category?) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onRetry = { store.dispatch(HomeIntent.Retry) },
        onStartQuiz = onStartQuiz,
        onOpenBookmarks = onOpenBookmarks,
        onOpenStats = onOpenStats,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeState,
    onRetry: () -> Unit,
    onStartQuiz: (Category?) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenStats: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Question Bank") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onOpenBookmarks, modifier = Modifier.testTag("bookmarks_button")) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks")
                    }
                    IconButton(onClick = onOpenStats, modifier = Modifier.testTag("stats_button")) {
                        Icon(Icons.Outlined.Insights, contentDescription = "Statistics")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && !state.hasContent -> LoadingState(Modifier.padding(padding))
            state.errorMessage != null && !state.hasContent -> MessageState(
                icon = Icons.Outlined.CloudOff,
                title = "Couldn't load questions",
                message = state.errorMessage ?: "",
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("category_list"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    StatsSummaryCard(state.stats, state.totalQuestions, Modifier.animateItem())
                }
                item(key = "quick") {
                    Button(
                        onClick = { onStartQuiz(null) },
                        modifier = Modifier.fillMaxWidth().animateItem().testTag("quick_quiz_button"),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start quick quiz")
                    }
                }
                item(key = "header") {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.animateItem().padding(top = 8.dp),
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    CategoryCard(category, onClick = { onStartQuiz(category) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(stats: UserStats, totalQuestions: Int, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryValue("${stats.accuracyPercent}%", "Accuracy")
            SummaryValue("${stats.totalAnswered}", "Answered")
            SummaryValue("${stats.currentStreak}", "Streak")
            SummaryValue("$totalQuestions", "Questions")
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("category_${category.id}"),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${category.questionCount} questions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
