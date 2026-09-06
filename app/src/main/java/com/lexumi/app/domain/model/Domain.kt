package com.lexumi.app.domain.model

data class UserProfile(val id: Long, val displayName: String)

data class Language(val id: Long, val profileId: Long, val name: String, val voiceName: String? = null, val remoteId: String? = null)

data class Section(val id: Long, val languageId: Long, val name: String, val position: Int, val remoteId: String? = null)

data class Topic(val id: Long, val sectionId: Long, val name: String, val position: Int, val remoteId: String? = null)

data class Rule(val id: Long, val languageId: Long, val name: String, val text: String, val imagePath: String? = null, val remoteId: String? = null)

/** Which of the four answer slots is correct, in multiple-choice (level 0) mode. */
data class MultipleChoiceOption(val text: String, val isCorrect: Boolean)

data class Word(
    val id: Long,
    val languageId: Long,
    val imagePath: String?,
    /** Resolved for the topic this was read for — the topic's own override translation if it has
     * forked one, otherwise [translations].first(). Use [translations] to see/edit the full shared
     * list (e.g. for an "add translation for everyone" action). */
    val translation: String,
    val translations: List<String>,
    val term: String,
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
    val remoteId: String? = null,
)

data class ImageContent(
    val id: Long,
    val topicId: Long,
    val name: String,
    val imagePath: String,
    val translation: String,
    val remoteId: String? = null,
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
    val remoteId: String? = null,
)

data class AudioDialog(
    val id: Long,
    val topicId: Long,
    val name: String,
    val audioPath: String,
    val translationText: String?,
    val ruleIds: List<Long>,
    val remoteId: String? = null,
)

enum class QuestionAnswerType { TRUE_FALSE, EXACT_TEXT }

data class TestQuestion(
    val id: Long,
    val questionText: String,
    val answerType: QuestionAnswerType,
    val correctBoolean: Boolean?,
    val acceptableAnswers: List<String>,
    val remoteId: String? = null,
)

data class Sentence(
    val id: Long,
    val languageId: Long,
    val text: String,
    /** Resolved for the topic this was read for — the topic's own override list if it has forked
     * one, otherwise the sentence's own shared list. */
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
    val remoteId: String? = null,
)

data class Story(
    val id: Long,
    val topicId: Long,
    val name: String,
    val text: String,
    val translation: String?,
    val ruleIds: List<Long>,
    val remoteId: String? = null,
)

/** Result of comparing a user's typed answer against the expected word/sentence. */
sealed class AnswerCheck {
    data object Correct : AnswerCheck()
    data class OneLetterTypo(val correctSpelling: String) : AnswerCheck()
    data class Wrong(val correctSpelling: String) : AnswerCheck()
}
