package dev.toufikforyou.questionbank.domain.model

sealed class DomainError(message: String) : Exception(message) {
    class EmptyQuestionBank : DomainError("এখানে অনুশীলনের জন্য কোনো প্রশ্ন নেই।")

    class InvalidQuizSize(val requested: Int) :
        DomainError("একটি কুইজে ১ থেকে ${QuizRequest.MAX_LIMIT.toBanglaDigits()}টি প্রশ্ন থাকতে পারে।")

    class PaperNotFound(val key: PaperKey, val note: String?) :
        DomainError(note ?: "এই বোর্ড ও সালের প্রশ্ন এখনো যুক্ত হয়নি।")

    class SubjectNotFound(val subjectId: String) : DomainError("বিষয়টি খুঁজে পাওয়া যায়নি।")

    class ChapterNotFound(val chapterId: String) : DomainError("অধ্যায়টি খুঁজে পাওয়া যায়নি।")

    class QueryTooShort(val minLength: Int) : DomainError("অন্তত ${minLength.toBanglaDigits()} অক্ষর লিখুন।")

    class QuestionNotFound(val questionId: String) : DomainError("প্রশ্নটি খুঁজে পাওয়া যায়নি।")

    class InvalidOption(val questionId: String, val optionId: String) :
        DomainError("উত্তরটি এই প্রশ্নের নয়।")
}
