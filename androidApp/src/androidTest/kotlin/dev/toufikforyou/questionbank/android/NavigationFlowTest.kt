package dev.toufikforyou.questionbank.android

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end flow against the real app graph: database, bundled seed and shared stores.
 * No login screen may appear at any point.
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun waitFor(matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForTag(tag: String) = waitFor(hasTestTag(tag))

    @Test
    fun launchGoesStraightToHomeWithoutLogin() {
        waitForTag("category_list")
        composeRule.onNodeWithTag("quick_quiz_button").assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin").assertIsDisplayed()
    }

    @Test
    fun quickQuizAnswerAndAdvance() {
        waitForTag("quick_quiz_button")
        composeRule.onNodeWithTag("quick_quiz_button").performClick()

        waitForTag("quiz_question_card")
        composeRule.onNodeWithText("Question 1 of 10").assertIsDisplayed()

        composeRule.onNodeWithTag("option_0").performClick()
        waitForTag("next_button")
        composeRule.onNodeWithTag("answer_feedback").assertIsDisplayed()

        composeRule.onNodeWithTag("next_button").performClick()
        waitFor(hasText("Question 2 of 10"))
    }

    @Test
    fun backFromQuizReturnsHome() {
        waitForTag("quick_quiz_button")
        composeRule.onNodeWithTag("quick_quiz_button").performClick()
        waitForTag("quiz_question_card")

        composeRule.onNodeWithContentDescription("Back").performClick()

        waitForTag("category_list")
    }

    @Test
    fun statsScreenIsReachableFromHome() {
        waitForTag("stats_button")
        composeRule.onNodeWithTag("stats_button").performClick()

        waitFor(hasText("Statistics"))
    }
}
