package com.indianservers.aiexplorer.arengine.rendering

import com.indianservers.aiexplorer.arengine.contract.ArLightEstimate
import com.indianservers.aiexplorer.arengine.contract.ArMatrix4
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import kotlin.math.max
import kotlin.math.sqrt

enum class ArRenderQuality { Ultra, High, Balanced, Low, Safety }

enum class ArThermalState { Nominal, Light, Moderate, Severe, Critical }

data class ArRenderFeatures(
    val quality: ArRenderQuality,
    val targetFramesPerSecond: Int,
    val meshDensityScale: Float,
    val antialiasingSamples: Int,
    val shadows: Boolean,
    val environmentalHdr: Boolean,
    val depthOcclusion: Boolean,
    val outlines: Boolean = true,
    val labels: Boolean = true,
)

/**
 * Hysteresis belongs to the caller's observation window; this function is deliberately pure so
 * thermal/frame-time behavior remains deterministic in simulator and JVM tests.
 */
object ArRenderQualityController {
    fun choose(
        thermalState: ArThermalState,
        averageFrameMillis: Double,
        batterySaver: Boolean = false,
        requested: ArRenderQuality? = null,
    ): ArRenderFeatures {
        require(averageFrameMillis.isFinite() && averageFrameMillis >= 0.0)
        val safeMaximum = when {
            thermalState == ArThermalState.Critical -> ArRenderQuality.Safety
            thermalState == ArThermalState.Severe || averageFrameMillis >= 45.0 -> ArRenderQuality.Low
            thermalState == ArThermalState.Moderate || batterySaver || averageFrameMillis >= 28.0 -> ArRenderQuality.Balanced
            averageFrameMillis <= 14.0 && thermalState == ArThermalState.Nominal -> ArRenderQuality.Ultra
            else -> ArRenderQuality.High
        }
        val quality = requested?.takeIf { it.ordinal >= safeMaximum.ordinal } ?: safeMaximum
        return features(quality)
    }

    private fun features(quality: ArRenderQuality) = when (quality) {
        ArRenderQuality.Ultra -> ArRenderFeatures(quality, 60, 1f, 4, shadows = true, environmentalHdr = true, depthOcclusion = true)
        ArRenderQuality.High -> ArRenderFeatures(quality, 45, .82f, 4, shadows = true, environmentalHdr = true, depthOcclusion = true)
        ArRenderQuality.Balanced -> ArRenderFeatures(quality, 30, .62f, 2, shadows = false, environmentalHdr = true, depthOcclusion = true)
        ArRenderQuality.Low -> ArRenderFeatures(quality, 24, .42f, 1, shadows = false, environmentalHdr = false, depthOcclusion = false)
        ArRenderQuality.Safety -> ArRenderFeatures(quality, 20, .28f, 1, shadows = false, environmentalHdr = false, depthOcclusion = false)
    }
}

data class ArNormalizedLighting(
    val direction: ArVector3,
    val intensity: ArVector3,
    val exposure: Float,
    val sphericalHarmonics: List<Float>,
    val valid: Boolean,
)

object ArLightingNormalizer {
    fun normalize(light: ArLightEstimate): ArNormalizedLighting {
        if (!light.valid) {
            return ArNormalizedLighting(ArVector3(0.0, -1.0, 0.0), ArVector3(1.0, 1.0, 1.0), 1f, emptyList(), false)
        }
        val magnitude = max(light.mainLightDirection.magnitude(), 1e-9)
        val direction = light.mainLightDirection * (1.0 / magnitude)
        val luminance = (
            light.mainLightIntensity.x * .2126 +
                light.mainLightIntensity.y * .7152 +
                light.mainLightIntensity.z * .0722
            ).coerceAtLeast(.05)
        val exposure = (light.pixelIntensity / sqrt(luminance)).coerceIn(.25, 4.0).toFloat()
        return ArNormalizedLighting(direction, light.mainLightIntensity, exposure, light.sphericalHarmonics.take(27), true)
    }
}

/** Revision-based cache key; mathematical values and IDs are intentionally not altered. */
class ArSceneUploadTracker {
    private var sceneId: String? = null
    private var revision: Long = Long.MIN_VALUE

    fun requiresUpload(id: String, currentRevision: Long): Boolean {
        require(id.isNotBlank())
        require(currentRevision >= 0L)
        if (sceneId == id && revision == currentRevision) return false
        sceneId = id
        revision = currentRevision
        return true
    }

    fun invalidate() {
        sceneId = null
        revision = Long.MIN_VALUE
    }
}

data class ArBoundingSphere(val center: ArVector3, val radius: Double) {
    init {
        require(radius.isFinite() && radius >= 0.0)
    }
}

object ArFrustumCuller {
    /** Conservative clip-space sphere test. False means the object is certainly outside. */
    fun visible(matrix: ArMatrix4, sphere: ArBoundingSphere): Boolean {
        val m = matrix.values
        val x = sphere.center.x
        val y = sphere.center.y
        val z = sphere.center.z
        val clipX = m[0] * x + m[4] * y + m[8] * z + m[12]
        val clipY = m[1] * x + m[5] * y + m[9] * z + m[13]
        val clipZ = m[2] * x + m[6] * y + m[10] * z + m[14]
        val clipW = m[3] * x + m[7] * y + m[11] * z + m[15]
        val maximumScale = maxOf(
            vectorLength(m[0], m[1], m[2]),
            vectorLength(m[4], m[5], m[6]),
            vectorLength(m[8], m[9], m[10]),
        )
        val radius = sphere.radius * maximumScale
        return clipX >= -clipW - radius && clipX <= clipW + radius &&
            clipY >= -clipW - radius && clipY <= clipW + radius &&
            clipZ >= -clipW - radius && clipZ <= clipW + radius
    }

    private fun vectorLength(x: Float, y: Float, z: Float) =
        sqrt((x * x + y * y + z * z).toDouble())
}

object ArModelMatrix {
    /** Column-major transform preserving the native anchor quaternion without Euler conversion. */
    fun compose(
        position: ArVector3,
        orientation: ArQuaternion,
        scale: Double,
    ): FloatArray {
        require(scale.isFinite() && scale > 0.0)
        val q = orientation.normalized()
        val xx = q.x * q.x
        val yy = q.y * q.y
        val zz = q.z * q.z
        val xy = q.x * q.y
        val xz = q.x * q.z
        val yz = q.y * q.z
        val wx = q.w * q.x
        val wy = q.w * q.y
        val wz = q.w * q.z
        val s = scale.toFloat()
        return floatArrayOf(
            ((1.0 - 2.0 * (yy + zz)) * s).toFloat(),
            ((2.0 * (xy + wz)) * s).toFloat(),
            ((2.0 * (xz - wy)) * s).toFloat(),
            0f,
            ((2.0 * (xy - wz)) * s).toFloat(),
            ((1.0 - 2.0 * (xx + zz)) * s).toFloat(),
            ((2.0 * (yz + wx)) * s).toFloat(),
            0f,
            ((2.0 * (xz + wy)) * s).toFloat(),
            ((2.0 * (yz - wx)) * s).toFloat(),
            ((1.0 - 2.0 * (xx + yy)) * s).toFloat(),
            0f,
            position.x.toFloat(),
            position.y.toFloat(),
            position.z.toFloat(),
            1f,
        )
    }
}
