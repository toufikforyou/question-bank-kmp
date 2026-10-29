package dev.toufikforyou.questionbank.domain.model

enum class ExamType { SSC, HSC }

enum class StudyGroup { COMMON, SCIENCE, BUSINESS, HUMANITIES, OPTIONAL }

data class Exam(
    val type: ExamType,
    val name: String,
    val fullName: String,
)

data class Board(
    val id: String,
    val name: String,
    val nameEn: String,
)

data class Chapter(
    val id: String,
    val subjectId: String,
    val number: Int,
    val name: String,
    val mcqCount: Int,
    val cqCount: Int,
) {
    val questionCount: Int get() = mcqCount + cqCount
}

data class Subject(
    val id: String,
    val exam: ExamType,
    val name: String,
    val nameEn: String,
    val code: String,
    val group: StudyGroup,
    val chapters: List<Chapter>,
) {
    val questionCount: Int get() = chapters.sumOf { it.questionCount }
}

/** Explains why a year has no (or only some) papers, e.g. an exam that was not held. */
data class YearNote(
    val exam: ExamType,
    val year: Int,
    val text: String,
)

/** Everything needed to navigate the bank: exams, boards, subjects and the years on record. */
data class Catalog(
    val exams: List<Exam>,
    val boards: List<Board>,
    val subjects: List<Subject>,
    val notes: List<YearNote>,
    val yearsByExam: Map<ExamType, List<Int>>,
    val isSample: Boolean,
) {
    private val boardsById = boards.associateBy { it.id }
    private val subjectsById = subjects.associateBy { it.id }
    private val chaptersById = subjects.flatMap { it.chapters }.associateBy { it.id }

    fun board(id: String): Board? = boardsById[id]
    fun subject(id: String): Subject? = subjectsById[id]
    fun chapter(id: String): Chapter? = chaptersById[id]
    fun exam(type: ExamType): Exam? = exams.firstOrNull { it.type == type }
    fun subjectsFor(exam: ExamType): List<Subject> = subjects.filter { it.exam == exam }
    fun yearsFor(exam: ExamType): List<Int> = yearsByExam[exam].orEmpty()
    fun noteFor(exam: ExamType, year: Int): YearNote? = notes.firstOrNull { it.exam == exam && it.year == year }

    companion object {
        val EMPTY = Catalog(emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), isSample = false)
    }
}
