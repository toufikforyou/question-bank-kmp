package dev.toufikforyou.questionbank.domain.model

data class Category(
    val id: String,
    val title: String,
    val description: String,
    val questionCount: Int,
)
