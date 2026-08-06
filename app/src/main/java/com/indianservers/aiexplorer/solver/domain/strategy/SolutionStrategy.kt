package com.indianservers.aiexplorer.solver.domain.strategy

import com.indianservers.aiexplorer.solver.domain.model.AdvancedMathProblem
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution

data class Applicability(
    val applicable: Boolean,
    val score: Int = 0,
    val reason: String,
)

data class SolverContext(
    val explanationProfile: ExplanationProfile,
    val requestedMethodId: String? = null,
    val realDomainOnly: Boolean = true,
)

data class StrategyResult(
    val solution: SolverSolution,
    val recommendedReason: String,
)

interface SolutionStrategy {
    val id: String
    val label: String
    fun isApplicable(problem: AdvancedMathProblem): Applicability
    fun solve(problem: AdvancedMathProblem, context: SolverContext): StrategyResult
}
