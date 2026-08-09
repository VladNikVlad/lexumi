package com.lexumi.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every time a field is added to an entity, the database "version" goes up
 * by one, and a Migration is added here describing exactly how to carry the
 * old rows forward into the new shape. Without this, Room has no choice but
 * to wipe the local database on an upgrade — this file is what prevents
 * that from happening to a real user's words, sections, and topics.
 *
 * Rule going forward: every entity change = one new Migration_N_(N+1) below,
 * added to the list at the bottom, alongside bumping LexumiDatabase.version.
 */

/** v1 -> v2: added [com.lexumi.app.data.local.entity.LanguageEntity.voiceName] (TTS voice per language). */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE languages ADD COLUMN voiceName TEXT")
    }
}

/** v2 -> v3: added [com.lexumi.app.data.local.entity.RuleEntity.imagePath] (photo attached to a rule). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE rules ADD COLUMN imagePath TEXT")
    }
}

/**
 * v3 -> v4: added [com.lexumi.app.data.local.entity.VideoEntity.localVideoPath] and made
 * `youtubeUrl` nullable (a video can now be an uploaded file instead of a YouTube link).
 * SQLite can't just relax a NOT NULL constraint in place, so the table is rebuilt.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE videos_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                name TEXT NOT NULL,
                youtubeUrl TEXT,
                localVideoPath TEXT,
                originalText TEXT,
                translationText TEXT,
                ruleIds TEXT NOT NULL,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO videos_new (id, topicId, name, youtubeUrl, localVideoPath, originalText, translationText, ruleIds)
            SELECT id, topicId, name, youtubeUrl, NULL, originalText, translationText, ruleIds FROM videos
            """.trimIndent()
        )
        db.execSQL("DROP TABLE videos")
        db.execSQL("ALTER TABLE videos_new RENAME TO videos")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_videos_topicId ON videos(topicId)")
    }
}

/**
 * v4 -> v5: added lifetime stats (point 5) — totalCorrect, bestStreak and an
 * internal currentStatsStreak counter — to both words and sentences.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN totalCorrect INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN currentStatsStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN totalCorrect INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN bestStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN currentStatsStreak INTEGER NOT NULL DEFAULT 0")
    }
}

/** v5 -> v6: added Sentence.known — the "Вже знаю" / "I already know" flag that excludes a sentence from future practice sessions. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sentences ADD COLUMN known INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * v6 -> v7: the mastery ladder rework — words and sentences now progress through 5 ratings
 * (0-4) instead of the old level/score pair. Existing words keep their old `level`/`score`
 * columns (now unused) and start over at rating 0 with fresh streaks; nothing is deleted.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE words ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN typedStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN typedReverseActive INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN voiceStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE words ADD COLUMN finalStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN directStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN reverseStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN audioStreak INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sentences ADD COLUMN voiceStreak INTEGER NOT NULL DEFAULT 0")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
