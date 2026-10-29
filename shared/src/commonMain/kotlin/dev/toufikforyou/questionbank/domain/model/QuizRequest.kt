package dev.toufikforyou.questionbank.domain.model

/** What to practise. Exactly one source is used: paper, chapter, subject or bookmarks. */
data class QuizRequest(
    val subjectId: String? = null,
    val chapterId: String? = null,
    val paper: PaperKey? = null,
    val bookmarkedOnly: Boolean = false,
    val limit: Int = DEFAULT_LIMIT,
    val shuffle: Boolean = true,
    /** Exam mode: one minute per MCQ, like the real board exam. */
    val timed: Boolean = false,
) {
    companion object {
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
        const val SECONDS_PER_MCQ = 60

        fun forSubject(subjectId: String): QuizRequest = QuizRequest(subjectId = subjectId)

        fun forChapter(chapterId: String): QuizRequest = QuizRequest(chapterId = chapterId)

        /** The paper's MCQs in their original order, timed like the real exam. */
        fun forPaper(paper: PaperKey): QuizRequest =
            QuizRequest(paper = paper, limit = MAX_LIMIT, shuffle = false, timed = true)

        fun forBookmarks(): QuizRequest = QuizRequest(bookmarkedOnly = true)
    }
}
