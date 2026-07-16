package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.MathNotebookDocument
import com.indianservers.aiexplorer.core.MathNotebookEngine
import com.indianservers.aiexplorer.core.NotebookCellKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MathNotebookTest {
    private val engine = MathNotebookEngine()

    @Test
    fun createsNamedValuesAndReactiveDependencies() {
        var document = engine.submit(MathNotebookDocument(), "a := 2")
        document = engine.submit(document, "b := a^2 + 3")
        document = engine.submit(document, "f(x) := a*x^2 + b")

        assertEquals("2", document.cells[0].exactOutput)
        assertEquals("7", document.cells[1].exactOutput)
        assertEquals(setOf("a"), document.cells[1].dependencies)
        assertEquals(NotebookCellKind.Function, document.cells[2].kind)
        assertEquals("(2)*x^2 + (7)", document.cells[2].graphExpression)

        document = engine.submit(document, "a := 4")
        assertEquals(3, document.cells.size)
        assertEquals("19", document.cells[1].exactOutput)
        assertEquals("(4)*x^2 + (19)", document.cells[2].graphExpression)
    }

    @Test
    fun preservesExactFractionsAcrossNamedCells() {
        var document = engine.submit(MathNotebookDocument(), "p := 1/3")
        document = engine.submit(document, "q := p + 1/6")

        assertEquals("1/3", document.cells[0].exactOutput)
        assertEquals("1/2", document.cells[1].exactOutput)
        assertEquals("0.5", document.cells[1].decimalOutput)
    }

    @Test
    fun reusesPriorCellsAndRecomputesReferences() {
        var document = engine.submit(MathNotebookDocument(), "a := 2")
        document = engine.submit(document, "#1 + 10")
        assertEquals("12", document.cells[1].exactOutput)

        document = engine.submit(document, "a := 5")
        assertEquals("15", document.cells[1].exactOutput)
    }

    @Test
    fun detectsUnknownAndCircularDependencies() {
        var document = engine.submit(MathNotebookDocument(), "a := missing + 1")
        assertTrue(document.cells.single().error!!.contains("Unknown symbol"))

        document = engine.submit(document, "missing := a + 1")
        assertTrue(document.hasErrors)
        assertTrue(document.cells.all { it.error!!.contains("Circular dependency") })
    }

    @Test
    fun deletionInvalidatesDependentsAndJsonEscapesInputs() {
        var document = engine.submit(MathNotebookDocument(), "a := 2")
        document = engine.submit(document, "b := a + 1")
        document = engine.remove(document, document.cells.first().id)

        assertTrue(document.cells.single().error!!.contains("Unknown symbol"))
        val json = engine.exportJson(engine.submit(MathNotebookDocument(), "f(x) := x + 1"))
        assertTrue(json.contains("schemaVersion"))
        assertTrue(json.contains("f(x) := x + 1"))
        assertFalse(json.contains("\n"))
    }
}
