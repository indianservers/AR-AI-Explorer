package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class GraphAppearanceRegressionTest {
    @Test fun newWorkspaceAndExpressionStartEmpty() {
        assertTrue(WorkspaceState().functions.isEmpty())
        assertEquals("", GraphAddKind.Expression.starter)
        assertEquals("", graphRenderExpression("", null, 1.0))
    }

    @Test fun appearanceSurvivesProjectSaveAndLoad() {
        val function = FunctionDefinition("f", "f(x)", "x^2", "#FF3300", appearance = GraphAppearance(GraphFill.Image, "green", 8f, .6f, GraphLineStyle.Dotted, "content://images/42"))
        val state = WorkspaceState(functions = listOf(function))
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!
        assertEquals(function, restored.functions.single())
    }

    @Test fun appearanceChangesAndDuplicationSupportUndoRedo() {
        val function = FunctionDefinition("f", "f(x)", "x^2", "cyan")
        val initial = WorkspaceState(functions = listOf(function))
        val changed = function.copy(colorKey = "coral", appearance = GraphAppearance(fill = GraphFill.Gradient, width = 7f))
        val command = UpdateFunctionCommand(0, function, changed)
        val updated = command.apply(initial)
        assertEquals(changed, updated.functions.single())
        assertEquals(initial.functions, command.undo(updated).functions)
        assertEquals(changed.appearance, changed.copy(id = "duplicate").appearance)
    }
}
