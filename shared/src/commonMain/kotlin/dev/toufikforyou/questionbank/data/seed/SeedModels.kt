package dev.toufikforyou.questionbank.data.seed

import kotlinx.serialization.Serializable

@Serializable
data class SeedBundle(
    val version: Int,
    val categories: List<SeedCategory>,
    val questions: List<SeedQuestion>,
)

@Serializable
data class SeedCategory(
    val id: String,
    val title: String,
    val description: String,
)

@Serializable
data class SeedQuestion(
    val id: String,
    val categoryId: String,
    val prompt: String,
    val options: List<OptionDto>,
    val correctOptionId: String,
    val explanation: String? = null,
    val difficulty: String = "MEDIUM",
)

@Serializable
data class OptionDto(
    val id: String,
    val text: String,
)
