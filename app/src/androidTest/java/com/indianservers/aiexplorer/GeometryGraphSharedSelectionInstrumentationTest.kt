package com.indianservers.aiexplorer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DView
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeometryGraphSharedSelectionInstrumentationTest {
    @Test fun selectionIdentityIsMirroredAcrossConcurrentAlgebraGraphAndTablePanels() {
        val controller = Unified2DMathController()
        val initial = controller.snapshot(WorkspaceState(functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "cyan"))))
        val selected = controller.select(initial, "f")

        val panelRows = listOf(Unified2DView.Algebra, Unified2DView.Graph, Unified2DView.Table).associateWith { view ->
            selected.objectsFor(view).filter { it.selected }.map { it.id }
        }
        assertEquals(setOf("f"), selected.selection.objectIds)
        assertTrue(panelRows.values.all { it == listOf("f") })
    }
}
