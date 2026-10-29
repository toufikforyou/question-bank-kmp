package dev.toufikforyou.questionbank.domain.model

enum class Difficulty { EASY, MEDIUM, HARD }

data class AnswerOption(
    val id: String,
    val text: String,
)

data class Question(
    val id: String,
    val categoryId: String,
    val prompt: String,
    val options: List<AnswerOption>,
    val correctOptionId: String,
    val explanation: String?,
    val difficulty: Difficulty,
    val isBookmarked: Boolean = false,
) {
    init {
        require(options.size >= 2) { "Question $id must have at least two options" }
        require(options.map { it.id }.toSet().size == options.size) { "Question $id has duplicate option ids" }
        require(options.any { it.id == correctOptionId }) { "Question $id has no option '$correctOptionId'" }
    }

    val correctOption: AnswerOption
        get() = options.first { it.id == correctOptionId }

    fun hasOption(optionId: String): Boolean = options.any { it.id == optionId }
}
