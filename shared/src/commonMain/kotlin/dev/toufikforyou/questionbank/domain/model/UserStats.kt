package dev.toufikforyou.questionbank.domain.model

data class UserStats(
    val totalAnswered: Int,
    val totalCorrect: Int,
    val accuracyPercent: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val bookmarkedCount: Int,
    val categoryStats: List<CategoryStats>,
) {
    companion object {
        val EMPTY = UserStats(
            totalAnswered = 0,
            totalCorrect = 0,
            accuracyPercent = 0,
            currentStreak = 0,
            bestStreak = 0,
            bookmarkedCount = 0,
            categoryStats = emptyList(),
        )
    }
}

data class CategoryStats(
    val categoryId: String,
    val title: String,
    val answered: Int,
    val correct: Int,
    val accuracyPercent: Int,
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val createdAtMillis: Long,
)
