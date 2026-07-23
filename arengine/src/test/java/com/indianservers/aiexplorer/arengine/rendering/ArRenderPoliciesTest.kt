package com.indianservers.aiexplorer.arengine.rendering

import com.indianservers.aiexplorer.arengine.contract.ArLightEstimate
import com.indianservers.aiexplorer.arengine.contract.ArMatrix4
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArRenderPoliciesTest {
    @Test
    fun qualityPreservesOutlinesAndLabelsAcrossEveryTier() {
        ArRenderQuality.entries.forEach { requested ->
            val features = ArRenderQualityController.choose(
                thermalState = ArThermalState.Nominal,
                averageFrameMillis = 10.0,
                requested = requested,
            )
            assertTrue(features.outlines)
            assertTrue(features.labels)
        }
        assertEquals(
            ArRenderQuality.Safety,
            ArRenderQualityController.choose(ArThermalState.Critical, 12.0).quality,
        )
    }

    @Test
    fun sceneUploadTrackerOnlyUploadsChangedRevisions() {
        val tracker = ArSceneUploadTracker()
        assertTrue(tracker.requiresUpload("scene", 1))
        assertFalse(tracker.requiresUpload("scene", 1))
        assertTrue(tracker.requiresUpload("scene", 2))
        assertTrue(tracker.requiresUpload("other", 2))
    }

    @Test
    fun modelMatrixPreservesQuaternionRotationTranslationAndScale() {
        val matrix = ArModelMatrix.compose(
            ArVector3(1.0, 2.0, 3.0),
            ArQuaternion.fromEulerDegrees(0.0, 90.0, 0.0),
            2.0,
        )
        assertEquals(1f, matrix[12], 1e-5f)
        assertEquals(2f, matrix[13], 1e-5f)
        assertEquals(3f, matrix[14], 1e-5f)
        assertEquals(-2f, matrix[2], 1e-5f)
        assertEquals(2f, matrix[8], 1e-5f)
    }

    @Test
    fun lightingNormalizesDirectionAndBoundsExposure() {
        val normalized = ArLightingNormalizer.normalize(
            ArLightEstimate(
                valid = true,
                pixelIntensity = 100f,
                mainLightDirection = ArVector3(0.0, -4.0, 0.0),
                mainLightIntensity = ArVector3(2.0, 2.0, 2.0),
                sphericalHarmonics = List(30) { 1f },
            ),
        )
        assertEquals(1.0, normalized.direction.magnitude(), 1e-9)
        assertEquals(4f, normalized.exposure)
        assertEquals(27, normalized.sphericalHarmonics.size)
    }

    @Test
    fun frustumCullingIsConservative() {
        assertTrue(ArFrustumCuller.visible(ArMatrix4.Identity, ArBoundingSphere(ArVector3.Zero, .1)))
        assertFalse(ArFrustumCuller.visible(ArMatrix4.Identity, ArBoundingSphere(ArVector3(5.0, 0.0, 0.0), .1)))
    }
}
