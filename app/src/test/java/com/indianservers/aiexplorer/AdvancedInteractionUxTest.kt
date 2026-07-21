package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.AdvancedInteractionFeatureRegistry
import com.indianservers.aiexplorer.core.AdvancedSpatialInteractionEngine
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.GraphDirectManipulationEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SpatialAlignment
import com.indianservers.aiexplorer.core.SpatialDragPlane
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedInteractionUxTest {
    @Test
    fun registryCoversAllFiftyRequestedGraphAndSpatialCapabilities() {
        assertEquals(25, AdvancedInteractionFeatureRegistry.graph.size)
        assertEquals(25, AdvancedInteractionFeatureRegistry.spatial.size)
        assertEquals(50, AdvancedInteractionFeatureRegistry.all.distinct().size)
    }

    @Test
    fun freehandFitProducesAccurateQuadraticAndResiduals() {
        val points = (-6..6).map { x -> Vec2(x.toDouble(), x * x + 2.0 * x + 1.0) }
        val fit = GraphDirectManipulationEngine.fit(points)!!

        assertTrue(fit.expression.contains("x^2"))
        assertTrue(fit.rSquared > .999999)
        assertTrue(fit.residuals.all { kotlin.math.abs(it.y) < 1e-7 })
    }

    @Test
    fun areaBrushAndCurveTranslationAreMathematicallyLive() {
        val expression = ExpressionEngine().compile("x")
        assertEquals(.5, GraphDirectManipulationEngine.signedArea(expression, 0.0, 1.0), 1e-4)
        val translated = GraphDirectManipulationEngine.translate("x^2", Vec2(2.0, 3.0))
        assertTrue(translated.contains("x-2"))
        assertTrue(translated.contains("+3"))
    }

    @Test
    fun spatialEngineSupportsPlanesExtrusionReflectionAlignmentAndCollision() {
        assertEquals(Vec3(1.0, 2.0, 0.0), AdvancedSpatialInteractionEngine.constrain(Vec3(1.0, 2.0, 3.0), SpatialDragPlane.XY))
        val cube = Solid(SolidType.Cube, width = 2.0, height = 2.0, depth = 2.0, position = Vec3(1.0, 0.0, 0.0))
        assertTrue(AdvancedSpatialInteractionEngine.extrude(cube, 0, .5).width > cube.width)
        assertEquals(-1.0, AdvancedSpatialInteractionEngine.reflect(cube, SpatialAlignment.X).position.x, 1e-9)

        val solids = listOf(cube, cube.copy(position = Vec3(1.5, 1.0, 0.0)), cube.copy(position = Vec3(4.0, 2.0, 0.0)))
        assertTrue(AdvancedSpatialInteractionEngine.collisions(solids).isNotEmpty())
        val aligned = AdvancedSpatialInteractionEngine.align(solids, setOf(0, 1, 2), SpatialAlignment.Y)
        assertEquals(1, aligned.map { it.position.y }.distinct().size)
        val distributed = AdvancedSpatialInteractionEngine.distribute(solids, setOf(0, 1, 2), SpatialAlignment.X)
        assertEquals(distributed[1].position.x - distributed[0].position.x, distributed[2].position.x - distributed[1].position.x, 1e-9)
    }
}
