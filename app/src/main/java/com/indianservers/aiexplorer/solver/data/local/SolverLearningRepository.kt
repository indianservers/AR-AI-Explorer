package com.indianservers.aiexplorer.solver.data.local

import android.content.Context
import com.indianservers.aiexplorer.solver.domain.analytics.SkillMasteryEstimate
import com.indianservers.aiexplorer.solver.domain.analytics.SolverLearningSummary
import org.json.JSONObject

class SolverLearningRepository(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun recordProblem(
        skill: String,
        independentlySolved: Boolean,
        verified: Boolean,
        method: String?,
        timeSpentMillis: Long,
    ) = update(skill) { record ->
        record.copy(
            attempted = record.attempted + 1,
            independentlySolved = record.independentlySolved + if (independentlySolved) 1 else 0,
            verificationSuccesses = record.verificationSuccesses + if (verified) 1 else 0,
            timeSpentMillis = record.timeSpentMillis + timeSpentMillis.coerceAtLeast(0),
            methods = increment(record.methods, method),
        )
    }

    fun recordHint(skill: String) = update(skill) { it.copy(hintsUsed = it.hintsUsed + 1) }

    fun recordIncorrectStep(skill: String, misconception: String?) = update(skill) {
        it.copy(
            incorrectSteps = it.incorrectSteps + 1,
            misconceptions = increment(it.misconceptions, misconception),
        )
    }

    fun recordPractice(skill: String, correct: Boolean) = update(skill) {
        it.copy(
            practiceAttempted = it.practiceAttempted + 1,
            practiceCorrect = it.practiceCorrect + if (correct) 1 else 0,
        )
    }

    fun summary(): SolverLearningSummary = SolverLearningSummary(load().values.sortedBy { it.skill })

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun update(skill: String, transform: (SkillMasteryEstimate) -> SkillMasteryEstimate) {
        val records = load().toMutableMap()
        records[skill] = transform(records[skill] ?: empty(skill))
        save(records)
    }

    private fun load(): Map<String, SkillMasteryEstimate> {
        val root = runCatching { JSONObject(preferences.getString(KEY_DATA, "{}").orEmpty()) }.getOrDefault(JSONObject())
        return root.keys().asSequence().associateWith { skill ->
            val item = root.optJSONObject(skill) ?: JSONObject()
            SkillMasteryEstimate(
                skill,
                item.optInt("attempted"),
                item.optInt("independentlySolved"),
                item.optInt("hintsUsed"),
                item.optInt("incorrectSteps"),
                item.optInt("verificationSuccesses"),
                item.optInt("practiceCorrect"),
                item.optInt("practiceAttempted"),
                item.optLong("timeSpentMillis"),
                map(item.optJSONObject("methods")),
                map(item.optJSONObject("misconceptions")),
            )
        }
    }

    private fun save(records: Map<String, SkillMasteryEstimate>) {
        val root = JSONObject()
        records.forEach { (skill, record) ->
            root.put(skill, JSONObject().apply {
                put("attempted", record.attempted)
                put("independentlySolved", record.independentlySolved)
                put("hintsUsed", record.hintsUsed)
                put("incorrectSteps", record.incorrectSteps)
                put("verificationSuccesses", record.verificationSuccesses)
                put("practiceCorrect", record.practiceCorrect)
                put("practiceAttempted", record.practiceAttempted)
                put("timeSpentMillis", record.timeSpentMillis)
                put("methods", json(record.methods))
                put("misconceptions", json(record.misconceptions))
            })
        }
        preferences.edit().putString(KEY_DATA, root.toString()).apply()
    }

    private fun empty(skill: String) = SkillMasteryEstimate(skill, 0, 0, 0, 0, 0, 0, 0, 0, emptyMap(), emptyMap())

    private fun increment(values: Map<String, Int>, key: String?): Map<String, Int> =
        if (key.isNullOrBlank()) values else values + (key to (values[key] ?: 0) + 1)

    private fun map(json: JSONObject?): Map<String, Int> =
        json?.keys()?.asSequence()?.associateWith(json::optInt).orEmpty()

    private fun json(values: Map<String, Int>) = JSONObject().apply { values.forEach(::put) }

    private companion object {
        const val FILE_NAME = "solver_phase_4_learning"
        const val KEY_DATA = "skill_aggregates"
    }
}

