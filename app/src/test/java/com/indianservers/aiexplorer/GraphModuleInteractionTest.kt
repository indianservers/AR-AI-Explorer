package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.GraphViewport
import com.indianservers.aiexplorer.core.GraphEquationNames
import com.indianservers.aiexplorer.core.GraphWorkspaceAction
import com.indianservers.aiexplorer.core.GraphWorkspaceReducer
import com.indianservers.aiexplorer.core.GraphWorkspaceState
import org.junit.Assert.*
import org.junit.Test

class GraphModuleInteractionTest {
    @Test fun equationNamesRemainValidAndUniqueBeyondAlphabeticRows() {
        val names = (0 until 100).map(GraphEquationNames::at)
        assertEquals(100, names.distinct().size)
        assertEquals("f(x)", names.first())
        assertEquals("z(x)", names[20])
        assertEquals("f22(x)", names[21])
        assertEquals("g(x)", GraphEquationNames.next(setOf("f(x)")))
        assertEquals("f(x)", GraphEquationNames.next(setOf("g(x)", "h(x)")))
    }

    @Test fun userCanAddEditHideAndRemoveManyEquations() {
        val reducer = GraphWorkspaceReducer()
        var state = GraphWorkspaceState()
        repeat(40) { index -> state = reducer.reduce(state, GraphWorkspaceAction.AddExpression("x+${index + 1}")) }
        assertEquals(40, state.rows.size)
        assertEquals(40, state.rows.map { it.id }.distinct().size)
        val selected = state.rows[21]
        state = reducer.reduce(state, GraphWorkspaceAction.EditExpression(selected.id, "sin(x)+3"))
        assertEquals("sin(x)+3", state.rows.first { it.id == selected.id }.source)
        state = reducer.reduce(state, GraphWorkspaceAction.ToggleVisibility(selected.id))
        assertFalse(state.rows.first { it.id == selected.id }.visible)
        state = reducer.reduce(state, GraphWorkspaceAction.DeleteExpression(selected.id))
        assertEquals(39, state.rows.size)
        assertFalse(state.rows.any { it.id == selected.id })
    }

    @Test fun adaptiveAxesStayReadableAtAnyPanAndZoom() {
        listOf(
            -7.0 to 7.0,
            1_000_000.0 to 1_000_014.0,
            -0.000_007 to 0.000_007,
            -700_000.0 to 700_000.0,
        ).forEach { (minimum, maximum) ->
            val ticks = GraphViewport.ticks(minimum, maximum)
            assertTrue("$minimum..$maximum produced ${ticks.size}", ticks.size in 7..16)
            assertTrue(ticks.zipWithNext().all { (a, b) -> b > a })
        }
    }

    @Test fun zoomRangeSupportsDeepInspectionAndWideOverview() {
        assertEquals(GraphViewport.maximumZoom, GraphViewport.zoom(1f, 1_000f))
        assertEquals(GraphViewport.minimumZoom, GraphViewport.zoom(1f, .00001f))
        assertEquals(2f, GraphViewport.zoom(1f, 2f), 0f)
    }
}
