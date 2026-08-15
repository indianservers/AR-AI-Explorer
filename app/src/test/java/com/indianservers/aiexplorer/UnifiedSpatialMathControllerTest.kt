package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMutation
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.WorkspaceState
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
