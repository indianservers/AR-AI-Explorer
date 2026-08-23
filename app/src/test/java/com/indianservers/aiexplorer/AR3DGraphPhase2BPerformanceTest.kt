package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphAdapterResult
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineAdapter
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEquationRequest
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphGenerationRequest
import kotlin.system.measureNanoTime
import org.junit.Assert.assertTrue
import org.junit.Test

class AR3DGraphPhase2BPerformanceTest {
    @Test
    fun recordsColdAndRepeatedEngineAdapterTimingsWithoutChangingQuality() {
        val adapter = GraphEngineAdapter(Existing3DGraphEngineBridge())
        val cases = linkedMapOf(
            "simple" to listOf("z=x^2+y^2"),
            "medium" to listOf("z=sin(sqrt(x^2+y^2))"),
            "complex" to listOf("z=(x^2-y^2)/(x^2+y^2+1)"),
            "multiple" to listOf("z=x+2*y", "z=x^2+y", "z=sin(x)+0.5*y"),
        )
        cases.forEach { (name, equations) ->
            var result: ARGraphAdapterResult? = null
            val elapsed = measureNanoTime {
                result = adapter.generate(
                    GraphGenerationRequest(
                        equations.mapIndexed { index, expression -> GraphEquationRequest("$name-$index", expression, colorIndex = index) },
                        density = 26,
                    ),
                )
            }
            assertTrue("$name generation failed", result is ARGraphAdapterResult.Success)
            val data = (result as ARGraphAdapterResult.Success).data
            println("PHASE2B_PERF case=$name elapsedMs=${elapsed / 1_000_000.0} meshes=${data.meshes.size} vertices=${data.meshes.sumOf { it.vertices.size }} indices=${data.meshes.sumOf { it.indices.size }}")
        }

        val before = usedHeap()
        repeat(5) { cycle ->
            val output = adapter.generate(
                GraphGenerationRequest(listOf(GraphEquationRequest("cycle-$cycle", "z=sin(x)+cos(y)")), density = 26),
            )
            assertTrue(output is ARGraphAdapterResult.Success)
        }
        val after = usedHeap()
        println("PHASE2B_PERF fiveCyclesHeapDeltaBytes=${after - before}")
    }

    private fun usedHeap(): Long = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }
}
