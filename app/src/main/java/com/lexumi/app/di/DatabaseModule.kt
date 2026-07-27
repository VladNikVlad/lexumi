package com.lexumi.app.di

import android.content.Context
import androidx.room.Room
import com.lexumi.app.data.local.LexumiDatabase
import com.lexumi.app.data.local.ALL_MIGRATIONS
import com.lexumi.app.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LexumiDatabase =
        Room.databaseBuilder(context, LexumiDatabase::class.java, LexumiDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            // Safety net only: if a future version is ever released without a
            // matching migration, this wipes rather than crashes. The real
            // protection is adding a Migration in Migrations.kt every time.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: LexumiDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideLanguageDao(db: LexumiDatabase): LanguageDao = db.languageDao()
    @Provides fun provideSectionDao(db: LexumiDatabase): SectionDao = db.sectionDao()
    @Provides fun provideTopicDao(db: LexumiDatabase): TopicDao = db.topicDao()
    @Provides fun provideRuleDao(db: LexumiDatabase): RuleDao = db.ruleDao()
    @Provides fun provideWordDao(db: LexumiDatabase): WordDao = db.wordDao()
    @Provides fun provideImageContentDao(db: LexumiDatabase): ImageContentDao = db.imageContentDao()
    @Provides fun provideVideoDao(db: LexumiDatabase): VideoDao = db.videoDao()
    @Provides fun provideAudioDialogDao(db: LexumiDatabase): AudioDialogDao = db.audioDialogDao()
    @Provides fun provideTestQuestionDao(db: LexumiDatabase): TestQuestionDao = db.testQuestionDao()
    @Provides fun provideSentenceDao(db: LexumiDatabase): SentenceDao = db.sentenceDao()
    @Provides fun provideStoryDao(db: LexumiDatabase): StoryDao = db.storyDao()
}
