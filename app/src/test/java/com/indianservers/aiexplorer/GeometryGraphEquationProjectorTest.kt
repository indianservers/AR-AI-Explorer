package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.Unified2DView
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryGraphEquationProjectorTest {
    private val controller = Unified2DMathController()

    @Test fun lineConstructionExposesStableEquationAndExactLength() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 2.0)),
            shapes = listOf(Shape2D("line-ab", Shape2DType.Line, listOf(0, 1), "AB")),
        )

        val snapshot = controller.snapshot(state)
        val equation = snapshot.document.objects.getValue("line-ab:equation")
        val measurement = snapshot.document.objects.getValue("line-ab:measurement")

        assertEquals(UniversalMathKind.Equation, equation.kind)
        assertEquals("1*x + -1*y = 0", (equation.payload as UniversalMathPayload.Properties).entries.getValue("equation"))
        assertEquals("sqrt(8)", (measurement.payload as UniversalMathPayload.Properties).entries.getValue("exact"))
        assertTrue(snapshot.objectsFor(Unified2DView.Graph).any { it.id == equation.id })
    }

    @Test fun movingGeometryPointRefreshesEquationAndMarksDerivedObjectsAffected() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 2.0)),
            shapes = listOf(Shape2D("line-ab", Shape2DType.Line, listOf(0, 1), "AB")),
        )
        val before = controller.snapshot(state)

        val result = controller.editCoordinates(before, "point-1", Vec2(2.0, 4.0)) as Unified2DMutation.Applied
        val equation = result.snapshot.document.objects.getValue("line-ab:equation")

        assertEquals("1*x + -0.5*y = 0", (equation.payload as UniversalMathPayload.Properties).entries.getValue("equation"))
        assertTrue("line-ab:equation" in result.affectedObjects)
        assertTrue("line-ab:measurement" in result.affectedObjects)
    }

    @Test fun circleAndPolygonExposeEquationsAndMeasurements() {
        val state = WorkspaceState(
            points = listOf(Vec2(1.0, 2.0), Vec2(4.0, 2.0), Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 3.0)),
            shapes = listOf(
                Shape2D("circle", Shape2DType.Circle, listOf(0, 1), "c"),
                Shape2D("triangle", Shape2DType.Triangle, listOf(2, 3, 4), "ABC"),
            ),
        )

        val snapshot = controller.snapshot(state)
        val circle = snapshot.document.objects.getValue("circle:equation")
        val area = snapshot.document.objects.getValue("triangle:measurement")

        assertEquals("(x - 1)^2 + (y - 2)^2 = 9", (circle.payload as UniversalMathPayload.Properties).entries.getValue("equation"))
        assertEquals("6", (area.payload as UniversalMathPayload.Properties).entries.getValue("exact"))
    }

    @Test fun threePointCircleRejectsCollinearEquationProjection() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(2.0, 2.0)),
            shapes = listOf(Shape2D("circle", Shape2DType.CircleThreePoints, listOf(0, 1, 2), "c")),
        )

        val snapshot = controller.snapshot(state)

        assertTrue("circle:equation" !in snapshot.document.objects)
    }
}
