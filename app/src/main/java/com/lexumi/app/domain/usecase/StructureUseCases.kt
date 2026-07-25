package com.lexumi.app.domain.usecase

import com.lexumi.app.domain.repository.LanguageRepository
import com.lexumi.app.domain.repository.SectionRepository
import com.lexumi.app.domain.repository.TopicRepository
import javax.inject.Inject

sealed class AddResult {
    data class Success(val id: Long) : AddResult()
    data object AlreadyExists : AddResult()
    data object Blank : AddResult()
}

class AddLanguageUseCase @Inject constructor(private val repo: LanguageRepository) {
    suspend operator fun invoke(profileId: Long, name: String): AddResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AddResult.Blank
        if (repo.exists(profileId, trimmed)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addLanguage(profileId, trimmed))
    }
}

class AddSectionUseCase @Inject constructor(private val repo: SectionRepository) {
    suspend operator fun invoke(languageId: Long, name: String): AddResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AddResult.Blank
        if (repo.exists(languageId, trimmed)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addSection(languageId, trimmed))
    }
}

class AddTopicUseCase @Inject constructor(private val repo: TopicRepository) {
    suspend operator fun invoke(sectionId: Long, name: String): AddResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return AddResult.Blank
        if (repo.exists(sectionId, trimmed)) return AddResult.AlreadyExists
        return AddResult.Success(repo.addTopic(sectionId, trimmed))
    }
}
