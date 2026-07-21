package com.indianservers.aiexplorer.tutor

import com.indianservers.aiexplorer.assistant.contracts.*
import kotlin.math.abs

class VisualExplanationEngine {
    fun explain(state: SimulationStateSnapshot, selected: Set<String> = emptySet(), previous: SimulationStateSnapshot? = null): VisualStateExplanation {
        val observations = state.values.entries.map { (id, value) -> "$id is ${format(value)} ${state.units[id].orEmpty()}".trim() }
        val changes = previous?.values?.mapNotNull { (id, old) -> state.values[id]?.takeIf { abs(it - old) > 1e-9 }?.let { "$id changed from ${format(old)} to ${format(it)}." } }.orEmpty()
        val causal = when (state.simulationId) {
            "wave" -> wave(state)
            "gas-law" -> gas(state)
            "motion-graph" -> motion(state)
            "electric-circuit" -> circuit(state)
            "blood-circulation" -> circulation(state)
            "titration" -> titration(state)
            else -> changes.ifEmpty { listOf("The displayed outputs are calculated from the current verified model state.") }
        }
        val formula = when (state.simulationId) {
            "wave" -> listOf("v = fλ")
            "gas-law" -> listOf("PV = nRT")
            "motion-graph" -> listOf("slope of position-time = velocity", "area under velocity-time = displacement")
            "electric-circuit" -> listOf("V = IR")
            "titration" -> listOf("n = cV")
            else -> emptyList()
        }
        val selectedText = selected.takeIf { it.isNotEmpty() }?.joinToString() ?: "the complete view"
        val summary = "This is ${state.simulationId.replace('-', ' ')} at ${state.timeMs} ms; focus is $selectedText."
        val limitations = listOf(state.labels["limitation"] ?: "This is a simplified model; values are simulated, not measured evidence.")
        return VisualStateExplanation(state.labels["title"] ?: "Explain this view", summary, observations, causal, formula, limitations, (listOf(summary) + observations + causal + formula + limitations).joinToString(" "))
    }
    private fun wave(s: SimulationStateSnapshot): List<String> { val f=s.values["frequency"]?:return emptyList();val v=s.values["speed"]?:return emptyList();val l=s.values["wavelength"]?:return emptyList();return listOf("At constant speed, increasing frequency shortens wavelength.", "Particles oscillate locally while the disturbance travels.", "The values agree because ${format(f)} × ${format(l)} = ${format(v)}.") }
    private fun gas(s: SimulationStateSnapshot)=listOf("Temperature changes average particle kinetic energy.", "The selected constant-volume or constant-pressure mode determines which container quantity responds.")
    private fun motion(s: SimulationStateSnapshot)=listOf("The graph trend is derived from the recorded position and velocity state.", "A steeper position-time slope means greater speed.")
    private fun circuit(s: SimulationStateSnapshot)=listOf("Changing resistance changes current for a fixed supply voltage.", "Charge is not consumed; components transfer electrical energy.")
    private fun circulation(s: SimulationStateSnapshot)=listOf("Highlighted vessels follow the selected pulmonary or systemic loop.", "Artery and vein names describe direction relative to the heart.")
    private fun titration(s: SimulationStateSnapshot)=listOf("Added titrant changes the amount ratio and therefore pH.", "The steep region marks rapid pH change near equivalence.")
    private fun format(v: Double) = if (abs(v - v.toLong()) < 1e-9) v.toLong().toString() else "%.3f".format(v)
}
