package com.indianservers.aiexplorer.spatial

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

/** Owns ARCore native resources; rendering remains behind the renderer-neutral spatial scene API. */
class ARCoreSessionController {
    private var session: Session? = null

    fun checkAvailability(activity: Activity): ARCapabilities {
        val availability = runCatching { ArCoreApk.getInstance().checkAvailability(activity) }.getOrElse {
            return ARCapabilities(ARAvailability.Error, message = "ARCore availability check failed: ${it.message ?: "unknown error"}")
        }
        return when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> ARCapabilities(ARAvailability.Ready, message = "ARCore is installed.")
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ARCapabilities(ARAvailability.InstallRequired, message = "Google Play Services for AR is required.")
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ARCapabilities(ARAvailability.UpdateRequired, message = "Google Play Services for AR must be updated.")
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ARCapabilities(ARAvailability.Unsupported, message = "This device does not support ARCore; spatial simulator is available.")
            ArCoreApk.Availability.UNKNOWN_CHECKING -> ARCapabilities(ARAvailability.Checking, message = "Checking this device with ARCore…")
            else -> ARCapabilities(ARAvailability.Error, message = "ARCore support could not be confirmed; spatial simulator is available.")
        }
    }

    fun prepare(activity: Activity, cameraPermissionGranted: Boolean, userRequestedInstall: Boolean = true): ARCapabilities {
        if (!cameraPermissionGranted) return ARCapabilities(ARAvailability.Ready, message = "Camera permission is needed only for live AR. Simulator remains available.")
        val availability = checkAvailability(activity)
        if (availability.availability == ARAvailability.Unsupported || availability.availability == ARAvailability.Error || availability.availability == ARAvailability.Checking) return availability
        return runCatching {
            when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ARCapabilities(ARAvailability.InstallRequired, message = "Finish installing Google Play Services for AR, then return here.")
                ArCoreApk.InstallStatus.INSTALLED -> createConfiguredSession(activity)
            }
        }.getOrElse { error ->
            ARCapabilities(ARAvailability.Error, message = "ARCore could not start: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun createConfiguredSession(activity: Activity): ARCapabilities {
        session?.close()
        val created = Session(activity)
        val depthSupported = created.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        val geospatialSupported = created.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)
        val config = created.config.apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            focusMode = Config.FocusMode.AUTO
            if (depthSupported) depthMode = Config.DepthMode.AUTOMATIC
        }
        created.configure(config)
        session = created
        return ARCapabilities(
            availability = ARAvailability.Ready,
            depthSupported = depthSupported,
            geospatialSupported = geospatialSupported,
            message = if (depthSupported) "ARCore session configured with Depth occlusion." else "ARCore session configured; Depth is unavailable on this device.",
        )
    }

    fun resume(): Result<Unit> = runCatching { session?.resume() ?: error("Prepare the ARCore session first.") }
    fun pause() { runCatching { session?.pause() } }
    fun close() { session?.close(); session = null }
}
