package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.mathworkspace.numbertheory.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigInteger as BI

class NumberTheoryEngineTest {
    @Test fun primalityIsDeterministicThroughUnsigned64AndRangeIsSieved() {
        assertTrue(PrimeEngine.test(BI("18446744073709551557")).isPrime)
        assertFalse(PrimeEngine.test(BI("18446744073709551615")).isPrime)
        assertEquals(listOf("2", "3", "5", "7", "11", "13", "17", "19"), PrimeEngine.generateRange(BI("2"), BI("20")).map(BI::toString))
    }

    @Test fun pollardRhoFactorizationAndDivisorStatisticsAreExact() = runBlocking {
        val f = FactorizationEngine.factor(BI("360"))
        assertEquals("2^3 × 3^2 × 5", f.display())
        assertEquals(BI("24"), f.divisorCount)
        assertEquals(BI("810"), f.properDivisorSum)
        assertEquals("Abundant", f.classification)
        assertEquals(24, NumberTheoryEngine.divisors(f).size)
        val semiprime = BI("1000000007") * BI("1000000009")
        assertEquals("1000000007 × 1000000009", FactorizationEngine.factor(semiprime).display())
    }

    @Test fun euclideanAndModularOperationsHandleSignedOperands() {
        val r = EuclideanAlgorithmEngine.solve(BI("-240"), BI("46"))
        assertEquals(BI("2"), r.gcd)
        assertEquals(r.gcd, BI("-240") * r.x + BI("46") * r.y)
        assertEquals(BI("4"), ModularArithmeticEngine.power(BI("3"), BI("100"), BI("7")))
        assertEquals(BI("5"), ModularArithmeticEngine.inverse(BI("3"), BI("7")))
        assertNull(ModularArithmeticEngine.inverse(BI("6"), BI("9")))
    }

    @Test fun linearCongruencesAndGeneralizedCrtHandleMultipleOrNoSolutions() {
        val solutions = CongruenceEngine.solveLinear(BI("4"), BI("8"), BI("12"))
        assertEquals(listOf("2", "5", "8", "11"), solutions.residues.map(BI::toString))
        assertFalse(CongruenceEngine.solveLinear(BI("4"), BI("7"), BI("12")).hasSolution)
        assertEquals(CrtResult(BI("14"), BI("18")), CongruenceEngine.chineseRemainder(listOf(BI("2") to BI("6"), BI("5") to BI("9"))))
        assertNull(CongruenceEngine.chineseRemainder(listOf(BI("1") to BI("4"), BI("2") to BI("6"))))
    }

    @Test fun diophantineReturnsAValidFamilyOfIntegerSolutions() {
        val result = DiophantineEngine.solve(BI("6"), BI("9"), BI("30"))
        assertTrue(result.hasSolution)
        assertEquals(BI("30"), BI("6") * result.x + BI("9") * result.y)
        assertEquals(BI("30"), BI("6") * (result.x + result.stepX) + BI("9") * (result.y + result.stepY))
        assertFalse(DiophantineEngine.solve(BI("6"), BI("9"), BI("31")).hasSolution)
        assertTrue(DiophantineEngine.solve(BI("0"), BI("0"), BI("0")).allIntegerPairs)
    }
}
