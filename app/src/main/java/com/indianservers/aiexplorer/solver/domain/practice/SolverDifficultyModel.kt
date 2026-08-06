package com.indianservers.aiexplorer.solver.domain.practice

import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.tutor.DifficultyAssessment
import com.indianservers.aiexplorer.solver.domain.tutor.DifficultyLevel

object SolverDifficultyModel {
    fun assess(source: String, solution: SolverSolution? = null, proofRequired: Boolean = false): DifficultyAssessment {
        val factors = linkedMapOf<String, Int>()
        val operations = Regex("""[+\-*/^=<>]""").findAll(source).count()
        val depth = groupingDepth(source)
        val variables = Regex("""\b[A-Za-z]\b""").findAll(source).map { it.value }.toSet().size
        factors["operations"] = operations.coerceAtMost(8)
        factors["expression depth"] = depth * 2
        factors["fractions"] = if ('/' in source) 3 else 0
        factors["radicals"] = if ("sqrt" in source.lowercase() || "root" in source.lowercase()) 4 else 0
        factors["variables"] = (variables - 1).coerceAtLeast(0) * 2
        factors["domain restrictions"] = solution?.restrictions?.size?.times(2) ?: 0
        factors["solution branches"] = if (solution?.finalAnswer.orEmpty().contains("+/-") || solution?.finalAnswer.orEmpty().contains("±")) 3 else 0
        factors["transformations"] = (solution?.steps?.size ?: 1).coerceAtMost(8)
        factors["theorem selection"] = if (solution?.ruleCitations?.size.orZero() > 2) 3 else 0
        factors["proof requirement"] = if (proofRequired) 8 else 0
        val score = factors.values.sum().coerceIn(0, 60)
        val level = when {
            proofRequired && score >= 32 -> DifficultyLevel.ProofOriented
            proofRequired -> DifficultyLevel.OlympiadStyle
            source.contains("differentiate", true) || source.contains("integrate", true) || source.contains("complex", true) -> DifficultyLevel.University
            score <= 5 -> DifficultyLevel.Foundation
            score <= 11 -> DifficultyLevel.Beginner
            score <= 20 -> DifficultyLevel.Intermediate
            else -> DifficultyLevel.Advanced
        }
        return DifficultyAssessment(
            level,
            score,
            factors.filterValues { it > 0 },
            "Estimated from structure, transformations and domain demands; large numbers alone do not increase the level.",
        )
    }

    private fun groupingDepth(source: String): Int {
        var depth = 0
        var maximum = 0
        source.forEach {
            if (it == '(' || it == '[' || it == '{') {
                depth++
                maximum = maxOf(maximum, depth)
            } else if (it == ')' || it == ']' || it == '}') {
                depth = (depth - 1).coerceAtLeast(0)
            }
        }
        return maximum
    }

    private fun Int?.orZero() = this ?: 0
}

