package com.indianservers.aiexplorer.ar3dgraph.ar

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class WorldVector3(val x: Float, val y: Float, val z: Float) {
    val finite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite()
    fun normalized(): WorldVector3 {
        val length = sqrt(x * x + y * y + z * z)
        require(length.isFinite() && length > 1e-7f) { "Ray direction is degenerate." }
        return WorldVector3(x / length, y / length, z / length)
    }
    operator fun plus(other: WorldVector3) = WorldVector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: WorldVector3) = WorldVector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = WorldVector3(x * scale, y * scale, z * scale)
}

data class WorldRay(val origin: WorldVector3, val direction: WorldVector3)
data class WorldQuaternion(val x: Float, val y: Float, val z: Float, val w: Float) {
    val finite: Boolean get() = x.isFinite() && y.isFinite() && z.isFinite() && w.isFinite()
}
data class WorldPlacementPose(val position: WorldVector3, val rotation: WorldQuaternion) {
    val finite: Boolean get() = position.finite && rotation.finite
}

object PlacementMath {
    const val DEFAULT_PLACEMENT_DISTANCE_METRES = 1.5f

    /**
     * Unprojects a screen point with OpenGL column-major view/projection matrices. The ray starts at
     * the actual AR camera position, rather than a plane hit or near-plane approximation.
     */
    fun screenRay(
        tapX: Float,
        tapY: Float,
        viewportWidth: Int,
        viewportHeight: Int,
        cameraPosition: WorldVector3,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
    ): Result<WorldRay> = runCatching {
        require(viewportWidth > 0 && viewportHeight > 0) { "Viewport dimensions must be positive." }
        require(tapX.isFinite() && tapY.isFinite() && cameraPosition.finite) { "Tap and camera values must be finite." }
        require(tapX in 0f..viewportWidth.toFloat() && tapY in 0f..viewportHeight.toFloat()) {
            "Tap must be inside the AR camera viewport."
        }
        require(viewMatrix.size == 16 && projectionMatrix.size == 16) { "View and projection matrices must contain 16 values." }
        val inverse = invert(multiply(projectionMatrix, viewMatrix))
            ?: error("Camera view-projection matrix is not invertible.")
        val ndcX = 2f * tapX / viewportWidth - 1f
        val ndcY = 1f - 2f * tapY / viewportHeight
        val far = transform(inverse, floatArrayOf(ndcX, ndcY, 1f, 1f))
        require(kotlin.math.abs(far[3]) > 1e-7f) { "Unprojected ray has an invalid homogeneous coordinate." }
        val worldFar = WorldVector3(far[0] / far[3], far[1] / far[3], far[2] / far[3])
        WorldRay(cameraPosition, (worldFar - cameraPosition).normalized())
    }

    fun placementPose(
        ray: WorldRay,
        cameraPosition: WorldVector3,
        distanceMetres: Float = DEFAULT_PLACEMENT_DISTANCE_METRES,
    ): Result<WorldPlacementPose> = runCatching {
        require(distanceMetres.isFinite() && distanceMetres > 0f) { "Placement distance must be positive." }
        val position = ray.origin + ray.direction.normalized() * distanceMetres
        val toCameraX = cameraPosition.x - position.x
        val toCameraZ = cameraPosition.z - position.z
        val yaw = atan2(toCameraX, toCameraZ)
        val half = yaw / 2f
        WorldPlacementPose(
            position = position,
            rotation = WorldQuaternion(0f, sin(half), 0f, cos(half)),
        ).also { require(it.finite) { "Placement pose contains non-finite values." } }
    }

    private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (column in 0..3) for (row in 0..3) {
            var value = 0f
            for (index in 0..3) value += a[index * 4 + row] * b[column * 4 + index]
            result[column * 4 + row] = value
        }
        return result
    }

    private fun transform(matrix: FloatArray, vector: FloatArray): FloatArray = FloatArray(4) { row ->
        matrix[row] * vector[0] +
            matrix[4 + row] * vector[1] +
            matrix[8 + row] * vector[2] +
            matrix[12 + row] * vector[3]
    }

    private fun invert(matrix: FloatArray): FloatArray? {
        val augmented = Array(4) { row ->
            DoubleArray(8) { column ->
                when {
                    column < 4 -> matrix[column * 4 + row].toDouble()
                    column - 4 == row -> 1.0
                    else -> 0.0
                }
            }
        }
        for (pivot in 0..3) {
            var best = pivot
            for (row in pivot + 1..3) if (kotlin.math.abs(augmented[row][pivot]) > kotlin.math.abs(augmented[best][pivot])) best = row
            if (kotlin.math.abs(augmented[best][pivot]) < 1e-12) return null
            val swap = augmented[pivot]; augmented[pivot] = augmented[best]; augmented[best] = swap
            val divisor = augmented[pivot][pivot]
            for (column in 0..7) augmented[pivot][column] /= divisor
            for (row in 0..3) if (row != pivot) {
                val factor = augmented[row][pivot]
                for (column in 0..7) augmented[row][column] -= factor * augmented[pivot][column]
            }
        }
        return FloatArray(16) { index ->
            val column = index / 4
            val row = index % 4
            augmented[row][column + 4].toFloat()
        }.takeIf { values -> values.all(Float::isFinite) }
    }
}
