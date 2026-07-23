package com.indianservers.aiexplorer.arengine.arcore

import android.app.Activity
import com.google.ar.core.ArCoreApk
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState

internal sealed interface ArCoreInstallOutcome {
    data object Installed : ArCoreInstallOutcome
    data class Requested(val update: Boolean) : ArCoreInstallOutcome
    data class Blocked(val state: ArRuntimeState) : ArCoreInstallOutcome
}

internal class ArCoreInstallCoordinator(
    private val activity: Activity,
    private val apk: ArCoreApk = ArCoreApk.getInstance(),
) {
    private var promptIssuedForAttempt = false
    private var lastAvailability: ArCoreApk.Availability? = null

    fun checkAvailability(): ArRuntimeState = runCatching {
        apk.checkAvailability(activity).also { lastAvailability = it }
    }.fold(ArCoreStateMapper::availability) { error -> ArCoreExceptionMapper.state(error) }

    fun checkAvailabilityAsync(callback: (ArRuntimeState) -> Unit) {
        runCatching {
            apk.checkAvailabilityAsync(activity) { availability ->
                lastAvailability = availability
                callback(ArCoreStateMapper.availability(availability))
            }
        }.onFailure { callback(ArCoreExceptionMapper.state(it)) }
    }

    fun requestInstall(explicitUserRequest: Boolean): ArCoreInstallOutcome {
        val availability = lastAvailability
        val mapped = availability?.let(ArCoreStateMapper::availability)
        if (mapped is ArRuntimeState.Unsupported || mapped is ArRuntimeState.RecoverableError) {
            return ArCoreInstallOutcome.Blocked(mapped)
        }
        val showPrompt = explicitUserRequest && !promptIssuedForAttempt
        return runCatching {
            apk.requestInstall(
                activity,
                showPrompt,
                ArCoreApk.InstallBehavior.OPTIONAL,
                ArCoreApk.UserMessageType.FEATURE,
            )
        }.fold(
            onSuccess = { status ->
                when (status) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        promptIssuedForAttempt = false
                        ArCoreInstallOutcome.Installed
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        promptIssuedForAttempt = true
                        ArCoreInstallOutcome.Requested(availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD)
                    }
                }
            },
            onFailure = { ArCoreInstallOutcome.Blocked(ArCoreExceptionMapper.state(it)) },
        )
    }

    fun beginNewAttempt() {
        promptIssuedForAttempt = false
    }
}
