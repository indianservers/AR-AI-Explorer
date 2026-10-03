package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnsupportedConfigurationException

data class ArSessionFeatures(val depth: Boolean, val hdr: Boolean, val instantPlacement: Boolean)

/** One configuration and downgrade policy for all live AR renderers. Plane finding is never disabled. */
object ArCoreConfiguration {
    fun candidates(depthSupported: Boolean, allowHdr: Boolean = true, allowInstant: Boolean = true): List<ArSessionFeatures> =
        (if (depthSupported) listOf(true, false) else listOf(false)).flatMap { depth ->
            (if (allowHdr) listOf(true, false) else listOf(false)).flatMap { hdr ->
                (if (allowInstant) listOf(true, false) else listOf(false)).map { instant -> ArSessionFeatures(depth, hdr, instant) }
            }
        }

    fun configure(session: Session, depthSupported: Boolean, allowHdr: Boolean = true, allowInstant: Boolean = true): ArSessionFeatures {
        candidates(depthSupported, allowHdr, allowInstant).forEach { features ->
            val config = Config(session)
                .setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL)
                .setFocusMode(Config.FocusMode.AUTO)
                .setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE)
                .setDepthMode(if (features.depth) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED)
                .setLightEstimationMode(if (features.hdr) Config.LightEstimationMode.ENVIRONMENTAL_HDR else Config.LightEstimationMode.AMBIENT_INTENSITY)
                .setInstantPlacementMode(if (features.instantPlacement) Config.InstantPlacementMode.LOCAL_Y_UP else Config.InstantPlacementMode.DISABLED)
            try { session.configure(config); return features }
            catch (_: UnsupportedConfigurationException) { /* Retain floor/wall detection while optional features fall back. */ }
        }
        error("ARCore rejected the baseline floor and wall tracking configuration.")
    }
}
