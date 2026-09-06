package com.lexumi.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lexumi.app.data.local.dao.*
import com.lexumi.app.data.local.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        LanguageEntity::class,
        SectionEntity::class,
        TopicEntity::class,
        RuleEntity::class,
        WordEntity::class,
        WordTopicCrossRefEntity::class,
        ImageContentEntity::class,
        VideoEntity::class,
        AudioDialogEntity::class,
        TestQuestionEntity::class,
        SentenceEntity::class,
        SentenceTopicCrossRefEntity::class,
        StoryEntity::class,
    ],
    version = 14,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LexumiDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun languageDao(): LanguageDao
    abstract fun sectionDao(): SectionDao
    abstract fun topicDao(): TopicDao
    abstract fun ruleDao(): RuleDao
    abstract fun wordDao(): WordDao
    abstract fun wordTopicCrossRefDao(): WordTopicCrossRefDao
    abstract fun imageContentDao(): ImageContentDao
    abstract fun videoDao(): VideoDao
    abstract fun audioDialogDao(): AudioDialogDao
    abstract fun testQuestionDao(): TestQuestionDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun sentenceTopicCrossRefDao(): SentenceTopicCrossRefDao
    abstract fun storyDao(): StoryDao

    companion object {
        const val DATABASE_NAME = "lexumi.db"
    }
}
