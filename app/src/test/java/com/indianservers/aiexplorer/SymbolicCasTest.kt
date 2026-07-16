package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.SymbolicComplex
import com.indianservers.aiexplorer.core.SymbolicVector
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
}
