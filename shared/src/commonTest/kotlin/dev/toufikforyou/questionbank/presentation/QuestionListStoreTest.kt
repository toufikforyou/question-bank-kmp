package dev.toufikforyou.questionbank.presentation

import app.cash.turbine.test
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.usecase.AnswerQuestionUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetBookmarkedQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetChapterQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperUseCase
import dev.toufikforyou.questionbank.domain.usecase.SearchQuestionsUseCase
import dev.toufikforyou.questionbank.domain.usecase.ToggleBookmarkUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.FixedTimeProvider
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.PHYSICS_CH1
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.creative
import dev.toufikforyou.questionbank.fakes.mcq
import dev.toufikforyou.questionbank.presentation.questions.QuestionFilter
import dev.toufikforyou.questionbank.presentation.questions.QuestionListEffect
import dev.toufikforyou.questionbank.presentation.questions.QuestionListIntent
import dev.toufikforyou.questionbank.presentation.questions.QuestionListSource
import dev.toufikforyou.questionbank.presentation.questions.QuestionListStore
import dev.toufikforyou.questionbank.presentation.quiz.HapticKind
import dev.toufikforyou.questionbank.presentation.quiz.OptionState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionListStoreTest {

    private val repeated = (1..6).map { at(listOf("dhaka", "rajshahi")[it % 2], 2018 + it, it) }
    private val repo = FakeQuestionRepository(
        listOf(
            mcq("m1", correctOptionId = "গ", appearances = listOf(at("dhaka", 2023, 2)) + repeated, prompt = "বলের একক কোনটি?"),
            mcq("m2", appearances = listOf(at("dhaka", 2023, 1))),
            creative("c1", appearances = listOf(at("dhaka", 2023, 3))),
        ),
    )

    private fun TestScope.store(source: QuestionListSource, unconfined: Boolean = true) = QuestionListStore(
        source = source,
        getCatalog = GetCatalogUseCase(repo),
        getPaper = GetPaperUseCase(repo),
        getChapterQuestions = GetChapterQuestionsUseCase(repo),
        searchQuestions = SearchQuestionsUseCase(repo),
        getBookmarked = GetBookmarkedQuestionsUseCase(repo),
        answerQuestion = AnswerQuestionUseCase(repo, FakeAttemptRepository(), FixedTimeProvider()),
        toggleBookmark = ToggleBookmarkUseCase(repo),
        dispatcher = if (unconfined) UnconfinedTestDispatcher(testScheduler) else StandardTestDispatcher(testScheduler),
    )

    @Test
    fun paperListsQuestionsInPaperOrderWithBoardTagsAndExamPractice() = runTest {
        val store = store(QuestionListSource.Paper(PaperKey(PHYSICS, "dhaka", 2023)))

        val state = store.currentState
        assertEquals("ঢাকা বোর্ড ২০২৩", state.title)
        assertEquals("পদার্থবিজ্ঞান", state.subtitle)
        assertEquals(listOf("m2", "m1", "c1"), state.items.map { it.question.id })
        assertEquals(listOf("১", "২", "৩"), state.items.map { it.number })
        val m1 = state.items[1]
        assertEquals("৭ বার এসেছে", m1.repeatLabel)
        assertEquals(5, m1.tags.size)
        assertEquals("+৩টি", m1.tags.last())
        assertEquals("অধ্যায় ১: ভৌত রাশি", m1.chapterName)
        assertTrue(state.practiceRequest!!.timed)
        assertEquals("পরীক্ষা দিন (২ মিনিট)", state.practiceLabel)
        store.close()
    }

    @Test
    fun filterSplitsMcqAndCreative() = runTest {
        val store = store(QuestionListSource.Paper(PaperKey(PHYSICS, "dhaka", 2023)))

        store.dispatch(QuestionListIntent.SetFilter(QuestionFilter.CQ))

        assertEquals(listOf("c1"), store.currentState.visibleItems.map { it.question.id })
        store.close()
    }

    @Test
    fun inlineAnswerRevealsResultOnceWithHaptic() = runTest {
        val store = store(QuestionListSource.Chapter(PHYSICS_CH1))

        store.effects.test {
            store.dispatch(QuestionListIntent.SelectOption("m1", "ক"))
            assertEquals(QuestionListEffect.Haptic(HapticKind.INCORRECT), awaitItem())
        }
        store.dispatch(QuestionListIntent.SelectOption("m1", "গ"))

        val state = store.currentState
        assertEquals(OptionState.INCORRECT, state.optionState("m1", "ক"))
        assertEquals(OptionState.CORRECT, state.optionState("m1", "গ"))
        assertTrue(state.isRevealed("m1"))
        store.close()
    }

    @Test
    fun revealWithoutAnsweringHighlightsCorrectOptionAndTogglesCreativeAnswers() = runTest {
        val store = store(QuestionListSource.Chapter(PHYSICS_CH1))

        store.dispatch(QuestionListIntent.ToggleReveal("m1"))
        store.dispatch(QuestionListIntent.ToggleReveal("c1"))

        assertEquals(OptionState.CORRECT, store.currentState.optionState("m1", "গ"))
        assertTrue(store.currentState.isRevealed("c1"))

        store.dispatch(QuestionListIntent.ToggleReveal("c1"))
        assertFalse(store.currentState.isRevealed("c1"))
        store.close()
    }

    @Test
    fun searchIsDebouncedAndIgnoresShortQueries() = runTest {
        val store = store(QuestionListSource.Search(ExamType.SSC), unconfined = false)
        runCurrent()
        assertEquals("প্রশ্ন খুঁজুন", store.currentState.title)

        store.dispatch(QuestionListIntent.UpdateQuery("ব"))
        advanceTimeBy(1_000)
        assertNull(repo.lastSearch)

        store.dispatch(QuestionListIntent.UpdateQuery("বল"))
        store.dispatch(QuestionListIntent.UpdateQuery("বলের"))
        advanceTimeBy(300)
        runCurrent()

        assertEquals("বলের", repo.lastSearch)
        assertEquals(listOf("m1"), store.currentState.items.map { it.question.id })
        assertEquals("১টি ফলাফল", store.currentState.subtitle)
        store.close()
    }

    @Test
    fun bookmarksListFollowsBookmarkChanges() = runTest {
        val store = store(QuestionListSource.Bookmarks)
        assertTrue(store.currentState.isEmpty)
        assertNull(store.currentState.practiceRequest)

        ToggleBookmarkUseCase(repo)("m2")

        assertEquals(listOf("m2"), store.currentState.items.map { it.question.id })
        assertNotNull(store.currentState.practiceRequest)

        store.dispatch(QuestionListIntent.ToggleBookmark("m2"))
        assertTrue(store.currentState.isEmpty)
        store.close()
    }

    @Test
    fun missingPaperShowsExplanation() = runTest {
        val store = store(QuestionListSource.Paper(PaperKey(PHYSICS, "rajshahi", 2015)))

        assertEquals("এই বোর্ড ও সালের প্রশ্ন এখনো যুক্ত হয়নি।", store.currentState.errorMessage)
        assertEquals("রাজশাহী বোর্ড ২০১৫", store.currentState.title)
        store.close()
    }
}
