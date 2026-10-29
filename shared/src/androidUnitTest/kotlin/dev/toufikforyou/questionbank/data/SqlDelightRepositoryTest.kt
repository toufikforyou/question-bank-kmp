package dev.toufikforyou.questionbank.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.repository.SqlDelightAttemptRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightProfileRepository
import dev.toufikforyou.questionbank.data.repository.SqlDelightQuestionRepository
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.StudyGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integration test of the real schema against the bundled sample bank, on the JVM. */
class SqlDelightRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { QuestionBankDatabase.Schema.create(it) }
    private val database = QuestionBankDatabase(driver)
    private val seed = File("../seed/questions.json").readText()
    private var readCount = 0
    private val repo = SqlDelightQuestionRepository(
        database = database,
        seedReader = { readCount++; seed },
        timeProvider = { 42L },
        ioDispatcher = Dispatchers.Unconfined,
    )

    @AfterTest
    fun tearDown() = driver.close()

    @Test
    fun bundledBankCoversBothExamsAllBoardsAndAllYears() = runTest {
        val started = System.nanoTime()
        val catalog = repo.getCatalog()
        println("First-launch import took ${(System.nanoTime() - started) / 1_000_000} ms")

        assertTrue(catalog.isSample)
        assertEquals(11, catalog.boards.size)
        assertEquals(23, catalog.subjectsFor(ExamType.SSC).size)
        assertEquals(43, catalog.subjectsFor(ExamType.HSC).size)
        assertEquals((2015..2025).toList().reversed(), catalog.yearsFor(ExamType.SSC))
        assertTrue(2020 !in catalog.yearsFor(ExamType.HSC), "HSC 2020 was not held")
        assertTrue(catalog.subjects.all { s -> s.chapters.isNotEmpty() && s.questionCount > 0 })
        assertEquals(StudyGroup.entries.toSet(), catalog.subjects.map { it.group }.toSet(), "every group has subjects")
        assertEquals(1, readCount, "seed must be parsed only once per process")
    }

    @Test
    fun everyBoardHasAFullPaperForASubjectAndYear() = runTest {
        val catalog = repo.getCatalog()
        catalog.boards.forEach { board ->
            val questions = repo.getPaperQuestions(PaperKey("ssc-physics", board.id, 2023))
            assertEquals(8, questions.size, "paper for ${board.id}")
            assertEquals(6, questions.count { it.isMcq })
            assertTrue(questions.all { q -> q.appearances.any { it.boardId == board.id && it.year == 2023 } })
        }
        val summaries = repo.getPaperSummaries("ssc-physics")
        assertTrue(summaries.size >= 11 * 8)
        assertTrue(repo.getPaperQuestions(PaperKey("ssc-math", "dhaka", 2021)).isEmpty(), "2021 had no compulsory subjects")
    }

    @Test
    fun chapterQuestionsAreMostRepeatedFirstAndSearchWorks() = runTest {
        val chapter = repo.getChapterQuestions("ssc-physics-3")
        assertTrue(chapter.isNotEmpty())
        assertTrue(chapter.zipWithNext().all { (a, b) -> a.repeatCount >= b.repeatCount })

        val results = repo.search("নিউটন", ExamType.SSC, 20)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.subjectId.startsWith("ssc-") })
    }

    @Test
    fun bookmarksPersistAcrossRepositoryInstances() = runTest {
        val first = repo.getChapterQuestions("ssc-math-1").first().id
        val second = repo.getChapterQuestions("hsc-ict-3").first().id
        repo.setBookmarked(first, true)
        repo.setBookmarked(second, true)

        assertEquals(listOf(second, first), repo.observeBookmarkedQuestions().first().map { it.id })
        assertTrue(repo.getQuestion(first)!!.isBookmarked)

        val reopened = SqlDelightQuestionRepository(database, { seed }, { 0L }, Dispatchers.Unconfined)
        assertEquals(2, reopened.observeBookmarkedQuestions().first().size)
    }

    @Test
    fun attemptsAndProfileRoundTrip() = runTest {
        val attempts = SqlDelightAttemptRepository(database, Dispatchers.Unconfined)
        attempts.record(AnswerRecord("q", "ssc-math", "ক", isCorrect = true, answeredAtMillis = 10))
        assertEquals(1, attempts.observeAttempts().first().size)

        val profile = SqlDelightProfileRepository(database, { 7L }, Dispatchers.Unconfined).getOrCreateProfile()
        assertEquals(profile, SqlDelightProfileRepository(database, { 99L }, Dispatchers.Unconfined).getOrCreateProfile())
    }
}
