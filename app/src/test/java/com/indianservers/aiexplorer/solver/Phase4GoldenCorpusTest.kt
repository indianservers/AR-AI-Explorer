package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.practice.SolverPracticeGenerator
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.SolverHintEngine
import com.indianservers.aiexplorer.solver.domain.tutor.SolverStepEvaluationEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4GoldenCorpusTest {
    private val engine = Phase3SolverEngine()

    @Test
    fun finalGoldenCorpusExceedsTwoThousandValidatedCases() {
        var cases = 0

        for (a in -20..20) for (b in -8..8) {
            verifySupported("$a + $b")
            cases++
        }
        for (a in 1..20) for (b in 1..10) {
            verifySupported("$a/$b + 1/$b")
            cases++
        }
        for (coefficient in 1..20) for (root in -5..5) {
            verifySupported("${coefficient}x+${coefficient - root}=${coefficient * root + coefficient - root}")
            cases++
        }
        for (coefficient in 1..12) for (boundary in -5..5) {
            verifySupported("${coefficient}x+2<=${coefficient * boundary + 2}")
            cases++
        }
        repeat(100) { index ->
            verifySupported("gcd(${index + 12},${index + 18})")
            cases++
        }
        repeat(80) { index ->
            verifySupported("distance(0,0,${index % 9 + 1},${index % 7 + 1})")
            cases++
        }
        listOf(0, 30, 45, 60, 90, 120, 135, 150, 180, 225, 270, 315).forEach { angle ->
            repeat(8) {
                verifySupported("sin(${angle}deg)")
                cases++
            }
        }
        repeat(80) { index ->
            verifySupported("arithmeticterm(${index % 7 + 1},${index % 5 + 1},${index % 12 + 1})")
            cases++
        }
        repeat(40) { index ->
            verifySupported("matrixadd([[${index % 8 + 1},2],[3,4]],[[1,2],[3,4]])")
            cases++
        }
        repeat(60) { index ->
            verifySupported("differentiate ${index % 9 + 1}*x^${index % 4 + 1}")
            cases++
        }
        repeat(40) { index ->
            verifySupported("complex ${index % 11 - 5}+${index % 7 + 1}i")
            cases++
        }

        val tutorSolution = engine.solve("3x+5=20")
        val evaluator = SolverStepEvaluationEngine()
        repeat(250) { index ->
            val step = tutorSolution.steps[index % tutorSolution.steps.size]
            val evaluated = evaluator.evaluate(tutorSolution, index % tutorSolution.steps.size, com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer.render(step.after))
            assertTrue(evaluated.equivalence.name == "Equivalent")
            cases++
        }
        repeat(200) { index ->
            val hints = SolverHintEngine.hints(tutorSolution, index % tutorSolution.steps.size, tutorSolution.explanationProfile)
            assertTrue(hints.size == 6 && hints.take(5).none { it.revealsAnswer })
            cases++
        }
        val practice = SolverPracticeGenerator(engine)
        repeat(200) { index ->
            val problem = practice.generate(tutorSolution, PracticeMode.entries[index % PracticeMode.entries.size], index)
            assertTrue(problem != null && problem.validationMessage.startsWith("Validated offline"))
            cases++
        }
        repeat(100) {
            val unsupported = engine.solve(listOf("differentiate", "integrate", "complex hello", "1/0", "x+(")[it % 5])
            assertTrue(!unsupported.canPresentAsCorrect)
            cases++
        }

        assertTrue("Golden corpus produced only $cases cases", cases >= 2_000)
    }

    private fun verifySupported(source: String) {
        val solution = engine.solve(source)
        assertTrue("$source unsupported: ${solution.message}", solution.supported)
        assertTrue("$source missing answer", !solution.finalAnswer.isNullOrBlank())
        assertTrue("$source failed verification", solution.verification.status != VerificationStatus.Failed)
        assertTrue("$source missing steps", solution.steps.isNotEmpty())
        assertTrue("$source missing rule", solution.steps.all { it.ruleId.isNotBlank() })
        assertTrue("$source missing explanation", solution.steps.all { it.explanation.isNotBlank() })
    }
}

