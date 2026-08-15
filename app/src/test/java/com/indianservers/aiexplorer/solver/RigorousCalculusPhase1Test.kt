package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.ContinuityClassification
import com.indianservers.aiexplorer.core.LimitClassification
import com.indianservers.aiexplorer.core.LimitMethod
import com.indianservers.aiexplorer.core.RigorousCalculusEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RigorousCalculusPhase1Test {
    private val engine = RigorousCalculusEngine()

    @Test fun removableLimitCarriesTwoSidedEvidenceAndContinuityType() {
        val report = engine.limit("(x^2-4)/(x-2)", 2.0)

        assertEquals(LimitClassification.Finite, report.classification)
        assertEquals(4.0, report.value!!, 1e-3)
        assertEquals(ContinuityClassification.Removable, report.continuity)
        assertEquals(LimitMethod.FactorCancellation, report.method)
        assertTrue(report.left.samples.size >= 8 && report.right.samples.size >= 8)
    }

    @Test fun oppositeInfiniteSidesProduceDneInsteadOfUnsafeFiniteAnswer() {
        val report = engine.limit("1/x", 0.0)

        assertEquals(LimitClassification.DoesNotExist, report.classification)
        assertEquals(ContinuityClassification.Infinite, report.continuity)
        assertTrue(report.value == null)
    }

    @Test fun jumpAndSqueezePatternsAreDistinguished() {
        val jump = engine.limit("abs(x)/x", 0.0)
        val squeeze = engine.limit("sin(x)/x", 0.0)

        assertEquals(LimitClassification.DoesNotExist, jump.classification)
        assertEquals(ContinuityClassification.Jump, jump.continuity)
        assertEquals(1.0, squeeze.value!!, 1e-3)
        assertEquals(LimitMethod.SqueezePattern, squeeze.method)
    }

    @Test fun derivativeApplicationsClassifyStationaryAndAbsoluteExtrema() {
        val report = engine.derivativeApplications("x^2", -2.0, 2.0)

        assertTrue(report.stationaryPoints.any { it.kind == "local minimum" && kotlin.math.abs(it.point.x) < .02 })
        assertEquals(0.0, report.absoluteMinimum!!.y, .02)
        assertEquals(4.0, report.absoluteMaximum!!.y, .02)
    }

    @Test fun calculatorRoutesLimitCommandsThroughRigorousEngine() {
        val result = AdvancedScientificCalculator().evaluate("limit (x^2-4)/(x-2) as x -> 2")!!

        assertEquals("4", result.primary)
        assertTrue(result.alternatives.any { it.first == "Continuity" && it.second == "Removable" })
        assertTrue(result.verification.contains("left-side"))
    }
}
