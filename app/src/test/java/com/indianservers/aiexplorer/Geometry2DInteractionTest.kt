package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.ConstraintFeedbackLevel
import com.indianservers.aiexplorer.workspace.Geometry2DInteractionEngine
import com.indianservers.aiexplorer.workspace.Geometry2DDirectManipulation
import com.indianservers.aiexplorer.workspace.GeometryHandleKind
import com.indianservers.aiexplorer.workspace.GeometryMacroDraft
import com.indianservers.aiexplorer.workspace.GeometryProtocolPlayback
import com.indianservers.aiexplorer.workspace.GeometryTraceSession
import com.indianservers.aiexplorer.workspace.GeometryConstraint2D
import com.indianservers.aiexplorer.workspace.GeometryConstraint2DType
import com.indianservers.aiexplorer.workspace.GeometryGroup2D
import com.indianservers.aiexplorer.workspace.GeometryProtocolStatus
import com.indianservers.aiexplorer.workspace.PointDependency
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.WorkspaceSnapshotCodec
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

    @Test fun monitoredConstraintsSurviveWorkspaceSnapshotRoundTrip() {
        val constraint = GeometryConstraint2D("parallel", GeometryConstraint2DType.Parallel, listOf(0, 1, 2, 3))
        val group = GeometryGroup2D("pair", "Parallel pair", setOf("lower", "upper"))
        val encoded = WorkspaceSnapshotCodec.encode(state().copy(geometryConstraints = listOf(constraint), geometryGroups = listOf(group)))
        val recovered = WorkspaceSnapshotCodec.decode(encoded, recover = false).state

        assertEquals(listOf(constraint), recovered?.geometryConstraints)
        assertEquals(listOf(group), recovered?.geometryGroups)
    }

    @Test fun boxSelectionAndConsistentHandlesRemainDirectlyManipulable() {
        val state = state()
        val selected = Geometry2DDirectManipulation.boxSelect(state, Vec2(-.1, -.1), Vec2(4.1, .1))
        val handles = Geometry2DDirectManipulation.handles(state, 0)

        assertEquals(setOf(0), selected)
        assertTrue(handles.any { it.kind == GeometryHandleKind.Endpoint })
        assertTrue(handles.any { it.kind == GeometryHandleKind.Translate })
        assertTrue(handles.any { it.kind == GeometryHandleKind.Rotate })
        assertTrue(handles.any { it.kind == GeometryHandleKind.Dilate })
        assertTrue(handles.any { it.kind == GeometryHandleKind.Reflect })
    }

    @Test fun dependencyAccessibilityRecoveryAndCrossViewLinksAreStructured() {
        val state = state()
        val graph = Geometry2DDirectManipulation.accessibilityGraph(state)
        val recovery = Geometry2DDirectManipulation.recoverDegenerate(state.copy(points = state.points + Vec2(0.0, 0.0)), listOf(0, 5))
        val linked = Geometry2DDirectManipulation.crossViewHighlight("lower")

        assertTrue(graph.first { it.id == "P5" }.parents.containsAll(listOf("P1", "P2")))
        assertTrue(recovery.any { it.label.contains("Separate") })
        assertTrue("algebra:lower" in linked.algebraIds)
        assertTrue("measure:lower" in linked.measurementIds)
    }

    @Test fun protocolMacrosTraceLabelsAndKeyboardShareOneInteractionPolicy() {
        val playback = GeometryProtocolPlayback(0, 2).play().tick().tick().tick()
        val draft = Geometry2DDirectManipulation.recordMacro(GeometryMacroDraft("triangle", recording = true), listOf("P1", "P2", "P1"))
        val trace = Geometry2DDirectManipulation.appendTrace(GeometryTraceSession("P1", recording = true, limit = 2), Vec2(1.0, 1.0))
        val moved = Geometry2DDirectManipulation.movePointByKeyboard(state(), 0, 1, -1, precision = true)

        assertEquals(2, playback.step)
        assertEquals(listOf("P1", "P2"), draft.recordedIds)
        assertEquals(1, trace.samples.size)
        assertEquals(Vec2(.01, -.01), moved)
    }
}
