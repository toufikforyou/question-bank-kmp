package dev.toufikforyou.questionbank.data.local

import dev.toufikforyou.questionbank.data.db.AttemptEntity
import dev.toufikforyou.questionbank.data.db.QuestionEntity
import dev.toufikforyou.questionbank.data.seed.OptionDto
import dev.toufikforyou.questionbank.data.seed.SeedParser
import dev.toufikforyou.questionbank.domain.model.AnswerOption
import dev.toufikforyou.questionbank.domain.model.AnswerRecord
import dev.toufikforyou.questionbank.domain.model.Difficulty
import dev.toufikforyou.questionbank.domain.model.Question
import kotlinx.serialization.builtins.ListSerializer

private val optionListSerializer = ListSerializer(OptionDto.serializer())

internal fun encodeOptions(options: List<OptionDto>): String =
    SeedParser.json.encodeToString(optionListSerializer, options)

internal fun QuestionEntity.toDomain(isBookmarked: Boolean = false): Question = Question(
    id = id,
    categoryId = category_id,
    prompt = prompt,
    options = SeedParser.json.decodeFromString(optionListSerializer, options_json).map { AnswerOption(it.id, it.text) },
    correctOptionId = correct_option_id,
    explanation = explanation,
    difficulty = difficultyOf(difficulty),
    isBookmarked = isBookmarked,
)

internal fun difficultyOf(raw: String): Difficulty =
    Difficulty.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: Difficulty.MEDIUM

internal fun AttemptEntity.toDomain(): AnswerRecord = AnswerRecord(
    questionId = question_id,
    categoryId = category_id,
    selectedOptionId = selected_option_id,
    isCorrect = is_correct != 0L,
    answeredAtMillis = answered_at,
)
