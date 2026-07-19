package com.indianservers.aiexplorer.physics.core

import kotlin.math.PI
import kotlin.math.sqrt

/** SI-unit calculations shared by Physics laboratories. */
object PhysicsCalculations {
    fun period(frequencyHz: Double): Double = if (frequencyHz > 0.0) 1.0 / frequencyHz else Double.POSITIVE_INFINITY

    fun frequency(periodSeconds: Double): Double = if (periodSeconds > 0.0) 1.0 / periodSeconds else 0.0

    fun waveSpeed(frequencyHz: Double, wavelengthMetres: Double): Double =
        requireNonNegative(frequencyHz, "frequency") * requireNonNegative(wavelengthMetres, "wavelength")

    fun wavelength(speedMetresPerSecond: Double, frequencyHz: Double): Double {
        require(speedMetresPerSecond >= 0.0) { "speed must not be negative" }
        require(frequencyHz > 0.0) { "frequency must be positive" }
        return speedMetresPerSecond / frequencyHz
    }

    fun stringWaveSpeed(tensionNewtons: Double, linearDensityKgPerMetre: Double): Double {
        require(tensionNewtons >= 0.0) { "tension must not be negative" }
        require(linearDensityKgPerMetre > 0.0) { "linear density must be positive" }
        return sqrt(tensionNewtons / linearDensityKgPerMetre)
    }

    fun phaseDifferenceRadians(distanceMetres: Double, wavelengthMetres: Double): Double {
        require(wavelengthMetres > 0.0) { "wavelength must be positive" }
        return 2.0 * PI * distanceMetres / wavelengthMetres
    }

    fun relativeWaveEnergy(amplitudeMetres: Double, frequencyHz: Double): Double {
        require(amplitudeMetres >= 0.0) { "amplitude must not be negative" }
        require(frequencyHz >= 0.0) { "frequency must not be negative" }
        return amplitudeMetres * amplitudeMetres * frequencyHz * frequencyHz
    }

    private fun requireNonNegative(value: Double, name: String): Double {
        require(value >= 0.0) { "$name must not be negative" }
        return value
    }
}
