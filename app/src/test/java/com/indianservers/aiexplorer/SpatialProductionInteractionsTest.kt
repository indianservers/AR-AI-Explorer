package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialProductionInteractionsTest {
    private val cube = SolidMeshFactory.create(Solid(SolidType.Cube, 2.0))

    @Test fun overlapCyclingIsolationAndOccludedSelectionAreDeterministic() {
        val hits = listOf(
            SpatialSubObjectHit(0, SpatialSubObjectType.Face, 0, 0.0, 1.0, Vec3(0.0, 0.0, 1.0)),
            SpatialSubObjectHit(1, SpatialSubObjectType.Face, 2, 0.0, 3.0, Vec3(0.0, 0.0, 3.0)),
        )
        val front = SpatialSelectionEngine.cycle(hits, null)
        val behind = SpatialSelectionEngine.cycle(hits, front)
        val isolated = SpatialSelectionEngine.isolate(SpatialSelection(setOf(1), 1))

        assertEquals(0, front?.solidIndex)
        assertEquals(1, behind?.solidIndex)
        assertEquals(setOf(1), SpatialSelectionEngine.visibleIndices(3, isolated))
    }

    @Test fun faceEdgeVertexSnappingAndMeasurementsUseMeshGeometry() {
        val targets = ConstraintAwareSpatialSnap.targets(cube)
        val vertex = targets.first { it.kind == SpatialSnapKind.Vertex }
        val snapped = ConstraintAwareSpatialSnap.snap(vertex.point + Vec3(.02, 0.0, 0.0), targets)
        val face = SpatialMeasurementEngine.faceArea(cube, 0)
        val section = SpatialMeasurementEngine.sectionPerimeter(listOf(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0), Vec3(1.0, 1.0, 0.0), Vec3(0.0, 1.0, 0.0)))

        assertEquals(SpatialSnapKind.Vertex, snapped.target?.kind)
        assertEquals(4.0, face.value, 1e-10)
        assertEquals(4.0, section.value, 1e-10)
    }

    @Test fun subObjectHistoryAndExtrusionPreviewUndoAtEditGranularity() {
        val before = Solid(SolidType.Cube, 2.0)
        val preview = SpatialExtrusionEngine.preview(0, 0, .5, before)
        val edit = MeshEdit3D("Extrude face", 0, before, preview.preview, SpatialSubObjectType.Face, 0)
        val history = MeshEditHistory3D().record(edit)
        val (undone, afterUndo) = history.undo(listOf(preview.preview))
        val (redone, _) = afterUndo.redo(undone)

        assertTrue(preview.valid)
        assertEquals(before, undone.single())
        assertEquals(preview.preview, redone.single())
    }

    @Test fun booleanMeshesNetsEulerRevolutionAndExportsProduceStandardArtifacts() {
        val shifted = cube.copy(vertices = cube.vertices.map { it + Vec3(.5, 0.0, 0.0) })
        val union = BooleanMeshEngine.apply(cube, shifted, BooleanMeshOperation.Union)
        val intersection = BooleanMeshEngine.apply(cube, shifted, BooleanMeshOperation.Intersection)
        val euler = SolidInteractionLab.euler(cube)
        val revolution = SolidInteractionLab.solidOfRevolution(listOf(Vec2(-1.0, .5), Vec2(1.0, 1.0)))

        assertTrue(union.mesh.faces.size > cube.faces.size)
        assertTrue(intersection.mesh.vertices.isNotEmpty())
        assertTrue(euler.holds)
        assertTrue(SolidInteractionLab.net(cube).size == cube.faces.size)
        assertTrue(revolution.faces.isNotEmpty())
        assertTrue(SpatialExportEngine.obj(cube).contains("\nf "))
        assertTrue(SpatialExportEngine.stl(cube).contains("facet normal"))
    }

    @Test fun surfaceTraceContourGradientLayersAccessibilityAndDeviceContractsAreInteractive() {
        val mesh = SurfaceMesh(listOf(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 1.0, 2.0)), 1, 2)
        val trace = SurfaceTraceEngine3D.constrain(SurfaceTrace3D("f", mesh.vertices[0]), mesh, Vec3(.9, .9, 2.1))
        val contour = ContourInteractionEngine.inspect(2.0, listOf(Vec3(0.0, 0.0, 2.0) to Vec3(1.0, 1.0, 2.0)), Vec3(.4, .4, 2.2))
        val playback = GradientPlayback3D(mesh.vertices).play().tick().moveWaypoint(1, Vec3(2.0, 2.0, 8.0))
        val layer = SpatialSurfaceLayer("f", "x^2+y^2", material = SpatialMaterial.Glass, quality = SpatialQuality.High)
        val accessible = SpatialAccessibilityEngine.describe(listOf(Solid(SolidType.Cube, 2.0)))

        assertEquals(mesh.vertices[1], trace.point)
        assertTrue(contour != null)
        assertEquals(Vec3(2.0, 2.0, 8.0), playback.path[1])
        assertTrue(layer.visible)
        assertTrue(accessible.single().measurements.any { it.startsWith("volume") })
        assertEquals(4, SpatialDeviceValidationMatrix.production.size)
        assertFalse(SpatialDeviceValidationMatrix.production.any { !it.picking || !it.panZoom || !it.precision })
    }
}
