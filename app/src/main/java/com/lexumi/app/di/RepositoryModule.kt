package com.lexumi.app.di

import com.lexumi.app.data.repository.*
import com.lexumi.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    @Binds @Singleton abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository
    @Binds @Singleton abstract fun bindSectionRepository(impl: SectionRepositoryImpl): SectionRepository
    @Binds @Singleton abstract fun bindTopicRepository(impl: TopicRepositoryImpl): TopicRepository
    @Binds @Singleton abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
    @Binds @Singleton abstract fun bindWordRepository(impl: WordRepositoryImpl): WordRepository
    @Binds @Singleton abstract fun bindImageContentRepository(impl: ImageContentRepositoryImpl): ImageContentRepository
    @Binds @Singleton abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository
    @Binds @Singleton abstract fun bindAudioDialogRepository(impl: AudioDialogRepositoryImpl): AudioDialogRepository
    @Binds @Singleton abstract fun bindSentenceRepository(impl: SentenceRepositoryImpl): SentenceRepository
    @Binds @Singleton abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository
}
