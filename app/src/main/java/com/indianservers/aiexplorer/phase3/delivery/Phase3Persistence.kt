package com.indianservers.aiexplorer.phase3.delivery

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import kotlinx.coroutines.flow.first

private val Context.phase3PlatformDataStore by preferencesDataStore("phase3_platform")

enum class PlatformEntityType { EXAM_SESSION, SYNC_OPERATION, CLASSROOM, ASSIGNMENT, SUBMISSION, FEEDBACK }

data class LocalPlatformRecord(
    val id: String,
    val type: PlatformEntityType,
    val version: Int,
    val updatedAt: Instant,
    val payload: String,
)

data class Phase3LocalSnapshot(val schemaVersion: Int = CURRENT_SCHEMA, val records: List<LocalPlatformRecord>) {
    companion object { const val CURRENT_SCHEMA = 1 }
}

interface Phase3Persistence {
    suspend fun load(): Phase3LocalSnapshot
    suspend fun save(snapshot: Phase3LocalSnapshot)
}

class DataStorePhase3Persistence(private val context: Context) : Phase3Persistence {
    private val schemaKey = intPreferencesKey("schema_version")
    private val recordsKey = stringPreferencesKey("records")

    override suspend fun load(): Phase3LocalSnapshot {
        val preferences = context.phase3PlatformDataStore.data.first()
        return Phase3RecordCodec.decode(preferences[schemaKey] ?: 1, preferences[recordsKey].orEmpty())
    }

    override suspend fun save(snapshot: Phase3LocalSnapshot) {
        context.phase3PlatformDataStore.edit { preferences ->
            preferences[schemaKey] = snapshot.schemaVersion
            preferences[recordsKey] = Phase3RecordCodec.encode(snapshot.records)
        }
    }
}

class LocalPlatformRepository(private val persistence: Phase3Persistence) {
    suspend fun upsert(record: LocalPlatformRecord) {
        val snapshot = persistence.load()
        val existing = snapshot.records.singleOrNull { it.id == record.id && it.type == record.type }
        require(existing == null || record.version >= existing.version) { "Stale local platform write" }
        persistence.save(snapshot.copy(records = snapshot.records.filterNot { it.id == record.id && it.type == record.type } + record))
    }

    suspend fun records(type: PlatformEntityType): List<LocalPlatformRecord> =
        persistence.load().records.filter { it.type == type }.sortedByDescending { it.updatedAt }
}

object Phase3RecordCodec {
    private fun protect(value: String) = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun restore(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    fun encode(records: List<LocalPlatformRecord>): String = records.joinToString("\n") { record ->
        listOf(record.type.name, protect(record.id), record.version, record.updatedAt.toEpochMilli(), protect(record.payload)).joinToString("|")
    }

    fun decode(schemaVersion: Int, raw: String): Phase3LocalSnapshot {
        val records = raw.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            runCatching {
                val parts = line.split('|', limit = 5)
                LocalPlatformRecord(restore(parts[1]), PlatformEntityType.valueOf(parts[0]), parts[2].toInt(), Instant.ofEpochMilli(parts[3].toLong()), restore(parts[4]))
            }.getOrNull()
        }.toList()
        return Phase3LocalSnapshot(schemaVersion.coerceAtLeast(1), records)
    }
}
