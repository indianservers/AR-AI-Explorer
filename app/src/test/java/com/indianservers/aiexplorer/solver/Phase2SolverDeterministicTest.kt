package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.engine.Phase2SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2SolverDeterministicTest {
    private val engine = Phase2SolverEngine()

    @Test
    fun fourHundredTwentyAdvancedDeterministicCasesRemainExactAndVerified() {
        val cases = buildList {
            for (a in 1..100) add(Case("gcd($a,${a * 3})", ProblemType.NumberTheory, a.toString()))
            for (a in 1..80) add(Case("lcm($a,${a + 1})", ProblemType.NumberTheory, (a * (a + 1)).toString()))
            for (n in 1..60) add(Case("arithmeticTerm(3,2,$n)", ProblemType.SequenceOrSeries, (3 + 2 * (n - 1)).toString()))
            for (n in 1..40) add(Case("geometricTerm(2,1,$n)", ProblemType.SequenceOrSeries, "2"))
            for (k in 1..40) add(Case("distance(0,0,${3 * k},${4 * k})", ProblemType.CoordinateGeometry, (5 * k).toString()))
            for (k in 1..40) add(Case("midpoint(0,0,${2 * k},${4 * k})", ProblemType.CoordinateGeometry, "($k, ${2 * k})"))
            for (n in 1..60) add(Case("mod(${n * 7 + 3},7)", ProblemType.NumberTheory, "3"))
        }
        assertEquals(420, cases.size)
        cases.forEachIndexed { index, case -> assertExact(index, case, engine.solve(case.input)) }
    }

    @Test
    fun representativePhase2DomainsProduceExpectedExactResults() {
        val cases = listOf(
            Case("sin(30deg)", ProblemType.TrigonometricProblem, "1/2"),
            Case("cos(180deg)", ProblemType.TrigonometricProblem, "-1"),
            Case("tan(45deg)", ProblemType.TrigonometricProblem, "1"),
            Case("distance(0,0,3,4)", ProblemType.CoordinateGeometry, "5"),
            Case("gradient(1,2,3,6)", ProblemType.CoordinateGeometry, "2"),
            Case("matrixMultiply([[1,2]],[[3],[4]])", ProblemType.MatrixOperation, "[[11]]"),
            Case("matrixAdd([[1,2],[3,4]],[[4,3],[2,1]])", ProblemType.MatrixOperation, "[[5, 5], [5, 5]]"),
            Case("primeFactors(84)", ProblemType.NumberTheory, "2*2*3*7"),
            Case("base(FF,16,10)", ProblemType.NumberTheory, "255"),
            Case("arithmeticSum(2,3,5)", ProblemType.SequenceOrSeries, "40"),
            Case("geometricSum(3,2,4)", ProblemType.SequenceOrSeries, "45"),
            Case("infiniteGeometricSum(1,1/2)", ProblemType.SequenceOrSeries, "2"),
            Case("f(x)=x^2+1; f(3)", ProblemType.FunctionAnalysis, "10"),
        )
        cases.forEachIndexed { index, case -> assertExact(index, case, engine.solve(case.input)) }
    }

    @Test
    fun oneHundredVerificationCasesExposeIndependentEvidence() {
        repeat(100) { index ->
            val n = index + 2
            val solution = engine.solve("gcd($n,${n * 5})")
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.verification.checks.isNotEmpty())
            assertTrue(solution.verification.checks.all { it.passed })
            assertTrue(solution.verification.message.contains("deterministic", true))
        }
    }

    @Test
    fun oneHundredInvalidDomainCasesFailWithoutInventedAnswers() {
        val invalid = buildList {
            for (i in 1..25) add("infiniteGeometricSum($i,1)")
            for (i in 1..25) add("base(1${i},1,10)")
            for (i in 1..25) add("mod($i,0)")
            for (i in 1..25) add("matrixMultiply([[1,$i]],[[1,$i]])")
        }
        assertEquals(100, invalid.size)
        invalid.forEach { input ->
            val solution = engine.solve(input)
            assertFalse("$input must fail safely", solution.canPresentAsCorrect)
            assertEquals(null, solution.finalAnswer)
            assertTrue(solution.message.isNotBlank())
        }
    }

    @Test
    fun fiftyAlternativeMethodSelectionsReturnTheSameVerifiedResult() {
        repeat(50) { index ->
            val n = index + 2
            val euclidean = engine.solve("gcd($n,${n * 6})", requestedMethodId = "euclidean")
            val factors = engine.solve("gcd($n,${n * 6})", requestedMethodId = "prime-factors")
            assertEquals(euclidean.exactAnswer, factors.exactAnswer)
            assertEquals(VerificationStatus.Verified, euclidean.verification.status)
            assertEquals(VerificationStatus.Verified, factors.verification.status)
            assertEquals("euclidean", euclidean.selectedMethodId)
            assertEquals("prime-factors", factors.selectedMethodId)
        }
    }

    @Test
    fun allExplanationProfilesKeepMathematicsInvariant() {
        val results = ExplanationProfile.entries.map { profile -> engine.solve("gcd(84,30)", profile = profile) }
        assertEquals(1, results.mapNotNull(SolverSolution::exactAnswer).distinct().size)
        assertEquals(4, results.map(SolverSolution::explanationProfile).distinct().size)
        results.forEach { assertEquals(VerificationStatus.Verified, it.verification.status) }
    }

    private fun assertExact(index: Int, case: Case, solution: SolverSolution) {
        assertTrue("Case $index unsupported: ${case.input} -> ${solution.message}", solution.supported)
        assertEquals("Classification: ${case.input}", case.type, solution.classification.type)
        assertEquals("Answer: ${case.input}", case.answer, solution.exactAnswer)
        assertTrue("Missing steps: ${case.input}", solution.steps.isNotEmpty())
        solution.steps.forEach { assertNotNull(SolverRuleRegistry.get(it.ruleId)) }
        assertEquals("Verification: ${case.input}", VerificationStatus.Verified, solution.verification.status)
        assertTrue(solution.canPresentAsCorrect)
    }

    private data class Case(val input: String, val type: ProblemType, val answer: String)
}
