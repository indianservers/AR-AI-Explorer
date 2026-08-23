package com.indianservers.aiexplorer.ar3dgraph.ar

import android.app.Activity
import android.util.Log
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCapabilityState
import com.indianservers.aiexplorer.ar3dgraph.presentation.ARSessionState

data class ARSessionTransition(
    val state: ARSessionState,
    val message: String,
    val capability: ARCapabilityState? = null,
)

interface ARSessionController : AutoCloseable {
    val state: ARSessionState
    fun requestInstall(userRequested: Boolean): ARSessionTransition
    fun create(): ARSessionTransition
    fun resume(): ARSessionTransition
    fun pause(): ARSessionTransition
    override fun close()
}

class ARCoreSessionManager(
    private val activity: Activity,
    private val apk: ArCoreApk = ArCoreApk.getInstance(),
) : ARSessionController {
    private var session: Session? = null

    override var state: ARSessionState = ARSessionState.Idle
        private set

    /** Read-only Phase 2 renderer access; session ownership and lifecycle remain here. */
    @Synchronized
    fun currentSession(): Session? = session

    @Synchronized
    override fun requestInstall(userRequested: Boolean): ARSessionTransition = runCatching {
        apk.requestInstall(
            activity,
            userRequested,
            ArCoreApk.InstallBehavior.OPTIONAL,
            ArCoreApk.UserMessageType.FEATURE,
        )
    }.fold(
        onSuccess = { status ->
            when (status) {
                ArCoreApk.InstallStatus.INSTALLED -> ARSessionTransition(state, "ARCore is installed.")
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ARSessionTransition(
                    state,
                    "ARCore installation or update was requested.",
                    ARCapabilityState.InstallationRequested,
                )
            }
        },
        onFailure = ::mapFailure,
    )

    @Synchronized
    override fun create(): ARSessionTransition {
        if (state == ARSessionState.Closed) return ARSessionTransition(state, "AR session is closed.")
        if (session != null) return ARSessionTransition(ARSessionState.Ready, "ARCore session is ready.")
        state = ARSessionState.SessionInitializing
        var candidate: Session? = null
        return runCatching {
            candidate = Session(activity)
            val activeCandidate = requireNotNull(candidate)
            val config = Config(activeCandidate)
                .setPlaneFindingMode(Config.PlaneFindingMode.DISABLED)
                .setInstantPlacementMode(Config.InstantPlacementMode.DISABLED)
                .setDepthMode(Config.DepthMode.DISABLED)
                .setLightEstimationMode(Config.LightEstimationMode.DISABLED)
            activeCandidate.configure(config)
            session = activeCandidate
            state = ARSessionState.Ready
            ARSessionTransition(state, "AR session ready.")
        }.getOrElse { error ->
            runCatching { candidate?.close() }
            session = null
            mapFailure(error)
        }
    }

    @Synchronized
    override fun resume(): ARSessionTransition {
        val active = session ?: return ARSessionTransition(ARSessionState.SessionError, "ARCore session has not been created.")
        return runCatching {
            active.resume()
            state = ARSessionState.SessionRunning
            ARSessionTransition(state, "AR camera session is running.")
        }.getOrElse(::mapFailure)
    }

    @Synchronized
    override fun pause(): ARSessionTransition {
        runCatching { session?.pause() }.onFailure { return mapFailure(it) }
        if (session != null) state = ARSessionState.SessionPaused
        return ARSessionTransition(state, "ARCore session paused.")
    }

    @Synchronized
    override fun close() {
        if (state == ARSessionState.Closed) return
        runCatching { session?.pause() }
        runCatching { session?.close() }
        session = null
        state = ARSessionState.Closed
    }

    private fun mapFailure(error: Throwable): ARSessionTransition {
        Log.e(TAG, "ARCore lifecycle failure", error)
        val transition = when (error) {
            is UnavailableArcoreNotInstalledException -> ARSessionTransition(ARSessionState.SessionError, "Google Play Services for AR is not installed.", ARCapabilityState.ARCoreNotInstalled)
            is UnavailableApkTooOldException -> ARSessionTransition(ARSessionState.SessionError, "Google Play Services for AR requires an update.", ARCapabilityState.ARCoreUpdateRequired)
            is UnavailableDeviceNotCompatibleException -> ARSessionTransition(ARSessionState.SessionError, "This device is not compatible with ARCore.", ARCapabilityState.Unsupported)
            is UnavailableSdkTooOldException -> ARSessionTransition(ARSessionState.SessionError, "This app build is not compatible with the installed ARCore service.")
            is UnavailableUserDeclinedInstallationException -> ARSessionTransition(ARSessionState.SessionError, "ARCore installation was declined.", ARCapabilityState.ARCoreNotInstalled)
            is CameraNotAvailableException -> ARSessionTransition(ARSessionState.SessionError, "The camera is in use or unavailable. Close other camera apps and retry.")
            is SecurityException -> ARSessionTransition(ARSessionState.SessionError, "Camera permission is required before starting AR.")
            else -> ARSessionTransition(ARSessionState.SessionError, "AR session couldn't start. Try again.")
        }
        state = transition.state
        return transition
    }

    private companion object { const val TAG = "AR3DGraphSession" }
}

class ARSessionLifecycleCoordinator(
    private val controller: ARSessionController,
) {
    fun enter(): ARSessionTransition {
        val install = controller.requestInstall(userRequested = false)
        if (install.capability != null) return install
        val created = controller.create()
        return if (created.state == ARSessionState.Ready) controller.resume() else created
    }

    fun pause(): ARSessionTransition = controller.pause()

    fun resume(): ARSessionTransition = if (controller.state == ARSessionState.SessionPaused) controller.resume()
    else ARSessionTransition(controller.state, "ARCore session is not paused.")

    fun close() = controller.close()
}
