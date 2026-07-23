package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.phase2.mathstudio.SharedExperienceEngine
import com.indianservers.aiexplorer.phase2.mathstudio.SharedMathView
import com.indianservers.aiexplorer.phase2.mathstudio.SharedObjectGraphBuilder
import com.indianservers.aiexplorer.phase2.mathstudio.SharedStudioHistory
import com.indianservers.aiexplorer.phase2.mathstudio.SharedWorkspaceMode
import com.indianservers.aiexplorer.phase2.mathstudio.StudioSessionRecorder
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedMathStudioEngine
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMathExperienceTest {
    private val studio = UnifiedMathStudioEngine()
    private val shared = SharedExperienceEngine()
    private val workspace = WorkspaceState(functions = listOf(
        FunctionDefinition("f", "f(x)", "x^2", "cyan"),
        FunctionDefinition("g", "g(x)", "f(x)+1", "violet"),
    ))

    @Test fun universalGraphProvidesLinkedRepresentationsAndDependencies() {
        val session = studio.construct(studio.fromWorkspace(workspace), "point2d(A,0,0)")
        val graph = SharedObjectGraphBuilder.build(session)
        assertTrue(graph.nodes.keys.containsAll(listOf("f@graph", "f@table", "f@cas", "point-0@geometry2d", "surface-main@spatial3d")))
        assertTrue("A@geometry2d" in graph.nodes)
        assertTrue("A@algebra" in graph.nodes)
        assertEquals(setOf("f"), graph.dependencies("g"))
        assertTrue(graph.representations("f").map { it.view }.containsAll(listOf(SharedMathView.Graph, SharedMathView.Table, SharedMathView.Cas)))
    }

    @Test fun selectionModeLayoutAndProvenanceStaySynchronized() {
        var session = studio.fromWorkspace(workspace)
        session = shared.select(session, "g@graph")
        assertEquals("g", session.selectedId)
        assertTrue("g@cas" in session.experience.selection.representationIds)
        session = shared.setMode(session, SharedWorkspaceMode.Research)
        assertEquals(4, session.experience.layout.activeViews.size)
        session = shared.toggleView(session, SharedMathView.Table)
        assertTrue(SharedMathView.Table in session.experience.layout.activeViews)
        val provenance = shared.inspectProvenance(session, "g")
        assertEquals(listOf("f"), provenance.directDependencies)
        assertTrue(provenance.explanation.contains("graph/CAS/table"))
    }

    @Test fun semanticHistoryPreservesAlternateFutureAsBranch() {
        val initial = studio.fromWorkspace(workspace)
        val history = SharedStudioHistory(initial)
        history.apply(studio.parameter(initial, "a", 1.0), "Set a to 1")
        history.apply(studio.parameter(history.current, "a", 2.0), "Set a to 2")
        history.undo()
        history.apply(studio.parameter(history.current, "a", 3.0), "Try a = 3")
        history.undo()
        assertEquals(2, history.branches().size)
        val branches = history.branches()
        val comparison = history.compare(branches[0].id, branches[1].id)
        assertEquals(setOf("parameter:a"), comparison.changedObjects)
        history.merge(branches[1].id)
        assertEquals(3.0, history.current.parameterValues.getValue("a"), 0.0)
        assertEquals(listOf("Set a to 1", "Merge preferred exploration branch"), history.timeline().map { it.label })
        assertFalse(history.canRedo)
    }

    @Test fun recorderReplaysInteractiveStatesAndModes() {
        var now = 100L
        val recorder = StudioSessionRecorder { now }
        val initial = studio.fromWorkspace(workspace)
        recorder.start("Quadratic exploration", initial)
        now = 160L
        val changed = studio.parameter(initial, "a", 4.0)
        recorder.capture("Move a", changed)
        now = 220L
        val recording = recorder.stop()
        assertFalse(recorder.isRecording)
        assertEquals(120L, recording.durationMillis)
        assertEquals(null, recorder.stateAt(recording, 20).parameterValues["a"])
        assertEquals(4.0, recorder.stateAt(recording, 80).parameterValues["a"]!!, 0.0)
    }
}
