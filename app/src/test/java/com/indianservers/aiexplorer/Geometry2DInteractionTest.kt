package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.ConstraintFeedbackLevel
import com.indianservers.aiexplorer.workspace.Geometry2DInteractionEngine
import com.indianservers.aiexplorer.workspace.GeometryConstraint2D
import com.indianservers.aiexplorer.workspace.GeometryConstraint2DType
import com.indianservers.aiexplorer.workspace.GeometryProtocolStatus
import com.indianservers.aiexplorer.workspace.PointDependency
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Geometry2DInteractionTest {
    private fun state() = WorkspaceState(
        points = listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 2.0), Vec2(4.0, 2.0), Vec2(2.0, 0.0)),
        shapes = listOf(
            Shape2D("lower", Shape2DType.Segment, listOf(0, 1), "Lower"),
            Shape2D("upper", Shape2DType.Line, listOf(2, 3), "Upper"),
        ),
        pointDependencies = listOf(PointDependency(4, listOf(0, 1), PointDependencyType.Midpoint, "Midpoint")),
        functions = emptyList(), solids = emptyList(), vectors3D = emptyList(),
    )

    @Test fun inspectorExplainsPropertiesParentsDependentsAndContextualTools() {
        val snapshot = Geometry2DInteractionEngine.inspect(state(), selectedPoint = -1, selectedShapes = setOf(0, 1))

        assertEquals("2 selected objects", snapshot.title)
        assertTrue("P5" in snapshot.dependentIds)
        assertTrue(snapshot.tools.any { it.toolName == "Intersection" && it.enabled })
        assertTrue(snapshot.tools.any { it.toolName == "Midpoint" && it.enabled })
    }

    @Test fun dependentPointInspectorReportsItsParents() {
        val snapshot = Geometry2DInteractionEngine.inspect(state(), selectedPoint = 4, selectedShapes = emptySet())

        assertEquals("Dependent point", snapshot.kind)
        assertEquals(listOf("P1", "P2"), snapshot.parentIds)
        assertTrue(snapshot.properties.any { it.first == "Coordinates" })
    }

    @Test fun constraintFeedbackDistinguishesSatisfiedNearlyAndViolatedRelations() {
        val base = state()
        val parallel = GeometryConstraint2D("parallel", GeometryConstraint2DType.Parallel, listOf(0, 1, 2, 3))
        val perpendicular = GeometryConstraint2D("perpendicular", GeometryConstraint2DType.Perpendicular, listOf(0, 1, 2, 3))
        val fixed = GeometryConstraint2D("length", GeometryConstraint2DType.FixedLength, listOf(0, 1), target = 4.0)

        assertEquals(ConstraintFeedbackLevel.Satisfied, Geometry2DInteractionEngine.evaluateConstraint(base, parallel).level)
        assertEquals(ConstraintFeedbackLevel.Violated, Geometry2DInteractionEngine.evaluateConstraint(base, perpendicular).level)
        assertEquals(ConstraintFeedbackLevel.Satisfied, Geometry2DInteractionEngine.evaluateConstraint(base, fixed).level)
    }

    @Test fun selectedObjectsProduceGraphicallyApplicableConstraintSuggestions() {
        val suggestions = Geometry2DInteractionEngine.constraintSuggestions(state(), -1, setOf(0, 1))

        assertEquals(setOf(GeometryConstraint2DType.EqualLength, GeometryConstraint2DType.Parallel, GeometryConstraint2DType.Perpendicular), suggestions.map { it.constraint.type }.toSet())
        assertTrue(suggestions.first { it.constraint.type == GeometryConstraint2DType.Parallel }.preview.level == ConstraintFeedbackLevel.Satisfied)
    }

    @Test fun protocolTimelineUsesActualWorkspaceDependenciesAndReplayCutoff() {
        val timeline = Geometry2DInteractionEngine.protocolTimeline(state(), throughStep = 6, focusedId = "P5")

        assertTrue(timeline.dependencyChain.containsAll(setOf("P1", "P2")))
        assertEquals(GeometryProtocolStatus.Current, timeline.entries[5].status)
        assertEquals(GeometryProtocolStatus.Future, timeline.entries[6].status)
        assertTrue("P5" in timeline.visibleIds)
        assertTrue(timeline.blockedBy.isEmpty())
    }
}
