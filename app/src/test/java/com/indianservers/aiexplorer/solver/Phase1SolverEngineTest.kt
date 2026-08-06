package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.engine.Phase1SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1SolverEngineTest {
    private val engine = Phase1SolverEngine()

    @Test
    fun atLeastOneHundredFiftyDeterministicProblemsAreSolvedAndVerified() {
        val cases = buildList {
            for (i in 1..60) add(Case("$i + ${i + 1} * 2", ProblemType.ArithmeticExpression, (3 * i + 2).toString()))
            for (i in 1..30) {
                val expected = ExactRational.of(i.toLong()) / ExactRational.of((i + 1).toLong()) +
                    ExactRational.of(1L) / ExactRational.of((i + 1).toLong())
                add(Case("$i/${i + 1} + 1/${i + 1}", ProblemType.FractionSimplification, expected.toString()))
            }
            for (target in -20..19) {
                val coefficient = target.absoluteValue + 1
                val constant = target + 7
                val right = coefficient * target + constant
                add(Case("${coefficient}x + $constant = $right", ProblemType.LinearEquation, "x = $target"))
            }
            for (target in -10..9) {
                val right = 3 * target + 4
                add(Case("3x + 4 <= $right", ProblemType.LinearInequality, "x <= $target"))
            }
            for (x in 1..10) {
                val y = x + 2
                add(Case("x + y = ${x + y}; x - y = ${x - y}", ProblemType.SimultaneousLinearEquations, "x = $x, y = $y"))
            }
        }
        assertTrue("Expected at least 150 solver cases", cases.size >= 150)
        cases.forEachIndexed { index, case ->
            assertVerified(index, case, engine.solve(case.input))
        }
    }

    @Test
    fun ratiosPercentRootsAbsoluteValuesAndNegativeInequalitiesAreExact() {
        listOf(
            Case("12:18", ProblemType.RatioOrProportion, "2:3"),
            Case("2:3 = 4:6", ProblemType.RatioOrProportion, "True"),
            Case("x:3 = 4:6", ProblemType.RatioOrProportion, "x = 2"),
            Case("25% * 80", ProblemType.PercentageProblem, "20"),
            Case("25% of 80", ProblemType.PercentageProblem, "20"),
            Case("sqrt(144)", ProblemType.ArithmeticExpression, "12"),
            Case("|-17|", ProblemType.ArithmeticExpression, "17"),
            Case("-2x + 3 <= 9", ProblemType.LinearInequality, "x >= -3"),
            Case("x/3 + 1/2 = 5/6", ProblemType.LinearEquation, "x = 1"),
        ).forEachIndexed { index, case -> assertVerified(index, case, engine.solve(case.input)) }
    }

    @Test
    fun unsupportedAndInvalidProblemsNeverInventAnswers() {
        val cases = listOf("1/0", "sqrt(2)", "x^2 = 4", "sin(x)", "2+")
        cases.forEach { source ->
            val solution = engine.solve(source)
            assertFalse("$source must not be presented as solved", solution.canPresentAsCorrect)
            assertEquals(null, solution.finalAnswer)
            assertTrue(solution.message.isNotBlank())
        }
    }

    @Test
    fun oneLinearEquationWithTwoVariablesReturnsAVerifiedSolutionFamily() {
        val solution = engine.solve("3x + y = 36")

        assertTrue(solution.supported)
        assertEquals(ProblemType.LinearEquation, solution.classification.type)
        assertEquals("x = (36 - y)/3", solution.finalAnswer)
        assertTrue(solution.steps.size >= 2)
        assertEquals(VerificationStatus.Verified, solution.verification.status)
        assertTrue(solution.canPresentAsCorrect)
    }

    @Test
    fun everyGeneratedStepReferencesATraceableRule() {
        val inputs = listOf("2+3*4", "18/24", "25%*80", "3x+5=20", "-2x+3<=9", "x+y=7;x-y=1", "expand 2(x+3)")
        inputs.forEach { input ->
            val solution = engine.solve(input)
            solution.steps.forEach { step ->
                assertNotNull(SolverRuleRegistry.get(step.ruleId))
                assertTrue(step.explanation.isNotBlank())
                assertTrue(step.affectedTerms.isNotEmpty())
            }
        }
    }

    private fun assertVerified(index: Int, case: Case, solution: SolverSolution) {
        assertTrue("Case $index was unsupported: ${case.input} -> ${solution.message}", solution.supported)
        assertEquals("Wrong classification for ${case.input}", case.type, solution.classification.type)
        assertEquals("Wrong final answer for ${case.input}", case.answer, solution.finalAnswer)
        assertTrue("No steps for ${case.input}", solution.steps.isNotEmpty())
        assertEquals("Verification failed for ${case.input}", VerificationStatus.Verified, solution.verification.status)
        assertTrue("Result should be presentable for ${case.input}", solution.canPresentAsCorrect)
    }

    private data class Case(val input: String, val type: ProblemType, val answer: String)

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}
