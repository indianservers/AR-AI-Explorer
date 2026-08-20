package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.input.InkPoint
import com.indianservers.aiexplorer.input.LocalHandwritingMathRecognizer
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.DeleteShapeCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidCommand
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.WorkspaceHistoryByModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathWorkspaceEnhancementTest {
    @Test
    fun contextualDeleteFor2dAnd3dObjectsRemainsUndoable() {
        val shapeHistory = CommandHistory()
        val initial2d = WorkspaceState(
            module = MathModule.Geometry2D,
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)),
            shapes = listOf(Shape2D("segment", Shape2DType.Segment, listOf(0, 1))),
        )
        val deleted2d = shapeHistory.execute(initial2d, DeleteShapeCommand(0, initial2d.shapes.first()))
        assertTrue(deleted2d.shapes.isEmpty())
        assertEquals(initial2d.shapes, shapeHistory.undo(deleted2d).shapes)

        val solidHistory = CommandHistory()
        val initial3d = WorkspaceState(module = MathModule.Geometry3D, solids = listOf(Solid(SolidType.Cube, width = 2.0)))
        val deleted3d = solidHistory.execute(initial3d, DeleteSolidCommand(0, initial3d.solids.first()))
        assertEquals(initial3d.solids.size - 1, deleted3d.solids.size)
        assertEquals(initial3d.solids, solidHistory.undo(deleted3d).solids)
    }

    @Test
    fun handwritingRecognizerConvertsBasicInkToMathsSymbols() {
        val horizontal = listOf(InkPoint(5f, 50f), InkPoint(95f, 50f))
        val vertical = listOf(InkPoint(50f, 5f), InkPoint(50f, 95f))

        assertEquals("-", LocalHandwritingMathRecognizer.recognize(listOf(horizontal)).primary)
        assertEquals("1", LocalHandwritingMathRecognizer.recognize(listOf(vertical)).primary)
        assertEquals("+", LocalHandwritingMathRecognizer.recognize(listOf(horizontal, vertical)).primary)
    }

    @Test
    fun undoAndRedoRemainIsolatedAfterSwitchingMathsWorkspace() {
        val histories = WorkspaceHistoryByModule()
        val initial = WorkspaceState(module = MathModule.Geometry2D)
        val geometryHistory = histories.forModule(MathModule.Geometry2D)
        val added = geometryHistory.execute(initial, AddPointCommand(Vec2(9.0, 4.0)))
        val switched = added.copy(module = MathModule.Graph2D)

        val graphHistory = histories.forModule(MathModule.Graph2D)
        val unchanged = graphHistory.undo(switched)
        assertEquals(added.points, unchanged.points)
        assertEquals(MathModule.Graph2D, unchanged.module)
        assertFalse(graphHistory.canRedo)

        val geometryState = unchanged.copy(module = MathModule.Geometry2D)
        val undone = geometryHistory.undo(geometryState)
        assertEquals(initial.points, undone.points)
        assertTrue(geometryHistory.canRedo)
        val redone = geometryHistory.redo(undone)
        assertEquals(added.points, redone.points)
        assertEquals(MathModule.Geometry2D, redone.module)
        assertFalse(geometryHistory.canRedo)
    }
}
