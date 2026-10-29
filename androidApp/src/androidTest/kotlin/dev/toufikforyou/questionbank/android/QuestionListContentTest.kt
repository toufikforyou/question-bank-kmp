package dev.toufikforyou.questionbank.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.toufikforyou.questionbank.android.ui.questions.QuestionListContent
import dev.toufikforyou.questionbank.android.ui.theme.QuestionBankTheme
import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.CreativePart
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.presentation.questions.QuestionItem
import dev.toufikforyou.questionbank.presentation.questions.QuestionListIntent
import dev.toufikforyou.questionbank.presentation.questions.QuestionListSource
import dev.toufikforyou.questionbank.presentation.questions.QuestionListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuestionListContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val mcq = Question(
        id = "m1", subjectId = "ssc-physics", chapterId = "ssc-physics-3", type = QuestionType.MCQ,
        prompt = "বলের এসআই একক কোনটি?",
        options = listOf(AnswerOption("ক", "নিউটন"), AnswerOption("খ", "জুল"), AnswerOption("গ", "ওয়াট"), AnswerOption("ঘ", "প্যাসকেল")),
        correctOptionId = "ক",
        explanation = "১ নিউটন = ১ kg·m/s²।",
    )
    private val cq = Question(
        id = "c1", subjectId = "ssc-physics", chapterId = "ssc-physics-2", type = QuestionType.CQ,
        prompt = "একটি গাড়ি স্থির অবস্থান থেকে চলা শুরু করল।",
        parts = listOf(CreativePart("ক", "ত্বরণ কাকে বলে?", 1, "বেগ বৃদ্ধির হার।")),
    )
    private val state = QuestionListState(
        source = QuestionListSource.Paper(PaperKey("ssc-physics", "dhaka", 2023)),
        isLoading = false,
        title = "ঢাকা বোর্ড ২০২৩",
        subtitle = "পদার্থবিজ্ঞান",
        items = listOf(
            QuestionItem(mcq, "১", "", "অধ্যায় ৩: বল", listOf("ঢাকা বোর্ড ২০২৩", "যশোর বোর্ড ২০১৯"), "২ বার এসেছে"),
            QuestionItem(cq, "২", "", "অধ্যায় ২: গতি", listOf("ঢাকা বোর্ড ২০২৩"), null),
        ),
    )
    private val intents = mutableListOf<QuestionListIntent>()

    private fun render(state: QuestionListState) = composeRule.setContent {
        QuestionBankTheme(dynamicColor = false) {
            QuestionListContent(state, onIntent = { intents += it }, onBack = {}, onPractise = {})
        }
    }

    @Test
    fun paperShowsQuestionsBoardTagsRepeatBadgeAndExamButton() {
        render(state)

        composeRule.onNodeWithTag("list_title").assertTextEquals("ঢাকা বোর্ড ২০২৩")
        composeRule.onNodeWithText("বলের এসআই একক কোনটি?").assertIsDisplayed()
        composeRule.onNodeWithTag("repeat_m1").assertIsDisplayed()
        composeRule.onNodeWithText("পরীক্ষা দিন (১ মিনিট)").assertIsDisplayed()
    }

    @Test
    fun tappingOptionAndRevealDispatchIntents() {
        render(state)

        composeRule.onNodeWithTag("option_m1_0").performClick()
        composeRule.onNodeWithTag("question_list").performScrollToNode(hasTestTag("reveal_c1"))
        composeRule.onNodeWithTag("reveal_c1").performClick()

        assertEquals(QuestionListIntent.SelectOption("m1", "ক"), intents[0])
        assertEquals(QuestionListIntent.ToggleReveal("c1"), intents[1])
    }

    @Test
    fun revealedCreativeQuestionShowsModelAnswer() {
        render(state.copy(items = state.items.filter { !it.question.isMcq }, revealed = setOf("c1")))

        composeRule.onNodeWithText("বেগ বৃদ্ধির হার।").assertIsDisplayed()
        composeRule.onNodeWithText("নমুনা উত্তর লুকান").assertIsDisplayed()
    }

    @Test
    fun bookmarkButtonDispatchesToggle() {
        render(state)

        composeRule.onNodeWithTag("bookmark_m1").performClick()

        assertTrue(intents.contains(QuestionListIntent.ToggleBookmark("m1")))
    }
}
