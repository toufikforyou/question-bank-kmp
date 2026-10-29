package dev.toufikforyou.questionbank.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.repository.SqlDelightAttemptRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightProfileRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightQuestionRepository
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integration test of the real SQLDelight schema and the bundled seed file, on the JVM. */
class SqlDelightRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { QuestionBankDatabase.Schema.create(it) }
    private val database = QuestionBankDatabase(driver)
    private val seed = File("../seed/questions.json").readText()
    private var readCount = 0
    private val questions = SqlDelightQuestionRepository(
        database = database,
        seedReader = { readCount++; seed },
        timeProvider = { 42L },
        ioDispatcher = Dispatchers.Unconfined,
    )

    @AfterTest
    fun tearDown() = driver.close()

    @Test
    fun bundledSeedImportsAllCategoriesAndQuestionsOnce() = runTest {
        questions.ensureSeeded()
        questions.ensureSeeded()

        val categories = questions.observeCategories().first()
        assertEquals(4, categories.size)
        assertEquals(24, questions.getQuestions().size)
        assertTrue(categories.all { it.questionCount == 6 })
        assertEquals(1, readCount, "seed must be parsed only once per process")
    }

    @Test
    fun bookmarksPersistAndAreReflectedInQuestions() = runTest {
        questions.setBookmarked("math-001", true)
        questions.setBookmarked("kotlin-002", true)

        assertEquals(listOf("kotlin-002", "math-001"), questions.observeBookmarkedQuestions().first().map { it.id })
        assertTrue(questions.getQuestion("math-001")!!.isBookmarked)

        val reopened = SqlDelightQuestionRepository(database, { seed }, { 0L }, Dispatchers.Unconfined)
        assertEquals(2, reopened.observeBookmarkedQuestions().first().size)
    }

    @Test
    fun attemptsAndProfileRoundTrip() = runTest {
        val attempts = SqlDelightAttemptRepository(database, Dispatchers.Unconfined)
        attempts.record(AnswerRecord("math-001", "math", "b", isCorrect = true, answeredAtMillis = 10))

        assertEquals(1, attempts.observeAttempts().first().size)

        val profiles = SqlDelightProfileRepository(database, { 7L }, Dispatchers.Unconfined)
        val first = profiles.getOrCreateProfile()
        val again = SqlDelightProfileRepository(database, { 99L }, Dispatchers.Unconfined).getOrCreateProfile()
        assertEquals(first, again)
        assertEquals(7L, again.createdAtMillis)
    }
}
