package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.PointDependency
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.Unified2DView
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Unified2DMathControllerTest {
    private val controller = Unified2DMathController()

    private fun state() = WorkspaceState(
        points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)),
        shapes = listOf(Shape2D("ab", Shape2DType.Segment, listOf(0, 1), "AB")),
        functions = listOf(
            FunctionDefinition("f", "f(x)", "x^2", "cyan"),
            FunctionDefinition("g", "g(x)", "1", "violet"),
        ),
    )

    @Test fun projectionUsesSameIdsAndSelectionAcrossGeometryGraphAndAlgebra() {
        val snapshot = controller.snapshot(state())
        val selected = controller.select(snapshot, "point-0")

        assertTrue(selected.objectsFor(Unified2DView.Geometry).any { it.id == "point-0" && it.selected })
        assertTrue(selected.objectsFor(Unified2DView.Graph).any { it.id == "point-0" && it.selected })
        assertTrue(selected.objectsFor(Unified2DView.Algebra).any { it.id == "point-0" && it.selected })
        assertEquals("point-0", selected.selection.primaryId)
    }

    @Test fun coordinateAndExpressionEditsRoundTripThroughAuthoritativeDocument() {
        val initial = controller.snapshot(state())
        val moved = controller.editCoordinates(initial, "point-0", Vec2(3.0, 4.0)) as Unified2DMutation.Applied
        val edited = controller.editExpression(moved.snapshot, "f", "x^3 - 1") as Unified2DMutation.Applied

        assertEquals(Vec2(3.0, 4.0), moved.snapshot.state.points[0])
        assertEquals("x^3 - 1", edited.snapshot.state.functions.first { it.id == "f" }.expression)
        assertTrue("point-0" in moved.affectedObjects)
        assertTrue("f" in edited.affectedObjects)
    }

    @Test fun dependentCoordinateEditsAreRejected() {
        val dependent = state().copy(
            points = state().points + Vec2(1.0, 0.0),
            pointDependencies = listOf(PointDependency(2, listOf(0, 1), PointDependencyType.Midpoint, "M")),
        )

        val result = controller.editCoordinates(controller.snapshot(dependent), "point-2", Vec2(8.0, 8.0))

        assertTrue(result is Unified2DMutation.Rejected)
        assertTrue((result as Unified2DMutation.Rejected).message.contains("construction"))
    }

    @Test fun presentationUpdatesGraphAndGeometryWorkspaceFields() {
        val initial = controller.snapshot(state())
        val hiddenFunction = controller.updatePresentation(initial, "f") { it.copy(visible = false, colorKey = "amber") } as Unified2DMutation.Applied
        val lockedShape = controller.updatePresentation(hiddenFunction.snapshot, "ab") { it.copy(locked = true, styleKey = "dashed") } as Unified2DMutation.Applied

        assertFalse(lockedShape.snapshot.state.functions.first { it.id == "f" }.visible)
        assertEquals("amber", lockedShape.snapshot.state.functions.first { it.id == "f" }.colorKey)
        assertTrue(lockedShape.snapshot.state.shapes.single().locked)
        assertEquals("dashed", lockedShape.snapshot.state.shapes.single().styleKey)
    }

    @Test fun graphIntersectionBecomesReusableGeometryPointWithFunctionDependencies() {
        val result = controller.createGraphIntersectionPoint(controller.snapshot(state()), "f", "g", branch = 0) as Unified2DMutation.Applied
        val point = result.snapshot.document.objects.getValue("point-2")

        assertEquals(setOf("f", "g"), point.dependencies)
        assertEquals(UniversalMathDefinition.Construction("Intersect", listOf("f", "g", "0")), point.definition)
        assertEquals(3, result.snapshot.state.points.size)
        assertEquals(1.0, kotlin.math.abs(result.snapshot.state.points.last().x), 1e-4)
        assertEquals("point-2", result.snapshot.selection.primaryId)
    }
}
