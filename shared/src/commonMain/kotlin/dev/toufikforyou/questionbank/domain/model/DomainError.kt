package dev.toufikforyou.questionbank.domain.model

sealed class DomainError(message: String) : Exception(message) {
    class EmptyQuestionBank(val categoryId: String?) :
        DomainError(if (categoryId == null) "The question bank is empty." else "No questions available in this category yet.")

    class InvalidQuizSize(val requested: Int) :
        DomainError("A quiz must contain between 1 and ${QuizRequest.MAX_LIMIT} questions (requested $requested).")

    class QuestionNotFound(val questionId: String) :
        DomainError("Question '$questionId' could not be found.")

    class InvalidOption(val questionId: String, val optionId: String) :
        DomainError("Option '$optionId' does not belong to question '$questionId'.")
}
