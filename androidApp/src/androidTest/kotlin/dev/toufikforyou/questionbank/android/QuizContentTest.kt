package dev.toufikforyou.questionbank.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.toufikforyou.questionbank.android.ui.quiz.QuizContent
import dev.toufikforyou.questionbank.android.ui.theme.QuestionBankTheme
import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.AnswerResult
import dev.toufikforyou.questionbank.domain.model.Difficulty
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuizScore
import dev.toufikforyou.questionbank.domain.model.QuizSession
import dev.toufikforyou.questionbank.presentation.quiz.QuizIntent
import dev.toufikforyou.questionbank.presentation.quiz.QuizState
import dev.toufikforyou.questionbank.presentation.quiz.QuizStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders the stateless quiz UI with hand-built states and asserts on the intents it emits. */
@RunWith(AndroidJUnit4::class)
class QuizContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val question = Question(
        id = "q1",
        categoryId = "math",
        prompt = "What is 2 + 2?",
        options = listOf(AnswerOption("a", "3"), AnswerOption("b", "4"), AnswerOption("c", "5")),
        correctOptionId = "b",
        explanation = "Two plus two equals four.",
        difficulty = Difficulty.EASY,
    )
    private val session = QuizSession(listOf(question, question.copy(id = "q2")))
    private val intents = mutableListOf<QuizIntent>()

    private fun render(state: QuizState) {
        composeRule.setContent {
            QuestionBankTheme(dynamicColor = false) {
                QuizContent(state = state, title = "Math", onIntent = { intents += it }, onBack = {})
            }
        }
    }

    @Test
    fun tappingAnOptionDispatchesSelectOption() {
        render(QuizState(status = QuizStatus.IN_PROGRESS, session = session))

        composeRule.onNodeWithText("What is 2 + 2?").assertIsDisplayed()
        composeRule.onNodeWithTag("option_1").performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.SelectOption("b")), intents)
        composeRule.onNodeWithTag("next_button").assertDoesNotExist()
    }

    @Test
    fun revealedAnswerShowsFeedbackAndNextButton() {
        val answer = AnswerResult("q1", selectedOptionId = "a", correctOptionId = "b", isCorrect = false, explanation = "Two plus two equals four.")
        render(
            QuizState(
                status = QuizStatus.IN_PROGRESS,
                session = session.record(answer),
                selectedOptionId = "a",
                lastAnswer = answer,
            ),
        )

        composeRule.onNodeWithTag("answer_feedback").assertIsDisplayed()
        composeRule.onNodeWithText("Not quite").assertIsDisplayed()
        composeRule.onNodeWithText("Two plus two equals four.").assertIsDisplayed()
        composeRule.onNodeWithTag("next_button").assertIsDisplayed().performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.Next), intents)
    }

    @Test
    fun bookmarkButtonDispatchesToggle() {
        render(QuizState(status = QuizStatus.IN_PROGRESS, session = session))

        composeRule.onNodeWithTag("bookmark_button").performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.ToggleBookmark), intents)
    }

    @Test
    fun finishedStateShowsScoreAndRestart() {
        val finished = QuizSession(listOf(question))
            .record(AnswerResult("q1", "b", "b", isCorrect = true, explanation = null))
            .advance()
        render(QuizState(status = QuizStatus.FINISHED, session = finished))

        assertEquals(QuizScore(1, 1), finished.score)
        composeRule.onNodeWithText("You answered 1 of 1 correctly.").assertIsDisplayed()
        composeRule.onNodeWithTag("restart_button").performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.Restart), intents)
    }
}
