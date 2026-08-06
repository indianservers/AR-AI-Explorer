package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution

/**
 * Routes secondary-school prose to small curriculum-specific interpreters.
 * These interpreters are deterministic and parameterized; they do not guess
 * when a question omits a required data table.
 */
class SolverSecondaryCurriculumEngine {
    private val class9 = SolverClass9CurriculumEngine()
    private val class10 = SolverClass10CurriculumEngine()
    private val class11 = SolverClass11CurriculumEngine()

    fun solve(source: String, profile: ExplanationProfile): SolverSolution? {
        val normalized = source
            .replace(Regex("""^\s*\d+\s*[.)]\s*"""), "")
            .replace('−', '-')
            .replace('×', '*')
            .replace('÷', '/')
            .replace("⁰", "^0").replace("¹", "^1")
            .replace("²", "^2").replace("³", "^3")
            .replace("⁴", "^4").replace("⁵", "^5")
            .replace("⁶", "^6").replace("⁷", "^7")
            .replace("⁸", "^8").replace("⁹", "^9")
            .replace('√', '√')
            .trim().trimEnd('.')
        return class9.solve(source, normalized, profile)
            ?: class10.solve(source, normalized, profile)
            ?: class11.solve(source, normalized, profile)
    }
}
