package com.indianservers.aiexplorer.spatial

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.indianservers.aiexplorer.core.Vec3
import java.util.UUID

/** Owns ARCore native resources; rendering remains behind the renderer-neutral spatial scene API. */
class ARCoreSessionController {
    private var session: Session? = null
    private var latestFrame: Frame? = null
    private val pendingHits = mutableMapOf<String, HitResult>()
    private val anchors = mutableMapOf<String, Anchor>()
    private var cameraTextureName: Int? = null

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

    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        if (width > 0 && height > 0) session?.setDisplayGeometry(rotation, width, height)
    }

    /** Must be called on the GL thread before [updateFrame]. */
    fun setCameraTextureName(textureName: Int) {
        require(textureName > 0)
        cameraTextureName = textureName
        session?.setCameraTextureName(textureName)
    }

    /** Advances the real ARCore camera compositor/tracking frame. */
    fun updateFrame(): Result<ARFrameState> = runCatching {
        val active = session ?: error("Prepare the ARCore session first.")
        cameraTextureName?.let(active::setCameraTextureName)
        val frame = active.update().also { latestFrame = it }
        val camera = frame.camera
        val planes = active.getAllTrackables(Plane::class.java).count { it.trackingState == TrackingState.TRACKING && it.subsumedBy == null }
        val depth = runCatching { frame.acquireDepthImage16Bits().use { it.width > 0 && it.height > 0 } }.getOrDefault(false)
        val lighting = frame.lightEstimate.let { estimate ->
            EnvironmentLighting(pixelIntensity = estimate.pixelIntensity.takeIf(Float::isFinite) ?: 1f, valid = estimate.state == com.google.ar.core.LightEstimate.State.VALID)
        }
        val tracking = camera.trackingState.toQuality()
        val view = FloatArray(16).also { camera.getViewMatrix(it, 0) }
        val projection = FloatArray(16).also { camera.getProjectionMatrix(it, 0, .05f, 100f) }
        ARFrameState(
            frame.timestamp,
            tracking,
            camera.displayOrientedPose.toSpatialPose(),
            planes > 0,
            planes,
            depth,
            lighting,
            tracking == TrackingQuality.Limited && anchors.values.any { it.trackingState == TrackingState.PAUSED },
            frame.hasDisplayGeometryChanged(),
            view.toList(),
            projection.toList(),
        )
    }

    fun hitTest(screenX: Float, screenY: Float): List<SpatialHit> {
        val frame = latestFrame ?: return emptyList()
        pendingHits.clear()
        return frame.hitTest(screenX, screenY).mapNotNull { hit ->
            val trackable = hit.trackable
            val accepted = when (trackable) {
                is Plane -> trackable.isPoseInPolygon(hit.hitPose)
                is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                else -> true
            }
            if (!accepted) return@mapNotNull null
            val token = UUID.randomUUID().toString(); pendingHits[token] = hit
            val type = when (trackable) { is Plane -> SpatialHitType.Plane; is Point -> SpatialHitType.Point; else -> SpatialHitType.Depth }
            val distance = hit.distance.toDouble()
            SpatialHit(type, hit.hitPose.translationVec(), distanceMeters = distance, confidence = if (type == SpatialHitType.Plane) .9 else .68, uncertaintyMeters = if (type == SpatialHitType.Plane) .015 + distance * .01 else .04 + distance * .025, trackableId = token)
        }.sortedBy { it.distanceMeters }
    }

    fun createAnchor(hit: SpatialHit, now: Long = System.currentTimeMillis(), lessonId: String? = null): Result<PersistentSpatialAnchor> = runCatching {
        val result = hit.trackableId?.let(pendingHits::remove) ?: error("The AR hit expired; tap the surface again.")
        val native = result.createAnchor()
        val id = "ar-anchor-${UUID.randomUUID()}"; anchors[id] = native
        PersistentSpatialAnchor(id, native.pose.toSpatialPose(), now, now, native.trackingState.toAnchorState(), lessonId = lessonId)
    }

    fun anchorStates(now: Long = System.currentTimeMillis()): List<PersistentSpatialAnchor> = anchors.map { (id, anchor) ->
        PersistentSpatialAnchor(id, anchor.pose.toSpatialPose(), now, now, anchor.trackingState.toAnchorState())
    }

    fun detachAnchor(id: String) { anchors.remove(id)?.detach() }

    fun close() {
        anchors.values.forEach { runCatching { it.detach() } }
        anchors.clear(); pendingHits.clear(); latestFrame = null
        session?.close(); session = null
    }
}

private fun TrackingState.toQuality() = when (this) { TrackingState.TRACKING -> TrackingQuality.Tracking; TrackingState.PAUSED -> TrackingQuality.Limited; TrackingState.STOPPED -> TrackingQuality.Lost }
private fun TrackingState.toAnchorState() = when (this) { TrackingState.TRACKING -> AnchorTrackingState.Tracking; TrackingState.PAUSED -> AnchorTrackingState.Relocalizing; TrackingState.STOPPED -> AnchorTrackingState.Lost }
private fun Pose.translationVec() = Vec3(tx().toDouble(), ty().toDouble(), tz().toDouble())
private fun Pose.toSpatialPose() = SpatialPose(translationVec(), uniformScale = 1.0)
