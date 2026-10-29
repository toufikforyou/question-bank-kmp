package dev.toufikforyou.questionbank.android.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.toufikforyou.questionbank.android.ui.components.LoadingState
import dev.toufikforyou.questionbank.android.ui.components.MessageState
import dev.toufikforyou.questionbank.android.ui.components.rememberHaptics
import dev.toufikforyou.questionbank.presentation.bookmarks.BookmarksEffect
import dev.toufikforyou.questionbank.presentation.bookmarks.BookmarksIntent
import dev.toufikforyou.questionbank.presentation.bookmarks.BookmarksStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(store: BookmarksStore, onBack: () -> Unit, onPractice: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            when (effect) {
                is BookmarksEffect.Haptic -> haptics.perform(effect.kind)
                is BookmarksEffect.ShowMessage -> launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.isEmpty -> MessageState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "No bookmarks yet",
                message = "Tap the bookmark icon during a quiz to save questions for later.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("bookmark_list"),
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "practice") {
                    Button(onClick = onPractice, modifier = Modifier.fillMaxWidth().animateItem()) {
                        Text("Practise ${state.questions.size} bookmarked")
                    }
                }
                items(state.questions, key = { it.id }) { question ->
                    Card(Modifier.animateItem()) {
                        ListItem(
                            headlineContent = { Text(question.prompt) },
                            supportingContent = { Text("Answer: ${question.correctOption.text}") },
                            trailingContent = {
                                IconButton(onClick = { store.dispatch(BookmarksIntent.Remove(question.id)) }) {
                                    Icon(Icons.Filled.Bookmark, contentDescription = "Remove bookmark")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}
