package com.indianservers.aiexplorer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GeometryGraphDevicePerformanceInstrumentationTest {
    @Test fun atomicDragCommitAndMemoryStayInsideDeviceValidationEnvelope() {
        val controller = Unified2DMathController()
        val points = List(500) { Vec2((it % 25).toDouble(), (it / 25).toDouble()) }
        var snapshot = controller.snapshot(WorkspaceState(points = points, shapes = emptyList()))
        repeat(5) { warmup ->
            val result = controller.transformPoints(snapshot, mapOf("point-0" to Vec2(warmup.toDouble(), 1.0)))
            snapshot = (result as Unified2DMutation.Applied).snapshot
        }
        val runtime = Runtime.getRuntime(); System.gc()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        val samples = LongArray(40)
        repeat(samples.size) { index ->
            val started = System.nanoTime()
            val result = controller.transformPoints(snapshot, mapOf("point-0" to Vec2(index.toDouble() / 10, 2.0)))
            snapshot = (result as Unified2DMutation.Applied).snapshot
            samples[index] = System.nanoTime() - started
        }
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val p95Millis = samples.sorted()[37] / 1_000_000.0
        assertTrue("Atomic commit p95 was ${p95Millis}ms", p95Millis < 100.0)
        assertTrue("Stress pass retained more than 64 MiB", memoryAfter - memoryBefore < 64L * 1024 * 1024)
    }
}
