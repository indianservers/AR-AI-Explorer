package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Geometry2DDragPlannerTest {
    @Test
    fun bodyDragDetachesAFreePointSharedWithAnUnselectedShape() {
        val state = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0)),
            shapes = listOf(
                Shape2D("a", Shape2DType.Segment, listOf(0, 1)),
                Shape2D("b", Shape2DType.Segment, listOf(0, 2)),
            ),
        )

        val plan = Geometry2DDragPlanner.plan(state, 0, emptySet())

        assertTrue(plan.detached)
        assertEquals(setOf(0), plan.selectedShapeIndices)
        assertEquals(0, plan.state.shapes[1].pointIndices.first())
        assertNotEquals(0, plan.state.shapes[0].pointIndices.first())
        assertEquals(state.points[0], plan.state.points[plan.state.shapes[0].pointIndices.first()])
    }

    @Test
    fun dependencyPointsRemainConnectedAndAreNotSilentlyDetached() {
        val base = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(1.0, 0.0)),
            shapes = listOf(
                Shape2D("a", Shape2DType.Segment, listOf(0, 2)),
                Shape2D("b", Shape2DType.Segment, listOf(1, 2)),
            ),
        )
        val state = base.copy(
            pointDependencies = listOf(PointDependency(2, listOf(0, 1), PointDependencyType.Midpoint)),
        )

        val plan = Geometry2DDragPlanner.plan(state, 0, emptySet())

        assertFalse(plan.detached)
        assertEquals(2, plan.state.shapes[0].pointIndices.last())
        assertFalse(2 in plan.movablePointIndices)
    }

    @Test
    fun explicitGroupsMoveTogetherWithoutBreakingSharedJunctions() {
        val state = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0)),
            shapes = listOf(
                Shape2D("a", Shape2DType.Segment, listOf(0, 1)),
                Shape2D("b", Shape2DType.Segment, listOf(0, 2)),
            ),
        ).copy(geometryGroups = listOf(GeometryGroup2D("g", "Group", setOf("a", "b"))))

        val plan = Geometry2DDragPlanner.plan(state, 0, emptySet())

        assertEquals(setOf(0, 1), plan.selectedShapeIndices)
        assertFalse(plan.detached)
        assertEquals(setOf(0, 1, 2), plan.movablePointIndices.toSet())
    }

    @Test
    fun lockedGroupCannotBePartiallyDraggedApart() {
        val state = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0)),
            shapes = listOf(
                Shape2D("a", Shape2DType.Segment, listOf(0, 1)),
                Shape2D("b", Shape2DType.Segment, listOf(0, 2)),
            ),
        ).copy(geometryGroups = listOf(GeometryGroup2D("g", "Group", setOf("a", "b"), locked = true)))

        val plan = Geometry2DDragPlanner.plan(state, 0, emptySet())

        assertEquals(setOf(0, 1), plan.selectedShapeIndices)
        assertTrue(plan.movablePointIndices.isEmpty())
        assertFalse(plan.detached)
    }

    @Test
    fun onlySelectedShapeHandlesAndStandalonePointsCaptureResizeDrags() {
        val state = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(5.0, 5.0)),
            shapes = listOf(Shape2D("a", Shape2DType.Segment, listOf(0, 1))),
        )

        assertEquals(setOf(2), Geometry2DDragPlanner.eligibleHandleIndices(state, emptySet()))
        assertEquals(setOf(0, 1, 2), Geometry2DDragPlanner.eligibleHandleIndices(state, setOf(0)))
    }

    @Test
    fun snapshotCommandRestoresDetachedGeometryAsOneUndoOperation() {
        val before = state(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0)),
            shapes = listOf(
                Shape2D("a", Shape2DType.Segment, listOf(0, 1)),
                Shape2D("b", Shape2DType.Segment, listOf(0, 2)),
            ),
        )
        val plan = Geometry2DDragPlanner.plan(before, 0, emptySet())
        val moved = plan.state.copy(
            points = plan.state.points.mapIndexed { index, point ->
                if (index in plan.movablePointIndices) point + Vec2(1.0, 1.0) else point
            },
        )
        val command = ReplaceGeometry2DCommand(before.geometry2DSnapshot(), moved.geometry2DSnapshot())

        assertEquals(moved.points, command.apply(before).points)
        assertEquals(before.points, command.undo(moved).points)
        assertEquals(before.shapes, command.undo(moved).shapes)
    }

    private fun state(points: List<Vec2>, shapes: List<Shape2D>) = WorkspaceState(
        points = points,
        shapes = shapes,
        functions = emptyList(),
        solids = emptyList(),
        vectors3D = emptyList(),
    )
}
