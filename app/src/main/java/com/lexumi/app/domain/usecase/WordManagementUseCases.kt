package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.model.Word
import com.lexumi.app.domain.repository.WordRepository
import javax.inject.Inject

/** Lets the user fix a word's text, swap/add/remove its picture, or change its rule — without resetting learning progress. */
class EditWordUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(word: Word, term: String, translation: String, imagePath: String?, ruleId: Long?): AddResult {
        val trimmedTerm = term.trim()
        if (trimmedTerm.isEmpty() || translation.isBlank()) return AddResult.Blank
        val duplicate = repo.getWords(word.topicId).any { it.id != word.id && it.term.equals(trimmedTerm, ignoreCase = true) }
        if (duplicate) return AddResult.AlreadyExists
        repo.updateWord(word.copy(term = trimmedTerm, translation = translation.trim(), imagePath = imagePath, ruleId = ruleId))
        return AddResult.Success(word.id)
    }
}

class DeleteWordUseCase @Inject constructor(private val repo: WordRepository) {
    suspend operator fun invoke(word: Word) = repo.deleteWord(word)
}
