package com.indianservers.aiexplorer.solver.testing

import kotlin.math.ceil

data class SolverPerformanceSample(
    val operation: String,
    val durationNanos: Long,
    val itemCount: Int = 1,
)

data class SolverPerformanceSummary(
    val operation: String,
    val samples: Int,
    val medianMillis: Double,
    val p95Millis: Double,
    val maximumMillis: Double,
)

class SolverPerformanceProbe {
    private val samples = mutableListOf<SolverPerformanceSample>()

    fun <T> measure(operation: String, itemCount: Int = 1, block: () -> T): T {
        val start = System.nanoTime()
        return block().also {
            samples += SolverPerformanceSample(operation, System.nanoTime() - start, itemCount)
        }
    }

    fun summaries(): List<SolverPerformanceSummary> = samples.groupBy(SolverPerformanceSample::operation).map { (operation, values) ->
        val sorted = values.map(SolverPerformanceSample::durationNanos).sorted()
        SolverPerformanceSummary(
            operation = operation,
            samples = sorted.size,
            medianMillis = percentile(sorted, .50) / 1_000_000.0,
            p95Millis = percentile(sorted, .95) / 1_000_000.0,
            maximumMillis = sorted.lastOrNull()?.div(1_000_000.0) ?: 0.0,
        )
    }

    fun clear() {
        samples.clear()
    }

    private fun percentile(sorted: List<Long>, percentile: Double): Long {
        if (sorted.isEmpty()) return 0L
        val index = (ceil(sorted.size * percentile).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }
}
