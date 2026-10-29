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
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
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
        subjectId = "ssc-math",
        chapterId = "ssc-math-1",
        type = QuestionType.MCQ,
        prompt = "২ + ২ = কত?",
        options = listOf(AnswerOption("ক", "৩"), AnswerOption("খ", "৪"), AnswerOption("গ", "৫"), AnswerOption("ঘ", "৬")),
        correctOptionId = "খ",
        explanation = "দুই আর দুই মিলে চার।",
    )
    private val session = QuizSession(listOf(question, question.copy(id = "q2")))
    private val intents = mutableListOf<QuizIntent>()

    private fun render(state: QuizState) {
        composeRule.setContent {
            QuestionBankTheme(dynamicColor = false) {
                QuizContent(state = state, title = "অনুশীলন", onIntent = { intents += it }, onBack = {})
            }
        }
    }

    @Test
    fun tappingAnOptionDispatchesSelectOption() {
        render(QuizState(status = QuizStatus.IN_PROGRESS, session = session))

        composeRule.onNodeWithText("২ + ২ = কত?").assertIsDisplayed()
        composeRule.onNodeWithText("প্রশ্ন ১ / ২").assertIsDisplayed()
        composeRule.onNodeWithTag("option_1").performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.SelectOption("খ")), intents)
        composeRule.onNodeWithTag("next_button").assertDoesNotExist()
    }

    @Test
    fun revealedAnswerShowsFeedbackAndNextButton() {
        val answer = AnswerResult("q1", selectedOptionId = "ক", correctOptionId = "খ", isCorrect = false, explanation = "দুই আর দুই মিলে চার।")
        render(QuizState(status = QuizStatus.IN_PROGRESS, session = session.record(answer), selectedOptionId = "ক", lastAnswer = answer))

        composeRule.onNodeWithText("ভুল হয়েছে").assertIsDisplayed()
        composeRule.onNodeWithText("দুই আর দুই মিলে চার।").assertIsDisplayed()
        composeRule.onNodeWithTag("next_button").assertIsDisplayed().performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.Next), intents)
    }

    @Test
    fun examModeShowsCountdown() {
        render(QuizState(status = QuizStatus.IN_PROGRESS, session = session, remainingSeconds = 125))

        composeRule.onNodeWithTag("quiz_timer").assertIsDisplayed()
        composeRule.onNodeWithText("০২:০৫").assertIsDisplayed()
    }

    @Test
    fun finishedStateShowsBanglaScoreAndRestart() {
        val finished = QuizSession(listOf(question))
            .record(AnswerResult("q1", "খ", "খ", isCorrect = true, explanation = null))
            .advance()
        render(QuizState(status = QuizStatus.FINISHED, session = finished, timeUp = true))

        composeRule.onNodeWithText("সময় শেষ!").assertIsDisplayed()
        composeRule.onNodeWithText("১টির মধ্যে ১টি সঠিক").assertIsDisplayed()
        composeRule.onNodeWithTag("restart_button").performClick()

        assertEquals(listOf<QuizIntent>(QuizIntent.Restart), intents)
    }
}
