package com.indianservers.aiexplorer.physics.mechanicalwaves

import com.indianservers.aiexplorer.physics.core.PhysicsCalculations
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

enum class WaveMotion(val label: String) { Transverse("Transverse"), Longitudinal("Longitudinal") }
enum class WaveDrive(val label: String) { Continuous("Continuous"), Pulse("Pulse") }
enum class WaveMedium(val label: String) { String("String"), Water("Water") }
enum class EndCondition(val label: String) { Open("Open"), Fixed("Fixed end"), Free("Free end") }

data class MechanicalWaveParameters(
    val amplitudeMetres: Double = 0.45,
    val frequencyHz: Double = 1.5,
    val wavelengthMetres: Double = 2.0,
    val phaseRadians: Double = 0.0,
    val dampingPerMetre: Double = 0.08,
    val tensionNewtons: Double = 18.0,
    val linearDensityKgPerMetre: Double = 2.0,
    val pulseWidthMetres: Double = 0.55,
    val simulationSpeed: Double = 1.0,
) {
    val speedMetresPerSecond: Double get() = PhysicsCalculations.waveSpeed(frequencyHz, wavelengthMetres)
    val periodSeconds: Double get() = PhysicsCalculations.period(frequencyHz)
}

data class WavePoint(val positionMetres: Double, val displacementMetres: Double)

object MechanicalWaveEngine {
    const val MEDIUM_LENGTH_METRES = 6.0

    fun displacement(
        xMetres: Double,
        timeSeconds: Double,
        parameters: MechanicalWaveParameters,
        drive: WaveDrive,
        endCondition: EndCondition,
        manualSourceMetres: Double = 0.0,
        pulseStartedAtSeconds: Double = 0.0,
    ): Double {
        val incident = travellingDisplacement(
            distanceMetres = xMetres,
            timeSeconds = timeSeconds,
            parameters = parameters,
            drive = drive,
            manualSourceMetres = manualSourceMetres,
            pulseStartedAtSeconds = pulseStartedAtSeconds,
        )
        if (endCondition == EndCondition.Open) return incident
        val reflectedDistance = 2.0 * MEDIUM_LENGTH_METRES - xMetres
        val reflected = travellingDisplacement(
            distanceMetres = reflectedDistance,
            timeSeconds = timeSeconds,
            parameters = parameters,
            drive = drive,
            manualSourceMetres = 0.0,
            pulseStartedAtSeconds = pulseStartedAtSeconds,
        )
        return incident + if (endCondition == EndCondition.Fixed) -reflected else reflected
    }

    fun sample(
        timeSeconds: Double,
        parameters: MechanicalWaveParameters,
        drive: WaveDrive,
        endCondition: EndCondition,
        sampleCount: Int = 161,
        manualSourceMetres: Double = 0.0,
        pulseStartedAtSeconds: Double = 0.0,
    ): List<WavePoint> {
        require(sampleCount >= 2) { "at least two samples are required" }
        return List(sampleCount) { index ->
            val x = MEDIUM_LENGTH_METRES * index / (sampleCount - 1.0)
            WavePoint(x, displacement(x, timeSeconds, parameters, drive, endCondition, manualSourceMetres, pulseStartedAtSeconds))
        }
    }

    fun velocityAt(xMetres: Double, timeSeconds: Double, parameters: MechanicalWaveParameters, drive: WaveDrive, endCondition: EndCondition): Double {
        val dt = 1e-4
        return (displacement(xMetres, timeSeconds + dt, parameters, drive, endCondition) -
            displacement(xMetres, timeSeconds - dt, parameters, drive, endCondition)) / (2.0 * dt)
    }

    private fun travellingDisplacement(
        distanceMetres: Double,
        timeSeconds: Double,
        parameters: MechanicalWaveParameters,
        drive: WaveDrive,
        manualSourceMetres: Double,
        pulseStartedAtSeconds: Double,
    ): Double {
        val speed = parameters.speedMetresPerSecond.coerceAtLeast(1e-6)
        val retardedTime = timeSeconds - distanceMetres / speed
        if (retardedTime < pulseStartedAtSeconds && drive == WaveDrive.Pulse) return 0.0
        val attenuation = exp(-parameters.dampingPerMetre * abs(distanceMetres))
        return when (drive) {
            WaveDrive.Continuous -> {
                val source = parameters.amplitudeMetres * sin(2.0 * PI * parameters.frequencyHz * retardedTime + parameters.phaseRadians)
                (source + manualSourceMetres * exp(-distanceMetres * 0.9)) * attenuation
            }
            WaveDrive.Pulse -> {
                val travelled = speed * (timeSeconds - pulseStartedAtSeconds)
                val width = parameters.pulseWidthMetres.coerceAtLeast(0.05)
                val gaussian = exp(-((distanceMetres - travelled) * (distanceMetres - travelled)) / (2.0 * width * width))
                parameters.amplitudeMetres * gaussian * attenuation
            }
        }
    }
}
