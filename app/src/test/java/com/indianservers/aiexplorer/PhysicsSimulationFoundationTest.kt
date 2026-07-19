package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.physics.core.PhysicsCalculations
import com.indianservers.aiexplorer.physics.mechanicalwaves.EndCondition
import com.indianservers.aiexplorer.physics.mechanicalwaves.MechanicalWaveEngine
import com.indianservers.aiexplorer.physics.mechanicalwaves.MechanicalWaveParameters
import com.indianservers.aiexplorer.physics.mechanicalwaves.WaveDrive
import com.indianservers.aiexplorer.physics.simulation.FixedTimestepClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PhysicsSimulationFoundationTest {
    @Test
    fun waveRelationshipsUseSiUnits() {
        assertEquals(6.0, PhysicsCalculations.waveSpeed(2.0, 3.0), 1e-12)
        assertEquals(3.0, PhysicsCalculations.wavelength(6.0, 2.0), 1e-12)
        assertEquals(0.5, PhysicsCalculations.period(2.0), 1e-12)
        assertEquals(2.0, PhysicsCalculations.frequency(0.5), 1e-12)
    }

    @Test
    fun stringWaveSpeedRespondsToTensionAndDensity() {
        assertEquals(3.0, PhysicsCalculations.stringWaveSpeed(18.0, 2.0), 1e-12)
        assertTrue(PhysicsCalculations.stringWaveSpeed(36.0, 2.0) > PhysicsCalculations.stringWaveSpeed(18.0, 2.0))
        assertTrue(PhysicsCalculations.stringWaveSpeed(18.0, 4.0) < PhysicsCalculations.stringWaveSpeed(18.0, 2.0))
    }

    @Test
    fun phaseAndRelativeEnergyAreConsistent() {
        assertEquals(2.0 * PI, PhysicsCalculations.phaseDifferenceRadians(2.0, 2.0), 1e-12)
        assertEquals(4.0, PhysicsCalculations.relativeWaveEnergy(1.0, 2.0), 1e-12)
    }

    @Test
    fun fixedClockProducesDeterministicStepsAndClampsLongFrames() {
        val clock = FixedTimestepClock(stepSeconds = 0.01, maxFrameSeconds = 0.1)
        var elapsed = 0.0
        val steps = clock.consume(frameSeconds = 1.0, speed = 1.0) { elapsed += it }
        assertEquals(10, steps)
        assertEquals(0.1, elapsed, 1e-12)
    }

    @Test
    fun fixedEndReflectionInvertsPulse() {
        val parameters = MechanicalWaveParameters(amplitudeMetres = 0.5, frequencyHz = 1.0, wavelengthMetres = 2.0, dampingPerMetre = 0.0, pulseWidthMetres = 0.2)
        val arrivalTime = MechanicalWaveEngine.MEDIUM_LENGTH_METRES / parameters.speedMetresPerSecond
        val open = MechanicalWaveEngine.displacement(MechanicalWaveEngine.MEDIUM_LENGTH_METRES, arrivalTime, parameters, WaveDrive.Pulse, EndCondition.Open)
        val fixed = MechanicalWaveEngine.displacement(MechanicalWaveEngine.MEDIUM_LENGTH_METRES, arrivalTime, parameters, WaveDrive.Pulse, EndCondition.Fixed)
        assertTrue(open > 0.0)
        assertEquals(0.0, fixed, 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun wavelengthRejectsZeroFrequency() {
        PhysicsCalculations.wavelength(3.0, 0.0)
    }
}
