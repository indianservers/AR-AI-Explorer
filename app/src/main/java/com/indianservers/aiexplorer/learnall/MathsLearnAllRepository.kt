package com.indianservers.aiexplorer.learnall

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

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

private class MathsLearnAllDb(context: Context) : SQLiteOpenHelper(context, "maths-learn-all.db", null, 2) {
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
                search_text TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_lessons_class_chapter_topic ON lessons(class_level, chapter, topic)")
        db.execSQL("CREATE INDEX idx_lessons_search ON lessons(search_text)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE lessons ADD COLUMN topic TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lessons_class_chapter_topic ON lessons(class_level, chapter, topic)")
        }
    }
}

class MathsLearnAllRepository(context: Context) {
    private val database = MathsLearnAllDb(context.applicationContext)
    private val appContext = context.applicationContext

    suspend fun seedBundledLessons(): Int = withContext(Dispatchers.IO) {
        val lessons = appContext.assets.open("maths_learn_all_lessons.json").bufferedReader().use { reader ->
            val array = JSONArray(reader.readText())
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        MathsLesson(
                            id = item.getString("id"),
                            classLevel = item.getString("classLevel"),
                            chapter = item.getString("chapter"),
                            topic = item.getString("topic"),
                            subtopic = item.getString("subtopic"),
                            introduction = item.getString("introduction"),
                            detailedExplanation = item.getString("detailedExplanation"),
                            realtimeExamples = item.getString("realtimeExamples"),
                            simplifiedExplanation = item.optString("simplifiedExplanation"),
                            advancedExplanation = item.optString("advancedExplanation"),
                            practicePrompt = item.optString("practicePrompt"),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }
        upsertLessons(lessons)
        lessons.size
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

    suspend fun lesson(id: String): MathsLesson? = withContext(Dispatchers.IO) {
        database.readableDatabase.query("lessons", lessonColumns, "id=?", arrayOf(id), null, null, null, "1").use { cursor ->
            cursor.takeIf(Cursor::moveToFirst)?.lesson()
        }
    }

    suspend fun upsertLessons(lessons: List<MathsLesson>) = withContext(Dispatchers.IO) {
        database.writableDatabase.transaction {
            lessons.forEach { lesson ->
                insertWithOnConflict("lessons", null, lesson.values(), SQLiteDatabase.CONFLICT_REPLACE)
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
        put("search_text", listOf(classLevel, chapter, topic, subtopic, introduction, detailedExplanation, realtimeExamples, simplifiedExplanation, advancedExplanation, practicePrompt).joinToString(" ").lowercase())
        put("updated_at", updatedAt)
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
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
    )

    private fun SQLiteDatabase.count(table: String): Int = rawQuery("SELECT COUNT(*) FROM $table", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    private fun SQLiteDatabase.countDistinct(table: String, expression: String): Int = rawQuery("SELECT COUNT(DISTINCT $expression) FROM $table", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
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
            "updated_at",
        )
    }
}
