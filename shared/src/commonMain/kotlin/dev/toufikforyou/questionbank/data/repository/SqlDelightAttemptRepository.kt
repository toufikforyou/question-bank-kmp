package dev.toufikforyou.questionbank.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.toufikforyou.questionbank.data.db.QuestionBankDatabase
import dev.toufikforyou.questionbank.data.local.toDomain
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.repository.AttemptRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SqlDelightAttemptRepository(
    private val database: QuestionBankDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) : AttemptRepository {

    private val queries get() = database.questionBankQueries

    override suspend fun record(attempt: AnswerRecord) {
        withContext(ioDispatcher) {
            queries.insertAttempt(
                question_id = attempt.questionId,
                subject_id = attempt.subjectId,
                selected_option_id = attempt.selectedOptionId,
                is_correct = if (attempt.isCorrect) 1L else 0L,
                answered_at = attempt.answeredAtMillis,
            )
        }
    }

    override fun observeAttempts(): Flow<List<AnswerRecord>> =
        queries.selectAttempts()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { it.toDomain() } }
}
