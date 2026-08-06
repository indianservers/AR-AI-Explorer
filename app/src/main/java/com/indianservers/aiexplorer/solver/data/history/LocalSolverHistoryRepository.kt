package com.indianservers.aiexplorer.solver.data.history

import android.content.Context
import android.util.Base64
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryEntry
import com.indianservers.aiexplorer.solver.domain.repository.SolverHistoryRepository

class LocalSolverHistoryRepository(context: Context) : SolverHistoryRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun entries(): List<SolverHistoryEntry> = preferences.getStringSet(KEY_ENTRIES, emptySet())
        .orEmpty()
        .mapNotNull(::decode)
        .sortedByDescending(SolverHistoryEntry::timestamp)

    override fun save(entry: SolverHistoryEntry) {
        val updated = entries().filterNot { it.id == entry.id }.plus(entry).sortedByDescending { it.timestamp }.take(MAX_ENTRIES)
        preferences.edit().putStringSet(KEY_ENTRIES, updated.mapTo(linkedSetOf(), ::encode)).apply()
    }

    override fun delete(id: String) {
        val updated = entries().filterNot { it.id == id }
        preferences.edit().putStringSet(KEY_ENTRIES, updated.mapTo(linkedSetOf(), ::encode)).apply()
    }

    override fun clear() {
        preferences.edit().remove(KEY_ENTRIES).apply()
    }

    private fun encode(entry: SolverHistoryEntry): String = listOf(
        VERSION,
        field(entry.id),
        field(entry.originalInput),
        field(entry.normalizedExpression),
        entry.problemType.name,
        field(entry.finalResult),
        entry.timestamp.toString(),
        entry.stepCount.toString(),
        entry.verificationStatus.name,
    ).joinToString("|")

    private fun decode(encoded: String): SolverHistoryEntry? = runCatching {
        val fields = encoded.split('|')
        require(fields.size == 9 && fields[0] == VERSION)
        SolverHistoryEntry(
            id = value(fields[1]),
            originalInput = value(fields[2]),
            normalizedExpression = value(fields[3]),
            problemType = ProblemType.valueOf(fields[4]),
            finalResult = value(fields[5]),
            timestamp = fields[6].toLong(),
            stepCount = fields[7].toInt(),
            verificationStatus = VerificationStatus.valueOf(fields[8]),
        )
    }.getOrNull()

    private fun field(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    private fun value(encoded: String): String = String(Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8)

    private companion object {
        const val PREFERENCES = "solver_phase_1_history"
        const val KEY_ENTRIES = "entries"
        const val VERSION = "1"
        const val MAX_ENTRIES = 100
    }
}

