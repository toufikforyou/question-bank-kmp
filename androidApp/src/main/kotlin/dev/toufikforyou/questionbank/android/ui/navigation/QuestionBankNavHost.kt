package dev.toufikforyou.questionbank.android.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.toufikforyou.questionbank.android.ui.components.rememberStore
import dev.toufikforyou.questionbank.android.ui.home.HomeScreen
import dev.toufikforyou.questionbank.android.ui.questions.QuestionListScreen
import dev.toufikforyou.questionbank.android.ui.quiz.QuizScreen
import dev.toufikforyou.questionbank.android.ui.stats.StatsScreen
import dev.toufikforyou.questionbank.android.ui.subject.SubjectScreen
import dev.toufikforyou.questionbank.di.AppContainer
import dev.toufikforyou.questionbank.domain.model.ExamType
import dev.toufikforyou.questionbank.domain.model.PaperKey
import dev.toufikforyou.questionbank.domain.model.QuizRequest

object Routes {
    const val HOME = "home"
    const val SUBJECT = "subject/{subjectId}"
    const val PAPER = "paper/{subjectId}/{boardId}/{year}"
    const val CHAPTER = "chapter/{chapterId}"
    const val SEARCH = "search/{exam}"
    const val BOOKMARKS = "bookmarks"
    const val STATS = "stats"
    const val QUIZ = "quiz?mode={mode}&id={id}&board={board}&year={year}"

    fun subject(subjectId: String) = "subject/${Uri.encode(subjectId)}"
    fun paper(key: PaperKey) = "paper/${Uri.encode(key.subjectId)}/${Uri.encode(key.boardId)}/${key.year}"
    fun chapter(chapterId: String) = "chapter/${Uri.encode(chapterId)}"
    fun search(exam: ExamType) = "search/${exam.name}"

    /** Encodes a practice request into the quiz route. */
    fun quiz(request: QuizRequest): String {
        val paper = request.paper
        return when {
            paper != null -> "quiz?mode=paper&id=${Uri.encode(paper.subjectId)}&board=${Uri.encode(paper.boardId)}&year=${paper.year}"
            request.bookmarkedOnly -> "quiz?mode=bookmarks"
            request.chapterId != null -> "quiz?mode=chapter&id=${Uri.encode(request.chapterId)}"
            else -> "quiz?mode=subject&id=${Uri.encode(request.subjectId.orEmpty())}"
        }
    }

    fun quizRequest(mode: String?, id: String?, board: String?, year: Int): QuizRequest = when (mode) {
        "paper" -> QuizRequest.forPaper(PaperKey(id.orEmpty(), board.orEmpty(), year))
        "bookmarks" -> QuizRequest.forBookmarks()
        "chapter" -> QuizRequest.forChapter(id.orEmpty())
        else -> QuizRequest.forSubject(id.orEmpty())
    }
}

private const val TRANSITION_MS = 320

@Composable
fun QuestionBankNavHost(container: AppContainer, navController: NavHostController = rememberNavController()) {
    val back: () -> Unit = { navController.popBackStack() }
    val practise: (QuizRequest) -> Unit = { navController.navigate(Routes.quiz(it)) }

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
                onOpenSubject = { navController.navigate(Routes.subject(it)) },
                onOpenPaper = { navController.navigate(Routes.paper(it)) },
                onSearch = { navController.navigate(Routes.search(it)) },
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(Routes.SUBJECT, arguments = listOf(navArgument("subjectId") { type = NavType.StringType })) { entry ->
            val subjectId = entry.arguments?.getString("subjectId").orEmpty()
            SubjectScreen(
                store = rememberStore { container.subjectStore(subjectId) },
                onBack = back,
                onOpenPaper = { navController.navigate(Routes.paper(it)) },
                onOpenChapter = { navController.navigate(Routes.chapter(it)) },
                onPractise = practise,
            )
        }
        composable(
            Routes.PAPER,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("boardId") { type = NavType.StringType },
                navArgument("year") { type = NavType.IntType },
            ),
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            val key = PaperKey(args.getString("subjectId").orEmpty(), args.getString("boardId").orEmpty(), args.getInt("year"))
            QuestionListScreen(rememberStore { container.paperStore(key) }, onBack = back, onPractise = practise)
        }
        composable(Routes.CHAPTER, arguments = listOf(navArgument("chapterId") { type = NavType.StringType })) { entry ->
            val chapterId = entry.arguments?.getString("chapterId").orEmpty()
            QuestionListScreen(rememberStore { container.chapterStore(chapterId) }, onBack = back, onPractise = practise)
        }
        composable(Routes.SEARCH, arguments = listOf(navArgument("exam") { type = NavType.StringType })) { entry ->
            val exam = ExamType.entries.firstOrNull { it.name == entry.arguments?.getString("exam") } ?: ExamType.SSC
            QuestionListScreen(rememberStore { container.searchStore(exam) }, onBack = back, onPractise = practise)
        }
        composable(Routes.BOOKMARKS) {
            QuestionListScreen(rememberStore { container.bookmarksStore() }, onBack = back, onPractise = practise)
        }
        composable(Routes.STATS) {
            StatsScreen(rememberStore { container.statsStore() }, onBack = back)
        }
        composable(
            Routes.QUIZ,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType; defaultValue = "subject" },
                navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("board") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("year") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            val args = requireNotNull(entry.arguments)
            val request = Routes.quizRequest(args.getString("mode"), args.getString("id"), args.getString("board"), args.getInt("year"))
            QuizScreen(store = rememberStore { container.quizStore() }, request = request, onBack = back)
        }
    }
}
