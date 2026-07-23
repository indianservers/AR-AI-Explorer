package com.indianservers.aiexplorer.arengine.contract

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Immutable platform-neutral vector in AR world metres or scene-local mathematical units. */
data class ArVector3(val x: Double, val y: Double, val z: Double) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite()) { "Vector components must be finite." }
    }

    operator fun plus(other: ArVector3) = ArVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: ArVector3) = ArVector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double): ArVector3 {
        require(scale.isFinite())
        return ArVector3(x * scale, y * scale, z * scale)
    }

    fun dot(other: ArVector3) = x * other.x + y * other.y + z * other.z
    fun magnitude() = sqrt(dot(this))

    companion object {
        val Zero = ArVector3(0.0, 0.0, 0.0)
        val Up = ArVector3(0.0, 1.0, 0.0)
    }
}

data class ArVector2(val x: Float, val y: Float) {
    init {
        require(x.isFinite() && y.isFinite()) { "Screen coordinates must be finite." }
    }
}

/**
 * Right-handed quaternion in `(x, y, z, w)` order, matching ARCore's pose convention.
 * Public transforms normalize before use so adapters may map sensor values without lossy Euler conversion.
 */
data class ArQuaternion(val x: Double, val y: Double, val z: Double, val w: Double) {
    init {
        require(x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()) { "Quaternion components must be finite." }
        require(normSquared() > 1e-18) { "Quaternion must have non-zero magnitude." }
    }

    fun normalized(): ArQuaternion {
        val inverse = 1.0 / sqrt(normSquared())
        return ArQuaternion(x * inverse, y * inverse, z * inverse, w * inverse)
    }

    fun conjugate() = ArQuaternion(-x, -y, -z, w)

    operator fun times(other: ArQuaternion) = ArQuaternion(
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w,
        w * other.w - x * other.x - y * other.y - z * other.z,
    )

    fun rotate(vector: ArVector3): ArVector3 {
        val unit = normalized()
        val axis = ArVector3(unit.x, unit.y, unit.z)
        val twiceCross = cross(axis, vector) * 2.0
        return vector + twiceCross * unit.w + cross(axis, twiceCross)
    }

    private fun normSquared() = x * x + y * y + z * z + w * w

    private fun cross(left: ArVector3, right: ArVector3) = ArVector3(
        left.y * right.z - left.z * right.y,
        left.z * right.x - left.x * right.z,
        left.x * right.y - left.y * right.x,
    )

    companion object {
        val Identity = ArQuaternion(0.0, 0.0, 0.0, 1.0)

        /** Applies X, then Y, then Z rotation in a right-handed coordinate system. */
        fun fromEulerDegrees(x: Double, y: Double, z: Double): ArQuaternion {
            val hx = Math.toRadians(x) / 2.0
            val hy = Math.toRadians(y) / 2.0
            val hz = Math.toRadians(z) / 2.0
            val qx = ArQuaternion(sin(hx), 0.0, 0.0, cos(hx))
            val qy = ArQuaternion(0.0, sin(hy), 0.0, cos(hy))
            val qz = ArQuaternion(0.0, 0.0, sin(hz), cos(hz))
            return (qz * qy * qx).normalized()
        }
    }
}

data class ArPose(
    val positionMeters: ArVector3 = ArVector3.Zero,
    val orientation: ArQuaternion = ArQuaternion.Identity,
)

data class ArLocalTransform(
    val offsetMeters: ArVector3 = ArVector3.Zero,
    val orientation: ArQuaternion = ArQuaternion.Identity,
    val uniformScale: Double = 1.0,
) {
    init {
        require(uniformScale.isFinite() && uniformScale > 0.0) { "Local scale must be finite and positive." }
    }
}

/**
 * A tracked anchor remains stable while [localTransform] is freely edited by the learner.
 * Mathematical engines continue to work in units; only this boundary converts units to metres.
 */
data class ArScenePlacement(
    val anchorId: String? = null,
    val anchorPose: ArPose = ArPose(),
    val localTransform: ArLocalTransform = ArLocalTransform(),
    val metersPerMathUnit: Double = 0.1,
) {
    init {
        require(metersPerMathUnit.isFinite() && metersPerMathUnit > 0.0) { "Metres per mathematical unit must be positive." }
    }
}

object ArCoordinateTransform {
    fun mathToWorld(point: ArVector3, placement: ArScenePlacement): ArVector3 {
        val scaled = point * (placement.metersPerMathUnit * placement.localTransform.uniformScale)
        val localMetres = placement.localTransform.offsetMeters + placement.localTransform.orientation.rotate(scaled)
        return placement.anchorPose.positionMeters + placement.anchorPose.orientation.rotate(localMetres)
    }

    fun worldToMath(point: ArVector3, placement: ArScenePlacement): ArVector3 {
        val anchorLocal = placement.anchorPose.orientation.normalized().conjugate()
            .rotate(point - placement.anchorPose.positionMeters)
        val sceneLocal = placement.localTransform.orientation.normalized().conjugate()
            .rotate(anchorLocal - placement.localTransform.offsetMeters)
        return sceneLocal * (1.0 / (placement.metersPerMathUnit * placement.localTransform.uniformScale))
    }
}

/** Column-major 4x4 matrix used by OpenGL-compatible adapters. */
data class ArMatrix4(val values: List<Float>) {
    init {
        require(values.size == 16) { "A 4x4 matrix requires 16 values." }
        require(values.all(Float::isFinite)) { "Matrix values must be finite." }
    }

    companion object {
        val Identity = ArMatrix4(
            listOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )
    }
}
