package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class GraphUxFoundationTest {
    @Test fun addMenuCoversGraphObjectKinds() {
        assertEquals(setOf("Expression", "Point", "Table", "Inequality", "Regression", "Slider"), GraphAddKind.entries.map { it.label }.toSet())
        assertTrue(GraphUxEngine.contextActions(true).containsAll(listOf("Edit", "Trace", "Domain", "Style")))
        assertTrue(GraphUxEngine.contextActions(false).containsAll(listOf("Add equation", "Fit view", "Snapshot")))
    }

    @Test fun viewHistoryMovesBackAndForwardByWholeGestures() {
        val history = GraphViewHistory()
        val first = GraphViewState(Vec2(2.0, 3.0), 2f)
        val second = GraphViewState(Vec2(-1.0, 5.0), .5f)
        history.commit(first); history.commit(second)
        assertEquals(first, history.back())
        assertEquals(GraphViewState(), history.back())
        assertEquals(first, history.forward())
    }

    @Test fun adaptiveAxisLabelsSupportPiFractionsAndScientificNotation() {
        assertEquals("π", GraphUxEngine.format(PI, AxisNumberFormat.Pi))
        assertEquals("1/3", GraphUxEngine.format(1.0 / 3.0, AxisNumberFormat.Fraction))
        assertTrue(GraphUxEngine.format(1e8, AxisNumberFormat.Adaptive).contains("e"))
    }

    @Test fun transformationsAndComparisonRemainDeterministic() {
        assertTrue(GraphUxEngine.transform("x^2", GraphTransformKind.TranslateX, 2.0).contains("x-2"))
        val engine = ExpressionEngine()
        val comparison = GraphUxEngine.compare(engine.compile("x^2"), engine.compile("x"), -2.0, 2.0, 20)
        assertEquals(21, comparison.size)
        assertEquals(0.0, comparison.minBy { kotlin.math.abs(it.x) }.difference, 1e-12)
    }

    @Test fun domainsLabelsLayersAndMinimapHaveStableGeometry() {
        val domain = GraphDomainSelection(-2.0, 4.0, leftClosed = false)
        assertFalse(domain.contains(-2.0)); assertTrue(domain.contains(0.0)); assertTrue(domain.contains(4.0))
        val labels = GraphUxEngine.avoidLabelCollisions(listOf(Vec2(0.0, 0.0), Vec2(0.0, 0.0)))
        assertNotEquals(labels[0], labels[1])
        val layers = GraphUxEngine.reorder(listOf(GraphLayer("a", 0), GraphLayer("b", 1)), "b", -1)
        assertEquals("b", layers.first().id)
        val mini = GraphUxEngine.minimap(listOf(Vec2(-5.0, -2.0), Vec2(8.0, 4.0)), GraphViewState(Vec2(1.0, 1.0), 2f))
        assertTrue(mini.world.width >= 13.0)
        assertTrue(mini.viewport.width > 0)
    }
}
