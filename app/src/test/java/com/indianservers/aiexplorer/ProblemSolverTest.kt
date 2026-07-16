package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathProblemSolver
import com.indianservers.aiexplorer.core.ProblemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemSolverTest {
    private val solver = MathProblemSolver()

    @Test
    fun solvesAndVerifiesLinearEquation() {
        val result = solver.solve("Solve 2x + 3 = 11")

        assertEquals(ProblemKind.LinearEquation, result.kind)
        assertEquals("x = 4", result.answer)
        assertTrue(result.supported)
        assertTrue(result.steps.size >= 3)
        assertTrue(result.verification.contains("difference = 0"))
    }

    @Test
    fun solvesQuadraticWithTwoRoots() {
        val result = solver.solve("x^2 - 5x + 6 = 0")

        assertEquals(ProblemKind.QuadraticEquation, result.kind)
        assertTrue(result.answer.contains("3"))
        assertTrue(result.answer.contains("2"))
        assertTrue(result.verification.contains("0 and 0"))
    }

    @Test
    fun solvesTwoVariableSystem() {
        val result = solver.solve("Solve system: 2x + y = 5; x - y = 1")

        assertEquals(ProblemKind.LinearSystem, result.kind)
        assertEquals("x = 2, y = 1", result.answer)
        assertTrue(result.verification.contains("0 and 0"))
    }

    @Test
    fun solvesLinearAndQuadraticInequalitiesWithSignCharts() {
        val linear = solver.solve("Solve -2x + 6 >= 0")
        val quadratic = solver.solve("Solve x^2 - 5x + 6 <= 0")

        assertEquals(ProblemKind.Inequality, linear.kind)
        assertEquals("x <= 3", linear.answer)
        assertTrue(linear.steps.any { it.title.contains("boundary", ignoreCase = true) })
        assertEquals(ProblemKind.Inequality, quadratic.kind)
        assertEquals("2 <= x <= 3", quadratic.answer)
        assertTrue(quadratic.steps.any { it.title == "Sign chart" })
    }

    @Test
    fun solvesArithmeticAndGeometricSeriesStepByStep() {
        val arithmetic = solver.solve("Arithmetic series a=3 d=2 n=10 sum")
        val geometric = solver.solve("Geometric sequence a=5 r=2 n=6")

        assertEquals(ProblemKind.SequenceSeries, arithmetic.kind)
        assertEquals("S10 = 120", arithmetic.answer)
        assertTrue(arithmetic.steps.any { it.expression.contains("S_n") })
        assertEquals(ProblemKind.SequenceSeries, geometric.kind)
        assertEquals("a6 = 160", geometric.answer)
        assertTrue(geometric.verification.contains("times the previous"))
    }

    @Test
    fun differentiatesPolynomialAndChecksSlope() {
        val result = solver.solve("Differentiate x^3 - 4x + 2")

        assertEquals(ProblemKind.Derivative, result.kind)
        assertEquals("3*x^2 - 4", result.answer)
        assertTrue(result.verification.contains("Finite-difference check"))
    }

    @Test
    fun differentiatesTrigProductsChainsAndHigherOrders() {
        val product = solver.solve("Differentiate x*sin(x)")
        val chain = solver.solve("Differentiate sin(x^2)")
        val second = solver.solve("Second derivative of sin(x)")

        assertEquals(ProblemKind.Derivative, product.kind)
        assertTrue(product.answer.contains("sin(x)"))
        assertTrue(product.answer.contains("cos(x)"))
        assertTrue(chain.answer.contains("cos(x^2)"))
        assertEquals("-sin(x)", second.answer)
    }

    @Test
    fun differentiatesQuotientsGeneralPowersAndInverseTrig() {
        val quotient = solver.solve("Differentiate sin(x)/x")
        val generalPower = solver.solve("Differentiate x^x")
        val inverseTrig = solver.solve("Differentiate asin(x)")

        assertTrue(quotient.answer.contains("x^2"))
        assertTrue(quotient.steps.any { it.expression.contains("Quotient rule") })
        assertTrue(generalPower.answer.contains("ln(x)"))
        assertTrue(generalPower.answer.contains("x^x"))
        assertTrue(inverseTrig.answer.contains("sqrt(1 - x^2)"))
    }

    @Test
    fun calculatesPartialDerivative() {
        val result = solver.solve("Partial derivative of x^2*y + sin(y) with respect to x")

        assertEquals(ProblemKind.Derivative, result.kind)
        assertTrue(result.answer.contains("2*x*y"))
    }

    @Test
    fun integratesPolynomialWithConstant() {
        val result = solver.solve("Integrate 6x^2 + 4")

        assertEquals(ProblemKind.Integral, result.kind)
        assertEquals("2*x^3 + 4*x + C", result.answer)
    }

    @Test
    fun integratesTrigAndVerifiedDefiniteForms() {
        val symbolic = solver.solve("Integrate sin(2x) with respect to x")
        val definite = solver.solve("Integrate sin(x) from 0 to pi")

        assertEquals(ProblemKind.Integral, symbolic.kind)
        assertTrue(symbolic.answer.contains("cos(2*x)"))
        assertTrue(symbolic.answer.endsWith("+ C"))
        assertEquals(ProblemKind.Integral, definite.kind)
        assertEquals("2", definite.answer)
        assertTrue(definite.verification.contains("reversed limits"))
    }

    @Test
    fun integratesLogTanAndNonElementaryDefiniteForms() {
        val logarithm = solver.solve("Integrate ln(x)")
        val tangent = solver.solve("Integrate tan(x)")
        val gaussian = solver.solve("Integrate exp(-x^2) from 0 to 1")

        assertTrue(logarithm.answer.contains("x*ln(x) - x"))
        assertTrue(tangent.answer.contains("ln(abs(cos(x)))"))
        assertEquals(.7468241, gaussian.answer.toDouble(), 1e-6)
    }

    @Test
    fun evaluatesNamedFunctionsAndConstants() {
        val result = solver.solve("Evaluate sin(pi/2) + cos(0)")

        assertEquals(ProblemKind.Arithmetic, result.kind)
        assertEquals("2", result.answer)
    }

    @Test
    fun calculatesStatisticsAndPercentage() {
        val statistics = solver.solve("Mean of 4, 7, 7, 10")
        val percentage = solver.solve("15% of 240")

        assertEquals(ProblemKind.Statistics, statistics.kind)
        assertEquals("Mean = 7", statistics.answer)
        assertEquals(ProblemKind.Percentage, percentage.kind)
        assertEquals("36", percentage.answer)
    }

    @Test
    fun refusesUnsupportedQuestionInsteadOfInventingAnswer() {
        val result = solver.solve("Prove every even number greater than two is the sum of two primes")

        assertFalse(result.supported)
        assertEquals(ProblemKind.Unsupported, result.kind)
        assertTrue(result.warnings.isNotEmpty())
    }
}
