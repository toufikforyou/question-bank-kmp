package dev.toufikforyou.questionbank.android

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        Thread.sleep(800) // let enter animations settle
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(composeRule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun captureMainScreens() {
        waitFor(hasTestTag("category_list"))
        capture("android-1-home")

        composeRule.onNodeWithTag("category_kotlin").performClick()
        waitFor(hasTestTag("quiz_question_card"))
        capture("android-2-question")

        composeRule.onNodeWithTag("bookmark_button").performClick()
        composeRule.onNodeWithTag("option_1").performClick()
        waitFor(hasTestTag("next_button"))
        capture("android-3-answer")

        repeat(6) {
            if (composeRule.onAllNodes(hasTestTag("next_button")).fetchSemanticsNodes().isEmpty()) {
                composeRule.onNodeWithTag("option_1").performClick()
                waitFor(hasTestTag("next_button"))
            }
            composeRule.onNodeWithTag("next_button").performClick()
            composeRule.waitForIdle()
        }
        waitFor(hasTestTag("quiz_result"))
        capture("android-4-results")

        composeRule.onNodeWithTag("done_button").performClick()
        waitFor(hasTestTag("stats_button"))
        composeRule.onNodeWithTag("stats_button").performClick()
        waitFor(hasText("By category").or(hasText("Statistics")))
        capture("android-5-statistics")

        composeRule.onNodeWithContentDescription("Back").performClick()
        waitFor(hasTestTag("bookmarks_button"))
        composeRule.onNodeWithTag("bookmarks_button").performClick()
        waitFor(hasTestTag("bookmark_list"))
        capture("android-6-bookmarks")
    }
}
