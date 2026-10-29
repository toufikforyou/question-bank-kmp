package dev.toufikforyou.questionbank.presentation

import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.usecase.GetCategoriesUseCase
import dev.toufikforyou.questionbank.domain.usecase.GetStatsUseCase
import dev.toufikforyou.questionbank.domain.usecase.InitializeAppUseCase
import dev.toufikforyou.questionbank.fakes.FakeAttemptRepository
import dev.toufikforyou.questionbank.fakes.FakeProfileRepository
import dev.toufikforyou.questionbank.fakes.FakeQuestionRepository
import dev.toufikforyou.questionbank.fakes.question
import dev.toufikforyou.questionbank.presentation.home.HomeIntent
import dev.toufikforyou.questionbank.presentation.home.HomeStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeStoreTest {

    private val profiles = FakeProfileRepository()

    private fun TestScope.createStore(repo: FakeQuestionRepository) = HomeStore(
        initializeApp = InitializeAppUseCase(profiles, repo),
        getCategories = GetCategoriesUseCase(repo),
        getStats = GetStatsUseCase(FakeAttemptRepository(), repo),
        dispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    @Test
    fun firstLaunchCreatesLocalProfileWithoutLogin() = runTest {
        val repo = FakeQuestionRepository(listOf(question("q1")), listOf(Category("general", "General", "", 1)))

        val store = createStore(repo)

        val state = store.currentState
        assertFalse(state.isLoading)
        assertEquals("Learner", state.profileName)
        assertEquals(1, profiles.created)
        assertEquals(1, repo.seedCalls)
        assertEquals(1, state.categories.size)
        store.close()
    }

    @Test
    fun initializationFailureIsRecoverableViaRetry() = runTest {
        val repo = FakeQuestionRepository(categories = listOf(Category("general", "General", "", 0)))
            .apply { failWith = IllegalStateException("seed corrupted") }
        val store = createStore(repo)
        assertNotNull(store.currentState.errorMessage)

        repo.failWith = null
        store.dispatch(HomeIntent.Retry)

        assertNull(store.currentState.errorMessage)
        assertEquals(1, store.currentState.categories.size)
        store.close()
    }
}
