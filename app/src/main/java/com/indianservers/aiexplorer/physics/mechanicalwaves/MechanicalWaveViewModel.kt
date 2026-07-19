package com.indianservers.aiexplorer.physics.mechanicalwaves

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.physics.core.PhysicsCalculations
import com.indianservers.aiexplorer.physics.simulation.FixedTimestepClock
import com.indianservers.aiexplorer.physics.simulation.GraphSample
import com.indianservers.aiexplorer.physics.simulation.LaboratorySection
import com.indianservers.aiexplorer.physics.simulation.SimulationPlayback

data class MechanicalWaveUiState(
    val parameters: MechanicalWaveParameters = MechanicalWaveParameters(),
    val motion: WaveMotion = WaveMotion.Transverse,
    val drive: WaveDrive = WaveDrive.Continuous,
    val medium: WaveMedium = WaveMedium.String,
    val endCondition: EndCondition = EndCondition.Open,
    val playback: SimulationPlayback = SimulationPlayback.Playing,
    val timeSeconds: Double = 0.0,
    val manualSourceMetres: Double = 0.0,
    val pulseStartedAtSeconds: Double = 0.0,
    val showParticles: Boolean = true,
    val showTrails: Boolean = false,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val markerOneMetres: Double = 1.0,
    val markerTwoMetres: Double = 3.0,
    val selectedSection: LaboratorySection = LaboratorySection.Explore,
    val graphPaused: Boolean = false,
    val graphSamples: List<GraphSample> = emptyList(),
    val challengeTargetWavelength: Double = 2.5,
    val quizChoice: Int? = null,
)

class MechanicalWaveViewModel : ViewModel() {
    private val clock = FixedTimestepClock()
    private var graphAccumulator = 0.0

    var state by mutableStateOf(MechanicalWaveUiState())
        private set

    fun togglePlayback() {
        state = state.copy(playback = if (state.playback == SimulationPlayback.Playing) SimulationPlayback.Paused else SimulationPlayback.Playing)
    }

    fun pause() {
        if (state.playback != SimulationPlayback.Paused) state = state.copy(playback = SimulationPlayback.Paused)
    }

    fun reset() {
        clock.reset()
        graphAccumulator = 0.0
        state = MechanicalWaveUiState(
            motion = state.motion,
            drive = state.drive,
            medium = state.medium,
            endCondition = state.endCondition,
            highContrast = state.highContrast,
            reducedMotion = state.reducedMotion,
        )
    }

    fun stepForward() {
        if (state.playback == SimulationPlayback.Playing) pause()
        integrate(1.0 / 60.0)
    }

    fun advanceFrame(frameSeconds: Double) {
        if (state.playback != SimulationPlayback.Playing) return
        clock.consume(frameSeconds, state.parameters.simulationSpeed) { integrate(it) }
    }

    fun setAmplitude(value: Double) = updateParameters { copy(amplitudeMetres = value.coerceIn(0.05, 1.0)) }
    fun setFrequency(value: Double) = updateParameters { copy(frequencyHz = value.coerceIn(0.25, 4.0)) }
    fun setWavelength(value: Double) = updateParameters { copy(wavelengthMetres = value.coerceIn(0.4, 5.0)) }
    fun setSpeed(value: Double) = updateParameters { copy(wavelengthMetres = (value.coerceIn(0.1, 20.0) / frequencyHz).coerceIn(0.4, 5.0)) }
    fun setPhase(value: Double) = updateParameters { copy(phaseRadians = value) }
    fun setDamping(value: Double) = updateParameters { copy(dampingPerMetre = value.coerceIn(0.0, 0.6)) }
    fun setPulseWidth(value: Double) = updateParameters { copy(pulseWidthMetres = value.coerceIn(0.15, 1.5)) }
    fun setSimulationSpeed(value: Double) = updateParameters { copy(simulationSpeed = value.coerceIn(0.1, 2.0)) }
    fun setTension(value: Double) = updateStringParameters(tension = value.coerceIn(1.0, 60.0), density = state.parameters.linearDensityKgPerMetre)
    fun setLinearDensity(value: Double) = updateStringParameters(tension = state.parameters.tensionNewtons, density = value.coerceIn(0.25, 5.0))

    fun setMotion(value: WaveMotion) { state = state.copy(motion = value) }
    fun setDrive(value: WaveDrive) { state = state.copy(drive = value); if (value == WaveDrive.Pulse) createPulse() }
    fun setMedium(value: WaveMedium) { state = state.copy(medium = value) }
    fun setEndCondition(value: EndCondition) { state = state.copy(endCondition = value) }
    fun setSection(value: LaboratorySection) { state = state.copy(selectedSection = value) }
    fun setParticles(value: Boolean) { state = state.copy(showParticles = value) }
    fun setTrails(value: Boolean) { state = state.copy(showTrails = value) }
    fun setHighContrast(value: Boolean) { state = state.copy(highContrast = value) }
    fun setReducedMotion(value: Boolean) { state = state.copy(reducedMotion = value) }
    fun setGraphPaused(value: Boolean) { state = state.copy(graphPaused = value) }
    fun setManualSource(value: Double) { state = state.copy(manualSourceMetres = value.coerceIn(-1.0, 1.0)) }
    fun releaseManualSource() { state = state.copy(manualSourceMetres = 0.0) }
    fun setMarkerOne(value: Double) { state = state.copy(markerOneMetres = value.coerceIn(0.0, MechanicalWaveEngine.MEDIUM_LENGTH_METRES)) }
    fun setMarkerTwo(value: Double) { state = state.copy(markerTwoMetres = value.coerceIn(0.0, MechanicalWaveEngine.MEDIUM_LENGTH_METRES)) }
    fun answerQuiz(index: Int) { state = state.copy(quizChoice = index) }

    fun createPulse() {
        state = state.copy(drive = WaveDrive.Pulse, pulseStartedAtSeconds = state.timeSeconds, playback = SimulationPlayback.Playing)
    }

    fun applyPreset(preset: String) {
        state = when (preset) {
            "Gentle string" -> state.copy(parameters = MechanicalWaveParameters(), motion = WaveMotion.Transverse, drive = WaveDrive.Continuous, medium = WaveMedium.String, endCondition = EndCondition.Open)
            "Fixed reflection" -> state.copy(parameters = MechanicalWaveParameters(frequencyHz = 1.0, wavelengthMetres = 2.4, dampingPerMetre = 0.02), motion = WaveMotion.Transverse, drive = WaveDrive.Pulse, medium = WaveMedium.String, endCondition = EndCondition.Fixed, pulseStartedAtSeconds = state.timeSeconds)
            "Compression" -> state.copy(parameters = MechanicalWaveParameters(amplitudeMetres = 0.3, frequencyHz = 1.2, wavelengthMetres = 1.6), motion = WaveMotion.Longitudinal, drive = WaveDrive.Continuous, endCondition = EndCondition.Open)
            "Water" -> state.copy(parameters = MechanicalWaveParameters(amplitudeMetres = 0.25, frequencyHz = 1.0, wavelengthMetres = 1.4, dampingPerMetre = 0.14), motion = WaveMotion.Transverse, drive = WaveDrive.Continuous, medium = WaveMedium.Water, endCondition = EndCondition.Open)
            else -> state
        }
    }

    private fun updateStringParameters(tension: Double, density: Double) {
        val speed = PhysicsCalculations.stringWaveSpeed(tension, density)
        val unconstrainedWavelength = speed / state.parameters.frequencyHz
        val adjustedFrequency = when {
            unconstrainedWavelength > 5.0 -> speed / 5.0
            unconstrainedWavelength < 0.4 -> speed / 0.4
            else -> state.parameters.frequencyHz
        }
        updateParameters {
            copy(
                tensionNewtons = tension,
                linearDensityKgPerMetre = density,
                frequencyHz = adjustedFrequency.coerceIn(0.25, 4.0),
                wavelengthMetres = (speed / adjustedFrequency).coerceIn(0.4, 5.0),
            )
        }
    }

    private fun updateParameters(block: MechanicalWaveParameters.() -> MechanicalWaveParameters) {
        state = state.copy(parameters = state.parameters.block())
    }

    private fun integrate(deltaSeconds: Double) {
        val newTime = state.timeSeconds + deltaSeconds
        graphAccumulator += deltaSeconds
        var samples = state.graphSamples
        if (!state.graphPaused && graphAccumulator >= 1.0 / 30.0) {
            graphAccumulator = 0.0
            val p = state.parameters
            val displacement = MechanicalWaveEngine.displacement(state.markerOneMetres, newTime, p, state.drive, state.endCondition, state.manualSourceMetres, state.pulseStartedAtSeconds)
            val velocity = MechanicalWaveEngine.velocityAt(state.markerOneMetres, newTime, p, state.drive, state.endCondition)
            samples = (samples + GraphSample(newTime, listOf(displacement, velocity))).takeLast(240)
        }
        state = state.copy(timeSeconds = newTime, graphSamples = samples)
    }
}
