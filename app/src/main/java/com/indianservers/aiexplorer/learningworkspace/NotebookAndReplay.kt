package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.assistant.contracts.SimulationStateSnapshot
import java.time.Instant

class ExperimentNotebook(private val entries: MutableMap<String, ExperimentNotebookEntry> = mutableMapOf()) {
    fun save(entry: ExperimentNotebookEntry): ExperimentNotebookEntry = entry.copy(updatedAt = Instant.now()).also { entries[it.id] = it }
    fun get(id: String) = entries[id]
    fun all() = entries.values.sortedByDescending { it.updatedAt }
    fun addObservation(id: String, text: String) = update(id) { it.copy(observations = it.observations + text) }
    fun capture(id: String, state: SimulationStateSnapshot) = update(id) { entry -> entry.copy(measurements = entry.measurements + state.values.map { (key, value) -> NotebookMeasurement(key, value, state.units[key].orEmpty()) }, savedStateIds = entry.savedStateIds + state.id) }
    fun saveGraph(id: String, graphReference: String) = update(id) { it.copy(graphReferences = it.graphReferences + graphReference) }
    fun conclude(id: String, conclusion: String) = update(id) { it.copy(conclusion = conclusion) }
    private fun update(id: String, action: (ExperimentNotebookEntry) -> ExperimentNotebookEntry): ExperimentNotebookEntry { val current = entries[id] ?: error("Unknown notebook entry $id"); return save(action(current)) }
}

fun interface ReplayReducer { fun reduce(state: SimulationStateSnapshot, event: SimulationInputEvent): SimulationStateSnapshot }
class DeterministicReplayEngine(private val reducer: ReplayReducer) {
    fun stateAt(replay: SimulationReplay, timeMs: Long): SimulationStateSnapshot {
        require(timeMs in 0..replay.durationMs)
        val base = replay.snapshots.filter { it.timeMs <= timeMs }.maxByOrNull { it.timeMs } ?: replay.initialState
        return replay.events.filter { it.atMs > base.timeMs && it.atMs <= timeMs }.sortedBy { it.atMs }.fold(base) { state, event -> reducer.reduce(state, event).copy(timeMs = event.atMs) }
    }
    fun deterministic(replay: SimulationReplay, tolerance: Double = 1e-9): Boolean {
        val one = stateAt(replay, replay.durationMs); val two = stateAt(replay, replay.durationMs)
        return one.values.keys == two.values.keys && one.values.all { (key, value) -> kotlin.math.abs(value - two.values.getValue(key)) <= tolerance }
    }
    fun stepBackward(replay: SimulationReplay, atMs: Long) = stateAt(replay, replay.events.filter { it.atMs < atMs }.maxOfOrNull { it.atMs } ?: 0)
    fun stepForward(replay: SimulationReplay, atMs: Long) = stateAt(replay, replay.events.filter { it.atMs > atMs }.minOfOrNull { it.atMs } ?: replay.durationMs)
}
