package com.indianservers.aiexplorer.mathdictionary

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest

data class MathDictionaryTermSummary(
    val id: Int,
    val termKey: String,
    val word: String,
    val category: String,
    val shortDefinition: String,
    val formulaLatex: String,
    val imageAsset: String,
    val iconAsset: String,
    val difficultyLevel: String,
    val firstLetter: String,
    val isFeatured: Boolean,
    val isBookmarked: Boolean,
)

data class MathDictionaryTermDetail(
    val id: Int,
    val termKey: String,
    val word: String,
    val pronunciation: String,
    val category: String,
    val shortDefinition: String,
    val detailedDefinition: String,
    val simpleExample: String,
    val exampleExplanation: String,
    val analogy: String,
    val formulaLatex: String,
    val imageAsset: String,
    val iconAsset: String,
    val difficultyLevel: String,
    val minimumGrade: Int?,
    val firstLetter: String,
    val isBookmarked: Boolean,
    val relatedTerms: List<MathDictionaryRelatedTerm>,
)

data class MathDictionaryRelatedTerm(
    val termKey: String,
    val word: String,
    val relationType: String,
)

data class MathDictionaryStats(
    val termCount: Int,
    val categoryCounts: Map<String, Int>,
)

data class MathDictionarySeedTerm(
    val id: Int,
    val termKey: String,
    val word: String,
    val normalizedWord: String,
    val pronunciation: String,
    val category: String,
    val shortDefinition: String,
    val detailedDefinition: String,
    val simpleExample: String,
    val exampleExplanation: String,
    val analogy: String,
    val formulaLatex: String,
    val imageAsset: String,
    val iconAsset: String,
    val difficultyLevel: String,
    val minimumGrade: Int,
    val firstLetter: String,
    val searchKeywords: String,
    val isFeatured: Boolean,
    val displayOrder: Int,
    val relatedTermKeys: List<String>,
)

private class MathDictionaryDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=ON")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS math_dictionary_terms(
                id INTEGER PRIMARY KEY NOT NULL,
                term_key TEXT NOT NULL UNIQUE,
                word TEXT NOT NULL,
                normalized_word TEXT NOT NULL UNIQUE,
                pronunciation TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL,
                short_definition TEXT NOT NULL,
                detailed_definition TEXT NOT NULL,
                simple_example TEXT NOT NULL,
                example_explanation TEXT NOT NULL,
                analogy TEXT NOT NULL,
                formula_latex TEXT NOT NULL DEFAULT '',
                image_asset TEXT NOT NULL DEFAULT '',
                icon_asset TEXT NOT NULL DEFAULT '',
                difficulty_level TEXT NOT NULL,
                minimum_grade INTEGER,
                first_letter TEXT NOT NULL,
                search_keywords TEXT NOT NULL DEFAULT '',
                is_featured INTEGER NOT NULL DEFAULT 0,
                display_order INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_math_dictionary_normalized_word ON math_dictionary_terms(normalized_word)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_math_dictionary_first_letter ON math_dictionary_terms(first_letter)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_math_dictionary_category ON math_dictionary_terms(category)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_math_dictionary_difficulty ON math_dictionary_terms(difficulty_level)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS math_dictionary_related_terms(
                term_id INTEGER NOT NULL,
                related_term_id INTEGER NOT NULL,
                relation_type TEXT NOT NULL DEFAULT 'related',
                display_order INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(term_id, related_term_id, relation_type),
                FOREIGN KEY(term_id) REFERENCES math_dictionary_terms(id) ON DELETE CASCADE,
                FOREIGN KEY(related_term_id) REFERENCES math_dictionary_terms(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_math_dictionary_related_term ON math_dictionary_related_terms(related_term_id)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS math_dictionary_bookmarks(
                term_id INTEGER PRIMARY KEY NOT NULL,
                is_bookmarked INTEGER NOT NULL DEFAULT 0,
                bookmarked_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(term_id) REFERENCES math_dictionary_terms(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS math_dictionary_metadata(
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
            """.trimIndent(),
        )
    }

    companion object {
        const val DB_NAME = "math-dictionary.db"
        const val DB_VERSION = 1
    }
}

class MathDictionaryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = MathDictionaryDb(appContext)

    suspend fun seedIfNeeded(): MathDictionaryStats = withContext(Dispatchers.IO) {
        val json = appContext.assets.open(SEED_ASSET).bufferedReader().use { it.readText() }
        val seedHash = sha256(json)
        val terms = parseSeedTerms(json)
        require(terms.size >= 600) { "Math dictionary seed must contain at least 600 terms." }
        require(terms.map { it.termKey }.distinct().size == terms.size) { "Duplicate math dictionary term_key values found." }
        require(terms.map { it.normalizedWord }.distinct().size == terms.size) { "Duplicate math dictionary normalized words found." }
        val keySet = terms.mapTo(linkedSetOf()) { it.termKey }
        require(terms.all { term -> term.relatedTermKeys.all { it in keySet } }) { "Dictionary related term key is missing." }

        database.writableDatabase.transaction {
            execSQL("PRAGMA foreign_keys=ON")
            if (metadataValue("seed_hash") != seedHash || count("math_dictionary_terms") != terms.size) {
                seedTerms(terms)
                putMetadata("seed_hash", seedHash)
                putMetadata("seed_count", terms.size.toString())
            }
        }
        stats()
    }

    suspend fun stats(): MathDictionaryStats = withContext(Dispatchers.IO) {
        database.readableDatabase.query(
            "math_dictionary_terms",
            arrayOf("category", "COUNT(*) AS count"),
            null,
            null,
            "category",
            null,
            "category ASC",
        ).use { cursor ->
            val counts = linkedMapOf<String, Int>()
            while (cursor.moveToNext()) counts[cursor.getString(0)] = cursor.getInt(1)
            MathDictionaryStats(counts.values.sum(), counts)
        }
    }

    suspend fun searchSummaries(
        query: String,
        firstLetter: String?,
        category: String?,
        difficulty: String?,
        bookmarksOnly: Boolean,
    ): List<MathDictionaryTermSummary> = withContext(Dispatchers.IO) {
        val filters = mutableListOf<String>()
        val args = mutableListOf<String>()
        val normalized = MathDictionarySearch.normalize(query)
        if (normalized.isNotBlank()) {
            filters += "(normalized_word LIKE ? OR search_keywords LIKE ? OR short_definition LIKE ?)"
            val like = "%$normalized%"
            args += like
            args += like
            args += like
        }
        if (!firstLetter.isNullOrBlank()) {
            filters += "first_letter=?"
            args += firstLetter
        }
        if (!category.isNullOrBlank()) {
            filters += "category=?"
            args += category
        }
        if (!difficulty.isNullOrBlank()) {
            filters += "difficulty_level=?"
            args += difficulty
        }
        if (bookmarksOnly) filters += "COALESCE(bookmarks.is_bookmarked, 0)=1"
        val where = filters.takeIf { it.isNotEmpty() }?.joinToString(" AND ")
        database.readableDatabase.rawQuery(
            """
            SELECT terms.id, term_key, word, category, short_definition, formula_latex, image_asset,
                   icon_asset, difficulty_level, first_letter, is_featured,
                   COALESCE(bookmarks.is_bookmarked, 0) AS is_bookmarked
            FROM math_dictionary_terms terms
            LEFT JOIN math_dictionary_bookmarks bookmarks ON bookmarks.term_id = terms.id
            ${where?.let { "WHERE $it" }.orEmpty()}
            ORDER BY normalized_word ASC
            """.trimIndent(),
            args.toTypedArray(),
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.toSummary()) } }
    }

    suspend fun loadTerm(termKey: String): MathDictionaryTermDetail? = withContext(Dispatchers.IO) {
        database.readableDatabase.rawQuery(
            """
            SELECT terms.*, COALESCE(bookmarks.is_bookmarked, 0) AS is_bookmarked
            FROM math_dictionary_terms terms
            LEFT JOIN math_dictionary_bookmarks bookmarks ON bookmarks.term_id = terms.id
            WHERE term_key=?
            LIMIT 1
            """.trimIndent(),
            arrayOf(termKey),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            cursor.toDetail(loadRelated(cursor.getInt(cursor.getColumnIndexOrThrow("id"))))
        }
    }

    suspend fun setBookmarked(termId: Int, bookmarked: Boolean) = withContext(Dispatchers.IO) {
        database.writableDatabase.transaction {
            insertWithOnConflict(
                "math_dictionary_bookmarks",
                null,
                ContentValues().apply {
                    put("term_id", termId)
                    put("is_bookmarked", if (bookmarked) 1 else 0)
                    put("bookmarked_at", if (bookmarked) System.currentTimeMillis() else 0L)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    private fun SQLiteDatabase.seedTerms(terms: List<MathDictionarySeedTerm>) {
        val now = System.currentTimeMillis()
        terms.forEach { term ->
            insertWithOnConflict("math_dictionary_terms", null, term.values(now), SQLiteDatabase.CONFLICT_REPLACE)
        }
        delete("math_dictionary_related_terms", null, null)
        val idsByKey = terms.associate { it.termKey to it.id }
        terms.forEach { term ->
            term.relatedTermKeys.distinct().forEachIndexed { index, relatedKey ->
                val relatedId = idsByKey[relatedKey] ?: return@forEachIndexed
                insertWithOnConflict(
                    "math_dictionary_related_terms",
                    null,
                    ContentValues().apply {
                        put("term_id", term.id)
                        put("related_term_id", relatedId)
                        put("relation_type", "related")
                        put("display_order", index)
                    },
                    SQLiteDatabase.CONFLICT_IGNORE,
                )
            }
        }
    }

    private fun loadRelated(termId: Int): List<MathDictionaryRelatedTerm> =
        database.readableDatabase.rawQuery(
            """
            SELECT related.term_key, related.word, rel.relation_type
            FROM math_dictionary_related_terms rel
            INNER JOIN math_dictionary_terms related ON related.id = rel.related_term_id
            WHERE rel.term_id=?
            ORDER BY rel.display_order ASC, related.normalized_word ASC
            """.trimIndent(),
            arrayOf(termId.toString()),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(MathDictionaryRelatedTerm(cursor.getString(0), cursor.getString(1), cursor.getString(2)))
                }
            }
        }

    private fun SQLiteDatabase.count(table: String): Int = rawQuery("SELECT COUNT(*) FROM $table", null).use {
        if (it.moveToFirst()) it.getInt(0) else 0
    }

    private fun SQLiteDatabase.metadataValue(key: String): String? =
        query("math_dictionary_metadata", arrayOf("value"), "key=?", arrayOf(key), null, null, null, "1").use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun SQLiteDatabase.putMetadata(key: String, value: String) {
        insertWithOnConflict(
            "math_dictionary_metadata",
            null,
            ContentValues().apply {
                put("key", key)
                put("value", value)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    companion object {
        const val SEED_ASSET = "math_dictionary_terms.json"

        fun parseSeedTerms(json: String): List<MathDictionarySeedTerm> {
            val array = JSONArray(json)
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val related = item.optJSONArray("related_term_keys")
                    add(
                        MathDictionarySeedTerm(
                            id = item.getInt("id"),
                            termKey = item.getString("term_key"),
                            word = item.getString("word"),
                            normalizedWord = item.getString("normalized_word"),
                            pronunciation = item.optString("pronunciation"),
                            category = item.getString("category"),
                            shortDefinition = item.getString("short_definition"),
                            detailedDefinition = item.getString("detailed_definition"),
                            simpleExample = item.getString("simple_example"),
                            exampleExplanation = item.getString("example_explanation"),
                            analogy = item.optString("analogy"),
                            formulaLatex = item.optString("formula_latex"),
                            imageAsset = item.optString("image_asset"),
                            iconAsset = item.optString("icon_asset"),
                            difficultyLevel = item.getString("difficulty_level"),
                            minimumGrade = item.optInt("minimum_grade", 0),
                            firstLetter = item.getString("first_letter"),
                            searchKeywords = item.optString("search_keywords"),
                            isFeatured = item.optBoolean("is_featured"),
                            displayOrder = item.getInt("display_order"),
                            relatedTermKeys = buildList {
                                if (related != null) for (i in 0 until related.length()) add(related.getString(i))
                            },
                        ),
                    )
                }
            }
        }

        fun normalizeQuery(query: String): String = MathDictionarySearch.normalize(query)
    }
}

private fun MathDictionarySeedTerm.values(now: Long): ContentValues = ContentValues().apply {
    put("id", id)
    put("term_key", termKey)
    put("word", word)
    put("normalized_word", normalizedWord)
    put("pronunciation", pronunciation)
    put("category", category)
    put("short_definition", shortDefinition)
    put("detailed_definition", detailedDefinition)
    put("simple_example", simpleExample)
    put("example_explanation", exampleExplanation)
    put("analogy", analogy)
    put("formula_latex", formulaLatex)
    put("image_asset", imageAsset)
    put("icon_asset", iconAsset)
    put("difficulty_level", difficultyLevel)
    put("minimum_grade", minimumGrade)
    put("first_letter", firstLetter)
    put("search_keywords", searchKeywords)
    put("is_featured", if (isFeatured) 1 else 0)
    put("display_order", displayOrder)
    put("created_at", now)
    put("updated_at", now)
}

private fun Cursor.toSummary(): MathDictionaryTermSummary = MathDictionaryTermSummary(
    id = getInt(0),
    termKey = getString(1),
    word = getString(2),
    category = getString(3),
    shortDefinition = getString(4),
    formulaLatex = getString(5),
    imageAsset = getString(6),
    iconAsset = getString(7),
    difficultyLevel = getString(8),
    firstLetter = getString(9),
    isFeatured = getInt(10) == 1,
    isBookmarked = getInt(11) == 1,
)

private fun Cursor.toDetail(related: List<MathDictionaryRelatedTerm>): MathDictionaryTermDetail = MathDictionaryTermDetail(
    id = getInt(getColumnIndexOrThrow("id")),
    termKey = getString(getColumnIndexOrThrow("term_key")),
    word = getString(getColumnIndexOrThrow("word")),
    pronunciation = getString(getColumnIndexOrThrow("pronunciation")),
    category = getString(getColumnIndexOrThrow("category")),
    shortDefinition = getString(getColumnIndexOrThrow("short_definition")),
    detailedDefinition = getString(getColumnIndexOrThrow("detailed_definition")),
    simpleExample = getString(getColumnIndexOrThrow("simple_example")),
    exampleExplanation = getString(getColumnIndexOrThrow("example_explanation")),
    analogy = getString(getColumnIndexOrThrow("analogy")),
    formulaLatex = getString(getColumnIndexOrThrow("formula_latex")),
    imageAsset = getString(getColumnIndexOrThrow("image_asset")),
    iconAsset = getString(getColumnIndexOrThrow("icon_asset")),
    difficultyLevel = getString(getColumnIndexOrThrow("difficulty_level")),
    minimumGrade = getInt(getColumnIndexOrThrow("minimum_grade")).takeIf { it > 0 },
    firstLetter = getString(getColumnIndexOrThrow("first_letter")),
    isBookmarked = getInt(getColumnIndexOrThrow("is_bookmarked")) == 1,
    relatedTerms = related,
)

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
