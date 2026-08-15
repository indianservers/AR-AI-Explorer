package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.AdvancedGraphFeatureEngine
import com.indianservers.aiexplorer.core.GraphDiscontinuityKind
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedGraphFeatureEngineTest {
    private val engine = AdvancedGraphFeatureEngine()

    @Test fun rationalFunctionDetectsDomainSplitAndAsymptotes() {
        val features = engine.analyze("1/(x-2)", -10.0, 10.0, 2000)

        assertTrue(features.domain.size >= 2)
        assertTrue(features.discontinuities.any { kotlin.math.abs(it.x - 2.0) < .02 && it.kind == GraphDiscontinuityKind.VerticalAsymptote })
        assertTrue(features.asymptotes.any { it.kind == "vertical" && it.equation.startsWith("x = 2") })
        assertTrue(features.asymptotes.any { it.kind == "horizontal" && it.equation == "y = 0" })
    }

    @Test fun cubicReportsMonotonicityConcavityAndSingleInflection() {
        val features = engine.analyze("x^3", -4.0, 4.0, 1600)

        assertTrue(features.increasing.isNotEmpty())
        assertTrue(features.decreasing.isEmpty())
        assertTrue(features.concaveDown.any { it.to < .1 })
        assertTrue(features.concaveUp.any { it.from > -.1 })
        assertEquals(1, features.inflectionPoints.size)
        assertEquals(0.0, features.inflectionPoints.single().x, .02)
        assertEquals(0.0, features.inflectionPoints.single().y, .02)
    }

    @Test fun tangentNormalAndVerticalNormalAreExplicit() {
        val quadratic = engine.tangentAndNormal("x^2", 1.0)
        val flat = engine.tangentAndNormal("x^2", 0.0)

        assertEquals(Vec2(1.0, 1.0), quadratic.point)
        assertEquals(2.0, quadratic.slope!!, 1e-5)
        assertEquals("y - 1 = 2*(x - 1)", quadratic.tangentEquation)
        assertEquals("y - 1 = -0.5*(x - 1)", quadratic.normalEquation)
        assertEquals("x = 0", flat.normalEquation)
    }

    @Test fun areaBetweenCurvesReturnsSignedAndGeometricEvidence() {
        val area = engine.areaBetween("x", "x^2", 0.0, 1.0)

        assertEquals(1.0 / 6.0, area.signedArea, 1e-8)
        assertEquals(1.0 / 6.0, area.geometricArea, 1e-8)
        assertTrue(area.errorEstimate < 1e-7)
    }

    @Test fun sharedMathGraphCarriesAdvancedFeatures() {
        val snapshot = MathObjectGraph().snapshot(WorkspaceState(functions = listOf(FunctionDefinition("f", "f(x)", "x^3", "cyan"))))
        val features = snapshot.graphObject("f(x)")!!.advancedFeatures

        assertNotNull(features)
        assertEquals(1, features!!.inflectionPoints.size)
    }
}
