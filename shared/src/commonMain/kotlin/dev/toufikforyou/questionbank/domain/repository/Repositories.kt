package dev.toufikforyou.questionbank.domain.repository

import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Catalog
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.PaperSummary
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts. Implementations must be safe to call from any thread/coroutine.
 */
interface QuestionRepository {
    /** Imports bundled seed data if the local database is missing or outdated. Idempotent. */
    suspend fun ensureSeeded()

    suspend fun getCatalog(): Catalog

    /** Every board/year paper on record for a subject. */
    suspend fun getPaperSummaries(subjectId: String): List<PaperSummary>

    /** Questions of one board paper, in paper order. Empty if the paper is not on record. */
    suspend fun getPaperQuestions(key: PaperKey): List<Question>

    /** Most frequently set questions first. */
    suspend fun getChapterQuestions(chapterId: String): List<Question>

    suspend fun getSubjectQuestions(subjectId: String): List<Question>

    suspend fun search(query: String, exam: ExamType, limit: Int): List<Question>

    suspend fun getQuestion(id: String): Question?

    /** Bookmarked questions, most recently bookmarked first. */
    fun observeBookmarkedQuestions(): Flow<List<Question>>

    suspend fun setBookmarked(questionId: String, bookmarked: Boolean)
}

interface AttemptRepository {
    suspend fun record(attempt: AnswerRecord)

    fun observeAttempts(): Flow<List<AnswerRecord>>
}

interface ProfileRepository {
    /** Returns the local profile, creating it on first launch. No login is ever required. */
    suspend fun getOrCreateProfile(): UserProfile
}
