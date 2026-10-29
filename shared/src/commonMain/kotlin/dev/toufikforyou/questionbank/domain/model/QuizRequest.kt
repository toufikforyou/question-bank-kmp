package dev.toufikforyou.questionbank.domain.model

data class QuizRequest(
    val categoryId: String? = null,
    val limit: Int = DEFAULT_LIMIT,
    val bookmarkedOnly: Boolean = false,
    val shuffle: Boolean = true,
) {
    companion object {
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
    }
}
