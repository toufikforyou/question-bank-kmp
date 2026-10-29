package dev.toufikforyou.questionbank.android

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flows against the real app graph: database, bundled sample bank and shared stores.
 * No login screen may appear at any point.
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(matcher: SemanticsMatcher, timeout: Long = SEED_TIMEOUT_MS) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTag(tag: String) = waitFor(hasTestTag(tag))

    @Test
    fun launchGoesStraightToHomeWithPaperFinder() {
        waitForTag("paper_finder")
        composeRule.onNodeWithTag("finder_open").assertIsDisplayed()
        composeRule.onNodeWithTag("exam_SSC").assertIsDisplayed()
        composeRule.onNodeWithTag("exam_HSC").assertIsDisplayed()
    }

    @Test
    fun finderOpensTheSelectedBoardPaper() {
        waitForTag("finder_open")
        composeRule.onNodeWithTag("finder_board").performClick()
        composeRule.onNodeWithText("রাজশাহী").performClick()
        composeRule.onNodeWithTag("finder_open").performClick()

        waitForTag("question_list")
        composeRule.onNodeWithTag("list_title").assertTextEquals("রাজশাহী বোর্ড ২০২৫")

        composeRule.onAllNodes(hasTestTagPrefix("option_")).onFirst().performClick()
        waitFor(hasTestTagPrefix("feedback_"))
    }

    @Test
    fun subjectChapterListShowsChapterQuestions() {
        waitForTag("home_list")
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("subject_ssc-physics"))
        composeRule.onNodeWithTag("subject_ssc-physics").performClick()

        waitForTag("paper_list")
        composeRule.onNodeWithTag("tab_chapters").performClick()
        composeRule.onNodeWithTag("chapter_ssc-physics-3").performClick()

        waitForTag("question_list")
        composeRule.onNodeWithTag("list_title").assertTextEquals("অধ্যায় ৩: বল")
    }

    @Test
    fun paperExamModeStartsTimedQuizAndBackReturns() {
        waitForTag("finder_open")
        composeRule.onNodeWithTag("finder_open").performClick()
        waitForTag("practise_button")
        composeRule.onNodeWithTag("practise_button").performClick()

        waitForTag("quiz_timer")
        composeRule.onNodeWithTag("quiz_question_card").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("ফিরে যান").performClick()
        waitForTag("question_list")
    }

    @Test
    fun searchFindsQuestions() {
        waitForTag("search_button")
        composeRule.onNodeWithTag("search_button").performClick()

        composeRule.onNodeWithTag("search_field").performTextInput("বর্গমূল")
        waitFor(hasText("ফলাফল", substring = true))
    }
}
