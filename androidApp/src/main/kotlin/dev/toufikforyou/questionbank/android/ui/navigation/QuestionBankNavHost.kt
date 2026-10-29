package dev.toufikforyou.questionbank.android.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.toufikforyou.questionbank.android.ui.bookmarks.BookmarksScreen
import dev.toufikforyou.questionbank.android.ui.components.rememberStore
import dev.toufikforyou.questionbank.android.ui.home.HomeScreen
import dev.toufikforyou.questionbank.android.ui.quiz.QuizScreen
import dev.toufikforyou.questionbank.android.ui.stats.StatsScreen
import dev.toufikforyou.questionbank.di.AppContainer

object Routes {
    const val HOME = "home"
    const val BOOKMARKS = "bookmarks"
    const val STATS = "stats"
    const val QUIZ = "quiz?categoryId={categoryId}&title={title}&bookmarked={bookmarked}"

    fun quiz(categoryId: String?, title: String, bookmarkedOnly: Boolean = false): String =
        "quiz?categoryId=${categoryId.orEmpty()}&title=${Uri.encode(title)}&bookmarked=$bookmarkedOnly"
}

private const val TRANSITION_MS = 320

@Composable
fun QuestionBankNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MS)) + fadeIn(tween(TRANSITION_MS)) },
        exitTransition = { fadeOut(tween(TRANSITION_MS / 2)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MS)) + fadeOut(tween(TRANSITION_MS)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                store = rememberStore { container.homeStore() },
                onStartQuiz = { category ->
                    navController.navigate(Routes.quiz(category?.id, category?.title ?: "Quick quiz"))
                },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(
            route = Routes.QUIZ,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("title") { type = NavType.StringType; defaultValue = "Quiz" },
                navArgument("bookmarked") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            QuizScreen(
                store = rememberStore { container.quizStore() },
                title = args.getString("title") ?: "Quiz",
                categoryId = args.getString("categoryId")?.takeIf { it.isNotBlank() },
                bookmarkedOnly = args.getBoolean("bookmarked"),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(
                store = rememberStore { container.bookmarksStore() },
                onBack = { navController.popBackStack() },
                onPractice = { navController.navigate(Routes.quiz(null, "Bookmarked", bookmarkedOnly = true)) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                store = rememberStore { container.statsStore() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
