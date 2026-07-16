package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.AdvancedGraphDefinition
import com.indianservers.aiexplorer.core.AdvancedGraphEngine
import com.indianservers.aiexplorer.core.AdvancedGraphKind
import com.indianservers.aiexplorer.core.AnalyticGeometry2D
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.BinomialDistribution
import com.indianservers.aiexplorer.core.Circle2D
import com.indianservers.aiexplorer.core.ConicKind
import com.indianservers.aiexplorer.core.ExponentialDistribution
import com.indianservers.aiexplorer.core.GraphDomain
import com.indianservers.aiexplorer.core.Line2D
import com.indianservers.aiexplorer.core.Line3D
import com.indianservers.aiexplorer.core.NormalDistribution
import com.indianservers.aiexplorer.core.PoissonDistribution
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.UniformDistribution
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedMathFoundationTest {
    private val graph = AdvancedGraphEngine()

    @Test
    fun classifiesAdvancedGraphDefinitions() {
        assertEquals(AdvancedGraphKind.Sequence, graph.classify("a(n)=n^2"))
        assertEquals(AdvancedGraphKind.Inequality, graph.classify("y <= x^2"))
        assertEquals(AdvancedGraphKind.Polar, graph.classify("r=2cos(t)"))
        assertEquals(AdvancedGraphKind.Parametric, graph.classify("x(t)=cos(t);y(t)=sin(t)"))
        assertEquals(AdvancedGraphKind.Implicit, graph.classify("x^2+y^2=4"))
    }

    @Test
    fun adaptivelySamplesCurvesAndReportsRange() {
        val sample = graph.adaptiveExplicit(
            AdvancedGraphDefinition("x^2", AdvancedGraphKind.Explicit, GraphDomain(-2.0, 2.0)),
        )

        assertTrue(sample.points.size >= 25)
        assertEquals(0.0, graph.range(sample)!!.start, 1e-6)
        assertEquals(4.0, graph.range(sample)!!.endInclusive, 1e-6)
        assertTrue(graph.arcLength(sample) > 8.0)
    }

    @Test
    fun createsSequencesVectorFieldsAndInequalityMasks() {
        val sequence = graph.sequence("a(n)=n^2", 1, 4)
        val vectors = graph.vectorField("-y", "x", GraphDomain(-1.0, 1.0), GraphDomain(-1.0, 1.0), 3, 3)
        val mask = graph.inequality("y<=x", GraphDomain(-1.0, 1.0), GraphDomain(-1.0, 1.0), 10, 10)

        assertEquals(listOf(1.0, 4.0, 9.0, 16.0), sequence.map { it.value })
        assertEquals(9, vectors.size)
        assertTrue(mask.any { it.satisfied } && mask.any { !it.satisfied })
    }

    @Test
    fun performsAnalytic2DGeometry() {
        val horizontal = Line2D(Vec2(0.0, 0.0), Vec2(1.0, 0.0))
        val vertical = Line2D(Vec2(2.0, -1.0), Vec2(0.0, 1.0))
        val intersection = AnalyticGeometry2D.intersect(horizontal, vertical)
        val circleHits = AnalyticGeometry2D.circleLineIntersections(Circle2D(Vec2(0.0, 0.0), 2.0), horizontal)
        val hull = AnalyticGeometry2D.convexHull(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(.5, .5), Vec2(0.0, 1.0)))

        assertEquals(Vec2(2.0, 0.0), intersection)
        assertEquals(2, circleHits.size)
        assertEquals(4, hull.size)
        assertEquals(ConicKind.Circle, AnalyticGeometry2D.classifyConic(1.0, 0.0, 1.0))
        assertEquals(3.0, AnalyticGeometry2D.distance(horizontal, Vec2(4.0, 3.0)), 1e-9)
    }

    @Test
    fun performsPlaneAndSurfaceCalculus() {
        val xy = AnalyticGeometry3D.planeThrough(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0))!!
        val hit = AnalyticGeometry3D.intersect(Line3D(Vec3(1.0, 2.0, 3.0), Vec3(0.0, 0.0, -1.0)), xy)
        val differential = SurfaceCalculus().analyze("x^2+y^2", 1.0, 2.0)

        assertEquals(Vec3(1.0, 2.0, 0.0), hit)
        assertEquals(5.0, differential.point.z, 1e-7)
        assertEquals(2.0, differential.gradient.x, 1e-4)
        assertEquals(4.0, differential.gradient.y, 1e-4)
        assertNull(AnalyticGeometry3D.intersect(Line3D(Vec3(0.0, 0.0, 1.0), Vec3(1.0, 0.0, 0.0)), xy))
    }

    @Test
    fun validatesContinuousDistributions() {
        val normal = NormalDistribution()
        val uniform = UniformDistribution(2.0, 6.0)
        val exponential = ExponentialDistribution(2.0)

        assertEquals(.5, normal.cumulative(0.0), 2e-7)
        assertEquals(0.0, normal.quantile(.5), 2e-6)
        assertEquals(.5, uniform.probabilityBetween(2.0, 4.0), 1e-9)
        assertEquals(.5, exponential.summary.mean, 1e-9)
        assertEquals(.5, exponential.cumulative(exponential.quantile(.5)), 1e-9)
    }

    @Test
    fun validatesDiscreteDistributions() {
        val binomial = BinomialDistribution(10, .5)
        val poisson = PoissonDistribution(3.0)

        assertEquals(.24609375, binomial.density(5.0), 1e-9)
        assertEquals(1.0, binomial.plotPoints().sumOf { it.probability }, 1e-9)
        assertEquals(3.0, poisson.summary.mean, 1e-9)
        assertTrue(poisson.cumulative(100.0) > .999999)
        assertNotNull(poisson.plotPoints().firstOrNull())
    }
}
