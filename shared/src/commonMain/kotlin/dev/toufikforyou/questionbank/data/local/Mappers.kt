package dev.toufikforyou.questionbank.data.local

import dev.toufikforyou.questionbank.data.db.AttemptEntity
import dev.toufikforyou.questionbank.data.db.QuestionEntity
import dev.toufikforyou.questionbank.data.seed.OptionDto
import dev.toufikforyou.questionbank.data.seed.PartDto
import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.data.seed.SeedQuestion
import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Appearance
import dev.toufikforyou.questionbank.domain.model.CreativePart
import dev.toufikforyou.questionbank.domain.model.Question
import dev.toufikforyou.questionbank.domain.model.QuestionType
import dev.toufikforyou.questionbank.domain.usecase.SearchQuestionsUseCase
import kotlinx.serialization.builtins.ListSerializer

private val optionListSerializer = ListSerializer(OptionDto.serializer())
private val partListSerializer = ListSerializer(PartDto.serializer())

internal fun encodeOptions(options: List<OptionDto>): String = SeedParser.json.encodeToString(optionListSerializer, options)

internal fun encodeParts(parts: List<PartDto>): String = SeedParser.json.encodeToString(partListSerializer, parts)

/** Text the search query is matched against: question, options, stimulus, parts and answers. */
internal fun SeedQuestion.searchText(): String = SearchQuestionsUseCase.normalize(
    listOfNotNull(prompt, stem, explanation).plus(options.map { it.text }).plus(parts.flatMap { listOf(it.text, it.answer) })
        .joinToString(" "),
)

internal fun QuestionEntity.toDomain(appearances: List<Appearance>, isBookmarked: Boolean = false): Question = Question(
    id = id,
    subjectId = subject_id,
    chapterId = chapter_id,
    type = QuestionType.entries.firstOrNull { it.name == type } ?: QuestionType.MCQ,
    prompt = prompt,
    options = SeedParser.json.decodeFromString(optionListSerializer, options_json).map { AnswerOption(it.id, it.text) },
    correctOptionId = correct_option_id,
    explanation = explanation,
    parts = SeedParser.json.decodeFromString(partListSerializer, parts_json).map { CreativePart(it.label, it.text, it.marks, it.answer) },
    appearances = appearances,
    isBookmarked = isBookmarked,
)

internal fun AttemptEntity.toDomain(): AnswerRecord = AnswerRecord(
    questionId = question_id,
    subjectId = subject_id,
    selectedOptionId = selected_option_id,
    isCorrect = is_correct != 0L,
    answeredAtMillis = answered_at,
)
