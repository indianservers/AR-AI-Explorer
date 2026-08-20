package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.GraphNumericConfidence
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.GraphRegressionKind
import com.indianservers.aiexplorer.core.GraphDomain
import com.indianservers.aiexplorer.core.RobustGraphAnalysisEngine
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpression
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Graph2DStrengthenedEngineTest {
    private val typed = TypedGraphEngine()

    @Test
    fun sequencesRecursionListsAndRegressionsShareTheTypedSampler() {
        val sequence = TypedGraphExpressionParser.parse("a(n)=n^2, n=1..5")
        val recursive = TypedGraphExpressionParser.parse("a(1)=1; a(n)=a(n-1)+2; n=1..5")
        val fibonacci = TypedGraphExpressionParser.parse("f(0)=0; f(1)=1; f(n)=f(n-1)+f(n-2); n=0..10")
        val list = TypedGraphExpressionParser.parse("[(0,1),(1,3),(2,5)]")
        val regression = TypedGraphExpressionParser.parse("regression(linear; (0,1),(1,3),(2,5),(3,7))")

        assertTrue(sequence is TypedGraphExpression.Sequence)
        assertEquals(listOf(1.0, 4.0, 9.0, 16.0, 25.0), typed.sample(sequence).points.map { it.y })
        assertTrue(recursive is TypedGraphExpression.Recursive)
        assertEquals(listOf(1.0, 3.0, 5.0, 7.0, 9.0), typed.sample(recursive).points.map { it.y })
        assertEquals(55.0, typed.sample(fibonacci).points.last().y, 0.0)
        assertTrue(list is TypedGraphExpression.ListData)
        assertEquals(3, typed.sample(list).points.size)
        val regressionSample = typed.sample(regression)
        assertEquals(GraphRegressionKind.Linear, regressionSample.regression?.kind)
        assertTrue(regressionSample.regression!!.rSquared > .999999)
    }

    @Test
    fun restrictionsAndCompoundChainedInequalitiesAreEvaluated() {
        val restricted = TypedGraphExpressionParser.parse("y=x^2{-1<=x<=1}")
        val restrictedSample = typed.sample(restricted, GraphDomain(-3.0, 3.0), samples = 600)
        val inequality = TypedGraphExpressionParser.parse("-2 < x < 2 and y <= x^2")
        val inequalitySample = typed.sample(inequality, GraphDomain(-3.0, 3.0), GraphDomain(-3.0, 3.0, "y"), samples = 160)

        assertTrue(restricted is TypedGraphExpression.Explicit && restricted.restriction != null)
        assertTrue(restrictedSample.curves.flatMap { it.points }.all { it.x in -1.001..1.001 })
        assertTrue(inequality is TypedGraphExpression.Inequality)
        assertTrue(inequalitySample.inequalityCells.any { it.satisfied })
        assertTrue(inequalitySample.inequalityCells.any { !it.satisfied })
        assertTrue(inequalitySample.implicitSegments.isNotEmpty())
        val logical = ExpressionEngine().compile("x < -2 or x > 2")
        assertEquals(1.0, logical.eval(mapOf("x" to 4.0)), 0.0)
    }

    @Test
    fun robustAnalysisFindsCrossingAndTouchingRootsWithEvidence() {
        val engine = RobustGraphAnalysisEngine()
        val crossing = engine.roots("x^2-1", -3.0, 3.0)
        val touching = engine.roots("(x-0.37)^2", -2.0, 2.0)

        assertEquals(2, crossing.size)
        assertEquals(listOf("-1", "1"), crossing.map { it.x.exact })
        assertTrue(crossing.all { it.x.confidence == GraphNumericConfidence.Exact && it.x.residual < 1e-10 })
        assertTrue(touching.any { kotlin.math.abs(it.point.x - .37) < 1e-6 && it.x.residual < 1e-10 })
    }

    @Test
    fun extremaTangentsIntegralsAndAreasCarryConfidenceAndError() {
        val engine = RobustGraphAnalysisEngine()
        val report = engine.analyze("x^2", -3.0, 3.0, at = 1.0, secondSource = "x", integralFrom = 0.0, integralTo = 1.0)

        assertTrue(report.extrema.any { kotlin.math.abs(it.point.x) < 1e-6 && it.classification == "local minimum" })
        assertEquals(2.0, report.tangent!!.slope.value, 1e-7)
        assertTrue(report.tangent.slope.confidence in setOf(GraphNumericConfidence.Exact, GraphNumericConfidence.High))
        assertEquals(1.0 / 3.0, report.integral!!.signed.value, 1e-8)
        assertTrue(report.integral.signed.errorEstimate < 1e-7)
        assertEquals(1.0, engine.integral("a*x", 0.0, 1.0, mapOf("a" to 2.0)).signed.value, 1e-9)
        assertEquals(1.0 / 6.0, report.areaBetween!!.geometric.value, 1e-8)
        assertNotNull(report.areaBetween.geometric.display)
    }
}
