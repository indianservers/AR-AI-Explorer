package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.GraphRowMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderPlaybackMode
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.UpdateGraphRowMetadataCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathObjectGraphTest {
    @Test
    fun detectsParameterRowsFromExpressions() {
        val state = WorkspaceState(
            functions = listOf(FunctionDefinition("f", "f(x)", "a*x^2 + b", "cyan")),
        )

        val snapshot = MathObjectGraph().snapshot(state)

        assertEquals(listOf("a", "b"), snapshot.parameterRows.map { it.name })
        assertEquals(1.0, snapshot.parameter("a")!!.value, 1e-9)
        assertEquals(0.0, snapshot.parameter("b")!!.value, 1e-9)
    }

    @Test
    fun parameterEditRecomputesGraphTableAndAlgebraTogether() {
        val state = WorkspaceState(
            functions = listOf(FunctionDefinition("f", "f(x)", "a*x^2 + 1", "cyan")),
        )
        val graph = MathObjectGraph()

        val first = graph.snapshot(state, parameterValues = mapOf("a" to 2.0), tableInputs = listOf(0.0, 2.0))
        val second = graph.snapshot(state, parameterValues = graph.withParameter(first, "a", 3.0), tableInputs = listOf(0.0, 2.0))
        val firstObject = first.graphObject("f(x)")!!
        val secondObject = second.graphObject("f(x)")!!

        assertEquals("(2)*x^2 + 1", firstObject.resolvedExpression)
        assertEquals("(3)*x^2 + 1", secondObject.resolvedExpression)
        assertEquals(9.0, firstObject.table.last().output.decimal, 1e-9)
        assertEquals(13.0, secondObject.table.last().output.decimal, 1e-9)
        assertTrue(firstObject.algebra.facts.any { it == "leadingCoefficient=2" })
        assertTrue(secondObject.algebra.facts.any { it == "leadingCoefficient=3" })
        assertTrue(firstObject.sample.points.isNotEmpty())
        assertTrue(secondObject.sample.points.isNotEmpty())
    }

    @Test
    fun generatedTableCombinesVisibleExpressionOutputs() {
        val state = WorkspaceState(
            functions = listOf(
                FunctionDefinition("f", "f(x)", "x^2", "cyan"),
                FunctionDefinition("g", "g(x)", "a*x", "violet"),
            ),
        )

        val snapshot = MathObjectGraph().snapshot(state, parameterValues = mapOf("a" to 4.0), tableInputs = listOf(-1.0, 0.0, 2.0))
        val row = snapshot.generatedTable.last()

        assertEquals(2.0, row.input, 1e-9)
        assertEquals(4.0, row.outputs.getValue("f(x)"), 1e-9)
        assertEquals(8.0, row.outputs.getValue("g(x)"), 1e-9)
    }

    @Test
    fun rowMetadataKeepsColorVisibilityAndInlineErrors() {
        val state = WorkspaceState(
            functions = listOf(
                FunctionDefinition("f", "f(x)", "x^2", "green", visible = false),
                FunctionDefinition("g", "g(x)", "sin(", "amber"),
            ),
        )

        val snapshot = MathObjectGraph().snapshot(state)

        assertEquals("green", snapshot.expressionRows[0].metadata.colorKey)
        assertEquals(false, snapshot.expressionRows[0].metadata.visible)
        assertTrue(snapshot.graphObjects.none { it.name == "f(x)" })
        assertEquals("amber", snapshot.expressionRows[1].metadata.colorKey)
        assertTrue(snapshot.expressionRows[1].metadata.error?.isNotBlank() == true)
    }

    @Test
    fun workspaceJsonExportsGraphRowAndSliderMetadata() {
        val state = WorkspaceState(
            graphRowMetadata = mapOf("f" to GraphRowMetadataState(collapsed = true, folder = "Quadratics", note = "Vertex form")),
            graphSliderMetadata = mapOf("a" to GraphSliderMetadataState(speed = 2.0, mode = GraphSliderPlaybackMode.Bounce, direction = -1)),
        )

        val json = WorkspaceJson.export(state)

        assertTrue(json.contains("\"graphRowMetadata\""))
        assertTrue(json.contains("\"rowId\":\"f\""))
        assertTrue(json.contains("\"folder\":\"Quadratics\""))
        assertTrue(json.contains("\"note\":\"Vertex form\""))
        assertTrue(json.contains("\"graphSliderMetadata\""))
        assertTrue(json.contains("\"parameter\":\"a\""))
        assertTrue(json.contains("\"mode\":\"Bounce\""))
        assertTrue(json.contains("\"direction\":-1"))
    }

    @Test
    fun graphRowMetadataCommandIsUndoable() {
        val initial = WorkspaceState()
        val command = UpdateGraphRowMetadataCommand(
            "f",
            null,
            GraphRowMetadataState(collapsed = true, folder = "Trig", note = "Animated"),
        )

        val updated = command.apply(initial)
        val undone = command.undo(updated)

        assertEquals("Trig", updated.graphRowMetadata.getValue("f").folder)
        assertTrue(updated.graphRowMetadata.getValue("f").collapsed)
        assertTrue("f" !in undone.graphRowMetadata)
    }
}
