package com.indianservers.aiexplorer.physics.core

import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class PhysicsMathWorkspaceEngineTest {
    @Test
    fun constantAccelerationLinksPositionVelocityAndGraphs() {
        val result = PhysicsMathWorkspaceEngine.kinematics(0.0, 5.0, 2.0, 5.0)

        assertEquals(50.0, result.finalPosition, 1e-12)
        assertEquals(15.0, result.finalVelocity, 1e-12)
        assertEquals(10.0, result.averageVelocity, 1e-12)
        assertEquals(161, result.position.points.size)
        assertEquals(result.finalPosition, result.position.points.last().y, 1e-12)
    }

    @Test
    fun projectileResolvesComponentsFlightAndTrajectory() {
        val result = PhysicsMathWorkspaceEngine.projectile(10.0, 45.0, 0.0, gravity = 10.0)

        assertEquals(10.0, result.horizontalRange, 1e-10)
        assertEquals(2.5, result.maximumHeight, 1e-10)
        assertEquals(0.0, result.trajectory.points.last().y, 1e-10)
        assertEquals(result.initialVelocity.x, result.impactVelocity.x, 1e-12)
        assertEquals(-result.initialVelocity.y, result.impactVelocity.y, 1e-10)
    }

    @Test
    fun vectorForceSatisfiesImpulseAndWorkEnergyTheorems() {
        val result = PhysicsMathWorkspaceEngine.forceEnergy(5.0, Vec2(20.0, 0.0), Vec2(0.0, 0.0), 3.0)

        assertEquals(Vec2(4.0, 0.0), result.acceleration)
        assertEquals(Vec2(12.0, 0.0), result.finalVelocity)
        assertEquals(Vec2(18.0, 0.0), result.displacement)
        assertEquals(360.0, result.work, 1e-12)
        assertEquals(result.work, result.kineticEnergyChange, 1e-12)
        assertEquals(Vec2(60.0, 0.0), result.momentumChange)
    }

    @Test
    fun oscillationReusesFrequencyRelationshipsAndConservesEnergy() {
        val result = PhysicsMathWorkspaceEngine.oscillation(2.0, .5, 3.0, 0.0, 4.0)

        assertEquals(PI, result.angularFrequency, 1e-12)
        assertEquals(2.0, result.period, 1e-12)
        assertEquals(3.0 * PI * PI, result.springConstant, 1e-12)
        assertEquals(6.0 * PI * PI, result.totalEnergy, 1e-12)
        assertEquals(2.0, result.position.points.first().y, 1e-12)
        assertTrue(result.velocity.points.any { it.y > 0.0 })
    }

    @Test
    fun unitConversionDelegatesToSharedSiSystem() {
        assertEquals(10.0, PhysicsMathWorkspaceEngine.convert(36.0, "km/h", "m/s"), 1e-12)
        assertEquals(273.15, PhysicsMathWorkspaceEngine.convert(0.0, "°C", "K"), 1e-12)
    }
}
