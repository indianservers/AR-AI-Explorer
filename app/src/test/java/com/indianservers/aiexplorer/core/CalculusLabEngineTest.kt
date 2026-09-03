package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculusLabEngineTest {
    private val engine = CalculusLabEngine()

    @Test
    fun derivativeCombinesExactAndNumericalEvidence() {
        val result = engine.derivative("x^3 - 3*x", at = 2.0, range = -3.0..3.0)

        assertTrue(result.symbolic.supported)
        assertEquals("-3 + 3*x^2", result.symbolic.exact)
        assertEquals(9.0, result.numerical.derivative, 1e-7)
        assertEquals(2.0, result.numerical.point.y, 1e-9)
        assertTrue(result.curve.size > 300)
    }

    @Test
    fun integralComparesRiemannSumWithAccurateArea() {
        val result = engine.integral("x^2", 0.0, 2.0, rectangleCount = 40, sample = RiemannSample.Midpoint)

        assertTrue(result.symbolic.supported)
        assertEquals(8.0 / 3.0, result.numerical.signedArea, 1e-8)
        assertEquals(40, result.rectangles.size)
        assertEquals(8.0 / 3.0, result.riemannEstimate, 0.002)
    }

    @Test
    fun limitRequiresAgreeingOneSidedEvidence() {
        val removable = engine.limit("(x^2-4)/(x-2)", 2.0)
        assertEquals(LimitClassification.Finite, removable.rigorous.classification)
        assertEquals(4.0, removable.rigorous.value ?: Double.NaN, 1e-3)

        val divergent = engine.limit("1/x", 0.0)
        assertTrue(divergent.rigorous.classification != LimitClassification.Finite)
    }

    @Test
    fun derivativeApplicationsFindIntervalExtrema() {
        val report = engine.applications("x^2", -2.0, 3.0)

        assertEquals(0.0, report.absoluteMinimum?.y ?: Double.NaN, 1e-4)
        assertEquals(9.0, report.absoluteMaximum?.y ?: Double.NaN, 1e-8)
        assertTrue(report.increasing.isNotEmpty())
        assertTrue(report.decreasing.isNotEmpty())
    }
}
