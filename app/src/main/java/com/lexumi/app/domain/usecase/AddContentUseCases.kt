package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.TestQuestion
import com.lexumi.app.domain.repository.*
import javax.inject.Inject

class AddRuleUseCase @Inject constructor(private val repo: RuleRepository) {
    // Rules are checked for uniqueness across the whole language, not per topic (point 7).
    suspend operator fun invoke(languageId: Long, name: String, text: String): AddResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || text.isBlank()) return AddResult.Blank
        if (repo.exists(languageId, trimmedName)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addRule(languageId, trimmedName, text))
    }
}

/** Word images must not exceed 100kb (point 8) — enforced by the caller before invoking this. */
class AddWordUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(topicId: Long, imagePath: String?, term: String, translation: String, ruleId: Long?): AddResult {
        val trimmedTerm = term.trim()
        if (trimmedTerm.isEmpty() || translation.isBlank()) return AddResult.Blank
        if (repo.exists(topicId, trimmedTerm)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addWord(topicId, imagePath, trimmedTerm, translation.trim(), ruleId))
    }
}

class AddImageContentUseCase @Inject constructor(private val repo: ImageContentRepository) {
    suspend operator fun invoke(topicId: Long, name: String, imagePath: String, translation: String): AddResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AddResult.Blank
        if (repo.exists(topicId, trimmed)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addImage(topicId, trimmed, imagePath, translation.trim()))
    }
}

class AddVideoUseCase @Inject constructor(private val repo: VideoRepository) {
    suspend operator fun invoke(
        topicId: Long, name: String, youtubeUrl: String, originalText: String?,
        translationText: String?, ruleIds: List<Long>, questions: List<TestQuestion>,
    ): AddResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || youtubeUrl.isBlank()) return AddResult.Blank
        if (repo.exists(topicId, trimmedName)) return AddResult.AlreadyExists
        return AddResult.Success(
            repo.addVideo(topicId, trimmedName, youtubeUrl.trim(), originalText, translationText, ruleIds, questions)
        )
    }
}

class AddAudioDialogUseCase @Inject constructor(private val repo: AudioDialogRepository) {
    suspend operator fun invoke(
        topicId: Long, name: String, audioPath: String, translationText: String?,
        ruleIds: List<Long>, questions: List<TestQuestion>,
    ): AddResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || audioPath.isBlank()) return AddResult.Blank
        if (repo.exists(topicId, trimmedName)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addDialog(topicId, trimmedName, audioPath, translationText, ruleIds, questions))
    }
}

class AddSentenceUseCase @Inject constructor(private val repo: SentenceRepository) {
    suspend operator fun invoke(topicId: Long, name: String, text: String, translations: List<String>, ruleIds: List<Long>): AddResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || text.isBlank() || translations.firstOrNull()?.isBlank() != false) return AddResult.Blank
        if (repo.exists(topicId, trimmedName)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addSentence(topicId, trimmedName, text, translations.filter { it.isNotBlank() }, ruleIds))
    }
}

class AddStoryUseCase @Inject constructor(private val repo: StoryRepository) {
    suspend operator fun invoke(topicId: Long, name: String, text: String, translation: String?, ruleIds: List<Long>): AddResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || text.isBlank()) return AddResult.Blank
        if (repo.exists(topicId, trimmedName)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addStory(topicId, trimmedName, text, translation, ruleIds))
    }
}
