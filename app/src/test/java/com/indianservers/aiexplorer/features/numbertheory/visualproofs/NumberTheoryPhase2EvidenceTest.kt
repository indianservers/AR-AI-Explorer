package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryVisualProofEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryPhase2EvidenceTest {
    private val engine = NumberTheoryVisualProofEngine()

    @Test
    fun `new sequence constructions preserve exact totals`() {
        val even = engine.evidence("even-sum", mapOf("n" to 8))
        assertEquals(72L, even.values.getValue("sum"))
        assertEquals(72L, even.sequence.sum())

        val oddBorder = engine.evidence("square-odd-difference", mapOf("n" to 9))
        assertEquals(17L, oddBorder.values.getValue("odd border"))

        val consecutive = engine.evidence("consecutive-integer-sum", mapOf("start" to -3, "count" to 7))
        assertEquals(0L, consecutive.values.getValue("sum"))
        assertTrue(consecutive.holds)
    }

    @Test
    fun `all divisibility visuals agree with direct remainder tests`() {
        (0..999).forEach { value ->
            assertEquals(value % 2 == 0, engine.evidence("divisibility-2", mapOf("value" to value)).labels["prediction"] == "yes")
            assertEquals(value % 9 == 0, engine.evidence("divisibility-9", mapOf("value" to value)).labels["prediction"] == "yes")
            assertEquals(value % 4 == 0, engine.evidence("divisibility-4", mapOf("value" to value)).labels["prediction"] == "yes")
            assertEquals(value % 8 == 0, engine.evidence("divisibility-8", mapOf("value" to value)).labels["prediction"] == "yes")
            assertEquals(value % 11 == 0, engine.evidence("divisibility-11", mapOf("value" to value)).labels["prediction"] == "yes")
        }
    }

    @Test
    fun `five ten and parity rules report the final digit correctly`() {
        listOf(0, 5, 10, 17, 735, 1240).forEach { value ->
            val fiveTen = engine.evidence("divisibility-5-10", mapOf("value" to value))
            assertEquals(value % 5 == 0, fiveTen.values.getValue("divisible by 5") == 1L)
            assertEquals(value % 10 == 0, fiveTen.values.getValue("divisible by 10") == 1L)
            val parity = engine.evidence("parity-last-digit", mapOf("value" to value))
            assertEquals(if (value % 2 == 0) "even" else "odd", parity.labels["prediction"])
        }
    }

    @Test
    fun `factor and gcd constructions preserve their identities`() {
        val grouping = engine.evidence("gcd-grouping", mapOf("a" to 24, "b" to 36))
        assertEquals(12L, grouping.values.getValue("largest group"))

        val product = engine.evidence("gcd-lcm-product", mapOf("a" to 18, "b" to 24))
        assertEquals(432L, product.values.getValue("each product"))
        assertTrue(product.holds)

        val blocks = engine.evidence("prime-building-blocks", mapOf("value" to 84))
        assertEquals(mapOf(2 to 2, 3 to 1, 7 to 1), blocks.primeFactors)
        assertTrue(blocks.holds)
    }

    @Test
    fun `composite square root proof handles composites and primes`() {
        val composite = engine.evidence("composite-sqrt", mapOf("value" to 84))
        assertEquals(2L, composite.values.getValue("small factor"))
        assertTrue(composite.holds)

        val prime = engine.evidence("composite-sqrt", mapOf("value" to 97))
        assertEquals(0L, prime.values.getValue("is composite"))
        assertTrue(prime.holds)
    }

    @Test
    fun `Euclid construction excludes every listed prime`() {
        (1..5).forEach { count ->
            val evidence = engine.evidence("euclid-primes", mapOf("count" to count))
            assertTrue(evidence.holds)
            assertTrue(evidence.values.getValue("new prime factor") !in evidence.sequence)
        }
    }

    @Test
    fun `prime explorations use exact finite data without claiming open results`() {
        val gaps = engine.evidence("prime-gaps", mapOf("limit" to 30))
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29), gaps.primes)
        assertEquals(6L, gaps.values.getValue("largest observed gap"))
        assertTrue(gaps.labels.getValue("reasoningStatus").contains("not a universal theorem"))

        val twins = engine.evidence("twin-primes", mapOf("limit" to 20))
        assertEquals(4L, twins.values.getValue("twin pairs"))
        assertTrue(twins.labels.getValue("reasoningStatus").contains("Open conjecture"))
        assertTrue(twins.labels.getValue("counterexample").contains("cannot prove"))
    }
}
