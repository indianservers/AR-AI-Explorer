package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathProblemSolver
import com.indianservers.aiexplorer.core.ProblemKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactComputationTest {
    private val solver = MathProblemSolver()

    @Test
    fun calculatesExactFractionsWithoutFloatingPointLoss() {
        val result = solver.solve("Calculate 1/3 + 1/6")
        assertEquals(ProblemKind.ExactArithmetic, result.kind)
        assertEquals("1/2", result.answer)
        assertTrue(result.verification.contains("coprime"))
    }

    @Test
    fun expandsCollectsAndFactorsPolynomials() {
        val expanded = solver.solve("Expand (x + 2)(x - 3)")
        val collected = solver.solve("Simplify 2x + 3x - 4 + 1")
        val factored = solver.solve("Factor x^2 - 5x + 6")
        val rationalRoots = solver.solve("Factor 2x^2 - 5x + 2")

        assertEquals("x^2 − x − 6", expanded.answer)
        assertEquals("5*x − 3", collected.answer)
        assertTrue(factored.answer.contains("(x − 2)"))
        assertTrue(factored.answer.contains("(x − 3)"))
        assertTrue(rationalRoots.answer.contains("(x − 1/2)"))
        assertEquals(ProblemKind.PolynomialAlgebra, factored.kind)
    }

    @Test
    fun computesExactMatrixDeterminantInverseRrefAndTranspose() {
        assertEquals("-2", solver.solve("Determinant [[1,2],[3,4]]").answer)
        assertEquals("[[-2, 1], [3/2, -1/2]]", solver.solve("Inverse [[1,2],[3,4]]").answer)
        assertEquals("[[1, 0, -1], [0, 1, 2], [0, 0, 0]]", solver.solve("RREF [[1,2,3],[2,4,6],[1,1,1]]").answer)
        assertEquals("[[1, 3], [2, 4]]", solver.solve("Transpose [[1,2],[3,4]]").answer)
    }

    @Test
    fun reportsSingularAndDimensionErrorsSafely() {
        val singular = solver.solve("Inverse [[1,2],[2,4]]")
        val incompatible = solver.solve("Convert 2 kg to m")

        assertEquals("No inverse", singular.answer)
        assertFalse(incompatible.supported)
        assertTrue(incompatible.verification.contains("different dimensions"))
    }

    @Test
    fun convertsScaledAndOffsetUnits() {
        assertEquals("5000 m", solver.solve("Convert 5 km to m").answer)
        assertEquals("32 °F", solver.solve("Convert 0 celsius to fahrenheit").answer)
        assertEquals("3.14159265 rad", solver.solve("Convert 180 degrees to rad").answer)
        assertEquals(ProblemKind.UnitConversion, solver.solve("Convert 2 hours to minutes").kind)
    }
}
