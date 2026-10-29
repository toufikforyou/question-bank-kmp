package dev.toufikforyou.questionbank.domain.repository

import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts. Implementations must be safe to call from any thread/coroutine.
 */
interface QuestionRepository {
    /** Imports bundled seed data if the local database is missing or outdated. Idempotent. */
    suspend fun ensureSeeded()

    fun observeCategories(): Flow<List<Category>>

    suspend fun getQuestions(categoryId: String? = null): List<Question>

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
