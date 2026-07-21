package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Plane3D
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.CpuSpatialRenderer
import com.indianservers.aiexplorer.spatial.EditableSpatialMesh
import com.indianservers.aiexplorer.spatial.MeshConstraint3D
import com.indianservers.aiexplorer.spatial.MeshSubObjectKind
import com.indianservers.aiexplorer.spatial.MeshSubSelection
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import com.indianservers.aiexplorer.spatial.SpatialAnalysisTools3D
import com.indianservers.aiexplorer.spatial.SpatialCameraState
import com.indianservers.aiexplorer.spatial.SpatialCameraValidator
import com.indianservers.aiexplorer.spatial.SpatialConstruction3D
import com.indianservers.aiexplorer.spatial.SpatialConstructionDocument
import com.indianservers.aiexplorer.spatial.SpatialConstructionEngine
import com.indianservers.aiexplorer.spatial.SpatialDeviceValidationMatrix
import com.indianservers.aiexplorer.spatial.SpatialGeometry
import com.indianservers.aiexplorer.spatial.SpatialGesture
import com.indianservers.aiexplorer.spatial.SpatialGestureIntent
import com.indianservers.aiexplorer.spatial.SpatialGesturePolicy3D
import com.indianservers.aiexplorer.spatial.SpatialGpuCapability
import com.indianservers.aiexplorer.spatial.SpatialInputDevice
import com.indianservers.aiexplorer.spatial.SpatialMeshEditor
import com.indianservers.aiexplorer.spatial.SpatialRenderBackend
import com.indianservers.aiexplorer.spatial.SpatialRendererQualitySelector
import com.indianservers.aiexplorer.spatial.SpatialScreenClass
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D
import com.indianservers.aiexplorer.spatial.SurfaceDomain
import com.indianservers.aiexplorer.spatial.ThermalLevel
import com.indianservers.aiexplorer.spatial.TypedSurfaceMesher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialAuthoringProductionTest {
    @Test fun typedSurfaceModelMeshesExplicitImplicitAndParametricForms() {
        val mesher = TypedSurfaceMesher(); val domain = SurfaceDomain(-1.0..1.0, -1.0..1.0, -1.0..1.0)
        val explicit = mesher.mesh(SurfaceDefinition3D.Explicit("e", "z=x^2+y^2", domain), 8)
        val implicit = mesher.mesh(SurfaceDefinition3D.Implicit("i", "x^2+y^2+z^2=1", domain), 8)
        val parametric = mesher.mesh(SurfaceDefinition3D.Parametric("p", "u", "v", "u*v", domain = domain), 8)
        assertTrue(explicit.geometry.triangles.isNotEmpty())
        assertTrue(implicit.geometry.triangles.isNotEmpty())
        assertTrue(parametric.geometry.triangles.isNotEmpty())
        assertTrue(implicit.watertightCandidate)
    }

    @Test fun directAndPointBasedConstructionsResolveTypedGeometry() {
        val engine = SpatialConstructionEngine(); var document = SpatialConstructionDocument()
        document = engine.add(document, SpatialConstruction3D.Point("A", Vec3(0.0, 0.0, 0.0)))
        document = engine.add(document, SpatialConstruction3D.Point("B", Vec3(1.0, 0.0, 0.0)))
        document = engine.add(document, SpatialConstruction3D.Point("C", Vec3(0.0, 1.0, 0.0)))
        document = engine.add(document, SpatialConstruction3D.Vector("v", "A", "B"))
        document = engine.add(document, SpatialConstruction3D.Line("l", "A", "B"))
        document = engine.add(document, SpatialConstruction3D.Plane("plane", "A", "B", "C"))
        document = engine.add(document, SpatialConstruction3D.DirectLine("direct-line", Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0)))
        document = engine.add(document, SpatialConstruction3D.DirectPlane("direct-plane", Vec3(0.0, 0.0, 2.0), Vec3(0.0, 0.0, 1.0)))
        assertEquals(8, engine.resolve(document).size)
    }

    @Test fun faceExtrusionHonoursDirectionDistanceAndSubObjectEditing() {
        val mesh = EditableSpatialMesh(
            listOf(Vec3(-1.0, -1.0, 0.0), Vec3(1.0, -1.0, 0.0), Vec3(1.0, 1.0, 0.0), Vec3(-1.0, 1.0, 0.0)),
            listOf(0, 1, 2, 0, 2, 3),
            listOf(MeshConstraint3D.Extrusion(Vec3(0.0, 0.0, 1.0), 2.0)),
        )
        val extruded = SpatialMeshEditor.extrude(mesh, setOf(0, 1), .2)
        assertEquals(8, extruded.mesh.vertices.size)
        assertTrue(extruded.createdVertices.all { kotlin.math.abs(extruded.mesh.vertices[it].z - 2.0) < 1e-9 })
        val moved = SpatialMeshEditor.move(extruded.mesh, MeshSubSelection(MeshSubObjectKind.Vertex, setOf(0)), Vec3(.25, 0.0, 0.0))
        assertEquals(-.75, moved.vertices[0].x, 1e-9)
    }

    @Test fun sectionsProjectionIntersectionsAndMeasurementsAreGeometric() {
        val geometry = cube()
        val section = SpatialAnalysisTools3D.crossSection(geometry, Plane3D(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 1.0))).first { it.closed }
        assertEquals(4.0, section.area, 1e-7)
        assertEquals(4.0, SpatialAnalysisTools3D.distance(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 4.0)).value, 1e-9)
        assertEquals(90.0, SpatialAnalysisTools3D.angle(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0)).value, 1e-9)
        assertEquals(24.0, SpatialAnalysisTools3D.meshArea(geometry), 1e-9)
        assertTrue(SpatialAnalysisTools3D.project(Vec3(0.0, 0.0, 3.0), geometry) != null)
    }

    @Test fun rendererSelectsGpuTiersAndAlwaysHasCpuFallback() {
        val gpu = SpatialRendererQualitySelector.choose(SpatialGpuCapability(3, 2, true, 4096), ThermalLevel.Nominal, 12.0)
        val cpu = SpatialRendererQualitySelector.choose(null, ThermalLevel.Moderate, 30.0)
        assertEquals(SpatialRenderBackend.GPU, gpu.backend)
        assertEquals(SpatialRenderBackend.CPU, cpu.backend)
        val scene = SharedSpatialSceneBuilder.build("cpu")
        val plan = CpuSpatialRenderer.compile(scene.copy(primitives = scene.primitives + com.indianservers.aiexplorer.spatial.SpatialPrimitive("cube", com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind.Solid, cube(), scene.primitives.first().material)), SpatialCameraState(Vec3(0.0, 0.0, 5.0), Vec3(0.0, 0.0, 0.0)), 800, 600)
        assertTrue(plan.triangles.isNotEmpty())
    }

    @Test fun cameraPickingTolerancesAndGesturesCoverPhoneTabletAndStylus() {
        val repaired = SpatialCameraValidator.validate(SpatialCameraState(Vec3(0.0, 0.0, 0.0), Vec3(0.0, 0.0, 0.0), fieldOfViewDegrees = 200.0))
        assertTrue(repaired.corrected)
        val cases = SpatialDeviceValidationMatrix.cases()
        assertEquals(6, cases.size)
        assertTrue(cases.all { it.cameraValid && it.pickingTolerance > 0 })
        val stylus = SpatialGesturePolicy3D.decide(SpatialScreenClass.Tablet, SpatialInputDevice.Stylus, SpatialGesture(1, Vec2(2.0, 0.0), barrelButton = true, pressure = .8), MeshSubObjectKind.Face)
        assertEquals(SpatialGestureIntent.ExtrudeFace, stylus.intent)
        assertFalse(stylus.consumesViewport)
    }

    private fun cube(): SpatialGeometry {
        val v = listOf(
            Vec3(-1.0, -1.0, -1.0), Vec3(1.0, -1.0, -1.0), Vec3(1.0, 1.0, -1.0), Vec3(-1.0, 1.0, -1.0),
            Vec3(-1.0, -1.0, 1.0), Vec3(1.0, -1.0, 1.0), Vec3(1.0, 1.0, 1.0), Vec3(-1.0, 1.0, 1.0),
        )
        val t = listOf(0,2,1,0,3,2,4,5,6,4,6,7,0,1,5,0,5,4,1,2,6,1,6,5,2,3,7,2,7,6,3,0,4,3,4,7)
        return SpatialGeometry(v, t)
    }
}
