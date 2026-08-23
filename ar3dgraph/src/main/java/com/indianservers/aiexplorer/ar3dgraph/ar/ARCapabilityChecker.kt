package com.indianservers.aiexplorer.ar3dgraph.ar

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.ar.core.ArCoreApk
import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCapabilityState

internal enum class ARAvailabilityCode {
    Checking,
    TimedOut,
    Supported,
    NotInstalled,
    UpdateRequired,
    Unsupported,
    Error,
}

internal fun interface ARAvailabilityProbe {
    fun check(callback: (ARAvailabilityCode) -> Unit)
}

internal fun interface ARRetryScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit)
}

data class ARCapabilityResult(
    val state: ARCapabilityState,
    val message: String,
)

internal class ARCoreAvailabilityProbe(
    private val context: Context,
    private val apk: ArCoreApk = ArCoreApk.getInstance(),
) : ARAvailabilityProbe {
    override fun check(callback: (ARAvailabilityCode) -> Unit) {
        runCatching {
            apk.checkAvailabilityAsync(context) { callback(it.toAvailabilityCode()) }
        }.onFailure { callback(ARAvailabilityCode.Error) }
    }

    private fun ArCoreApk.Availability.toAvailabilityCode(): ARAvailabilityCode = when (this) {
        ArCoreApk.Availability.UNKNOWN_CHECKING -> ARAvailabilityCode.Checking
        ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> ARAvailabilityCode.TimedOut
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> ARAvailabilityCode.Supported
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ARAvailabilityCode.NotInstalled
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ARAvailabilityCode.UpdateRequired
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ARAvailabilityCode.Unsupported
        ArCoreApk.Availability.UNKNOWN_ERROR -> if (hasArCorePackage()) ARAvailabilityCode.Error else ARAvailabilityCode.NotInstalled
    }

    private fun hasArCorePackage(): Boolean = runCatching {
        context.packageManager.getPackageInfo("com.google.ar.core", 0)
    }.isSuccess
}

class ARCapabilityChecker internal constructor(
    private val probe: ARAvailabilityProbe,
    private val scheduler: ARRetryScheduler,
    private val maxTransientChecks: Int = 3,
) {
    constructor(context: Context) : this(
        probe = ARCoreAvailabilityProbe(context.applicationContext),
        scheduler = ARRetryScheduler { delay, action -> Handler(Looper.getMainLooper()).postDelayed(action, delay) },
    )

    private var generation = 0

    fun check(callback: (ARCapabilityResult) -> Unit) {
        val activeGeneration = ++generation
        checkAttempt(activeGeneration, attempt = 1, callback)
    }

    fun cancel() {
        generation++
    }

    private fun checkAttempt(generationAtStart: Int, attempt: Int, callback: (ARCapabilityResult) -> Unit) {
        probe.check { code ->
            if (generationAtStart != generation) return@check
            if ((code == ARAvailabilityCode.Checking || code == ARAvailabilityCode.TimedOut) && attempt < maxTransientChecks) {
                scheduler.schedule(400L * attempt) {
                    if (generationAtStart == generation) checkAttempt(generationAtStart, attempt + 1, callback)
                }
            } else {
                callback(map(code))
            }
        }
    }

    internal fun map(code: ARAvailabilityCode): ARCapabilityResult = when (code) {
        ARAvailabilityCode.Supported -> ARCapabilityResult(ARCapabilityState.Supported, "ARCore is available.")
        ARAvailabilityCode.NotInstalled -> ARCapabilityResult(ARCapabilityState.ARCoreNotInstalled, "Google Play Services for AR is not installed.")
        ARAvailabilityCode.UpdateRequired -> ARCapabilityResult(ARCapabilityState.ARCoreUpdateRequired, "Google Play Services for AR must be updated.")
        ARAvailabilityCode.Unsupported -> ARCapabilityResult(ARCapabilityState.Unsupported, "This device does not support ARCore. The rest of the app remains available.")
        ARAvailabilityCode.Checking, ARAvailabilityCode.TimedOut -> ARCapabilityResult(ARCapabilityState.Error, "ARCore availability could not be confirmed. Try again.")
        ARAvailabilityCode.Error -> ARCapabilityResult(ARCapabilityState.Error, "ARCore availability check failed safely. Try again.")
    }
}
