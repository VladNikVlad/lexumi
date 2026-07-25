package com.lexumi.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")

    // Language flow (points 2, 3, 14)
    data object LanguageMenu : Screen("language_menu")
    data object AddLanguage : Screen("add_language")

    // Home per selected language (point 14)
    data object Home : Screen("home/{languageId}") {
        fun build(languageId: Long) = "home/$languageId"
    }

    // Sections / Topics (points 4, 5, 15, 16)
    data object Sections : Screen("sections/{languageId}") {
        fun build(languageId: Long) = "sections/$languageId"
    }
    data object AddSection : Screen("add_section/{languageId}") {
        fun build(languageId: Long) = "add_section/$languageId"
    }
    data object Topics : Screen("topics/{sectionId}") {
        fun build(sectionId: Long) = "topics/$sectionId"
    }
    data object AddTopic : Screen("add_topic/{sectionId}") {
        fun build(sectionId: Long) = "add_topic/$sectionId"
    }

    // Add-to-topic content menu (point 6) and its forms (points 7-13)
    data object AddContentMenu : Screen("add_content_menu/{topicId}") {
        fun build(topicId: Long) = "add_content_menu/$topicId"
    }
    data object AddRule : Screen("add_rule/{topicId}") {
        fun build(topicId: Long) = "add_rule/$topicId"
    }
    data object AddWord : Screen("add_word/{topicId}") {
        fun build(topicId: Long) = "add_word/$topicId"
    }
    data object BulkAddWords : Screen("bulk_add_words/{topicId}") {
        fun build(topicId: Long) = "bulk_add_words/$topicId"
    }
    data object AddImage : Screen("add_image/{topicId}") {
        fun build(topicId: Long) = "add_image/$topicId"
    }
    data object AddVideo : Screen("add_video/{topicId}") {
        fun build(topicId: Long) = "add_video/$topicId"
    }
    data object AddAudioDialog : Screen("add_audio_dialog/{topicId}") {
        fun build(topicId: Long) = "add_audio_dialog/$topicId"
    }
    data object AddSentence : Screen("add_sentence/{topicId}") {
        fun build(topicId: Long) = "add_sentence/$topicId"
    }
    data object BulkAddSentences : Screen("bulk_add_sentences/{topicId}") {
        fun build(topicId: Long) = "bulk_add_sentences/$topicId"
    }
    data object AddStory : Screen("add_story/{topicId}") {
        fun build(topicId: Long) = "add_story/$topicId"
    }

    // Topic study actions (point 17)
    data object TopicAction : Screen("topic_action/{topicId}") {
        fun build(topicId: Long) = "topic_action/$topicId"
    }

    // Rules (points 18, 19)
    data object RulesList : Screen("rules_list/{topicId}") {
        fun build(topicId: Long) = "rules_list/$topicId"
    }
    data object RuleDetail : Screen("rule_detail/{ruleId}") {
        fun build(ruleId: Long) = "rule_detail/$ruleId"
    }

    // Learn words / review (points 20, 26)
    data object LearnWords : Screen("learn_words/{topicId}") {
        fun build(topicId: Long) = "learn_words/$topicId"
    }
    data object ReviewWords : Screen("review_words")

    // Image quiz (point 21)
    data object ImageQuiz : Screen("image_quiz/{topicId}") {
        fun build(topicId: Long) = "image_quiz/$topicId"
    }

    // Video (point 22)
    data object VideoList : Screen("video_list/{topicId}") {
        fun build(topicId: Long) = "video_list/$topicId"
    }
    data object VideoPlayer : Screen("video_player/{videoId}") {
        fun build(videoId: Long) = "video_player/$videoId"
    }

    // Audio dialogs (point 10, 23)
    data object AudioList : Screen("audio_list/{topicId}") {
        fun build(topicId: Long) = "audio_list/$topicId"
    }
    data object AudioPlayer : Screen("audio_player/{dialogId}") {
        fun build(dialogId: Long) = "audio_player/$dialogId"
    }

    // Sentences (point 25)
    data object SentencePractice : Screen("sentence_practice/{topicId}") {
        fun build(topicId: Long) = "sentence_practice/$topicId"
    }

    // Stories (point 24)
    data object StoriesList : Screen("stories_list/{topicId}") {
        fun build(topicId: Long) = "stories_list/$topicId"
    }
    data object StoryReader : Screen("story_reader/{storyId}") {
        fun build(storyId: Long) = "story_reader/$storyId"
    }

    // Settings / profile (icon in the top corner, point 2 + settings extras)
    data object Settings : Screen("settings")
    data object Welcome : Screen("welcome") // first-run profile creation
}
