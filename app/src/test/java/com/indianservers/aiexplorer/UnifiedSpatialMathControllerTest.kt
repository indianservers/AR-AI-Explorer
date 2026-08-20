package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SpatialMaterial
import com.indianservers.aiexplorer.core.SpatialQuality
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.SpatialSurfaceKind
import com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode
import com.indianservers.aiexplorer.core.SurfaceDomain3D
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMutation
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.UniversalMathValueStatus
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceSnapshotCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.Graph3DViewState
import com.indianservers.aiexplorer.workspace.Graph2DViewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedSpatialMathControllerTest {
    private val controller = UnifiedSpatialMathController()

    private fun state() = WorkspaceState(
        surfaceExpression = "a*x^2 + y",
        solids = listOf(Solid(SolidType.Cuboid, 2.0, 3.0, 4.0, position = Vec3(1.0, 2.0, 3.0))),
        vectors3D = listOf(Vector3D("v", Vec3(0.0, 0.0, 0.0), Vec3(1.0, 2.0, 2.0), "v")),
    )

    @Test fun surfaceSlidersAndTableUseOneResolvedExpression() {
        val initial = controller.snapshot(state(), parameterValues = mapOf("a" to 2.0), axes = listOf(0.0, 1.0, 2.0))
        val changed = controller.setParameter(initial, "a", 3.0) as UnifiedSpatialMutation.Applied

        assertEquals(listOf("a"), initial.parameters.map { it.name })
        assertEquals(9.0, initial.surfaceTable.first { it.x == 2.0 && it.y == 1.0 }.z ?: Double.NaN, 1e-9)
        assertEquals(13.0, changed.snapshot.surfaceTable.first { it.x == 2.0 && it.y == 1.0 }.z ?: Double.NaN, 1e-9)
        assertTrue("surface-main" in changed.affectedObjects)
    }

    @Test fun surfaceEditRoundTripsThroughAuthoritativeDocument() {
        val result = controller.editSurface(controller.snapshot(state()), "sin(x) + cos(y)") as UnifiedSpatialMutation.Applied

        assertEquals("sin(x) + cos(y)", result.snapshot.state.surfaceExpression)
        assertEquals(UniversalMathDefinition.Symbolic("sin(x) + cos(y)"), result.snapshot.document.objects.getValue("surface-main").definition)
    }

    @Test fun multipleSurfaceLayersAreCanonicalAndLosslesslyPersisted() {
        val layers = listOf(
            SpatialSurfaceLayer(
                id = "ignored-primary-id",
                expression = "z = x^2 + y^2",
                material = SpatialMaterial.Metal,
                domain = SurfaceDomain3D(-5.0, 5.0, -4.0, 4.0),
                quality = SpatialQuality.Ultra,
                opacity = .65,
                paletteKey = "aurora",
                colorIndex = 3,
                textureKey = "contour",
                glow = false,
                renderMode = SpatialSurfaceRenderMode.Surface,
            ),
            SpatialSurfaceLayer(
                id = "surface-wave",
                expression = "z = sin(x) + cos(y)",
                visible = false,
                material = SpatialMaterial.Glass,
                quality = SpatialQuality.High,
                opacity = .4,
                renderMode = SpatialSurfaceRenderMode.Wireframe,
            ),
        )

        val changed = controller.replaceSurfaceLayers(controller.snapshot(state()), layers) as UnifiedSpatialMutation.Applied
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed.snapshot.state), recover = false).state!!

        assertEquals(listOf("surface-main", "surface-wave"), changed.snapshot.state.surfaceLayers.map { it.id })
        assertEquals(setOf("surface-main", "surface-wave"), changed.snapshot.document.objects.values.filter { it.sourceView == "3D graph" }.map { it.id }.toSet())
        assertTrue(changed.snapshot.document.objects.getValue("spatial-scene").dependencies.containsAll(setOf("surface-main", "surface-wave")))
        assertEquals(changed.snapshot.state.surfaceLayers, restored.surfaceLayers)
        assertEquals("z = x^2 + y^2", restored.surfaceExpression)
        val exported = WorkspaceJson.export(changed.snapshot.state)
        assertTrue(exported.contains("\"surfaceLayers\""))
        assertTrue(exported.contains("surface-wave"))
        assertTrue(exported.contains("\"renderMode\":\"Wireframe\""))
    }

    @Test fun deletingEverySurfaceRemovesCanonicalLayersAndSceneDependencies() {
        val changed = controller.replaceSurfaceLayers(controller.snapshot(state()), emptyList()) as UnifiedSpatialMutation.Applied

        assertTrue(changed.snapshot.state.surfaceLayers.isEmpty())
        assertEquals("0", changed.snapshot.state.surfaceExpression)
        assertTrue(changed.snapshot.document.objects.values.none { it.sourceView == "3D graph" })
        assertTrue(changed.snapshot.document.objects.getValue("spatial-scene").dependencies.none { it.startsWith("surface-") })
    }

    @Test fun schemaSixSingleSurfaceMigratesIntoPrimaryLayer() {
        val current = WorkspaceSnapshotCodec.encode(state())
        val legacy = current.lineSequence()
            .filterNot { it.startsWith("H|") }
            .joinToString("\n")
            .replaceFirst("AIEXPLORER_WORKSPACE|10|", "AIEXPLORER_WORKSPACE|6|")

        val recovered = WorkspaceSnapshotCodec.decode(legacy, recover = true)
        val recoveredState = recovered.state!!

        assertEquals(listOf("surface-main"), recoveredState.surfaceLayers.map { it.id })
        assertEquals("a*x^2 + y", recoveredState.surfaceLayers.single().expression)
        assertTrue(recovered.recovered)
    }

    @Test fun graph3dViewRoundTripsWithTheWorkspaceSnapshot() {
        val view = Graph3DViewState(
            density = 44f, rotation = -72f, tilt = 18f, roll = 12f, zoom = 2.25f,
            panX = 31f, panY = -19f, sliceZ = .75f, traceX = -1.5f, traceY = 2.5f,
            showContours = true, showSlice = true, showGradient = true, showBox = true,
            showOrientationCube = false, activeTool = "Slice", viewPreset = "XZ",
        )
        val restored = WorkspaceSnapshotCodec.decode(WorkspaceSnapshotCodec.encode(state().copy(graph3DView = view))).state!!

        assertEquals(view, restored.graph3DView)
    }

    @Test fun implicitAndParametricSurfacesValidatePersistAndRemainCanonical() {
        val layers = listOf(
            SpatialSurfaceLayer("surface-main", "x^2+y^2+z^2=4", kind = SpatialSurfaceKind.Implicit),
            SpatialSurfaceLayer("surface-torus", "cos(u)*(3+cos(v))", kind = SpatialSurfaceKind.Parametric, expressionY = "sin(u)*(3+cos(v))", expressionZ = "sin(v)"),
        )

        val changed = controller.replaceSurfaceLayers(controller.snapshot(state()), layers) as UnifiedSpatialMutation.Applied
        val restored = WorkspaceSnapshotCodec.decode(WorkspaceSnapshotCodec.encode(changed.snapshot.state)).state!!

        assertEquals(layers, restored.surfaceLayers)
        assertEquals(setOf("surface-main", "surface-torus"), changed.snapshot.document.objects.values.filter { it.sourceView == "3D graph" }.map { it.id }.toSet())
    }

    @Test fun aStaleInvalidLayerDoesNotBlockAddingAnotherGraph() {
        val stale = state().copy(
            surfaceExpression = "sin(",
            surfaceLayers = listOf(SpatialSurfaceLayer("surface-main", "sin(")),
            universalMathDocument = null,
        )
        val layers = stale.surfaceLayers + SpatialSurfaceLayer("surface-valid", "sin(x)+cos(y)")

        val changed = controller.replaceSurfaceLayers(controller.snapshot(stale), layers) as UnifiedSpatialMutation.Applied

        assertEquals(2, changed.snapshot.state.surfaceLayers.size)
        assertEquals("sin(x)+cos(y)", changed.snapshot.state.surfaceLayers[1].expression)
        assertEquals(UniversalMathValueStatus.ParseError, changed.snapshot.document.objects.getValue("surface-main").valueState.status)
    }

    @Test fun graph2dViewportAxesAndUnitsRoundTrip() {
        val view = Graph2DViewState(3.5, -2.25, 1.75f, "time", "distance", "s", "m", "Scientific", false, true, false)
        val restored = WorkspaceSnapshotCodec.decode(WorkspaceSnapshotCodec.encode(state().copy(graph2DView = view))).state!!
        assertEquals(view, restored.graph2DView)
    }

    @Test fun solidAndVectorEditsProjectBackToWorkspace() {
        val initial = controller.snapshot(state())
        val solid = controller.updateSolid(initial, 0) { it.copy(width = 5.0, position = Vec3(4.0, 5.0, 6.0)) } as UnifiedSpatialMutation.Applied
        val vector = controller.updateVector(solid.snapshot, "v") { it.copy(end = Vec3(3.0, 4.0, 0.0)) } as UnifiedSpatialMutation.Applied

        assertEquals(5.0, vector.snapshot.state.solids.single().width, 1e-9)
        assertEquals(Vec3(4.0, 5.0, 6.0), vector.snapshot.state.solids.single().position)
        assertEquals(Vec3(3.0, 4.0, 0.0), vector.snapshot.state.vectors3D.single().end)
        assertEquals(5.0, vector.snapshot.document.objects.getValue("vector-v").valueState.values.getValue("magnitude").decimal!!, 1e-9)
    }

    @Test fun invalidSpatialMutationsFailClosed() {
        val initial = controller.snapshot(state())

        assertTrue(controller.updateSolid(initial, 0) { it.copy(radius = -1.0) } is UnifiedSpatialMutation.Rejected)
        assertTrue(controller.updateVector(initial, "v") { it.copy(end = Vec3(Double.NaN, 0.0, 0.0)) } is UnifiedSpatialMutation.Rejected)
        assertTrue(controller.setParameter(initial, "a", 1000.0) is UnifiedSpatialMutation.Rejected)
    }

    @Test fun solverHandoffPreservesIdsDefinitionsAndProvenance() {
        val snapshot = controller.snapshot(state())
        val handoff = controller.solverHandoff(snapshot, setOf("surface-main", "vector-v"), "analyze").getOrThrow()

        assertEquals(setOf("surface-main", "vector-v"), handoff.sourceObjectIds)
        assertTrue(handoff.query.contains("a*x^2 + y"))
        assertTrue(handoff.query.contains("(0.0, 0.0, 0.0, 1.0, 2.0, 2.0)"))
        assertTrue(handoff.provenance.contains(snapshot.document.id))
        assertFalse(handoff.provenance.isBlank())
    }
}
