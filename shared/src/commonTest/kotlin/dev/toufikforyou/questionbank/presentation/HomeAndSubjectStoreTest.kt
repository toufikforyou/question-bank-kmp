package dev.toufikforyou.questionbank.presentation

import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.usecase.GetCatalogUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetPaperSummariesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeProfileRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.HSC_ICT
import dev.toufikforyou.questionbank.fakes.PHYSICS
import dev.toufikforyou.questionbank.fakes.at
import dev.toufikforyou.questionbank.fakes.mcq
import dev.toufikforyou.questionbank.presentation.home.HomeIntent
import dev.toufikforyou.questionbank.presentation.home.HomeStore
import dev.toufikforyou.questionbank.presentation.subject.SubjectIntent
import dev.toufikforyou.questionbank.presentation.subject.SubjectStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeAndSubjectStoreTest {

    private val profiles = FakeProfileRepository()
    private val repo = FakeQuestionRepository(
        listOf(
            mcq("a", appearances = listOf(at("dhaka", 2024, 1), at("rajshahi", 2023, 1))),
            mcq("b", appearances = listOf(at("dhaka", 2024, 2))),
        ),
    )

    private fun TestScope.home() = HomeStore(
        initializeApp = InitializeAppUseCase(profiles, repo),
        getCatalog = GetCatalogUseCase(repo),
        getStats = GetStatsUseCase(FakeAttemptRepository(), repo),
        dispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun firstLaunchCreatesLocalProfileAndPrefillsFinder() = runTest {
        val store = home()

        val state = store.currentState
        assertFalse(state.isLoading)
        assertEquals(1, profiles.created)
        assertTrue(state.isSample)
        assertEquals(ExamType.SSC, state.selectedExam)
        assertEquals(PaperKey(PHYSICS, "dhaka", 2024), state.finderKey)
        assertEquals(listOf("বিজ্ঞান"), state.subjectSections.map { it.title })
        store.close()
    }

    @Test
    fun switchingExamResetsFinderToThatExam() = runTest {
        val store = home()

        store.dispatch(HomeIntent.SelectExam(ExamType.HSC))

        val state = store.currentState
        assertEquals(HSC_ICT, state.finderSubjectId)
        assertEquals(2024, state.finderYear)
        assertEquals("dhaka", state.finderBoardId)
        assertEquals("এইচএসসি", state.examLabel)
        store.close()
    }

    @Test
    fun finderSelectionsUpdateKey() = runTest {
        val store = home()

        store.dispatch(HomeIntent.SelectFinderBoard("rajshahi"))
        store.dispatch(HomeIntent.SelectFinderYear(2023))

        assertEquals(PaperKey(PHYSICS, "rajshahi", 2023), store.currentState.finderKey)
        store.close()
    }

    @Test
    fun initializationFailureIsRecoverableViaRetry() = runTest {
        repo.failWith = IllegalStateException("seed corrupted")
        val store = home()
        assertNotNull(store.currentState.errorMessage)

        repo.failWith = null
        store.dispatch(HomeIntent.Retry)

        assertNull(store.currentState.errorMessage)
        assertTrue(store.currentState.hasContent)
        store.close()
    }

    @Test
    fun subjectGroupsPapersByYearAndFiltersByBoard() = runTest {
        val store = SubjectStore(PHYSICS, GetCatalogUseCase(repo), GetPaperSummariesUseCase(repo), UnconfinedTestDispatcher(testScheduler))

        val sections = store.currentState.yearSections
        assertEquals(listOf(2024, 2023), sections.map { it.year })
        assertEquals("ঢাকা বোর্ড ২০২৪", sections.first().papers.single().title)
        assertEquals("বহুনির্বাচনি ২ · সৃজনশীল ০", sections.first().papers.single().countLabel)
        assertEquals("এসএসসি · বিষয় কোড ১৩৬ · বিজ্ঞান", store.currentState.subtitle)

        store.dispatch(SubjectIntent.FilterBoard("rajshahi"))

        assertEquals(listOf(2023), store.currentState.yearSections.map { it.year })
        store.close()
    }

    @Test
    fun subjectShowsYearNotesEvenWithoutPapers() = runTest {
        val store = SubjectStore(HSC_ICT, GetCatalogUseCase(repo), GetPaperSummariesUseCase(repo), UnconfinedTestDispatcher(testScheduler))

        val note = store.currentState.yearSections.single { it.year == 2020 }
        assertTrue(note.papers.isEmpty())
        assertEquals("২০২০ সালের পরীক্ষা অনুষ্ঠিত হয়নি।", note.note)
        store.close()
    }
}
