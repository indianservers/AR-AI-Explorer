package com.indianservers.aiexplorer.probabilitystats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProbabilityPhase3Test {
    @Test
    fun linearRegressionRecoversExactLine() {
        val points = (0..8).map { StatPoint(it.toDouble(), 2.0 + 3.0 * it) }
        val result = Phase3RegressionEngine.fit(points, RegressionModel.Linear)

        assertEquals(null, result.error)
        assertEquals(2.0, result.coefficients[0], 1e-9)
        assertEquals(3.0, result.coefficients[1], 1e-9)
        assertEquals(1.0, result.pearson, 1e-9)
        assertEquals(1.0, result.rSquared, 1e-9)
        assertTrue(result.residuals.all { abs(it) < 1e-9 })
    }

    @Test
    fun quadraticRegressionRecoversExactCurve() {
        val points = (-4..4).map { x -> StatPoint(x.toDouble(), 1.0 - 2.0 * x + .5 * x * x) }
        val result = Phase3RegressionEngine.fit(points, RegressionModel.Quadratic)

        assertEquals(1.0, result.coefficients[0], 1e-8)
        assertEquals(-2.0, result.coefficients[1], 1e-8)
        assertEquals(.5, result.coefficients[2], 1e-8)
        assertEquals(1.0, result.rSquared, 1e-9)
    }

    @Test
    fun regressionRejectsInvalidTransformDomain() {
        val points = listOf(StatPoint(-1.0, 2.0), StatPoint(1.0, 3.0))
        assertTrue(Phase3RegressionEngine.fit(points, RegressionModel.Logarithmic).error != null)
    }

    @Test
    fun knownMeanConfidenceIntervalIsCorrect() {
        val result = Phase3InferenceEngine.confidence(
            ConfidenceRequest(ConfidenceKind.OneMean, .95, 100.0, 15.0, 100),
        )

        assertTrue(result.valid)
        assertEquals(1.5, result.standardError, 1e-9)
        assertEquals(1.96, result.criticalValue, .002)
        assertEquals(97.06, result.lower, .01)
        assertEquals(102.94, result.upper, .01)
    }

    @Test
    fun proportionValidationPreventsImpossibleInput() {
        val result = Phase3InferenceEngine.confidence(
            ConfidenceRequest(ConfidenceKind.OneProportion, .95, 1.2, 0.0, 50),
        )
        assertFalse(result.valid)
    }

    @Test
    fun hypothesisUsesFailToRejectLanguage() {
        val weak = Phase3InferenceEngine.oneSampleTest(70.1, 70.0, 10.0, 40, .05)
        val strong = Phase3InferenceEngine.oneSampleTest(80.0, 70.0, 10.0, 100, .05)

        assertTrue(weak.conclusion.startsWith("Fail to reject"))
        assertTrue(strong.conclusion.startsWith("Reject"))
        assertFalse(weak.conclusion.contains("accept", ignoreCase = true))
    }

    @Test
    fun commandParserHasExplicitAllowList() {
        val points = listOf(StatPoint(1.0, 2.0), StatPoint(2.0, 4.0))

        assertTrue(Phase3CommandEngine.execute("Mean(Y)", points) is CommandResult.Success)
        assertTrue(Phase3CommandEngine.execute("System(exit)", points) is CommandResult.Error)
        assertTrue(Phase3CommandEngine.execute("not a command", points) is CommandResult.Error)
    }
}
