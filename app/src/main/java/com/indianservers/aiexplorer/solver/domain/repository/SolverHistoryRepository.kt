package com.indianservers.aiexplorer.solver.domain.repository

import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus

data class SolverHistoryEntry(
    val id: String,
    val originalInput: String,
    val normalizedExpression: String,
    val problemType: ProblemType,
    val finalResult: String,
    val timestamp: Long,
    val stepCount: Int,
    val verificationStatus: VerificationStatus,
)

interface SolverHistoryRepository {
    fun entries(): List<SolverHistoryEntry>
    fun save(entry: SolverHistoryEntry)
    fun delete(id: String)
    fun clear()
}

