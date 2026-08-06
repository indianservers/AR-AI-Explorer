package com.indianservers.aiexplorer.features.numbertheory.visualproofs

import com.indianservers.aiexplorer.features.numbertheory.visualproofs.calculation.NumberTheoryMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberTheoryMathTest {
    @Test
    fun `closed sums agree with direct sums`() {
        (1..200).forEach { n ->
            assertEquals((1L..n).sum(), NumberTheoryMath.naturalSum(n))
            assertEquals((1..n).sumOf { 2L * it - 1 }, NumberTheoryMath.oddSum(n))
            assertEquals((1..n).sumOf { 2L * it }, NumberTheoryMath.evenSum(n))
        }
    }

    @Test
    fun `arithmetic sequence formula handles positive and negative differences`() {
        assertEquals(45L, NumberTheoryMath.arithmeticSum(5, 2, 5))
        assertEquals(30L, NumberTheoryMath.arithmeticSum(10, -2, 5))
    }

    @Test
    fun `factor pairs multiply back to the source`() {
        listOf(1, 2, 12, 36, 97, 360).forEach { value ->
            assertTrue(NumberTheoryMath.factorPairs(value).all { (a, b) -> a * b == value })
        }
        assertEquals(listOf(1 to 97), NumberTheoryMath.factorPairs(97))
    }

    @Test
    fun `gcd lcm and Euclidean divisions remain exact`() {
        assertEquals(6, NumberTheoryMath.gcd(48, 18))
        assertEquals(144, NumberTheoryMath.lcm(48, 18))
        val steps = NumberTheoryMath.euclideanSteps(252, 105)
        assertEquals(21, steps.last().divisor)
        assertTrue(steps.all { it.dividend == it.quotient * it.divisor + it.remainder })
    }

    @Test
    fun `prime tools return canonical results`() {
        assertEquals(mapOf(2 to 3, 3 to 2, 5 to 1), NumberTheoryMath.primeFactors(360))
        assertEquals(listOf(2, 3, 5, 7, 11, 13, 17, 19), NumberTheoryMath.primesUpTo(20))
    }

    @Test
    fun `normalized modulo supports negative integers`() {
        assertEquals(6, NumberTheoryMath.normalizedMod(-1, 7))
        assertEquals(0, NumberTheoryMath.normalizedMod(-21, 7))
        assertEquals(2, NumberTheoryMath.normalizedMod(44, 7))
    }
}
