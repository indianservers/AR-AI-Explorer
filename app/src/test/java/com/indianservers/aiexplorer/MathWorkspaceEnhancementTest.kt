package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.input.InkPoint
import com.indianservers.aiexplorer.input.LocalHandwritingMathRecognizer
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.DeleteShapeCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidCommand
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathWorkspaceEnhancementTest {
    @Test
    fun contextualDeleteFor2dAnd3dObjectsRemainsUndoable() {
        val shapeHistory = CommandHistory()
        val initial2d = WorkspaceState(module = MathModule.Geometry2D)
        val deleted2d = shapeHistory.execute(initial2d, DeleteShapeCommand(0, initial2d.shapes.first()))
        assertTrue(deleted2d.shapes.isEmpty())
        assertEquals(initial2d.shapes, shapeHistory.undo(deleted2d).shapes)

        val solidHistory = CommandHistory()
        val initial3d = WorkspaceState(module = MathModule.Geometry3D)
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
    fun oneHistoryUndoesAndRedoesEditsAfterSwitchingMathsWorkspace() {
        val history = CommandHistory()
        val initial = WorkspaceState(module = MathModule.Geometry2D)
        val added = history.execute(initial, AddPointCommand(Vec2(9.0, 4.0)))
        val switched = added.copy(module = MathModule.Graph2D)

        val undone = history.undo(switched)
        assertEquals(initial.points, undone.points)
        assertEquals(MathModule.Graph2D, undone.module)
        assertTrue(history.canRedo)

        val redone = history.redo(undone)
        assertEquals(added.points, redone.points)
        assertEquals(MathModule.Graph2D, redone.module)
        assertFalse(history.canRedo)
    }
}
