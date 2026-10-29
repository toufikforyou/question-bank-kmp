package dev.toufikforyou.questionbank.android

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Captures store-style screenshots of the main screens. Skipped unless the instrumentation
 * argument `screenshots=true` is passed (see .github/workflows/screenshots.yml).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotCaptureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun onlyWhenRequested() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("screenshots") == "true")
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = SEED_TIMEOUT_MS) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(900) // let enter animations settle
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(composeRule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun back() {
        composeRule.onNodeWithContentDescription("ফিরে যান").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun captureMainScreens() {
        waitFor(hasTestTag("paper_finder"))
        capture("android-1-home")

        composeRule.onNodeWithTag("finder_open").performClick()
        waitFor(hasTestTag("question_list"))
        capture("android-2-board-paper")

        composeRule.onAllNodes(hasTestTagPrefix("option_")).onFirst().performClick()
        waitFor(hasTestTagPrefix("feedback_"))
        capture("android-3-answer")

        composeRule.onNodeWithTag("practise_button").performClick()
        waitFor(hasTestTag("quiz_timer"))
        capture("android-4-exam-mode")
        back()
        waitFor(hasTestTag("question_list"))
        back()

        waitFor(hasTestTag("home_list"))
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("subject_ssc-physics"))
        composeRule.onNodeWithTag("subject_ssc-physics").performClick()
        waitFor(hasTestTag("paper_list"))
        capture("android-5-subject-papers")

        composeRule.onNodeWithTag("tab_chapters").performClick()
        composeRule.onNodeWithTag("chapter_ssc-physics-2").performClick()
        waitFor(hasTestTag("question_list"))
        composeRule.onNodeWithTag("filter_cq").performClick()
        composeRule.waitForIdle()
        Thread.sleep(600)
        composeRule.onNodeWithTag("question_list").performScrollToNode(hasTestTagPrefix("reveal_"))
        val revealTag = composeRule.onAllNodes(hasTestTagPrefix("reveal_")).fetchSemanticsNodes()
            .first().config[SemanticsProperties.TestTag]
        composeRule.onNodeWithTag(revealTag).performClick()
        capture("android-6-creative")
    }
}
