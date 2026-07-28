@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.lexumi.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lexumi.app.presentation.addcontent.*
import com.lexumi.app.presentation.audio.AudioListScreen
import com.lexumi.app.presentation.audio.AudioPlayerScreen
import com.lexumi.app.presentation.home.HomeScreen
import com.lexumi.app.presentation.imagequiz.ImageQuizScreen
import com.lexumi.app.presentation.language.AddLanguageScreen
import com.lexumi.app.presentation.language.LanguageMenuScreen
import com.lexumi.app.presentation.learnwords.LearnWordsScreen
import com.lexumi.app.presentation.learnwords.ReviewWordsScreen
import com.lexumi.app.presentation.rules.RulesListScreen
import com.lexumi.app.presentation.section.AddSectionScreen
import com.lexumi.app.presentation.section.SectionsScreen
import com.lexumi.app.presentation.sentences.SentencePracticeScreen
import com.lexumi.app.presentation.settings.SettingsScreen
import com.lexumi.app.presentation.splash.SplashScreen
import com.lexumi.app.presentation.stories.StoriesListScreen
import com.lexumi.app.presentation.stories.StoryReaderScreen
import com.lexumi.app.presentation.topic.AddTopicScreen
import com.lexumi.app.presentation.topic.TopicsScreen
import com.lexumi.app.presentation.topicaction.TopicActionScreen
import com.lexumi.app.presentation.video.VideoListScreen
import com.lexumi.app.presentation.video.VideoPlayerScreen
import com.lexumi.app.presentation.welcome.WelcomeScreen

@Composable
fun LexumiNavGraph() {
    val navController = rememberNavController()
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateWelcome = {
                    navController.navigate(Screen.Welcome.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNavigateLanguageMenu = {
                    navController.navigate(Screen.LanguageMenu.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNavigateHome = { languageId ->
                    navController.navigate(Screen.Home.build(languageId)) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(onDone = {
                navController.navigate(Screen.LanguageMenu.route) { popUpTo(Screen.Welcome.route) { inclusive = true } }
            })
        }

        composable(Screen.LanguageMenu.route) {
            LanguageMenuScreen(
                onAddLanguage = { navController.navigate(Screen.AddLanguage.route) },
                onLanguageChosen = { languageId -> navController.navigate(Screen.Home.build(languageId)) },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.AddLanguage.route) {
            AddLanguageScreen(
                onCreated = { languageId ->
                    navController.navigate(Screen.Home.build(languageId)) { popUpTo(Screen.LanguageMenu.route) { inclusive = true } }
                },
                onBack = back,
            )
        }

        composable(Screen.Home.route, arguments = listOf(navArgument("languageId") { type = NavType.LongType })) { entry ->
            val languageId = entry.arguments!!.getLong("languageId")
            HomeScreen(
                onLearn = { navController.navigate(Screen.Sections.build(languageId)) },
                onAddSection = { navController.navigate(Screen.AddSection.build(languageId)) },
                onRepeatWords = { navController.navigate(Screen.ReviewWords.route) },
                onContinueLast = { topicId, route -> navController.navigate("$route/$topicId") },
                onChooseOtherSection = { navController.navigate(Screen.Sections.build(languageId)) },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Sections.route, arguments = listOf(navArgument("languageId") { type = NavType.LongType })) { entry ->
            val languageId = entry.arguments!!.getLong("languageId")
            SectionsScreen(
                onSectionChosen = { sectionId -> navController.navigate(Screen.Topics.build(sectionId)) },
                onAddSection = { navController.navigate(Screen.AddSection.build(languageId)) },
                onBack = back,
            )
        }

        composable(Screen.AddSection.route, arguments = listOf(navArgument("languageId") { type = NavType.LongType })) { entry ->
            val languageId = entry.arguments!!.getLong("languageId")
            AddSectionScreen(
                onCreated = { sectionId ->
                    navController.navigate(Screen.Topics.build(sectionId)) { popUpTo(Screen.Home.build(languageId)) }
                },
                onBack = back,
            )
        }

        composable(Screen.Topics.route, arguments = listOf(navArgument("sectionId") { type = NavType.LongType })) { entry ->
            val sectionId = entry.arguments!!.getLong("sectionId")
            TopicsScreen(
                onTopicChosen = { topicId -> navController.navigate(Screen.TopicAction.build(topicId)) },
                onAddTopic = { navController.navigate(Screen.AddTopic.build(sectionId)) },
                onBack = back,
            )
        }

        composable(Screen.AddTopic.route, arguments = listOf(navArgument("sectionId") { type = NavType.LongType })) {
            AddTopicScreen(
                onCreated = { topicId -> navController.navigate(Screen.AddContentMenu.build(topicId)) },
                onBack = back,
            )
        }

        composable(Screen.AddContentMenu.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) { entry ->
            val topicId = entry.arguments!!.getLong("topicId")
            AddContentMenuScreen(
                onAddRule = { navController.navigate(Screen.AddRule.build(topicId)) },
                onAddWord = { navController.navigate(Screen.AddWord.build(topicId)) },
                onBulkAddWords = { navController.navigate(Screen.BulkAddWords.build(topicId)) },
                onAddImage = { navController.navigate(Screen.AddImage.build(topicId)) },
                onAddVideo = { navController.navigate(Screen.AddVideo.build(topicId)) },
                onAddAudioDialog = { navController.navigate(Screen.AddAudioDialog.build(topicId)) },
                onAddSentence = { navController.navigate(Screen.AddSentence.build(topicId)) },
                onBulkAddSentences = { navController.navigate(Screen.BulkAddSentences.build(topicId)) },
                onAddStory = { navController.navigate(Screen.AddStory.build(topicId)) },
                onDone = {
                    navController.navigate(Screen.TopicAction.build(topicId)) {
                        popUpTo(Screen.AddContentMenu.build(topicId)) { inclusive = true }
                    }
                },
                onBack = back,
            )
        }

        composable(Screen.AddRule.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddRuleScreen(onCreated = back, onBack = back)
        }
        composable(Screen.AddWord.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddWordScreen(onCreated = back, onBack = back)
        }
        composable(Screen.BulkAddWords.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            BulkAddWordsScreen(onDone = back, onBack = back)
        }
        composable(Screen.AddImage.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddImageContentScreen(onCreated = back, onBack = back)
        }
        composable(Screen.AddVideo.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddVideoScreen(onCreated = back, onBack = back)
        }
        composable(Screen.AddAudioDialog.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddAudioDialogScreen(onCreated = back, onBack = back)
        }
        composable(Screen.AddSentence.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddSentenceScreen(onCreated = back, onBack = back)
        }
        composable(Screen.BulkAddSentences.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            BulkAddSentencesScreen(onDone = back, onBack = back)
        }
        composable(Screen.AddStory.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AddStoryScreen(onCreated = back, onBack = back)
        }

        composable(Screen.TopicAction.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) { entry ->
            val topicId = entry.arguments!!.getLong("topicId")
            TopicActionScreen(
                onLearnRules = { navController.navigate(Screen.RulesList.build(topicId)) },
                onLearnWords = { navController.navigate(Screen.LearnWords.build(topicId)) },
                onWatchVideo = { navController.navigate(Screen.VideoList.build(topicId)) },
                onListenDialogs = { navController.navigate(Screen.AudioList.build(topicId)) },
                onReadStories = { navController.navigate(Screen.StoriesList.build(topicId)) },
                onImageTests = { navController.navigate(Screen.ImageQuiz.build(topicId)) },
                onSentences = { navController.navigate(Screen.SentencePractice.build(topicId)) },
                onAddContent = { navController.navigate(Screen.AddContentMenu.build(topicId)) },
                onBack = back,
            )
        }

        composable(Screen.RulesList.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            RulesListScreen(onBack = back)
        }

        composable(Screen.LearnWords.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            LearnWordsScreen(onSessionDone = back, onBack = back)
        }

        composable(Screen.ReviewWords.route) {
            ReviewWordsScreen(onDone = back, onBack = back)
        }

        composable(Screen.ImageQuiz.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            ImageQuizScreen(onDone = back, onBack = back)
        }

        composable(Screen.VideoList.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            VideoListScreen(onVideoClick = { videoId -> navController.navigate(Screen.VideoPlayer.build(videoId)) }, onBack = back)
        }
        composable(Screen.VideoPlayer.route, arguments = listOf(navArgument("videoId") { type = NavType.LongType })) {
            VideoPlayerScreen(onBack = back)
        }

        composable(Screen.AudioList.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            AudioListScreen(onDialogClick = { dialogId -> navController.navigate(Screen.AudioPlayer.build(dialogId)) }, onBack = back)
        }
        composable(Screen.AudioPlayer.route, arguments = listOf(navArgument("dialogId") { type = NavType.LongType })) {
            AudioPlayerScreen(onBack = back)
        }

        composable(Screen.SentencePractice.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            SentencePracticeScreen(onDone = back, onBack = back)
        }

        composable(Screen.StoriesList.route, arguments = listOf(navArgument("topicId") { type = NavType.LongType })) {
            StoriesListScreen(onStoryClick = { storyId -> navController.navigate(Screen.StoryReader.build(storyId)) }, onBack = back)
        }
        composable(Screen.StoryReader.route, arguments = listOf(navArgument("storyId") { type = NavType.LongType })) {
            StoryReaderScreen(onBack = back)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = back,
                onLoggedOut = { navController.navigate(Screen.Welcome.route) { popUpTo(0) } },
                onDataCleared = { navController.navigate(Screen.Welcome.route) { popUpTo(0) } },
            )
        }
    }
}
