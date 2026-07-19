package com.indianservers.aiexplorer.physics.simulation

enum class SimulationPlayback { Playing, Paused }

enum class LaboratorySection(val label: String) {
    Explore("Explore"),
    Experiment("Experiment"),
    Formula("Formula"),
    Graph("Graph"),
    Explanation("Explanation"),
    Application("Real world"),
    Challenge("Challenge"),
    Quiz("Quiz"),
}

data class GraphSample(val timeSeconds: Double, val values: List<Double>)

data class MeasurementValue(val label: String, val value: String, val unit: String = "")

/**
 * Fixed-step accumulator. Render cadence can vary without changing the numerical time step.
 * Large frame gaps are clamped so returning from the background cannot produce a simulation jump.
 */
class FixedTimestepClock(
    val stepSeconds: Double = 1.0 / 120.0,
    private val maxFrameSeconds: Double = 0.1,
) {
    private var accumulatorSeconds = 0.0

    fun consume(frameSeconds: Double, speed: Double = 1.0, onStep: (Double) -> Unit): Int {
        require(frameSeconds >= 0.0) { "frame time must not be negative" }
        require(speed >= 0.0) { "simulation speed must not be negative" }
        accumulatorSeconds += frameSeconds.coerceAtMost(maxFrameSeconds) * speed
        var count = 0
        while (accumulatorSeconds + 1e-12 >= stepSeconds) {
            onStep(stepSeconds)
            accumulatorSeconds -= stepSeconds
            count++
        }
        return count
    }

    fun reset() {
        accumulatorSeconds = 0.0
    }
}
