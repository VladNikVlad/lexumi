package com.lexumi.app.domain.model

data class UserProfile(val id: Long, val displayName: String)

data class Language(val id: Long, val profileId: Long, val name: String, val voiceName: String? = null)

data class Section(val id: Long, val languageId: Long, val name: String, val position: Int)

data class Topic(val id: Long, val sectionId: Long, val name: String, val position: Int)

data class Rule(val id: Long, val languageId: Long, val name: String, val text: String, val imagePath: String? = null)

/** Which of the four answer slots is correct, in multiple-choice (level 0) mode. */
data class MultipleChoiceOption(val text: String, val isCorrect: Boolean)

data class Word(
    val id: Long,
    val topicId: Long,
    val imagePath: String?,
    val term: String,
    val translation: String,
    val ruleId: Long?,
    /** Mastery ladder: 0 = new, 1 = typed both directions, 2 = say-it-aloud cards, 3 = hear-only, 4 = mastered. */
    val rating: Int = 0,
    val correctStreak: Int = 0,
    val typedStreak: Int = 0,
    val typedReverseActive: Boolean = false,
    val voiceStreak: Int = 0,
    val finalStreak: Int = 0,
    val timesSeen: Int = 0,
    val inReviewList: Boolean = false,
    val totalCorrect: Int = 0,
    val bestStreak: Int = 0,
    val currentStatsStreak: Int = 0,
)

data class ImageContent(
    val id: Long,
    val topicId: Long,
    val name: String,
    val imagePath: String,
    val translation: String,
)

data class VideoContent(
    val id: Long,
    val topicId: Long,
    val name: String,
    val youtubeUrl: String?,
    val localVideoPath: String?,
    val originalText: String?,
    val translationText: String?,
    val ruleIds: List<Long>,
)

data class AudioDialog(
    val id: Long,
    val topicId: Long,
    val name: String,
    val audioPath: String,
    val translationText: String?,
    val ruleIds: List<Long>,
)

enum class QuestionAnswerType { TRUE_FALSE, EXACT_TEXT }

data class TestQuestion(
    val id: Long,
    val questionText: String,
    val answerType: QuestionAnswerType,
    val correctBoolean: Boolean?,
    val acceptableAnswers: List<String>,
)

data class Sentence(
    val id: Long,
    val topicId: Long,
    val name: String,
    val text: String,
    val translations: List<String>,
    val ruleIds: List<Long>,
    /** Mastery ladder: 0 = target->native typed, 1 = native->target typed, 2 = audio-only, 3 = say-it-aloud, 4 = mastered. */
    val rating: Int = 0,
    val directStreak: Int = 0,
    val reverseStreak: Int = 0,
    val audioStreak: Int = 0,
    val voiceStreak: Int = 0,
    val timesSeen: Int = 0,
    val totalCorrect: Int = 0,
    val bestStreak: Int = 0,
    val currentStatsStreak: Int = 0,
    val known: Boolean = false,
)

data class Story(
    val id: Long,
    val topicId: Long,
    val name: String,
    val text: String,
    val translation: String?,
    val ruleIds: List<Long>,
)

/** Result of comparing a user's typed answer against the expected word/sentence. */
sealed class AnswerCheck {
    data object Correct : AnswerCheck()
    data class OneLetterTypo(val correctSpelling: String) : AnswerCheck()
    data class Wrong(val correctSpelling: String) : AnswerCheck()
}
