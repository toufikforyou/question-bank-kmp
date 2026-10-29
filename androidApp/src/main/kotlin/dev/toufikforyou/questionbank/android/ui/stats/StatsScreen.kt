package dev.toufikforyou.questionbank.android.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.domain.model.SubjectStats
import dev.toufikforyou.questionbank.presentation.format.Labels
import dev.toufikforyou.questionbank.presentation.stats.StatsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(store: StatsStore, onBack: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("পরিসংখ্যান") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান") }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val stats = state.stats
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("stats_list"),
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "overview") {
                ElevatedCard(Modifier.fillMaxWidth().animateItem()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${Labels.percent(stats.accuracyPercent)} সঠিক", style = MaterialTheme.typography.headlineMedium)
                        Text("${Labels.number(stats.totalAnswered)}টি উত্তরের মধ্যে ${Labels.number(stats.totalCorrect)}টি সঠিক")
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Text("চলতি ধারা: ${Labels.number(stats.currentStreak)}")
                            Text("সেরা ধারা: ${Labels.number(stats.bestStreak)}")
                        }
                        Text("${Labels.number(stats.bookmarkedCount)}টি প্রশ্ন সংরক্ষিত", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (stats.subjectStats.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "প্রশ্নের উত্তর দিলে এখানে বিষয়ভিত্তিক অগ্রগতি দেখা যাবে।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            items(stats.subjectStats, key = { it.subjectId }) { subject ->
                SubjectProgress(subject, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun SubjectProgress(stats: SubjectStats, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(stats.accuracyPercent / 100f, animationSpec = tween(700), label = "accuracy")
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Labels.exam(stats.exam)} · ${stats.title}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(Labels.percent(stats.accuracyPercent), color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "${Labels.number(stats.answered)}টির মধ্যে ${Labels.number(stats.correct)}টি সঠিক",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
