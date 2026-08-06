package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryVisualProofEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryPhase3EvidenceTest {
    private val engine = NumberTheoryVisualProofEngine()

    @Test
    fun `modular addition supports positive and negative movement`() {
        val standard = engine.evidence("modular-addition", mapOf("a" to 8, "b" to 7, "modulus" to 12))
        assertEquals(3L, standard.values.getValue("remainder"))
        assertTrue(standard.holds)

        val negative = engine.evidence("modular-addition", mapOf("a" to -9, "b" to 4, "modulus" to 7))
        assertEquals(2L, negative.values.getValue("remainder"))
        assertTrue(negative.holds)
    }

    @Test
    fun `modular multiplication matches repeated jumps`() {
        val evidence = engine.evidence("modular-multiplication", mapOf("a" to 4, "b" to 5, "modulus" to 7))
        assertEquals(listOf(0L, 4L, 1L, 5L, 2L, 6L), evidence.sequence)
        assertEquals(6L, evidence.values.getValue("remainder"))
        assertTrue(evidence.holds)
    }

    @Test
    fun `negative modulo always normalizes into the clock range`() {
        (-40..-1).forEach { value ->
            (2..16).forEach { modulus ->
                val evidence = engine.evidence("negative-modulo", mapOf("value" to value, "modulus" to modulus))
                val remainder = evidence.values.getValue("remainder")
                assertTrue(remainder in 0 until modulus.toLong())
                assertTrue(evidence.holds)
            }
        }
    }

    @Test
    fun `remainder classes contain exactly congruent visible members`() {
        val evidence = engine.evidence("remainder-classes", mapOf("value" to -5, "modulus" to 4))
        assertEquals(3L, evidence.values.getValue("class label"))
        assertEquals(listOf(-9L, -5L, -1L, 3L, 7L, 11L, 15L), evidence.sequence)
        assertTrue(evidence.holds)
    }

    @Test
    fun `product quotient and nested power laws preserve exact values`() {
        val product = engine.evidence("exponent-product", mapOf("base" to 3, "m" to 4, "n" to 2))
        assertEquals(729L, product.values.getValue("value"))
        assertTrue(product.holds)

        val quotient = engine.evidence("exponent-quotient", mapOf("base" to 2, "m" to 8, "n" to 3))
        assertEquals(32L, quotient.values.getValue("value"))
        assertTrue(quotient.holds)

        val nested = engine.evidence("power-of-power", mapOf("base" to 2, "m" to 3, "n" to 4))
        assertEquals(4096L, nested.values.getValue("value"))
        assertTrue(nested.holds)
    }

    @Test
    fun `zero and negative exponents retain nonzero-base conditions`() {
        val zero = engine.evidence("zero-exponent", mapOf("base" to 9))
        assertEquals(1L, zero.values.getValue("value"))
        assertTrue(zero.holds)

        val negative = engine.evidence("negative-exponent", mapOf("base" to 3, "n" to 4))
        assertEquals(81L, negative.values.getValue("denominator value"))
        assertTrue(negative.labels.getValue("formula").contains("1 ÷ 81"))
        assertTrue(negative.holds)
    }

    @Test
    fun `proper divisor sums classify perfect deficient and abundant numbers`() {
        val perfect = engine.evidence("perfect-numbers", mapOf("value" to 28))
        assertEquals(28L, perfect.values.getValue("proper divisor sum"))
        assertEquals("perfect", perfect.labels["prediction"])

        val deficient = engine.evidence("perfect-numbers", mapOf("value" to 13))
        assertEquals("deficient", deficient.labels["prediction"])

        val abundant = engine.evidence("perfect-numbers", mapOf("value" to 12))
        assertEquals("abundant", abundant.labels["prediction"])
    }
}
