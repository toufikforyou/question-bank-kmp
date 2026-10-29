package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.DomainError
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository

class SearchQuestionsUseCase(
    private val repository: QuestionRepository,
) {
    suspend operator fun invoke(query: String, exam: ExamType, limit: Int = DEFAULT_LIMIT): Result<List<Question>> = resultOf {
        val normalized = normalize(query)
        if (normalized.length < MIN_QUERY_LENGTH) throw DomainError.QueryTooShort(MIN_QUERY_LENGTH)
        repository.search(normalized, exam, limit.coerceIn(1, MAX_LIMIT))
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val DEFAULT_LIMIT = 50
        const val MAX_LIMIT = 200

        /** Lower-cases and collapses whitespace so searches match the stored search text. */
        fun normalize(text: String): String = text.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    }
}
