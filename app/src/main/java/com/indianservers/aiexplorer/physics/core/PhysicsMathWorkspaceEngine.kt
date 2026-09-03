package com.indianservers.aiexplorer.physics.core

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.physics.formulas.units.PhysicsUnitSystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class PhysicsSeries(val label: String, val unit: String, val points: List<Vec2>)

data class KinematicsResult(
    val finalPosition: Double,
    val finalVelocity: Double,
    val displacement: Double,
    val averageVelocity: Double,
    val position: PhysicsSeries,
    val velocity: PhysicsSeries,
    val acceleration: PhysicsSeries,
    val equations: List<String>,
)

data class ProjectileResult(
    val initialVelocity: Vec2,
    val flightTime: Double,
    val horizontalRange: Double,
    val maximumHeight: Double,
    val impactVelocity: Vec2,
    val trajectory: PhysicsSeries,
    val equations: List<String>,
)

data class ForceEnergyResult(
    val netForce: Vec2,
    val acceleration: Vec2,
    val finalVelocity: Vec2,
    val displacement: Vec2,
    val work: Double,
    val kineticEnergyChange: Double,
    val momentumChange: Vec2,
    val equations: List<String>,
)

data class OscillationResult(
    val angularFrequency: Double,
    val period: Double,
    val springConstant: Double,
    val totalEnergy: Double,
    val position: PhysicsSeries,
    val velocity: PhysicsSeries,
    val acceleration: PhysicsSeries,
    val equations: List<String>,
)

enum class CollisionType { Elastic, PerfectlyInelastic }

data class CollisionResult(
    val type: CollisionType,
    val firstFinalVelocity: Double,
    val secondFinalVelocity: Double,
    val initialMomentum: Double,
    val finalMomentum: Double,
    val initialKineticEnergy: Double,
    val finalKineticEnergy: Double,
    val coefficientOfRestitution: Double,
    val equations: List<String>,
) {
    val momentumResidual: Double get() = finalMomentum - initialMomentum
    val kineticEnergyChange: Double get() = finalKineticEnergy - initialKineticEnergy
}

object PhysicsMathWorkspaceEngine {
    fun collision1D(
        firstMass: Double,
        firstVelocity: Double,
        secondMass: Double,
        secondVelocity: Double,
        type: CollisionType,
    ): CollisionResult {
        require(listOf(firstMass, firstVelocity, secondMass, secondVelocity).all(Double::isFinite)) { "Enter finite collision values." }
        require(firstMass > 0.0 && secondMass > 0.0) { "Both masses must be positive." }
        val totalMass = firstMass + secondMass
        val (firstFinal, secondFinal) = when (type) {
            CollisionType.Elastic -> {
                val v1 = ((firstMass - secondMass) * firstVelocity + 2.0 * secondMass * secondVelocity) / totalMass
                val v2 = (2.0 * firstMass * firstVelocity + (secondMass - firstMass) * secondVelocity) / totalMass
                v1 to v2
            }
            CollisionType.PerfectlyInelastic -> {
                val shared = (firstMass * firstVelocity + secondMass * secondVelocity) / totalMass
                shared to shared
            }
        }
        fun kinetic(mass: Double, velocity: Double) = .5 * mass * velocity * velocity
        val initialMomentum = firstMass * firstVelocity + secondMass * secondVelocity
        val finalMomentum = firstMass * firstFinal + secondMass * secondFinal
        val relativeBefore = firstVelocity - secondVelocity
        val restitution = if (kotlin.math.abs(relativeBefore) < 1e-12) 0.0 else (secondFinal - firstFinal) / relativeBefore
        return CollisionResult(
            type, firstFinal, secondFinal, initialMomentum, finalMomentum,
            kinetic(firstMass, firstVelocity) + kinetic(secondMass, secondVelocity),
            kinetic(firstMass, firstFinal) + kinetic(secondMass, secondFinal),
            restitution,
            listOf("m₁u₁ + m₂u₂ = m₁v₁ + m₂v₂", "e = (v₂ − v₁)/(u₁ − u₂)", "K = ½mv²"),
        )
    }

    fun kinematics(
        initialPosition: Double,
        initialVelocity: Double,
        acceleration: Double,
        duration: Double,
        samples: Int = 160,
    ): KinematicsResult {
        require(listOf(initialPosition, initialVelocity, acceleration, duration).all(Double::isFinite)) { "Enter finite motion values." }
        require(duration > 0.0 && samples in 16..2_000) { "Duration must be positive." }
        fun position(t: Double) = initialPosition + initialVelocity * t + .5 * acceleration * t * t
        fun velocity(t: Double) = initialVelocity + acceleration * t
        val times = (0..samples).map { duration * it / samples }
        val finalPosition = position(duration)
        return KinematicsResult(
            finalPosition = finalPosition,
            finalVelocity = velocity(duration),
            displacement = finalPosition - initialPosition,
            averageVelocity = (finalPosition - initialPosition) / duration,
            position = PhysicsSeries("position", "m", times.map { Vec2(it, position(it)) }),
            velocity = PhysicsSeries("velocity", "m/s", times.map { Vec2(it, velocity(it)) }),
            acceleration = PhysicsSeries("acceleration", "m/s²", times.map { Vec2(it, acceleration) }),
            equations = listOf(
                "x(t) = x₀ + v₀t + ½at²",
                "v(t) = v₀ + at",
                "v² = v₀² + 2a(x − x₀)",
            ),
        )
    }

    fun projectile(
        speed: Double,
        angleDegrees: Double,
        initialHeight: Double,
        gravity: Double = 9.80665,
        samples: Int = 180,
    ): ProjectileResult {
        require(listOf(speed, angleDegrees, initialHeight, gravity).all(Double::isFinite)) { "Enter finite projectile values." }
        require(speed >= 0.0 && initialHeight >= 0.0 && gravity > 0.0 && samples in 16..2_000) { "Speed and height cannot be negative; gravity must be positive." }
        val angle = angleDegrees * PI / 180.0
        val initialVelocity = Vec2(speed * cos(angle), speed * sin(angle))
        val flightTime = (initialVelocity.y + sqrt(initialVelocity.y * initialVelocity.y + 2.0 * gravity * initialHeight)) / gravity
        fun point(t: Double) = Vec2(initialVelocity.x * t, initialHeight + initialVelocity.y * t - .5 * gravity * t * t)
        val impactVelocity = Vec2(initialVelocity.x, initialVelocity.y - gravity * flightTime)
        return ProjectileResult(
            initialVelocity = initialVelocity,
            flightTime = flightTime,
            horizontalRange = initialVelocity.x * flightTime,
            maximumHeight = initialHeight + initialVelocity.y * initialVelocity.y / (2.0 * gravity),
            impactVelocity = impactVelocity,
            trajectory = PhysicsSeries("trajectory", "m", (0..samples).map { point(flightTime * it / samples) }),
            equations = listOf(
                "v₀x = v₀ cos θ,  v₀y = v₀ sin θ",
                "x(t) = v₀x t",
                "y(t) = h₀ + v₀y t − ½gt²",
            ),
        )
    }

    fun forceEnergy(
        mass: Double,
        force: Vec2,
        initialVelocity: Vec2,
        duration: Double,
    ): ForceEnergyResult {
        require(mass.isFinite() && mass > 0.0 && duration.isFinite() && duration >= 0.0) { "Mass must be positive and duration cannot be negative." }
        require(listOf(force.x, force.y, initialVelocity.x, initialVelocity.y).all(Double::isFinite)) { "Enter finite vector components." }
        val acceleration = force * (1.0 / mass)
        val finalVelocity = initialVelocity + acceleration * duration
        val displacement = initialVelocity * duration + acceleration * (.5 * duration * duration)
        val work = force.x * displacement.x + force.y * displacement.y
        val initialSpeedSquared = initialVelocity.x * initialVelocity.x + initialVelocity.y * initialVelocity.y
        val finalSpeedSquared = finalVelocity.x * finalVelocity.x + finalVelocity.y * finalVelocity.y
        return ForceEnergyResult(
            netForce = force,
            acceleration = acceleration,
            finalVelocity = finalVelocity,
            displacement = displacement,
            work = work,
            kineticEnergyChange = .5 * mass * (finalSpeedSquared - initialSpeedSquared),
            momentumChange = force * duration,
            equations = listOf(
                "ΣF = ma",
                "J = ΣF Δt = Δp",
                "W = ΣF · Δr = ΔK",
            ),
        )
    }

    fun oscillation(
        amplitude: Double,
        frequency: Double,
        mass: Double,
        phaseDegrees: Double,
        duration: Double,
        samples: Int = 240,
    ): OscillationResult {
        require(listOf(amplitude, frequency, mass, phaseDegrees, duration).all(Double::isFinite)) { "Enter finite oscillation values." }
        require(amplitude >= 0.0 && frequency > 0.0 && mass > 0.0 && duration > 0.0 && samples in 16..2_000) { "Amplitude cannot be negative; frequency, mass and duration must be positive." }
        val omega = 2.0 * PI * frequency
        val phase = phaseDegrees * PI / 180.0
        fun position(t: Double) = amplitude * cos(omega * t + phase)
        fun velocity(t: Double) = -amplitude * omega * sin(omega * t + phase)
        fun acceleration(t: Double) = -omega * omega * position(t)
        val times = (0..samples).map { duration * it / samples }
        val springConstant = mass * omega * omega
        return OscillationResult(
            angularFrequency = omega,
            period = PhysicsCalculations.period(frequency),
            springConstant = springConstant,
            totalEnergy = .5 * springConstant * amplitude * amplitude,
            position = PhysicsSeries("displacement", "m", times.map { Vec2(it, position(it)) }),
            velocity = PhysicsSeries("velocity", "m/s", times.map { Vec2(it, velocity(it)) }),
            acceleration = PhysicsSeries("acceleration", "m/s²", times.map { Vec2(it, acceleration(it)) }),
            equations = listOf(
                "x(t) = A cos(ωt + φ)",
                "v(t) = −Aω sin(ωt + φ)",
                "a(t) = −ω²x(t)",
                "E = ½mω²A²",
            ),
        )
    }

    fun convert(value: Double, from: String, to: String): Double = PhysicsUnitSystem.convert(value, from, to)

    fun magnitude(vector: Vec2): Double = hypot(vector.x, vector.y)
}
