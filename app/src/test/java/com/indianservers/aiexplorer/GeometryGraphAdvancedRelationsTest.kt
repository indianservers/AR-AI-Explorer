package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.PointDependency
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.Unified2DView
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.recomputed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryGraphAdvancedRelationsTest {
    private val controller = Unified2DMathController()

    @Test fun axisAlignedEllipseProducesExactCanonicalEquationAndMajorAxis() {
        val state = WorkspaceState(
            points = listOf(Vec2(-2.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, kotlin.math.sqrt(5.0))),
            shapes = listOf(Shape2D("ellipse", Shape2DType.Ellipse, listOf(0, 1, 2), "e")),
        )
        val snapshot = controller.snapshot(state)

        val equation = snapshot.document.objects.getValue("ellipse:equation")
        val axis = snapshot.document.objects.getValue("ellipse:measurement")
        assertEquals("(x - 0)^2/9 + (y - 0)^2/5 = 1", (equation.payload as UniversalMathPayload.Properties).entries.getValue("equation"))
        assertEquals("6", (axis.payload as UniversalMathPayload.Properties).entries.getValue("exact"))
    }

    @Test fun rotatedEllipseRetainsFocusOrientationInEquation() {
        val root = kotlin.math.sqrt(2.0)
        val state = WorkspaceState(
            points = listOf(Vec2(-root, -root), Vec2(root, root), Vec2(-3 / root, 3 / root)),
            shapes = listOf(Shape2D("ellipse", Shape2DType.Ellipse, listOf(0, 1, 2), "rotated")),
        )
        val equation = (controller.snapshot(state).document.objects.getValue("ellipse:equation").payload as UniversalMathPayload.Properties).entries.getValue("equation")

        assertTrue(equation.contains("0.7071067812"))
        assertTrue(equation.contains("x - 0"))
        assertTrue(equation.contains("y - 0"))
    }

    @Test fun parallelAndPerpendicularConstructionsExposeGraphRelations() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 1.0), Vec2(0.0, 0.0), Vec2(2.0, 2.0)),
            shapes = listOf(
                Shape2D("parallel", Shape2DType.Parallel, listOf(0, 1, 2), "p"),
                Shape2D("perpendicular", Shape2DType.Perpendicular, listOf(0, 1, 2), "n"),
            ),
        )
        val snapshot = controller.snapshot(state)

        assertEquals("1*x + -1*y = -1", ((snapshot.document.objects.getValue("parallel:equation").payload as UniversalMathPayload.Properties).entries.getValue("equation")))
        assertEquals("1*x + 1*y = 1", ((snapshot.document.objects.getValue("perpendicular:equation").payload as UniversalMathPayload.Properties).entries.getValue("equation")))
    }

    @Test fun transformedPointCreatesStableRelationAndRefreshesWithParent() {
        val state = WorkspaceState(
            points = listOf(Vec2(1.0, 2.0), Vec2(4.0, 5.0)),
            pointDependencies = listOf(PointDependency(1, listOf(0), PointDependencyType.Translate, "B", listOf(3.0, 3.0))),
        ).recomputed()
        val before = controller.snapshot(state)
        val relation = before.document.objects.getValue("point-1:relation")

        assertEquals(UniversalMathKind.Relation, relation.kind)
        assertEquals("point-1 = Translate(point-0, 3, 3)", (relation.payload as UniversalMathPayload.Properties).entries.getValue("relation"))
        assertTrue(before.objectsFor(Unified2DView.Graph).any { it.id == relation.id })

        val moved = controller.editCoordinates(before, "point-0", Vec2(2.0, 4.0)) as Unified2DMutation.Applied
        val refreshed = moved.snapshot.document.objects.getValue("point-1:relation")
        assertEquals(5.0, refreshed.valueState.values.getValue("x").decimal!!, 1e-9)
        assertEquals(7.0, refreshed.valueState.values.getValue("y").decimal!!, 1e-9)
        assertTrue(refreshed.id in moved.affectedObjects)
    }

    @Test fun degenerateEllipseFailsClosedWithoutEquation() {
        val state = WorkspaceState(
            points = listOf(Vec2(-2.0, 0.0), Vec2(2.0, 0.0), Vec2(2.0, 0.0)),
            shapes = listOf(Shape2D("ellipse", Shape2DType.Ellipse, listOf(0, 1, 2), "e")),
        )

        assertTrue("ellipse:equation" !in controller.snapshot(state).document.objects)
    }
}
