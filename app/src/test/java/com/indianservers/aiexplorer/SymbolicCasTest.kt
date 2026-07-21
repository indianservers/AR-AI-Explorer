package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.SymbolicComplex
import com.indianservers.aiexplorer.core.SymbolicVector
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.VariableAssumption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolicCasTest {
    private val cas = SymbolicCasEngine()

    @Test
    fun simplifyNormalizesExactFractionsAndDecimalAgreement() {
        val row = cas.simplify("1/3 + 1/6 + 0*x")

        assertTrue(row.supported)
        assertEquals("1/2", row.exact)
        assertEquals("0.5", row.decimal)
        assertTrue(row.steps.any { it.title.contains("identities", ignoreCase = true) })
    }

    @Test
    fun expandAndSubstituteUseSameTypedAst() {
        val expanded = cas.expand("(x+1)^2")
        val substituted = cas.substitute("(x+1)^2", mapOf("x" to "2"))

        assertEquals("1 + 2*x + x^2", expanded.exact)
        assertEquals("9", substituted.exact)
        assertEquals("9", substituted.decimal)
    }

    @Test
    fun factorFindsSmallRationalRoots() {
        val row = cas.factor("x^2 - 5*x + 6")

        assertTrue(row.supported)
        assertTrue(row.exact.contains("(x - 2)"))
        assertTrue(row.exact.contains("(x - 3)"))
    }

    @Test
    fun unsupportedOperationsRefuseSafely() {
        val row = cas.casRow("sin(x)", "laplace")

        assertFalse(row.supported)
        assertEquals("Not supported", row.exact)
    }

    @Test
    fun complexAndVectorHelpersAreExact() {
        val one = ExactRational.ONE
        val two = ExactRational.of(2)
        val z = SymbolicComplex(one, two) * SymbolicComplex(one, -two)
        val v = SymbolicVector(listOf(one, two, ExactRational.of(3)))
        val w = SymbolicVector(listOf(two, one, ExactRational.ZERO))

        assertEquals("5", z.real.toString())
        assertEquals("0", z.imaginary.toString())
        assertEquals("4", v.dot(w).toString())
        assertEquals(listOf("8/5", "4/5", "0"), v.projectionOnto(w).components.map { it.toString() })
    }

    @Test
    fun assumptionsGuardSignAndCancellationIdentities() {
        val unknownSign = cas.simplify("sqrt(x^2)", MathAssumptionSet())
        val nonNegative = cas.simplify("sqrt(x^2)", MathAssumptionSet().with(VariableAssumption("x", nonNegative = true)))
        val cancellable = cas.simplify("x/x", MathAssumptionSet().with(VariableAssumption("x", nonZero = true)))

        assertEquals("abs(x)", unknownSign.exact)
        assertEquals("x", nonNegative.exact)
        assertEquals("1", cancellable.exact)
        assertTrue(cancellable.assumptions.any { it.contains("x") })
    }

    @Test
    fun decomposesRationalFunctionsIntoVerifiedPartialFractions() {
        val row = cas.partialFractions("(3*x+5)/((x+1)*(x+2))")
        val factored = cas.factor("(x^2-1)/(x^2+3*x+2)")

        assertTrue(row.supported)
        assertTrue(row.exact.contains("2/(x + 1)"))
        assertTrue(row.exact.contains("1/(x + 2)"))
        assertTrue(row.steps.any { it.title == "Verify" })
        assertTrue(factored.supported)
        assertTrue(factored.exact.contains("(x - 1)"))
        assertTrue(factored.exact.contains("(x + 2)"))
    }

    @Test
    fun solvesExactLinearSystemsAndInequalityIntersections() {
        val system = cas.solveSystem(listOf("x+y=5", "x-y=1"), listOf("x", "y"))
        val inequalities = cas.solveInequalities(listOf("2*x+1>=5", "x<5"))

        assertEquals("x = 3, y = 2", system.exact)
        assertEquals("2 <= x < 5", inequalities.exact)
    }

    @Test
    fun differentiatesIntegratesAndEvaluatesRemovableLimits() {
        assertEquals("2*x + 3*x^2", cas.derivative("x^3+x^2").exact)
        val integral = cas.integral("3*x^2+2*x")
        assertTrue(integral.supported)
        assertEquals("x^2 + x^3 + C", integral.exact)
        assertEquals("12", cas.limit("(x^2-36)/(x-6)", approaching = "6").exact)
    }

    @Test
    fun performsExactMatrixWorkflows() {
        assertEquals("-2", cas.determinant("[[1,2],[3,4]]").exact)
        assertEquals("[[1, 0, -1], [0, 1, 2], [0, 0, 0]]", cas.rowReduce("[[1,2,3],[2,4,6],[1,1,1]]").exact)
        assertEquals("3, 2", cas.eigenvalues("[[3,0],[0,2]]").exact)
    }

    @Test
    fun solvesAndChecksInitialLinearOdes() {
        val row = cas.solveOde("y' = 2*y + 4, y(0)=3")

        assertTrue(row.supported)
        assertEquals("y = -2 + 5*exp(2*x)", row.exact)
        assertTrue(row.steps.any { it.title == "Verify" })
    }
}
