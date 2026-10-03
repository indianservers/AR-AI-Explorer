package com.indianservers.aiexplorer.arengine.arcore

import android.app.Activity
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.Trackable
import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArRuntime
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import java.util.UUID

/**
 * Production ARCore implementation. Mathematical engines never enter this class; they communicate
 * through renderer-neutral scene and runtime contracts owned by the `arengine` module.
 */
class ArCoreRuntime(
    private val activity: Activity,
) : ArRuntime {
    private val installCoordinator = ArCoreInstallCoordinator(activity)
    private val trackableIds = mutableMapOf<Trackable, String>()
    private val frameMapper = ArCoreFrameMapper(trackableIds)
    private val anchorRegistry = ArCoreAnchorRegistry()
    private val pendingHits = linkedMapOf<String, HitResult>()
    private var session: Session? = null
    private var latestFrame: Frame? = null
    private var capabilities: ArCapabilities? = null
    private var cameraTextureName: Int? = null
    private var displayGeometry: Triple<Int, Int, Int>? = null
    private var depthAcquisitionEnabled = false

    override var state: ArRuntimeState = ArRuntimeState.Idle
        private set

    @Synchronized
    override fun checkAvailability(): ArRuntimeState {
        if (!ensureOpen()) return state
        if (session != null && (state is ArRuntimeState.Ready || state is ArRuntimeState.Running || state is ArRuntimeState.Paused)) return state
        state = installCoordinator.checkAvailability()
        return state
    }

    override fun checkAvailabilityAsync(callback: (ArRuntimeState) -> Unit) {
        synchronized(this) {
            if (!ensureOpen()) {
                callback(state)
                return
            }
            if (session != null && (state is ArRuntimeState.Ready || state is ArRuntimeState.Running || state is ArRuntimeState.Paused)) {
                callback(state)
                return
            }
            state = ArRuntimeState.Checking
        }
        installCoordinator.checkAvailabilityAsync { result ->
            val delivered = synchronized(this) {
                if (state is ArRuntimeState.Closed || session != null && (state is ArRuntimeState.Ready || state is ArRuntimeState.Running || state is ArRuntimeState.Paused)) state else {
                    state = result
                    result
                }
            }
            callback(delivered)
        }
    }

    /**
     * Call with `userRequestedInstall=true` only for an explicit learner action. After ARCore pauses
     * and resumes the Activity, repeated calls automatically suppress a second installation prompt.
     */
    @Synchronized
    override fun prepare(cameraPermissionGranted: Boolean, userRequestedInstall: Boolean): ArRuntimeState {
        if (!ensureOpen()) return state
        if (!cameraPermissionGranted) {
            state = ArRuntimeState.PermissionRequired()
            return state
        }
        if (session != null && (state is ArRuntimeState.Ready || state is ArRuntimeState.Running || state is ArRuntimeState.Paused)) return state
        val availabilityState = installCoordinator.checkAvailability()
        if (availabilityState is ArRuntimeState.Checking ||
            availabilityState is ArRuntimeState.Unsupported ||
            availabilityState is ArRuntimeState.RecoverableError ||
            availabilityState is ArRuntimeState.FatalError
        ) {
            state = availabilityState
            return state
        }
        state = when (val installation = installCoordinator.requestInstall(userRequestedInstall)) {
            ArCoreInstallOutcome.Installed -> createConfiguredSession()
            is ArCoreInstallOutcome.Requested -> ArRuntimeState.InstallRequested(installation.update)
            is ArCoreInstallOutcome.Blocked -> installation.state
        }
        return state
    }

    @Synchronized
    override fun resume(): Result<ArRuntimeState> = runCatching {
        ensureOpenOrThrow()
        if (state is ArRuntimeState.Running) return@runCatching state
        val active = session ?: error("Prepare the ARCore runtime before resume.")
        val readyCapabilities = capabilities ?: error("ARCore capabilities are unavailable.")
        active.resume()
        cameraTextureName?.let(active::setCameraTextureName)
        displayGeometry?.let { (rotation, width, height) -> active.setDisplayGeometry(rotation, width, height) }
        ArRuntimeState.Running(readyCapabilities).also { state = it }
    }.onFailure(::recordFailure)

    @Synchronized
    override fun pause(): ArRuntimeState {
        if (!ensureOpen()) return state
        val activeCapabilities = capabilities
        runCatching { session?.pause() }.onFailure(::recordFailure)
        if (state !is ArRuntimeState.RecoverableError && state !is ArRuntimeState.FatalError && activeCapabilities != null) {
            state = ArRuntimeState.Paused(activeCapabilities)
        }
        return state
    }

    @Synchronized
    override fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        ensureOpenOrThrow()
        require(rotation in 0..3) { "Display rotation must be one of Android's four rotation constants." }
        require(width > 0 && height > 0)
        displayGeometry = Triple(rotation, width, height)
        session?.setDisplayGeometry(rotation, width, height)
    }

    /** Must be called on the renderer's OpenGL thread. */
    @Synchronized
    override fun setCameraTextureName(textureName: Int) {
        ensureOpenOrThrow()
        require(textureName > 0)
        cameraTextureName = textureName
        runCatching { session?.setCameraTextureName(textureName) }.onFailure(::recordFailure)
    }

    @Synchronized
    override fun setDepthEnabled(enabled: Boolean) {
        ensureOpenOrThrow()
        depthAcquisitionEnabled = enabled && capabilities?.depthSupported == true
    }

    @Synchronized
    override fun updateFrame(): Result<ArFrameSnapshot> = runCatching {
        ensureOpenOrThrow()
        require(state is ArRuntimeState.Running) { "ARCore must be running before a frame is requested." }
        val active = session ?: error("Prepare the ARCore runtime first.")
        cameraTextureName?.let(active::setCameraTextureName)
        val frame = active.update().also { latestFrame = it }
        frameMapper.map(active, frame, depthAcquisitionEnabled)
    }.onFailure(::recordFailure)

    @Synchronized
    override fun hitTest(screenPoint: ArVector2): List<ArHitCandidate> {
        if (state !is ArRuntimeState.Running) return emptyList()
        val frame = latestFrame ?: return emptyList()
        pendingHits.clear()
        val nativeHits = buildList {
            addAll(frame.hitTest(screenPoint.x, screenPoint.y))
            if (capabilities?.instantPlacementSupported == true) {
                addAll(frame.hitTestInstantPlacement(screenPoint.x, screenPoint.y, 1.0f))
            }
        }
        return ArHitPolicy.rank(nativeHits.mapNotNull(::mapHit))
    }

    @Synchronized
    override fun acquireCameraImage(): com.indianservers.aiexplorer.arengine.contract.ArCameraImage? {
        if (state !is ArRuntimeState.Running) return null
        val frame = latestFrame ?: return null
        return try {
            frame.acquireCameraImage().use { image ->
                if (image.format != android.graphics.ImageFormat.YUV_420_888) return null
                val output = FloatArray(6)
                frame.transformCoordinates2d(com.google.ar.core.Coordinates2d.IMAGE_NORMALIZED,
                    floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f), com.google.ar.core.Coordinates2d.VIEW_NORMALIZED, output)
                com.indianservers.aiexplorer.arengine.contract.ArCameraImage(
                    android.os.SystemClock.uptimeMillis(), image.width, image.height,
                    image.planes.map { plane ->
                        val buffer = plane.buffer.duplicate()
                        val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
                        com.indianservers.aiexplorer.arengine.contract.ArCameraImagePlane(bytes, plane.rowStride, plane.pixelStride)
                    }, (0..2).map { ArVector2(output[it*2], output[it*2+1]) })
            }
        } catch (_: com.google.ar.core.exceptions.NotYetAvailableException) { null }
          catch (_: com.google.ar.core.exceptions.DeadlineExceededException) { null }
    }

    @Synchronized
    override fun createAnchor(hitId: String, nowMillis: Long): Result<ArAnchorHandle> = runCatching {
        ensureOpenOrThrow()
        require(state is ArRuntimeState.Running) { "ARCore must be running before an anchor is created." }
        val hit = pendingHits.remove(hitId) ?: error("The AR hit expired; scan and tap the surface again.")
        anchorRegistry.create(hit, nowMillis)
    }.onFailure(::recordFailure)

    @Synchronized
    override fun anchors(): List<ArAnchorHandle> = anchorRegistry.snapshots(System.currentTimeMillis())

    @Synchronized
    override fun detachAnchor(anchorId: String): Boolean = anchorRegistry.detach(anchorId)

    @Synchronized
    override fun close() {
        if (state is ArRuntimeState.Closed) return
        anchorRegistry.clear()
        pendingHits.clear()
        trackableIds.clear()
        latestFrame = null
        runCatching { session?.pause() }
        runCatching { session?.close() }
        session = null
        capabilities = null
        depthAcquisitionEnabled = false
        state = ArRuntimeState.Closed
    }

    @Synchronized
    fun beginNewInstallAttempt() {
        ensureOpenOrThrow()
        installCoordinator.beginNewAttempt()
    }

    private fun createConfiguredSession(): ArRuntimeState {
        state = ArRuntimeState.CreatingSession
        var created: Session? = null
        return runCatching {
            anchorRegistry.clear()
            pendingHits.clear()
            trackableIds.clear()
            latestFrame = null
            runCatching { session?.close() }
            session = null
            val candidate = Session(activity).also { created = it }
            val depthSupported = candidate.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            val rawDepthSupported = candidate.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
            val geospatialSupported = runCatching { candidate.isGeospatialModeSupported(Config.GeospatialMode.ENABLED) }.getOrDefault(false)
            val features = ArCoreConfiguration.configure(candidate, depthSupported)
            cameraTextureName?.let(candidate::setCameraTextureName)
            displayGeometry?.let { (rotation, width, height) -> candidate.setDisplayGeometry(rotation, width, height) }
            session = candidate
            ArCapabilities(
                availability = ArAvailability.Ready,
                depthSupported = features.depth,
                rawDepthSupported = rawDepthSupported,
                environmentalHdrSupported = features.hdr,
                instantPlacementSupported = features.instantPlacement,
                geospatialSupported = geospatialSupported,
                message = buildString {
                    append("ARCore session ready")
                    append(if (features.depth) " with Depth" else " without Depth")
                    append(if (features.hdr) " and environmental HDR." else " and ambient lighting.")
                },
            ).also { capabilities = it }.let(ArRuntimeState::Ready)
        }.getOrElse { error ->
            runCatching { created?.close() }
            session = null
            capabilities = null
            ArCoreExceptionMapper.state(error)
        }
    }

    private fun mapHit(hit: HitResult): ArHitCandidate? {
        val trackable = hit.trackable
        if (trackable.trackingState != com.google.ar.core.TrackingState.TRACKING) return null
        val accepted = when (trackable) {
            is Plane -> trackable.subsumedBy == null && trackable.isPoseInPolygon(hit.hitPose)
            is Point -> trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
            is DepthPoint, is InstantPlacementPoint -> true
            else -> false
        }
        if (!accepted) return null
        val type = when (trackable) {
            is Plane -> ArHitType.Plane
            is DepthPoint -> ArHitType.Depth
            is Point -> ArHitType.OrientedPoint
            is InstantPlacementPoint -> ArHitType.InstantPlacement
            else -> return null
        }
        val rawDistance = hit.distance.toDouble()
        if (!rawDistance.isFinite() || rawDistance < 0.0) return null
        val distance = rawDistance.coerceAtLeast(0.0)
        val confidence = when (type) {
            ArHitType.Plane -> 0.95
            ArHitType.Depth -> 0.85
            ArHitType.OrientedPoint -> 0.68
            ArHitType.InstantPlacement -> if ((trackable as InstantPlacementPoint).trackingMethod == InstantPlacementPoint.TrackingMethod.FULL_TRACKING) 0.72 else 0.45
            ArHitType.Simulator -> 0.5
        }
        val uncertainty = when (type) {
            ArHitType.Plane -> 0.012 + distance * 0.008
            ArHitType.Depth -> 0.018 + distance * 0.012
            ArHitType.OrientedPoint -> 0.035 + distance * 0.02
            ArHitType.InstantPlacement -> 0.08 + distance * 0.04
            ArHitType.Simulator -> 0.05
        }
        val id = "hit-${UUID.randomUUID()}"
        val pose = runCatching { ArCorePoseMapper.map(hit.hitPose) }.getOrNull() ?: return null
        pendingHits[id] = hit
        return ArHitCandidate(
            id = id,
            type = type,
            pose = pose,
            distanceMeters = distance,
            confidence = confidence,
            uncertaintyMeters = uncertainty,
            trackableId = trackableIds.getOrPut(trackable) { "${type.name.lowercase()}-${UUID.randomUUID()}" },
            planeOrientation = (trackable as? Plane)?.let { ArCoreStateMapper.planeOrientation(it.type) },
            trackingState = ArCoreStateMapper.tracking(trackable.trackingState),
        )
    }

    private fun recordFailure(error: Throwable) {
        if (state is ArRuntimeState.Closed) return
        if ((error is IllegalArgumentException || error is IllegalStateException) &&
            !error.javaClass.name.startsWith("com.google.ar.core")
        ) return
        state = ArCoreExceptionMapper.state(error)
    }

    private fun ensureOpen(): Boolean = state !is ArRuntimeState.Closed

    private fun ensureOpenOrThrow() {
        check(ensureOpen()) { "ARCore runtime is closed." }
    }
}
