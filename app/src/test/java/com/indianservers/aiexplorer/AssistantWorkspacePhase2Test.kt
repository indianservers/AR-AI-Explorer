package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantActionType
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantCommandParser
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantCommandType
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantContextFactory
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantSummarizer
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantTargetKind
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantWorkspacePhase2Test {
    @Test fun selected2DShapeSummaryIncludesDeleteAndMeasurements() {
        val state = WorkspaceState(
            module = MathModule.Geometry2D,
            points = listOf(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 3.0)),
            shapes = listOf(Shape2D("tri-1", Shape2DType.Triangle, listOf(0, 1, 2), "Triangle A")),
        )
        val context = WorkspaceAssistantContextFactory.from(state, selectedShapeIndex = 0)
        val summary = WorkspaceAssistantSummarizer.summarize(state, selectedShapeIndex = 0)
        assertEquals(WorkspaceAssistantTargetKind.SHAPE_2D, context.targetKind)
        assertEquals("Triangle A", summary.title)
        assertTrue(summary.measurements.any { "Area 6" in it })
        assertTrue(summary.nextActions.any { it.type == WorkspaceAssistantActionType.DELETE && it.enabled })
        assertTrue(summary.nextActions.all { it.compact })
    }

    @Test fun locked2DShapeDisablesDestructiveCompactActions() {
        val state = WorkspaceState(
            module = MathModule.Geometry2D,
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 2.0)),
            shapes = listOf(Shape2D("line-1", Shape2DType.Segment, listOf(0, 1), "Locked segment", locked = true)),
        )
        val summary = WorkspaceAssistantSummarizer.summarize(state, selectedShapeIndex = 0)
        assertFalse(summary.nextActions.single { it.type == WorkspaceAssistantActionType.DELETE }.enabled)
        assertFalse(summary.nextActions.single { it.type == WorkspaceAssistantActionType.ROTATE }.enabled)
    }

    @Test fun selected3DSolidSummaryIncludesFormulasAndActions() {
        val state = WorkspaceState(
            module = MathModule.Geometry3D,
            solids = listOf(Solid(SolidType.Cube, width = 2.0)),
        )
        val summary = WorkspaceAssistantSummarizer.summarize(state, selectedSolidIndex = 0)
        assertEquals("Cube", summary.title)
        assertTrue(summary.measurements.any { "Volume 8" in it })
        assertTrue(summary.formulas.any { "V = a" in it })
        assertTrue(summary.nextActions.any { it.type == WorkspaceAssistantActionType.RESIZE })
    }

    @Test fun workspaceCommandParserUnderstandsGeometryActions() {
        val twoD = WorkspaceState(module = MathModule.Geometry2D)
        val threeD = WorkspaceState(module = MathModule.Geometry3D)
        assertEquals(WorkspaceAssistantCommandType.ADD_2D_SHAPE, WorkspaceAssistantCommandParser.parse("draw triangle", twoD).type)
        assertEquals("Triangle", WorkspaceAssistantCommandParser.parse("draw triangle", twoD).targetName)
        assertEquals(WorkspaceAssistantCommandType.ADD_3D_SOLID, WorkspaceAssistantCommandParser.parse("add cube", threeD).type)
        assertEquals(WorkspaceAssistantCommandType.DELETE_SELECTED, WorkspaceAssistantCommandParser.parse("delete selected object", twoD).type)
        assertEquals(15.0, WorkspaceAssistantCommandParser.parse("rotate selected", twoD).amount!!, 0.0)
        assertEquals(.9, WorkspaceAssistantCommandParser.parse("make it smaller", threeD).amount!!, 0.0)
        assertEquals(WorkspaceAssistantCommandType.CLEAR_ALL, WorkspaceAssistantCommandParser.parse("clear all", twoD).type)
    }

    @Test fun graphWorkspaceSummaryUsesVisibleExpressions() {
        val state = WorkspaceState(module = MathModule.Graph2D)
        val summary = WorkspaceAssistantSummarizer.summarize(state)
        assertEquals("2D graph workspace", summary.title)
        assertTrue(summary.measurements.any { "f" in it && "x^2" in it })
        assertTrue(summary.nextActions.any { it.type == WorkspaceAssistantActionType.ADD })
    }
}
