package dev.toufikforyou.questionbank.domain.model

enum class QuestionType { MCQ, CQ }

data class AnswerOption(
    val id: String,
    val text: String,
)

/** One part (ক/খ/গ/ঘ) of a creative question with its marks and a model answer. */
data class CreativePart(
    val label: String,
    val text: String,
    val marks: Int,
    val answer: String,
)

/** Where a question was set: board, year and its number in that paper. */
data class Appearance(
    val boardId: String,
    val year: Int,
    val number: Int,
)

/** Identifies one board paper, e.g. SSC Physics, Dhaka board, 2023. */
data class PaperKey(
    val subjectId: String,
    val boardId: String,
    val year: Int,
)

data class Question(
    val id: String,
    val subjectId: String,
    val chapterId: String,
    val type: QuestionType,
    /** MCQ question text, or the stimulus (উদ্দীপক) of a creative question. */
    val prompt: String,
    val options: List<AnswerOption> = emptyList(),
    val correctOptionId: String? = null,
    val explanation: String? = null,
    val parts: List<CreativePart> = emptyList(),
    val appearances: List<Appearance> = emptyList(),
    val isBookmarked: Boolean = false,
) {
    init {
        when (type) {
            QuestionType.MCQ -> {
                require(options.size >= 2) { "MCQ $id must have at least two options" }
                require(options.map { it.id }.toSet().size == options.size) { "MCQ $id has duplicate option ids" }
                require(options.any { it.id == correctOptionId }) { "MCQ $id has no option '$correctOptionId'" }
            }
            QuestionType.CQ -> require(parts.isNotEmpty()) { "CQ $id must have parts" }
        }
    }

    val isMcq: Boolean get() = type == QuestionType.MCQ
    val correctOption: AnswerOption? get() = options.firstOrNull { it.id == correctOptionId }
    val totalMarks: Int get() = parts.sumOf { it.marks }
    val repeatCount: Int get() = appearances.size
    val isRepeated: Boolean get() = appearances.size > 1

    fun hasOption(optionId: String): Boolean = options.any { it.id == optionId }
}

/** A full board paper with its questions in paper order. */
data class QuestionPaper(
    val key: PaperKey,
    val subject: Subject,
    val board: Board,
    val questions: List<Question>,
) {
    val mcqs: List<Question> get() = questions.filter { it.isMcq }
    val creativeQuestions: List<Question> get() = questions.filter { !it.isMcq }
}

/** Availability of one paper for the subject screen. */
data class PaperSummary(
    val key: PaperKey,
    val mcqCount: Int,
    val cqCount: Int,
)
