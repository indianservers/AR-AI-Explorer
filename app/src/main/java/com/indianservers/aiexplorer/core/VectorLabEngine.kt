package com.indianservers.aiexplorer.core

import kotlin.math.acos

data class VectorPairAnalysis(
    val a: Vec3,
    val b: Vec3,
    val sum: Vec3,
    val difference: Vec3,
    val dot: Double,
    val cross: Vec3,
    val angleDegrees: Double?,
    val projectionAOnB: Vec3?,
    val rejectionAFromB: Vec3?,
    val cosineSimilarity: Double?,
    val scalarComponentAOnB: Double?,
    val parallelogramArea: Double,
    val areParallel: Boolean,
    val areOrthogonal: Boolean,
)

data class OrthonormalBasis(val first: Vec3, val second: Vec3)

object VectorLabEngine {
    fun analyse(a: Vec3, b: Vec3): VectorPairAnalysis {
        val magnitudeProduct = a.magnitude() * b.magnitude()
        val dot = a.dot(b)
        val projection = project(a, b)
        val cross = cross(a, b)
        return VectorPairAnalysis(
            a = a,
            b = b,
            sum = a + b,
            difference = a - b,
            dot = dot,
            cross = cross,
            angleDegrees = magnitudeProduct.takeIf { it > EPSILON }
                ?.let { Math.toDegrees(acos((dot / it).coerceIn(-1.0, 1.0))) },
            projectionAOnB = projection,
            rejectionAFromB = projection?.let { a - it },
            cosineSimilarity = magnitudeProduct.takeIf { it > EPSILON }?.let { (dot / it).coerceIn(-1.0, 1.0) },
            scalarComponentAOnB = b.magnitude().takeIf { it > EPSILON }?.let { dot / it },
            parallelogramArea = cross.magnitude(),
            areParallel = magnitudeProduct > EPSILON && cross.magnitude() <= EPSILON * magnitudeProduct,
            areOrthogonal = magnitudeProduct > EPSILON && kotlin.math.abs(dot) <= EPSILON * magnitudeProduct,
        )
    }

    fun cross(a: Vec3, b: Vec3) = Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x,
    )

    fun project(vector: Vec3, onto: Vec3): Vec3? {
        val square = onto.dot(onto)
        if (square <= EPSILON) return null
        return onto * (vector.dot(onto) / square)
    }

    fun unit(vector: Vec3): Vec3? = vector.magnitude().takeIf { it > EPSILON }
        ?.let { vector * (1.0 / it) }

    fun linearCombination(a: Vec3, aScale: Double, b: Vec3, bScale: Double) =
        a * aScale + b * bScale

    fun gramSchmidt(a: Vec3, b: Vec3): OrthonormalBasis? {
        val first = unit(a) ?: return null
        val orthogonal = b - first * b.dot(first)
        val second = unit(orthogonal) ?: return null
        return OrthonormalBasis(first, second)
    }
}

private const val EPSILON = 1e-12
