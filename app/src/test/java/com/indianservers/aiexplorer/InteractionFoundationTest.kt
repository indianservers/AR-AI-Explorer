package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test

class InteractionFoundationTest {
    @Test fun universalSelectionSupportsReplaceAddLassoAndClear() {
        var state = UniversalSelectionEngine.select(SelectionState(), "a")
        state = UniversalSelectionEngine.select(state, "b", additive = true)
        assertEquals(setOf("a", "b"), state.selectedIds)
        assertEquals("b", state.primaryId)
        assertEquals(setOf("c", "d"), UniversalSelectionEngine.lasso(listOf("c", "d")).selectedIds)
        assertTrue(UniversalSelectionEngine.clear().selectedIds.isEmpty())
    }

    @Test fun lassoAndBoundsOperateInWorldCoordinates() {
        val polygon = listOf(Vec2(-2.0, -2.0), Vec2(2.0, -2.0), Vec2(2.0, 2.0), Vec2(-2.0, 2.0))
        assertTrue(InteractionGeometry.pointInPolygon(Vec2(0.0, 0.0), polygon))
        assertFalse(InteractionGeometry.pointInPolygon(Vec2(3.0, 0.0), polygon))
        val bounds = InteractionGeometry.bounds(polygon)!!
        assertEquals(Vec2(0.0, 0.0), bounds.center)
        assertEquals(4.0, bounds.width, 0.0)
    }

    @Test fun snappingReportsGridAxesPointsCentresAndIntersections() {
        val snapped = SmartSnapEngine.snap(
            Vec2(.07, 2.08),
            nearby = listOf(Vec2(4.0, 2.0)),
            centers = listOf(Vec2(.1, 5.0)),
            intersections = listOf(Vec2(8.0, 2.1)),
        )
        assertEquals(.1, snapped.point.x, 1e-12)
        assertEquals(2.1, snapped.point.y, 1e-12)
        assertTrue(snapped.guides.any { it.kind in setOf(SnapKind.Axis, SnapKind.Grid, SnapKind.Alignment, SnapKind.Intersection, SnapKind.Center) })
    }

    @Test fun segmentIntersectionsAndRotationAreDeterministic() {
        val hits = InteractionGeometry.segmentIntersections(listOf(Vec2(-1.0, 0.0) to Vec2(1.0, 0.0), Vec2(0.0, -1.0) to Vec2(0.0, 1.0)))
        assertEquals(listOf(Vec2(0.0, 0.0)), hits)
        assertEquals(90.0, InteractionGeometry.rotationDegrees(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(0.0, 1.0)), 1e-10)
    }

    @Test fun tangentAndEqualSpacingCandidatesCanDriveSmartGuides() {
        val tangents = InteractionGeometry.tangentPoints(Vec2(2.0, 0.0), Vec2(0.0, 0.0), 1.0)
        assertEquals(2, tangents.size)
        tangents.forEach { assertEquals(1.0, it.distanceTo(Vec2(0.0, 0.0)), 1e-10) }
        val spacing = InteractionGeometry.equalSpacingCandidates(listOf(Vec2(0.0, 0.0), Vec2(2.0, 2.0)))
        assertTrue(Vec2(1.0, 1.0) in spacing)
        val snapped = SmartSnapEngine.snap(Vec2(1.04, 1.03), emptyList(), gridSize = 0.0, equalSpacing = spacing)
        assertEquals(Vec2(1.0, 1.0), snapped.point)
        assertTrue(snapped.guides.any { it.label == "equal spacing" })
    }

    @Test fun axisLocksPrecisionAndFitViewAreBounded() {
        assertEquals(Vec2(3.0, 0.0), SmartSnapEngine.constrain(Vec2(3.0, 4.0), AxisConstraint.X))
        assertEquals(Vec3(0.0, 0.0, 5.0), SmartSnapEngine.constrain(Vec3(3.0, 4.0, 5.0), AxisConstraint.Z))
        assertEquals(Vec2(.18, .36), PrecisionInteraction.apply(Vec2(1.0, 2.0), true))
        val fit = InteractionGeometry.fit(listOf(Vec2(-10.0, -2.0), Vec2(10.0, 2.0)), 2.0)
        assertEquals(Vec2(0.0, 0.0), fit.center)
        assertTrue(fit.zoom in .05f..20f)
    }
}
