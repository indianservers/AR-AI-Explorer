package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Plane3D
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.workspace.ParametricCurve3D
import com.indianservers.aiexplorer.workspace.SharedSpatialMathEngine
import com.indianservers.aiexplorer.workspace.SpatialEquationForm
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedSpatialMathSchemaTest {
    private val engine = SharedSpatialMathEngine()

    @Test fun typedSurfaceEquationsRemainRendererIndependent() {
        val records = listOf(
            SurfaceDefinition3D.Explicit("bowl", "x^2+y^2"),
            SurfaceDefinition3D.Implicit("sphere", "x^2+y^2+z^2=4"),
            SurfaceDefinition3D.Parametric("saddle", "u", "v", "u*v"),
        ).map(engine::equation)

        assertEquals(listOf(SpatialEquationForm.ExplicitSurface, SpatialEquationForm.ImplicitSurface, SpatialEquationForm.ParametricSurface), records.map { it.form })
        assertEquals("z=x^2+y^2", records[0].equations.single())
        assertEquals(listOf("u", "v"), records[2].parameters)
    }

    @Test fun explicitImplicitAndParametricDifferentialsProduceTangentPlanes() {
        val explicit = engine.differential(SurfaceDefinition3D.Explicit("bowl", "x^2+y^2"), 1.0, 2.0)
        val implicit = engine.differential(SurfaceDefinition3D.Implicit("sphere", "x^2+y^2+z^2=4"), 2.0, 0.0, 0.0)
        val parametric = engine.differential(SurfaceDefinition3D.Parametric("plane", "u", "v", "u+v"), 1.0, 2.0)

        assertEquals(Vec3(1.0, 2.0, 5.0), explicit.point)
        assertEquals(-2.0, explicit.gradient.x, 1e-5)
        assertEquals(-4.0, explicit.gradient.y, 1e-5)
        assertEquals(4.0, implicit.gradient.x, 1e-4)
        assertTrue(parametric.unitNormal.magnitude() > .999)
        assertTrue(explicit.tangentPlaneEquation.endsWith("= 0"))
    }

    @Test fun spaceCurvesAndPlaneIntersectionsAreSharedObjects() {
        val helix = engine.sample(ParametricCurve3D("helix", "cos(t)", "sin(t)", "t", range = 0.0..6.283185307), 100)
        val line = engine.planeIntersection(
            Plane3D(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0)),
            Plane3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)),
        )

        assertEquals(101, helix.points.size)
        assertEquals(0, helix.discardedSamples)
        assertNotNull(line)
        assertTrue(kotlin.math.abs(line!!.direction.normalized().z) > .999)
    }

    @Test fun crossSectionCarriesWorldAndPlaneCoordinates() {
        val section = engine.crossSection(
            SurfaceDefinition3D.Explicit("sloped", "x"),
            Plane3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 1.0)),
            density = 12,
        )

        assertTrue(section.loops.isNotEmpty())
        assertEquals(section.loops.map { it.points.size }, section.projectedLoops.map { it.size })
        assertTrue(section.projectedLoops.flatten().all { kotlin.math.abs(it.world.z) < 1e-6 })
    }

    @Test fun unifiedSpatialSnapshotPublishesTypedSurfaceSchema() {
        val snapshot = UnifiedSpatialMathController().snapshot(WorkspaceState(surfaceExpression = "x^2+y^2"))

        assertEquals("surface-main", snapshot.spatialSchema.equations.single().id)
        assertEquals(SpatialEquationForm.ExplicitSurface, snapshot.spatialSchema.equations.single().form)
    }
}
