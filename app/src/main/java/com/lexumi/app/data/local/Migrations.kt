package com.lexumi.app.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
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

/**
 * v7 -> v8: backend sync groundwork. Each of these tables can now be linked to its Supabase
 * counterpart via `remoteId` (the row's uuid there) — null means "not published / not yet
 * downloaded from the cloud". Publishing (admin) or downloading (regular user) fills this in;
 * nothing else about how these tables behave locally changes.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE languages ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE sections ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE topics ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE words ADD COLUMN remoteId TEXT")
    }
}

/**
 * v8 -> v9: backend sync groundwork, part 2 — rules, sentences, videos, stories and their
 * test questions can now be linked to Supabase the same way words/languages/sections/topics
 * already were (MIGRATION_7_8). image_content and audio_dialogs deliberately don't get one yet —
 * both reference a local-only file with no URL alternative, so syncing them needs real file
 * storage (Supabase Storage), which isn't implemented.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE rules ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE sentences ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE videos ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE stories ADD COLUMN remoteId TEXT")
        db.execSQL("ALTER TABLE test_questions ADD COLUMN remoteId TEXT")
    }
}

/**
 * v9 -> v10: drops `sentences.name` — every sentence was already named after its own text (the
 * edit flow and bulk-import both set name == text), so it was pure dead weight, never shown
 * anywhere distinct from `text`. Duplicate-detection now checks `text` directly instead.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sentences_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                text TEXT NOT NULL,
                translations TEXT NOT NULL,
                ruleIds TEXT NOT NULL,
                rating INTEGER NOT NULL,
                directStreak INTEGER NOT NULL,
                reverseStreak INTEGER NOT NULL,
                audioStreak INTEGER NOT NULL,
                voiceStreak INTEGER NOT NULL,
                score REAL NOT NULL,
                timesSeen INTEGER NOT NULL,
                totalCorrect INTEGER NOT NULL,
                bestStreak INTEGER NOT NULL,
                currentStatsStreak INTEGER NOT NULL,
                known INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sentences_new (id, topicId, text, translations, ruleIds, rating, directStreak, reverseStreak,
                audioStreak, voiceStreak, score, timesSeen, totalCorrect, bestStreak, currentStatsStreak, known, remoteId)
            SELECT id, topicId, text, translations, ruleIds, rating, directStreak, reverseStreak,
                audioStreak, voiceStreak, score, timesSeen, totalCorrect, bestStreak, currentStatsStreak, known, remoteId
            FROM sentences
            """.trimIndent()
        )
        db.execSQL("DROP TABLE sentences")
        db.execSQL("ALTER TABLE sentences_new RENAME TO sentences")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_topicId ON sentences(topicId)")
    }
}

/**
 * v10 -> v11: backend sync groundwork, part 3 — image cards (`image_contents`) can now be linked
 * to Supabase too. Images here are capped at 100kb (see `util/ImageCompressor.kt`), small enough
 * to embed directly as base64 in the row instead of needing real file storage — same trick used
 * for word/rule images, which don't need a migration since they already have `remoteId`.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE image_contents ADD COLUMN remoteId TEXT")
    }
}

/**
 * v11 -> v12: audio dialogues can now be linked to Supabase too. `remoteId` here works exactly
 * like every other entity's — the row's id in the `audio_dialogs` Postgres table. Unlike word/
 * rule/image-card pictures, an audio recording isn't size-capped and can be several MB, so the
 * audio bytes themselves are uploaded to Supabase Storage (a real file bucket, at a path derived
 * from this same row id) rather than embedded as base64 text in the row.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE audio_dialogs ADD COLUMN remoteId TEXT")
    }
}

private fun Cursor.strOrNull(col: String): String? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getString(i)
}
private fun Cursor.longOrNull(col: String): Long? {
    val i = getColumnIndexOrThrow(col)
    return if (isNull(i)) null else getLong(i)
}
private fun Cursor.str(col: String) = getString(getColumnIndexOrThrow(col))
private fun Cursor.long(col: String) = getLong(getColumnIndexOrThrow(col))
private fun Cursor.int(col: String) = getInt(getColumnIndexOrThrow(col))
private fun Cursor.double(col: String) = getDouble(getColumnIndexOrThrow(col))

/**
 * v12 -> v13: words/sentences become language-scoped shared rows instead of belonging to a single
 * topic — the same term/text added to two topics of the same language now refers to one physical
 * row, linked from each topic via a new cross-ref table (mirrors how [com.lexumi.app.data.local.entity.RuleEntity]
 * is already language-scoped and shared). This merges any *existing* duplicates (same language,
 * same term/text, case/whitespace-insensitive) into one surviving row — highest `rating`, then
 * highest `totalCorrect`, then lowest `id`, to keep the most progress — and every original topic
 * link becomes a cross-ref row; a losing row's translation is preserved as that topic's override
 * if it differs from the survivor's.
 *
 * `remoteId` is reset to null throughout (words, sentences, and their new cross-ref rows) — the
 * old Supabase `words`/`sentences` tables (topic-scoped) are being wiped and republished under the
 * new language-scoped schema, so any previously-remembered remote id would be stale.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val conv = Converters()

        val topicLanguage = HashMap<Long, Long>()
        db.query("SELECT t.id AS topicId, s.languageId AS languageId FROM topics t JOIN sections s ON t.sectionId = s.id").use { c ->
            while (c.moveToNext()) topicLanguage[c.long("topicId")] = c.long("languageId")
        }

        migrateWords(db, conv, topicLanguage)
        migrateSentences(db, conv, topicLanguage)
    }

    private fun migrateWords(db: SupportSQLiteDatabase, conv: Converters, topicLanguage: Map<Long, Long>) {
        db.execSQL(
            """
            CREATE TABLE words_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                languageId INTEGER NOT NULL,
                imagePath TEXT,
                term TEXT NOT NULL,
                translations TEXT NOT NULL,
                ruleId INTEGER,
                rating INTEGER NOT NULL,
                level INTEGER NOT NULL,
                correctStreak INTEGER NOT NULL,
                typedStreak INTEGER NOT NULL,
                typedReverseActive INTEGER NOT NULL,
                voiceStreak INTEGER NOT NULL,
                finalStreak INTEGER NOT NULL,
                score REAL NOT NULL,
                timesSeen INTEGER NOT NULL,
                lastSeenAt INTEGER,
                inReviewList INTEGER NOT NULL,
                addedToReviewAt INTEGER,
                totalCorrect INTEGER NOT NULL,
                bestStreak INTEGER NOT NULL,
                currentStatsStreak INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(languageId) REFERENCES languages(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE word_topic_cross_ref (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                wordId INTEGER NOT NULL,
                translationOverride TEXT,
                position INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY(wordId) REFERENCES words_new(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        data class OldWord(
            val id: Long, val topicId: Long, val imagePath: String?, val term: String, val translation: String,
            val ruleId: Long?, val rating: Int, val level: Int, val correctStreak: Int, val typedStreak: Int,
            val typedReverseActive: Int, val voiceStreak: Int, val finalStreak: Int, val score: Double,
            val timesSeen: Int, val lastSeenAt: Long?, val inReviewList: Int, val addedToReviewAt: Long?,
            val totalCorrect: Int, val bestStreak: Int, val currentStatsStreak: Int,
        )

        val oldWords = mutableListOf<OldWord>()
        db.query("SELECT * FROM words").use { c ->
            while (c.moveToNext()) {
                oldWords += OldWord(
                    id = c.long("id"), topicId = c.long("topicId"), imagePath = c.strOrNull("imagePath"),
                    term = c.str("term"), translation = c.str("translation"), ruleId = c.longOrNull("ruleId"),
                    rating = c.int("rating"), level = c.int("level"), correctStreak = c.int("correctStreak"),
                    typedStreak = c.int("typedStreak"), typedReverseActive = c.int("typedReverseActive"),
                    voiceStreak = c.int("voiceStreak"), finalStreak = c.int("finalStreak"), score = c.double("score"),
                    timesSeen = c.int("timesSeen"), lastSeenAt = c.longOrNull("lastSeenAt"),
                    inReviewList = c.int("inReviewList"), addedToReviewAt = c.longOrNull("addedToReviewAt"),
                    totalCorrect = c.int("totalCorrect"), bestStreak = c.int("bestStreak"),
                    currentStatsStreak = c.int("currentStatsStreak"),
                )
            }
        }

        val groups = oldWords.groupBy { (topicLanguage[it.topicId] ?: -1L) to it.term.trim().lowercase() }
        for ((key, group) in groups) {
            val languageId = key.first
            if (languageId < 0) continue // topic with no resolvable language (shouldn't happen) — skip defensively
            val survivor = group.sortedWith(
                compareByDescending<OldWord> { it.rating }.thenByDescending { it.totalCorrect }.thenBy { it.id }
            ).first()

            val wordValues = ContentValues().apply {
                put("languageId", languageId)
                put("imagePath", survivor.imagePath)
                put("term", survivor.term)
                put("translations", conv.fromStringList(listOf(survivor.translation)))
                put("ruleId", survivor.ruleId)
                put("rating", survivor.rating)
                put("level", survivor.level)
                put("correctStreak", survivor.correctStreak)
                put("typedStreak", survivor.typedStreak)
                put("typedReverseActive", survivor.typedReverseActive)
                put("voiceStreak", survivor.voiceStreak)
                put("finalStreak", survivor.finalStreak)
                put("score", survivor.score)
                put("timesSeen", survivor.timesSeen)
                put("lastSeenAt", survivor.lastSeenAt)
                put("inReviewList", survivor.inReviewList)
                put("addedToReviewAt", survivor.addedToReviewAt)
                put("totalCorrect", survivor.totalCorrect)
                put("bestStreak", survivor.bestStreak)
                put("currentStatsStreak", survivor.currentStatsStreak)
                putNull("remoteId")
            }
            val newWordId = db.insert("words_new", SQLiteDatabase.CONFLICT_NONE, wordValues)

            for (old in group) {
                val override = if (old.translation.trim().equals(survivor.translation.trim(), ignoreCase = true)) null else old.translation
                val crossRefValues = ContentValues().apply {
                    put("topicId", old.topicId)
                    put("wordId", newWordId)
                    put("translationOverride", override)
                    put("position", 0)
                    putNull("remoteId")
                }
                db.insert("word_topic_cross_ref", SQLiteDatabase.CONFLICT_NONE, crossRefValues)
            }
        }

        db.execSQL("DROP TABLE words")
        db.execSQL("ALTER TABLE words_new RENAME TO words")
        db.execSQL("CREATE INDEX index_words_languageId ON words(languageId)")
        db.execSQL("CREATE INDEX index_word_topic_cross_ref_topicId ON word_topic_cross_ref(topicId)")
        db.execSQL("CREATE INDEX index_word_topic_cross_ref_wordId ON word_topic_cross_ref(wordId)")
    }

    private fun migrateSentences(db: SupportSQLiteDatabase, conv: Converters, topicLanguage: Map<Long, Long>) {
        db.execSQL(
            """
            CREATE TABLE sentences_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                languageId INTEGER NOT NULL,
                text TEXT NOT NULL,
                translations TEXT NOT NULL,
                ruleIds TEXT NOT NULL,
                rating INTEGER NOT NULL,
                directStreak INTEGER NOT NULL,
                reverseStreak INTEGER NOT NULL,
                audioStreak INTEGER NOT NULL,
                voiceStreak INTEGER NOT NULL,
                score REAL NOT NULL,
                timesSeen INTEGER NOT NULL,
                totalCorrect INTEGER NOT NULL,
                bestStreak INTEGER NOT NULL,
                currentStatsStreak INTEGER NOT NULL,
                known INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(languageId) REFERENCES languages(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE sentence_topic_cross_ref (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                sentenceId INTEGER NOT NULL,
                translationsOverride TEXT,
                position INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY(sentenceId) REFERENCES sentences_new(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        data class OldSentence(
            val id: Long, val topicId: Long, val text: String, val translations: String, val ruleIds: String,
            val rating: Int, val directStreak: Int, val reverseStreak: Int, val audioStreak: Int, val voiceStreak: Int,
            val score: Double, val timesSeen: Int, val totalCorrect: Int, val bestStreak: Int,
            val currentStatsStreak: Int, val known: Int,
        )

        val oldSentences = mutableListOf<OldSentence>()
        db.query("SELECT * FROM sentences").use { c ->
            while (c.moveToNext()) {
                oldSentences += OldSentence(
                    id = c.long("id"), topicId = c.long("topicId"), text = c.str("text"),
                    translations = c.str("translations"), ruleIds = c.str("ruleIds"), rating = c.int("rating"),
                    directStreak = c.int("directStreak"), reverseStreak = c.int("reverseStreak"),
                    audioStreak = c.int("audioStreak"), voiceStreak = c.int("voiceStreak"), score = c.double("score"),
                    timesSeen = c.int("timesSeen"), totalCorrect = c.int("totalCorrect"), bestStreak = c.int("bestStreak"),
                    currentStatsStreak = c.int("currentStatsStreak"), known = c.int("known"),
                )
            }
        }

        val groups = oldSentences.groupBy { (topicLanguage[it.topicId] ?: -1L) to it.text.trim().lowercase() }
        for ((key, group) in groups) {
            val languageId = key.first
            if (languageId < 0) continue
            val survivor = group.sortedWith(
                compareByDescending<OldSentence> { it.rating }.thenByDescending { it.totalCorrect }.thenBy { it.id }
            ).first()

            val sentenceValues = ContentValues().apply {
                put("languageId", languageId)
                put("text", survivor.text)
                put("translations", survivor.translations)
                put("ruleIds", survivor.ruleIds)
                put("rating", survivor.rating)
                put("directStreak", survivor.directStreak)
                put("reverseStreak", survivor.reverseStreak)
                put("audioStreak", survivor.audioStreak)
                put("voiceStreak", survivor.voiceStreak)
                put("score", survivor.score)
                put("timesSeen", survivor.timesSeen)
                put("totalCorrect", survivor.totalCorrect)
                put("bestStreak", survivor.bestStreak)
                put("currentStatsStreak", survivor.currentStatsStreak)
                put("known", survivor.known)
                putNull("remoteId")
            }
            val newSentenceId = db.insert("sentences_new", SQLiteDatabase.CONFLICT_NONE, sentenceValues)

            for (old in group) {
                val override = if (old.translations == survivor.translations) null else old.translations
                val crossRefValues = ContentValues().apply {
                    put("topicId", old.topicId)
                    put("sentenceId", newSentenceId)
                    put("translationsOverride", override)
                    put("position", 0)
                    putNull("remoteId")
                }
                db.insert("sentence_topic_cross_ref", SQLiteDatabase.CONFLICT_NONE, crossRefValues)
            }
        }

        db.execSQL("DROP TABLE sentences")
        db.execSQL("ALTER TABLE sentences_new RENAME TO sentences")
        db.execSQL("CREATE INDEX index_sentences_languageId ON sentences(languageId)")
        db.execSQL("CREATE INDEX index_sentence_topic_cross_ref_topicId ON sentence_topic_cross_ref(topicId)")
        db.execSQL("CREATE INDEX index_sentence_topic_cross_ref_sentenceId ON sentence_topic_cross_ref(sentenceId)")
    }
}

/**
 * v13 -> v14: fixes a schema bug in MIGRATION_12_13 — `word_topic_cross_ref`/
 * `sentence_topic_cross_ref` were created (with a foreign key to `words_new`/`sentences_new`)
 * *before* those tables got renamed to their final `words`/`sentences` names. SQLite is
 * documented to rewrite foreign keys automatically when the table they point at is renamed, but
 * that didn't happen on every device — leaving the cross-ref tables' FK metadata permanently
 * pointing at a table name that no longer exists, which fails Room's schema validation on every
 * app launch (`Migration didn't properly handle: word_topic_cross_ref`), crashing before the app
 * even opens. This recreates both cross-ref tables with their FK correctly naming `words`/
 * `sentences` directly — no rename involved this time, so there's nothing for SQLite to get
 * wrong — and copies every row across untouched. No data is lost; this only fixes table metadata.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE word_topic_cross_ref_fixed (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                wordId INTEGER NOT NULL,
                translationOverride TEXT,
                position INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY(wordId) REFERENCES words(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO word_topic_cross_ref_fixed (id, topicId, wordId, translationOverride, position, remoteId)
            SELECT id, topicId, wordId, translationOverride, position, remoteId FROM word_topic_cross_ref
            """.trimIndent()
        )
        db.execSQL("DROP TABLE word_topic_cross_ref")
        db.execSQL("ALTER TABLE word_topic_cross_ref_fixed RENAME TO word_topic_cross_ref")
        db.execSQL("CREATE INDEX index_word_topic_cross_ref_topicId ON word_topic_cross_ref(topicId)")
        db.execSQL("CREATE INDEX index_word_topic_cross_ref_wordId ON word_topic_cross_ref(wordId)")

        db.execSQL(
            """
            CREATE TABLE sentence_topic_cross_ref_fixed (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                topicId INTEGER NOT NULL,
                sentenceId INTEGER NOT NULL,
                translationsOverride TEXT,
                position INTEGER NOT NULL,
                remoteId TEXT,
                FOREIGN KEY(topicId) REFERENCES topics(id) ON DELETE CASCADE,
                FOREIGN KEY(sentenceId) REFERENCES sentences(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sentence_topic_cross_ref_fixed (id, topicId, sentenceId, translationsOverride, position, remoteId)
            SELECT id, topicId, sentenceId, translationsOverride, position, remoteId FROM sentence_topic_cross_ref
            """.trimIndent()
        )
        db.execSQL("DROP TABLE sentence_topic_cross_ref")
        db.execSQL("ALTER TABLE sentence_topic_cross_ref_fixed RENAME TO sentence_topic_cross_ref")
        db.execSQL("CREATE INDEX index_sentence_topic_cross_ref_topicId ON sentence_topic_cross_ref(topicId)")
        db.execSQL("CREATE INDEX index_sentence_topic_cross_ref_sentenceId ON sentence_topic_cross_ref(sentenceId)")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14,
)
