package com.indianservers.aiexplorer.mathworkspace.vector

import kotlin.math.acos
import kotlin.math.sqrt

data class MathVector3(val x: Double, val y: Double, val z: Double = 0.0) {
    operator fun plus(other: MathVector3) = MathVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: MathVector3) = MathVector3(x - other.x, y - other.y, z - other.z)
    operator fun times(k: Double) = MathVector3(x * k, y * k, z * k)
    fun dot(other: MathVector3) = x * other.x + y * other.y + z * other.z
    fun cross(other: MathVector3) = MathVector3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
    fun magnitude() = sqrt(dot(this))
    fun unit() = magnitude().takeIf { it > 1e-14 }?.let { this * (1.0 / it) } ?: MathVector3(0.0, 0.0, 0.0)
    fun projectionOn(other: MathVector3): MathVector3 = other.takeIf { it.dot(it) > 1e-28 }?.let { it * (dot(it) / it.dot(it)) } ?: MathVector3(0.0, 0.0, 0.0)
    fun angleDegrees(other: MathVector3): Double? {
        val denominator = magnitude() * other.magnitude()
        if (denominator <= 1e-14) return null
        return Math.toDegrees(acos((dot(other) / denominator).coerceIn(-1.0, 1.0)))
    }
}

data class VectorOperationResult(val vector: MathVector3? = null, val scalar: Double? = null, val label: String)

object VectorMath {
    fun calculate(operation: VectorOperation, a: MathVector3, b: MathVector3, alpha: Double, beta: Double): VectorOperationResult = when (operation) {
        VectorOperation.Add -> VectorOperationResult(a + b, label = "A + B")
        VectorOperation.Subtract -> VectorOperationResult(a - b, label = "A − B")
        VectorOperation.Scalar -> VectorOperationResult(a * alpha + b * beta, label = "αA + βB")
        VectorOperation.Magnitude -> VectorOperationResult(scalar = a.magnitude(), label = "|A|")
        VectorOperation.Unit -> VectorOperationResult(a.unit(), label = "Unit A")
        VectorOperation.Dot -> VectorOperationResult(scalar = a.dot(b), label = "A · B")
        VectorOperation.Cross -> VectorOperationResult(a.cross(b), label = "A × B")
        VectorOperation.Angle -> VectorOperationResult(scalar = a.angleDegrees(b), label = "Angle between A and B")
        VectorOperation.Projection -> VectorOperationResult(a.projectionOn(b), label = "projᵦ A")
    }
}
