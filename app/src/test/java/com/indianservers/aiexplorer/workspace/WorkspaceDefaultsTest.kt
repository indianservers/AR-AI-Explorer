package com.indianservers.aiexplorer.workspace

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceDefaultsTest {
    @Test
    fun freshGeometryCanvasesDoNotPreloadObjects() {
        val state = WorkspaceState()

        assertTrue(state.points.isEmpty())
        assertTrue(state.shapes.isEmpty())
        assertTrue(state.solids.isEmpty())
        assertTrue(state.vectors3D.isEmpty())
    }

    @Test
    fun clearWorkspaceCommandIsUndoable() {
        val before = WorkspaceState(
            points = listOf(com.indianservers.aiexplorer.core.Vec2(1.0, 2.0)),
            functions = emptyList(),
        )
        val cleared = before.copy(points = emptyList())
        val command = ReplaceWorkspaceCommand(before, cleared)

        assertTrue(command.apply(before).points.isEmpty())
        assertEquals(before.points, command.undo(cleared).points)
    }
}
