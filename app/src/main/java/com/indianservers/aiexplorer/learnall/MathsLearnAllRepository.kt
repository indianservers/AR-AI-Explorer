package com.indianservers.aiexplorer.learnall

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

data class MathsLessonSection(
    val title: String = "",
    val body: String = "",
    val bullets: List<String> = emptyList(),
)

data class MathsLessonContent(
    val introduction: List<MathsLessonSection> = emptyList(),
    val detailedExplanation: List<MathsLessonSection> = emptyList(),
    val realtimeExamples: List<MathsLessonSection> = emptyList(),
    val simplifiedExplanation: List<MathsLessonSection> = emptyList(),
    val advancedExplanation: List<MathsLessonSection> = emptyList(),
    val practicePrompt: List<MathsLessonSection> = emptyList(),
) {
    fun plainIntroduction(): String = introduction.plainText()
    fun plainDetailedExplanation(): String = detailedExplanation.plainText()
    fun plainRealtimeExamples(): String = realtimeExamples.plainText()
    fun plainSimplifiedExplanation(): String = simplifiedExplanation.plainText()
    fun plainAdvancedExplanation(): String = advancedExplanation.plainText()
    fun plainPracticePrompt(): String = practicePrompt.plainText()
}

data class MathsLesson(
    val id: String,
    val classLevel: String,
    val chapter: String,
    val topic: String,
    val subtopic: String,
    val introduction: String,
    val detailedExplanation: String,
    val realtimeExamples: String,
    val simplifiedExplanation: String = "",
    val advancedExplanation: String = "",
    val practicePrompt: String = "",
    val content: MathsLessonContent = MathsLessonContent(),
    val updatedAt: Long,
)

data class MathsLessonSummary(
    val id: String,
    val classLevel: String,
    val chapter: String,
    val topic: String,
    val subtopic: String,
)

data class MathsLearnAllStats(
    val lessonCount: Int,
    val chapterCount: Int,
    val classCount: Int,
)

data class MathsConcept(
    val title: String,
    val icon: String,
    val summary: String,
    val subtopics: List<String>,
    val levels: List<String>,
    val lessonCount: Int,
)

data class MathsLessonProgress(
    val lessonId: String,
    val saved: Boolean,
    val completed: Boolean,
    val attempts: Int,
    val hintsUsed: Int,
    val lastViewedAt: Long,
    val updatedAt: Long,
)

enum class MathsHomeSearchKind { Tool, Concept, Lesson, Topic, Chapter }

data class MathsHomeSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: MathsHomeSearchKind,
    val lessonId: String? = null,
    val conceptTitle: String? = null,
    val query: String = title,
)

data class LearningGenome(
    val learnerId: String = "local",
    val preferredPace: String = "balanced",
    val explanationStyle: String = "visual_step_by_step",
    val challengeLevel: String = "adaptive",
    val confidenceMode: String = "ask_when_useful",
    val weakConcepts: List<String> = emptyList(),
    val strongConcepts: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class LearningResumeCard(
    val lesson: MathsLessonSummary?,
    val completedCount: Int,
    val savedCount: Int,
    val recentCount: Int,
    val coachMessage: String,
)

data class OfflineLearningCoachResponse(
    val title: String,
    val message: String,
    val suggestedQuery: String,
    val suggestedLessonId: String?,
)

private data class BundledConcept(
    val title: String,
    val icon: String,
    val summary: String,
    val subtopics: String,
    val levels: String,
    val lessonCount: Int,
)

private class MathsLearnAllDb(context: Context) : SQLiteOpenHelper(context, "maths-learn-all.db", null, 6) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE lessons(
                id TEXT PRIMARY KEY NOT NULL,
                class_level TEXT NOT NULL,
                chapter TEXT NOT NULL,
                topic TEXT NOT NULL DEFAULT '',
                subtopic TEXT NOT NULL,
                introduction TEXT NOT NULL,
                detailed_explanation TEXT NOT NULL,
                realtime_examples TEXT NOT NULL,
                simplified_explanation TEXT NOT NULL DEFAULT '',
                advanced_explanation TEXT NOT NULL DEFAULT '',
                practice_prompt TEXT NOT NULL DEFAULT '',
                content_json TEXT NOT NULL DEFAULT '',
                search_text TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_lessons_class_chapter_topic ON lessons(class_level, chapter, topic)")
        db.execSQL("CREATE INDEX idx_lessons_search ON lessons(search_text)")
        db.createSeedTables()
        db.createLearnerStateTables()
        db.createPersonalizedLearningTables()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.ensureColumn("lessons", "topic", "ALTER TABLE lessons ADD COLUMN topic TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_class_chapter_topic ON lessons(class_level, chapter, topic)")
        }
        if (oldVersion < 3) db.createSeedTables()
        if (oldVersion < 4) db.createLearnerStateTables()
        if (oldVersion < 5) db.ensureColumn("lessons", "content_json", "ALTER TABLE lessons ADD COLUMN content_json TEXT NOT NULL DEFAULT ''")
        if (oldVersion < 6) db.createPersonalizedLearningTables()
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            db.createSeedTables()
            db.ensureColumn("lessons", "topic", "ALTER TABLE lessons ADD COLUMN topic TEXT NOT NULL DEFAULT ''")
            db.ensureColumn("lessons", "content_json", "ALTER TABLE lessons ADD COLUMN content_json TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_class_chapter_topic ON lessons(class_level, chapter, topic)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_search ON lessons(search_text)")
            db.createLearnerStateTables()
            db.createPersonalizedLearningTables()
        }
    }

    private fun SQLiteDatabase.ensureColumn(table: String, column: String, alterSql: String) {
        val tableExists = rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }
        if (!tableExists) return
        val exists = rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            generateSequence { if (cursor.moveToNext()) cursor.getString(1) else null }.any { it == column }
        }
        if (!exists) execSQL(alterSql)
    }

    private fun SQLiteDatabase.createSeedTables() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS metadata(
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS concepts(
                title TEXT PRIMARY KEY NOT NULL,
                icon TEXT NOT NULL,
                summary TEXT NOT NULL,
                subtopics TEXT NOT NULL,
                levels TEXT NOT NULL,
                lesson_count INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun SQLiteDatabase.createLearnerStateTables() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS lesson_progress(
                lesson_id TEXT PRIMARY KEY NOT NULL,
                saved INTEGER NOT NULL DEFAULT 0,
                completed INTEGER NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                hints_used INTEGER NOT NULL DEFAULT 0,
                last_viewed_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_lesson_progress_saved ON lesson_progress(saved, updated_at)")
        execSQL("CREATE INDEX IF NOT EXISTS idx_lesson_progress_recent ON lesson_progress(last_viewed_at)")
        execSQL("CREATE INDEX IF NOT EXISTS idx_lesson_progress_completed ON lesson_progress(completed, updated_at)")
    }

    private fun SQLiteDatabase.createPersonalizedLearningTables() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learner_profile(
                learner_id TEXT PRIMARY KEY NOT NULL,
                display_name TEXT NOT NULL DEFAULT '',
                class_level TEXT NOT NULL DEFAULT '',
                board TEXT NOT NULL DEFAULT '',
                language TEXT NOT NULL DEFAULT 'en',
                timezone TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_genome(
                learner_id TEXT PRIMARY KEY NOT NULL,
                preferred_pace TEXT NOT NULL DEFAULT 'balanced',
                explanation_style TEXT NOT NULL DEFAULT 'visual_step_by_step',
                challenge_level TEXT NOT NULL DEFAULT 'adaptive',
                confidence_mode TEXT NOT NULL DEFAULT 'ask_when_useful',
                weak_concepts TEXT NOT NULL DEFAULT '',
                strong_concepts TEXT NOT NULL DEFAULT '',
                last_recommendation TEXT NOT NULL DEFAULT '',
                online_llm_context TEXT NOT NULL DEFAULT '',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS concept_mastery(
                learner_id TEXT NOT NULL,
                concept_key TEXT NOT NULL,
                concept_title TEXT NOT NULL,
                mastery_score REAL NOT NULL DEFAULT 0,
                confidence_score REAL NOT NULL DEFAULT 0,
                retention_score REAL NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                correct_attempts INTEGER NOT NULL DEFAULT 0,
                hints_used INTEGER NOT NULL DEFAULT 0,
                last_seen_at INTEGER NOT NULL DEFAULT 0,
                next_review_at INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(learner_id, concept_key)
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_concept_mastery_review ON concept_mastery(learner_id, next_review_at, mastery_score)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_sessions(
                session_id TEXT PRIMARY KEY NOT NULL,
                learner_id TEXT NOT NULL,
                started_at INTEGER NOT NULL,
                ended_at INTEGER NOT NULL DEFAULT 0,
                active_lesson_id TEXT NOT NULL DEFAULT '',
                active_concept_key TEXT NOT NULL DEFAULT '',
                mode TEXT NOT NULL DEFAULT 'learn',
                summary TEXT NOT NULL DEFAULT '',
                resume_payload TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_sessions_resume ON learning_sessions(learner_id, ended_at, started_at)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_events(
                event_id TEXT PRIMARY KEY NOT NULL,
                learner_id TEXT NOT NULL,
                lesson_id TEXT NOT NULL DEFAULT '',
                concept_key TEXT NOT NULL DEFAULT '',
                event_type TEXT NOT NULL,
                value TEXT NOT NULL DEFAULT '',
                occurred_at INTEGER NOT NULL,
                sync_state TEXT NOT NULL DEFAULT 'local'
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_events_timeline ON learning_events(learner_id, occurred_at)")
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_events_lesson ON learning_events(lesson_id, occurred_at)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_recommendations(
                recommendation_id TEXT PRIMARY KEY NOT NULL,
                learner_id TEXT NOT NULL,
                lesson_id TEXT NOT NULL DEFAULT '',
                concept_key TEXT NOT NULL DEFAULT '',
                reason TEXT NOT NULL,
                priority REAL NOT NULL DEFAULT 0,
                message TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                dismissed_at INTEGER NOT NULL DEFAULT 0,
                accepted_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_recommendations_active ON learning_recommendations(learner_id, dismissed_at, accepted_at, priority)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS offline_assistant_memory(
                memory_id TEXT PRIMARY KEY NOT NULL,
                learner_id TEXT NOT NULL,
                source TEXT NOT NULL,
                prompt TEXT NOT NULL,
                response TEXT NOT NULL,
                related_lesson_id TEXT NOT NULL DEFAULT '',
                related_concept_key TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_offline_assistant_memory_recent ON offline_assistant_memory(learner_id, created_at)")
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS learning_search_index(
                item_id TEXT PRIMARY KEY NOT NULL,
                item_type TEXT NOT NULL,
                title TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                lesson_id TEXT NOT NULL DEFAULT '',
                concept_title TEXT NOT NULL DEFAULT '',
                search_text TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_search_type ON learning_search_index(item_type, title)")
        execSQL("CREATE INDEX IF NOT EXISTS idx_learning_search_text ON learning_search_index(search_text)")
    }
}

class MathsLearnAllRepository(context: Context) {
    private val database = MathsLearnAllDb(context.applicationContext)
    private val appContext = context.applicationContext

    suspend fun seedBundledLessons(): Int = withContext(Dispatchers.IO) {
        val lessonsJson = appContext.readBundledLessonsText()
        val conceptsJson = appContext.assets.open("maths_concepts.json").bufferedReader().use { it.readText() }
        val seedHash = sha256("$lessonsJson\n$conceptsJson")

        database.readableDatabase.useReadable { db ->
            if (db.metadataValue("seed_hash") == seedHash && db.count("lessons") > 0 && db.count("learning_search_index") > 0) {
                return@withContext db.count("lessons")
            }
        }

        val lessons = JSONArray(lessonsJson).let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val content = item.optJSONObject("sections")?.lessonContent() ?: lessonContentFromLegacyFields(item)
                    add(
                        MathsLesson(
                            id = item.getString("id"),
                            classLevel = item.getString("classLevel"),
                            chapter = item.getString("chapter"),
                            topic = item.getString("topic"),
                            subtopic = item.getString("subtopic"),
                            introduction = item.optString("introduction").ifBlank { content.plainIntroduction() },
                            detailedExplanation = item.optString("detailedExplanation").ifBlank { content.plainDetailedExplanation() },
                            realtimeExamples = item.optString("realtimeExamples").ifBlank { content.plainRealtimeExamples() },
                            simplifiedExplanation = item.optString("simplifiedExplanation").ifBlank { content.plainSimplifiedExplanation() },
                            advancedExplanation = item.optString("advancedExplanation").ifBlank { content.plainAdvancedExplanation() },
                            practicePrompt = item.optString("practicePrompt").ifBlank { content.plainPracticePrompt() },
                            content = content,
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }
        val concepts = JSONArray(conceptsJson).let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        BundledConcept(
                            title = item.getString("title"),
                            icon = item.getString("icon"),
                            summary = item.getString("summary"),
                            subtopics = item.getJSONArray("subtopics").toString(),
                            levels = item.getJSONArray("levels").toString(),
                            lessonCount = item.getInt("lessonCount"),
                        ),
                    )
                }
            }
        }
        replaceBundledContent(lessons, concepts, seedHash)
        lessons.size
    }

    suspend fun homeSearch(query: String, limit: Int = 12): List<MathsHomeSearchResult> = withContext(Dispatchers.IO) {
        runCatching { seedBundledLessons() }.getOrElse { return@withContext emptyList() }
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return@withContext emptyList()
        val safeLimit = limit.coerceIn(1, 50).toString()
        database.readableDatabase.rawQuery(
            """
            SELECT item_id, item_type, title, subtitle, lesson_id, concept_title
            FROM learning_search_index
            WHERE search_text LIKE ?
            ORDER BY
                CASE item_type
                    WHEN 'concept' THEN 0
                    WHEN 'lesson' THEN 1
                    WHEN 'topic' THEN 2
                    WHEN 'chapter' THEN 3
                    ELSE 4
                END,
                title COLLATE NOCASE
            LIMIT $safeLimit
            """.trimIndent(),
            arrayOf("%$normalized%"),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val type = cursor.getString(1)
                    add(
                        MathsHomeSearchResult(
                            id = cursor.getString(0),
                            title = cursor.getString(2),
                            subtitle = cursor.getString(3),
                            kind = when (type) {
                                "concept" -> MathsHomeSearchKind.Concept
                                "lesson" -> MathsHomeSearchKind.Lesson
                                "topic" -> MathsHomeSearchKind.Topic
                                "chapter" -> MathsHomeSearchKind.Chapter
                                else -> MathsHomeSearchKind.Tool
                            },
                            lessonId = cursor.getString(4).takeIf(String::isNotBlank),
                            conceptTitle = cursor.getString(5).takeIf(String::isNotBlank),
                            query = cursor.getString(2),
                        ),
                    )
                }
            }
        }
    }

    suspend fun learningGenome(learnerId: String = "local"): LearningGenome = withContext(Dispatchers.IO) {
        ensureLocalProfile(learnerId)
        database.readableDatabase.query(
            "learning_genome",
            arrayOf("learner_id", "preferred_pace", "explanation_style", "challenge_level", "confidence_mode", "weak_concepts", "strong_concepts", "updated_at"),
            "learner_id=?",
            arrayOf(learnerId),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                LearningGenome(
                    learnerId = cursor.getString(0),
                    preferredPace = cursor.getString(1),
                    explanationStyle = cursor.getString(2),
                    challengeLevel = cursor.getString(3),
                    confidenceMode = cursor.getString(4),
                    weakConcepts = cursor.getString(5).csvList(),
                    strongConcepts = cursor.getString(6).csvList(),
                    updatedAt = cursor.getLong(7),
                )
            } else {
                LearningGenome(learnerId = learnerId)
            }
        }
    }

    suspend fun saveLearningGenome(genome: LearningGenome) = withContext(Dispatchers.IO) {
        ensureLocalProfile(genome.learnerId)
        database.writableDatabase.insertWithOnConflict(
            "learning_genome",
            null,
            ContentValues().apply {
                put("learner_id", genome.learnerId)
                put("preferred_pace", genome.preferredPace)
                put("explanation_style", genome.explanationStyle)
                put("challenge_level", genome.challengeLevel)
                put("confidence_mode", genome.confidenceMode)
                put("weak_concepts", genome.weakConcepts.joinToString(","))
                put("strong_concepts", genome.strongConcepts.joinToString(","))
                put("updated_at", System.currentTimeMillis())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun recordLearningEvent(
        lessonId: String,
        eventType: String,
        value: String = "",
        learnerId: String = "local",
        conceptKey: String = "",
    ) = withContext(Dispatchers.IO) {
        ensureLocalProfile(learnerId)
        val now = System.currentTimeMillis()
        database.writableDatabase.insertWithOnConflict(
            "learning_events",
            null,
            ContentValues().apply {
                put("event_id", "$learnerId-$eventType-$now")
                put("learner_id", learnerId)
                put("lesson_id", lessonId)
                put("concept_key", conceptKey)
                put("event_type", eventType)
                put("value", value)
                put("occurred_at", now)
                put("sync_state", "local")
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun resumeCard(learnerId: String = "local"): LearningResumeCard = withContext(Dispatchers.IO) {
        seedBundledLessons()
        ensureLocalProfile(learnerId)
        val progress = progress()
        val recentLessonId = progress.filter { it.lastViewedAt > 0 }.maxByOrNull { it.lastViewedAt }?.lessonId
        val recentLesson = recentLessonId?.let { summariesForIds(listOf(it)).firstOrNull() }
        val completed = progress.count { it.completed }
        val saved = progress.count { it.saved }
        val recent = progress.count { it.lastViewedAt > 0 }
        val message = when {
            recentLesson != null && recentLesson.id !in progress.filter { it.completed }.map { it.lessonId }.toSet() ->
                "Resume ${recentLesson.subtopic}. I will start with a quick recap, then one practice check."
            completed == 0 -> "Start with one short lesson. I will adapt hints and review timing as you learn."
            else -> "You have completed $completed lessons. Next I will balance review, weak spots, and fresh concepts."
        }
        LearningResumeCard(recentLesson, completed, saved, recent, message)
    }

    suspend fun offlineCoach(prompt: String, learnerId: String = "local"): OfflineLearningCoachResponse = withContext(Dispatchers.IO) {
        val results = homeSearch(prompt, limit = 5)
        val genome = learningGenome(learnerId)
        val bestLesson = results.firstOrNull { it.kind == MathsHomeSearchKind.Lesson } ?: results.firstOrNull()
        val message = buildString {
            append("Offline Genie helper: ")
            append(
                when {
                    bestLesson?.lessonId != null -> "I found ${bestLesson.title}. Start by reading the key idea, then solve one example without hints."
                    bestLesson?.conceptTitle != null -> "I found the concept ${bestLesson.conceptTitle}. Open it to see related sub-lessons."
                    else -> "I could not find an exact lesson yet. Try a shorter topic name like quadratic, circle, matrix, probability, or derivative."
                },
            )
            append(" Style: ${genome.explanationStyle.replace('_', ' ')}; pace: ${genome.preferredPace}.")
        }
        val response = OfflineLearningCoachResponse(
            title = bestLesson?.title ?: "Learning Helper",
            message = message,
            suggestedQuery = bestLesson?.query ?: prompt,
            suggestedLessonId = bestLesson?.lessonId,
        )
        saveAssistantMemory(prompt, response.message, response.suggestedLessonId.orEmpty(), bestLesson?.conceptTitle.orEmpty(), learnerId)
        response
    }

    suspend fun stats(): MathsLearnAllStats = withContext(Dispatchers.IO) {
        database.readableDatabase.useReadable { db ->
            MathsLearnAllStats(
                lessonCount = db.count("lessons"),
                chapterCount = db.countDistinct("lessons", "class_level || '|' || chapter"),
                classCount = db.countDistinct("lessons", "class_level"),
            )
        }
    }

    suspend fun concepts(): List<MathsConcept> = withContext(Dispatchers.IO) {
        database.readableDatabase.query(
            "concepts",
            arrayOf("title", "icon", "summary", "subtopics", "levels", "lesson_count"),
            null,
            null,
            null,
            null,
            "title COLLATE NOCASE",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.concept()) } }
    }

    suspend fun classes(): List<String> = withContext(Dispatchers.IO) {
        database.readableDatabase.query(
            true,
            "lessons",
            arrayOf("class_level"),
            null,
            null,
            null,
            null,
            "class_level COLLATE NOCASE",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    }

    suspend fun chapters(classLevel: String?): List<String> = withContext(Dispatchers.IO) {
        val selection = classLevel?.let { "class_level=?" }
        val args = classLevel?.let { arrayOf(it) }
        database.readableDatabase.query(
            true,
            "lessons",
            arrayOf("chapter"),
            selection,
            args,
            null,
            null,
            "chapter COLLATE NOCASE",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    }

    suspend fun topics(classLevel: String?, chapter: String?): List<String> = withContext(Dispatchers.IO) {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        classLevel?.let { clauses += "class_level=?"; args += it }
        chapter?.let { clauses += "chapter=?"; args += it }
        database.readableDatabase.query(
            true,
            "lessons",
            arrayOf("topic"),
            clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            args.takeIf { it.isNotEmpty() }?.toTypedArray(),
            null,
            null,
            "topic COLLATE NOCASE",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }
    }

    suspend fun summaries(classLevel: String?, chapter: String?, topic: String?, query: String): List<MathsLessonSummary> = withContext(Dispatchers.IO) {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        classLevel?.let { clauses += "class_level=?"; args += it }
        chapter?.let { clauses += "chapter=?"; args += it }
        topic?.let { clauses += "topic=?"; args += it }
        query.trim().takeIf(String::isNotBlank)?.let {
            clauses += "search_text LIKE ?"
            args += "%${it.lowercase()}%"
        }
        database.readableDatabase.query(
            "lessons",
            arrayOf("id", "class_level", "chapter", "topic", "subtopic"),
            clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND "),
            args.takeIf { it.isNotEmpty() }?.toTypedArray(),
            null,
            null,
            "class_level COLLATE NOCASE, chapter COLLATE NOCASE, topic COLLATE NOCASE, subtopic COLLATE NOCASE",
            "250",
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.summary()) } }
    }

    suspend fun summariesForConcept(concept: MathsConcept): List<MathsLessonSummary> = withContext(Dispatchers.IO) {
        val terms = (listOf(concept.title) + concept.subtopics).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (terms.isEmpty()) return@withContext emptyList()
        val clauses = terms.flatMap { listOf("chapter LIKE ?", "topic LIKE ?", "subtopic LIKE ?", "search_text LIKE ?") }
        val args = terms.flatMap { term ->
            val like = "%${term.lowercase()}%"
            listOf(like, like, like, like)
        }
        database.readableDatabase.query(
            "lessons",
            arrayOf("id", "class_level", "chapter", "topic", "subtopic"),
            clauses.joinToString(" OR "),
            args.toTypedArray(),
            null,
            null,
            "class_level COLLATE NOCASE, chapter COLLATE NOCASE, topic COLLATE NOCASE, subtopic COLLATE NOCASE",
            "500",
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.summary()) } }
    }

    suspend fun summariesForIds(ids: List<String>): List<MathsLessonSummary> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        database.readableDatabase.rawQuery(
            "SELECT id, class_level, chapter, topic, subtopic FROM lessons WHERE id IN ($placeholders)",
            ids.toTypedArray(),
        ).use { cursor ->
            val byId = buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.summary())
            }
            ids.mapNotNull(byId::get)
        }
    }

    suspend fun lesson(id: String): MathsLesson? = withContext(Dispatchers.IO) {
        database.readableDatabase.query("lessons", lessonColumns, "id=?", arrayOf(id), null, null, null, "1").use { cursor ->
            cursor.takeIf(Cursor::moveToFirst)?.lesson()
        }
    }

    suspend fun progress(): List<MathsLessonProgress> = withContext(Dispatchers.IO) {
        database.readableDatabase.query(
            "lesson_progress",
            progressColumns,
            null,
            null,
            null,
            null,
            "updated_at DESC",
            null,
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.progress()) } }
    }

    suspend fun markViewed(lessonId: String) = withContext(Dispatchers.IO) {
        database.writableDatabase.upsertProgress(lessonId) { previous ->
            previous.copy(lastViewedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun toggleSaved(lessonId: String): MathsLessonProgress = withContext(Dispatchers.IO) {
        database.writableDatabase.upsertProgress(lessonId) { previous ->
            previous.copy(saved = !previous.saved, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun markCompleted(lessonId: String): MathsLessonProgress = withContext(Dispatchers.IO) {
        database.writableDatabase.upsertProgress(lessonId) { previous ->
            previous.copy(completed = true, attempts = previous.attempts + 1, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun recordHint(lessonId: String): MathsLessonProgress = withContext(Dispatchers.IO) {
        database.writableDatabase.upsertProgress(lessonId) { previous ->
            previous.copy(hintsUsed = previous.hintsUsed + 1, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun upsertLessons(lessons: List<MathsLesson>) = withContext(Dispatchers.IO) {
        database.writableDatabase.transaction {
            lessons.forEach { lesson ->
                insertWithOnConflict("lessons", null, lesson.values(), SQLiteDatabase.CONFLICT_REPLACE)
            }
        }
    }

    private fun replaceBundledContent(lessons: List<MathsLesson>, concepts: List<BundledConcept>, seedHash: String) {
        database.writableDatabase.transaction {
            delete("lessons", null, null)
            delete("concepts", null, null)
            lessons.forEach { lesson ->
                insertWithOnConflict("lessons", null, lesson.values(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            concepts.forEach { concept ->
                insertWithOnConflict("concepts", null, concept.values(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            rebuildLearningSearchIndex(lessons, concepts)
            putMetadata("seed_hash", seedHash)
            putMetadata("lesson_count", lessons.size.toString())
            putMetadata("concept_count", concepts.size.toString())
        }
    }

    private fun SQLiteDatabase.rebuildLearningSearchIndex(lessons: List<MathsLesson>, concepts: List<BundledConcept>) {
        delete("learning_search_index", null, null)
        concepts.forEach { concept ->
            insertWithOnConflict(
                "learning_search_index",
                null,
                ContentValues().apply {
                    put("item_id", "concept:${concept.title}")
                    put("item_type", "concept")
                    put("title", concept.title)
                    put("subtitle", concept.summary)
                    put("concept_title", concept.title)
                    put("search_text", listOf(concept.title, concept.summary, concept.subtopics, concept.levels).joinToString(" ").lowercase())
                    put("updated_at", System.currentTimeMillis())
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
        lessons.forEach { lesson ->
            insertWithOnConflict(
                "learning_search_index",
                null,
                ContentValues().apply {
                    put("item_id", "lesson:${lesson.id}")
                    put("item_type", "lesson")
                    put("title", lesson.subtopic)
                    put("subtitle", listOf(lesson.classLevel, lesson.chapter, lesson.topic).filter(String::isNotBlank).joinToString(" · "))
                    put("lesson_id", lesson.id)
                    put("concept_title", lesson.chapter)
                    put("search_text", listOf(lesson.classLevel, lesson.chapter, lesson.topic, lesson.subtopic, lesson.introduction, lesson.detailedExplanation, lesson.realtimeExamples).joinToString(" ").lowercase())
                    put("updated_at", lesson.updatedAt)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
        lessons.groupBy { listOf(it.classLevel, it.chapter).joinToString("|") }.forEach { (_, grouped) ->
            val first = grouped.first()
            insertWithOnConflict(
                "learning_search_index",
                null,
                ContentValues().apply {
                    put("item_id", "chapter:${first.classLevel}:${first.chapter}")
                    put("item_type", "chapter")
                    put("title", first.chapter)
                    put("subtitle", "${first.classLevel} · ${grouped.size} lessons")
                    put("concept_title", first.chapter)
                    put("search_text", grouped.flatMap { listOf(it.classLevel, it.chapter, it.topic, it.subtopic) }.joinToString(" ").lowercase())
                    put("updated_at", grouped.maxOf { it.updatedAt })
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
        lessons.groupBy { listOf(it.classLevel, it.chapter, it.topic).joinToString("|") }.forEach { (_, grouped) ->
            val first = grouped.first()
            if (first.topic.isNotBlank()) {
                insertWithOnConflict(
                    "learning_search_index",
                    null,
                    ContentValues().apply {
                        put("item_id", "topic:${first.classLevel}:${first.chapter}:${first.topic}")
                        put("item_type", "topic")
                        put("title", first.topic)
                        put("subtitle", "${first.chapter} · ${grouped.size} lessons")
                        put("concept_title", first.chapter)
                        put("search_text", grouped.flatMap { listOf(it.classLevel, it.chapter, it.topic, it.subtopic) }.joinToString(" ").lowercase())
                        put("updated_at", grouped.maxOf { it.updatedAt })
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
        }
    }

    private fun MathsLesson.values() = ContentValues().apply {
        put("id", id)
        put("class_level", classLevel)
        put("chapter", chapter)
        put("topic", topic)
        put("subtopic", subtopic)
        put("introduction", introduction)
        put("detailed_explanation", detailedExplanation)
        put("realtime_examples", realtimeExamples)
        put("simplified_explanation", simplifiedExplanation)
        put("advanced_explanation", advancedExplanation)
        put("practice_prompt", practicePrompt)
        put("content_json", content.toJsonString())
        put("search_text", listOf(classLevel, chapter, topic, subtopic, introduction, detailedExplanation, realtimeExamples, simplifiedExplanation, advancedExplanation, practicePrompt).joinToString(" ").lowercase())
        put("updated_at", updatedAt)
    }

    private fun BundledConcept.values() = ContentValues().apply {
        put("title", title)
        put("icon", icon)
        put("summary", summary)
        put("subtopics", subtopics)
        put("levels", levels)
        put("lesson_count", lessonCount)
    }

    private fun Cursor.summary() = MathsLessonSummary(
        id = getString(getColumnIndexOrThrow("id")),
        classLevel = getString(getColumnIndexOrThrow("class_level")),
        chapter = getString(getColumnIndexOrThrow("chapter")),
        topic = getString(getColumnIndexOrThrow("topic")),
        subtopic = getString(getColumnIndexOrThrow("subtopic")),
    )

    private fun Cursor.lesson() = MathsLesson(
        id = getString(getColumnIndexOrThrow("id")),
        classLevel = getString(getColumnIndexOrThrow("class_level")),
        chapter = getString(getColumnIndexOrThrow("chapter")),
        topic = getString(getColumnIndexOrThrow("topic")),
        subtopic = getString(getColumnIndexOrThrow("subtopic")),
        introduction = getString(getColumnIndexOrThrow("introduction")),
        detailedExplanation = getString(getColumnIndexOrThrow("detailed_explanation")),
        realtimeExamples = getString(getColumnIndexOrThrow("realtime_examples")),
        simplifiedExplanation = getString(getColumnIndexOrThrow("simplified_explanation")),
        advancedExplanation = getString(getColumnIndexOrThrow("advanced_explanation")),
        practicePrompt = getString(getColumnIndexOrThrow("practice_prompt")),
        content = getString(getColumnIndexOrThrow("content_json")).contentFromJsonOrFallback(
            introduction = getString(getColumnIndexOrThrow("introduction")),
            detailedExplanation = getString(getColumnIndexOrThrow("detailed_explanation")),
            realtimeExamples = getString(getColumnIndexOrThrow("realtime_examples")),
            simplifiedExplanation = getString(getColumnIndexOrThrow("simplified_explanation")),
            advancedExplanation = getString(getColumnIndexOrThrow("advanced_explanation")),
            practicePrompt = getString(getColumnIndexOrThrow("practice_prompt")),
        ),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
    )

    private fun Cursor.concept() = MathsConcept(
        title = getString(getColumnIndexOrThrow("title")),
        icon = getString(getColumnIndexOrThrow("icon")),
        summary = getString(getColumnIndexOrThrow("summary")),
        subtopics = getString(getColumnIndexOrThrow("subtopics")).jsonStringList(),
        levels = getString(getColumnIndexOrThrow("levels")).jsonStringList(),
        lessonCount = getInt(getColumnIndexOrThrow("lesson_count")),
    )

    private fun Cursor.progress() = MathsLessonProgress(
        lessonId = getString(getColumnIndexOrThrow("lesson_id")),
        saved = getInt(getColumnIndexOrThrow("saved")) == 1,
        completed = getInt(getColumnIndexOrThrow("completed")) == 1,
        attempts = getInt(getColumnIndexOrThrow("attempts")),
        hintsUsed = getInt(getColumnIndexOrThrow("hints_used")),
        lastViewedAt = getLong(getColumnIndexOrThrow("last_viewed_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
    )

    private fun MathsLessonProgress.values() = ContentValues().apply {
        put("lesson_id", lessonId)
        put("saved", if (saved) 1 else 0)
        put("completed", if (completed) 1 else 0)
        put("attempts", attempts)
        put("hints_used", hintsUsed)
        put("last_viewed_at", lastViewedAt)
        put("updated_at", updatedAt)
    }

    private fun SQLiteDatabase.progressFor(lessonId: String): MathsLessonProgress =
        query("lesson_progress", progressColumns, "lesson_id=?", arrayOf(lessonId), null, null, null, "1").use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.progress()
            } else {
                MathsLessonProgress(lessonId, saved = false, completed = false, attempts = 0, hintsUsed = 0, lastViewedAt = 0L, updatedAt = 0L)
            }
        }

    private fun SQLiteDatabase.upsertProgress(lessonId: String, transform: (MathsLessonProgress) -> MathsLessonProgress): MathsLessonProgress {
        val next = transform(progressFor(lessonId))
        insertWithOnConflict("lesson_progress", null, next.values(), SQLiteDatabase.CONFLICT_REPLACE)
        return next
    }

    private fun ensureLocalProfile(learnerId: String) {
        val now = System.currentTimeMillis()
        database.writableDatabase.insertWithOnConflict(
            "learner_profile",
            null,
            ContentValues().apply {
                put("learner_id", learnerId)
                put("created_at", now)
                put("updated_at", now)
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
        database.writableDatabase.insertWithOnConflict(
            "learning_genome",
            null,
            ContentValues().apply {
                put("learner_id", learnerId)
                put("updated_at", now)
            },
            SQLiteDatabase.CONFLICT_IGNORE,
        )
    }

    private fun saveAssistantMemory(
        prompt: String,
        response: String,
        lessonId: String,
        conceptKey: String,
        learnerId: String,
    ) {
        val now = System.currentTimeMillis()
        database.writableDatabase.insertWithOnConflict(
            "offline_assistant_memory",
            null,
            ContentValues().apply {
                put("memory_id", "$learnerId-assistant-$now")
                put("learner_id", learnerId)
                put("source", "offline-genie")
                put("prompt", prompt)
                put("response", response)
                put("related_lesson_id", lessonId)
                put("related_concept_key", conceptKey)
                put("created_at", now)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun SQLiteDatabase.count(table: String): Int = rawQuery("SELECT COUNT(*) FROM $table", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    private fun SQLiteDatabase.countDistinct(table: String, expression: String): Int = rawQuery("SELECT COUNT(DISTINCT $expression) FROM $table", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    private fun SQLiteDatabase.metadataValue(key: String): String? = query("metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null, "1").use {
        if (it.moveToFirst()) it.getString(0) else null
    }

    private fun SQLiteDatabase.putMetadata(key: String, value: String) {
        insertWithOnConflict("metadata", null, ContentValues().apply {
            put("key", key)
            put("value", value)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private inline fun <T> SQLiteDatabase.useReadable(block: (SQLiteDatabase) -> T): T = block(this)

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private companion object {
        val lessonColumns = arrayOf(
            "id",
            "class_level",
            "chapter",
            "topic",
            "subtopic",
            "introduction",
            "detailed_explanation",
            "realtime_examples",
            "simplified_explanation",
            "advanced_explanation",
            "practice_prompt",
            "content_json",
            "updated_at",
        )

        val progressColumns = arrayOf(
            "lesson_id",
            "saved",
            "completed",
            "attempts",
            "hints_used",
            "last_viewed_at",
            "updated_at",
        )

        fun sha256(text: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun String.jsonStringList(): List<String> = JSONArray(this).let { array ->
            buildList { for (index in 0 until array.length()) add(array.getString(index)) }
        }

        fun String.csvList(): List<String> = split(',').map(String::trim).filter(String::isNotBlank)

        fun String.escapeLikePattern(): String = buildString {
            this@escapeLikePattern.forEach { char ->
                if (char == '%' || char == '_' || char == '\\') append('\\')
                append(char)
            }
        }
    }
}

private fun Context.readBundledLessonsText(): String {
    val gzName = "maths_learn_all_lessons.v2.json.gz"
    val hasStructuredAsset = assets.list("")?.contains(gzName) == true
    return if (hasStructuredAsset) {
        GZIPInputStream(assets.open(gzName)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    } else {
        assets.open("maths_learn_all_lessons.json").bufferedReader().use { it.readText() }
    }
}

private fun JSONObject.lessonContent(): MathsLessonContent = MathsLessonContent(
    introduction = optSections("intro"),
    detailedExplanation = optSections("learn"),
    realtimeExamples = optSections("examples"),
    simplifiedExplanation = optSections("simple"),
    advancedExplanation = optSections("advanced"),
    practicePrompt = optSections("practice"),
)

private fun lessonContentFromLegacyFields(item: JSONObject): MathsLessonContent = MathsLessonContent(
    introduction = listOf(MathsLessonSection(body = item.optString("introduction"))).filterNonBlank(),
    detailedExplanation = listOf(MathsLessonSection(body = item.optString("detailedExplanation"))).filterNonBlank(),
    realtimeExamples = listOf(MathsLessonSection(body = item.optString("realtimeExamples"))).filterNonBlank(),
    simplifiedExplanation = listOf(MathsLessonSection(body = item.optString("simplifiedExplanation"))).filterNonBlank(),
    advancedExplanation = listOf(MathsLessonSection(body = item.optString("advancedExplanation"))).filterNonBlank(),
    practicePrompt = listOf(MathsLessonSection(body = item.optString("practicePrompt"))).filterNonBlank(),
)

private fun JSONObject.optSections(name: String): List<MathsLessonSection> {
    val array = optJSONArray(name) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val section = array.optJSONObject(index) ?: continue
            val bullets = section.optJSONArray("bullets")?.let { bulletArray ->
                buildList {
                    for (bulletIndex in 0 until bulletArray.length()) {
                        bulletArray.optString(bulletIndex).takeIf(String::isNotBlank)?.let(::add)
                    }
                }
            }.orEmpty()
            add(
                MathsLessonSection(
                    title = section.optString("title"),
                    body = section.optString("body"),
                    bullets = bullets,
                ),
            )
        }
    }.filterNonBlank()
}

private fun List<MathsLessonSection>.filterNonBlank(): List<MathsLessonSection> =
    filter { it.title.isNotBlank() || it.body.isNotBlank() || it.bullets.isNotEmpty() }

private fun List<MathsLessonSection>.plainText(): String =
    flatMap { section -> listOf(section.title, section.body) + section.bullets }
        .filter(String::isNotBlank)
        .joinToString(" ")

private fun MathsLessonContent.toJsonString(): String = JSONObject().apply {
    put("intro", introduction.toJsonArray())
    put("learn", detailedExplanation.toJsonArray())
    put("examples", realtimeExamples.toJsonArray())
    put("simple", simplifiedExplanation.toJsonArray())
    put("advanced", advancedExplanation.toJsonArray())
    put("practice", practicePrompt.toJsonArray())
}.toString()

private fun List<MathsLessonSection>.toJsonArray(): JSONArray = JSONArray().also { array ->
    forEach { section ->
        array.put(
            JSONObject().apply {
                if (section.title.isNotBlank()) put("title", section.title)
                if (section.body.isNotBlank()) put("body", section.body)
                if (section.bullets.isNotEmpty()) put("bullets", JSONArray(section.bullets))
            },
        )
    }
}

private fun String.contentFromJsonOrFallback(
    introduction: String,
    detailedExplanation: String,
    realtimeExamples: String,
    simplifiedExplanation: String,
    advancedExplanation: String,
    practicePrompt: String,
): MathsLessonContent {
    if (isNotBlank()) {
        runCatching { return JSONObject(this).lessonContent() }
    }
    return MathsLessonContent(
        introduction = listOf(MathsLessonSection(body = introduction)).filterNonBlank(),
        detailedExplanation = listOf(MathsLessonSection(body = detailedExplanation)).filterNonBlank(),
        realtimeExamples = listOf(MathsLessonSection(body = realtimeExamples)).filterNonBlank(),
        simplifiedExplanation = listOf(MathsLessonSection(body = simplifiedExplanation)).filterNonBlank(),
        advancedExplanation = listOf(MathsLessonSection(body = advancedExplanation)).filterNonBlank(),
        practicePrompt = listOf(MathsLessonSection(body = practicePrompt)).filterNonBlank(),
    )
}
