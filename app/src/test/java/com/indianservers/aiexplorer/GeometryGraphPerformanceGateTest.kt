package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class GeometryGraphPerformanceGateTest {
    @Test fun thousandPointDocumentTransformsAtomicallyWithinHostStressBudget() {
        val points = List(1_000) { Vec2((it % 50).toDouble(), (it / 50).toDouble()) }
        val controller = Unified2DMathController()
        val initial = controller.snapshot(WorkspaceState(points = points, shapes = emptyList()))
        lateinit var result: Unified2DMutation
        val elapsed = measureTimeMillis {
            result = controller.transformPoints(initial, (0 until 100).associate { "point-$it" to (points[it] + Vec2(1.0, -1.0)) })
        }

        assertTrue(result is Unified2DMutation.Applied)
        assertEquals(initial.document.revision + 1, (result as Unified2DMutation.Applied).snapshot.document.revision)
        assertTrue("Host stress gate took ${elapsed}ms", elapsed < 5_000)
    }

    @Test fun denseGraphSchemaAnalysisStaysInsideBoundedMemorySizedWorkload() {
        val functions = List(80) { index -> FunctionDefinition("f$index", "f$index(x)", "x^2+${index}", "cyan") }
        lateinit var snapshot: com.indianservers.aiexplorer.workspace.MathObjectGraphSnapshot
        val elapsed = measureTimeMillis { snapshot = MathObjectGraph().snapshot(WorkspaceState(functions = functions), tableInputs = (-20..20).map(Int::toDouble)) }

        assertEquals(80, snapshot.graphObjects.size)
        assertTrue(snapshot.graphObjects.all { it.advancedFeatures != null })
        assertTrue("Graph stress gate took ${elapsed}ms", elapsed < 15_000)
    }
}
