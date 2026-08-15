package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.VerificationStrength
import com.indianservers.aiexplorer.solver.domain.verification.SolverAdvancedVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverAdvancedVerifierTest {
    private val verifier = SolverAdvancedVerifier()

    @Test
    fun derivativeUsesIndependentFiniteDifferences() {
        val evidence = verifier.verify("differentiate x^3+2*x", "3*x^2+2", "kernel")
        assertEquals(VerificationStatus.Verified, evidence.result.status)
        assertEquals(VerificationStrength.NumericallyVerified, evidence.strength)
        assertTrue(evidence.result.checks.size >= 3)
    }

    @Test
    fun antiderivativeUsesReverseDifferentiation() {
        val evidence = verifier.verify("integrate 3*x^2+cos(x)", "x^3+sin(x) + C", "kernel")
        assertEquals(VerificationStatus.Verified, evidence.result.status)
        assertTrue(evidence.result.checks.all { it.passed })
    }

    @Test
    fun mismatchedDerivativeFailsClosed() {
        val evidence = verifier.verify("differentiate x^3", "2*x", "kernel")
        assertEquals(VerificationStatus.Failed, evidence.result.status)
        assertEquals(VerificationStrength.Failed, evidence.strength)
    }

    @Test
    fun unhandledAdvancedOperationIsExplicitlyPartial() {
        val evidence = verifier.verify("taylor sin(x) order 7", "x", "kernel")
        assertEquals(VerificationStatus.Inconclusive, evidence.result.status)
        assertEquals(VerificationStrength.PartiallyVerified, evidence.strength)
    }
}
