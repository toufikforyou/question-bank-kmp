package dev.toufikforyou.questionbank.android

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/** Matches nodes whose test tag starts with [prefix], e.g. "option_" for options of unknown questions. */
fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("testTag starts with '$prefix'") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}

const val SEED_TIMEOUT_MS = 30_000L
