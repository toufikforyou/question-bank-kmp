package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.Category
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
    private val repository: QuestionRepository,
) {
    operator fun invoke(): Flow<List<Category>> = repository.observeCategories()
}
