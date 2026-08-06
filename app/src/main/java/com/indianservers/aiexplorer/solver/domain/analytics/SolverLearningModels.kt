package com.indianservers.aiexplorer.solver.domain.analytics

data class SkillMasteryEstimate(
    val skill: String,
    val attempted: Int,
    val independentlySolved: Int,
    val hintsUsed: Int,
    val incorrectSteps: Int,
    val verificationSuccesses: Int,
    val practiceCorrect: Int,
    val practiceAttempted: Int,
    val timeSpentMillis: Long,
    val methods: Map<String, Int>,
    val misconceptions: Map<String, Int>,
) {
    val estimatePercent: Int
        get() {
            if (attempted + practiceAttempted == 0) return 0
            val solveRate = independentlySolved.toDouble() / attempted.coerceAtLeast(1)
            val practiceRate = practiceCorrect.toDouble() / practiceAttempted.coerceAtLeast(1)
            val verificationRate = verificationSuccesses.toDouble() / attempted.coerceAtLeast(1)
            val hintPenalty = (hintsUsed.toDouble() / attempted.coerceAtLeast(1)).coerceAtMost(1.0)
            val errorPenalty = (incorrectSteps.toDouble() / (attempted + practiceAttempted).coerceAtLeast(1)).coerceAtMost(1.0)
            return ((.40 * solveRate + .30 * practiceRate + .20 * verificationRate + .10 * (1 - hintPenalty) - .10 * errorPenalty) * 100)
                .toInt().coerceIn(0, 100)
        }

    val trend: String
        get() = when {
            estimatePercent >= 75 && incorrectSteps <= independentlySolved -> "improving"
            hintsUsed + incorrectSteps > independentlySolved + practiceCorrect -> "needs review"
            else -> "developing"
        }
}

data class SolverLearningSummary(
    val skills: List<SkillMasteryEstimate>,
) {
    val totalAttempted: Int get() = skills.sumOf { it.attempted }
    val improving: List<String> get() = skills.filter { it.trend == "improving" }.map { it.skill }
    val needsReview: List<String> get() = skills.filter { it.trend == "needs review" }.map { it.skill }
}

