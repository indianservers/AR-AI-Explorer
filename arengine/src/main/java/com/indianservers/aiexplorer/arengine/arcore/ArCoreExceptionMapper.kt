package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.FatalException
import com.google.ar.core.exceptions.MissingGlContextException
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.ResourceExhaustedException
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.exceptions.TextureNotSetException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState

internal object ArCoreExceptionMapper {
    fun state(error: Throwable): ArRuntimeState = when (error) {
        is UnavailableArcoreNotInstalledException -> ArRuntimeState.InstallRequired(
            reason = "Google Play Services for AR is not installed.",
        )
        is UnavailableApkTooOldException -> ArRuntimeState.InstallRequired(
            update = true,
            reason = "Google Play Services for AR is out of date.",
        )
        is UnavailableSdkTooOldException -> ArRuntimeState.FatalError(
            "This AI Explorer build uses an ARCore SDK that is too old for the installed service.",
            error.javaClass.simpleName,
        )
        is UnavailableDeviceNotCompatibleException -> ArRuntimeState.Unsupported(
            "This device is not compatible with ARCore. Continue in the spatial simulator.",
        )
        is UnavailableUserDeclinedInstallationException -> ArRuntimeState.RecoverableError(
            "AR installation was declined. Live AR can be enabled later; the simulator remains available.",
            error.javaClass.simpleName,
        )
        is CameraNotAvailableException -> ArRuntimeState.RecoverableError(
            "The camera is unavailable. Close other camera apps and try live AR again.",
            error.javaClass.simpleName,
        )
        is TextureNotSetException -> ArRuntimeState.RecoverableError(
            "The AR camera texture is not ready yet.",
            error.javaClass.simpleName,
        )
        is MissingGlContextException -> ArRuntimeState.RecoverableError(
            "The AR renderer does not currently own an OpenGL context.",
            error.javaClass.simpleName,
        )
        is SessionPausedException -> ArRuntimeState.RecoverableError(
            "The AR session is paused.",
            error.javaClass.simpleName,
        )
        is NotTrackingException -> ArRuntimeState.RecoverableError(
            "Tracking is temporarily unavailable. Hold still and scan a textured surface.",
            error.javaClass.simpleName,
        )
        is ResourceExhaustedException -> ArRuntimeState.RecoverableError(
            "ARCore resources are temporarily exhausted. Remove unused anchors or restart live AR.",
            error.javaClass.simpleName,
        )
        is SecurityException -> ArRuntimeState.PermissionRequired()
        is FatalException -> ArRuntimeState.FatalError(
            "ARCore encountered an unrecoverable error: ${error.message ?: "unknown failure"}.",
            error.javaClass.simpleName,
        )
        else -> ArRuntimeState.RecoverableError(
            error.message ?: "ARCore operation failed.",
            error.javaClass.simpleName,
        )
    }
}
