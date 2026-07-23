package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.ArCoreApk
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArPlaneOrientation
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingFailure
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState

internal object ArCoreStateMapper {
    fun availability(value: ArCoreApk.Availability): ArRuntimeState = when (value) {
        ArCoreApk.Availability.SUPPORTED_INSTALLED -> ArRuntimeState.Ready(
            ArCapabilities(ArAvailability.Ready, message = "Google Play Services for AR is installed."),
        )
        ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> ArRuntimeState.InstallRequired(
            reason = "Google Play Services for AR must be installed.",
        )
        ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> ArRuntimeState.InstallRequired(
            update = true,
            reason = "Google Play Services for AR must be updated.",
        )
        ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> ArRuntimeState.Unsupported(
            "This device is not ARCore compatible. The spatial simulator remains available.",
        )
        ArCoreApk.Availability.UNKNOWN_CHECKING -> ArRuntimeState.Checking
        ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> ArRuntimeState.RecoverableError(
            "ARCore compatibility checking timed out. Check the network or use the simulator.",
            "AvailabilityTimedOut",
        )
        ArCoreApk.Availability.UNKNOWN_ERROR -> ArRuntimeState.RecoverableError(
            "ARCore compatibility could not be determined. The simulator remains available.",
            "AvailabilityUnknown",
        )
    }

    fun tracking(value: TrackingState): ArTrackingState = when (value) {
        TrackingState.TRACKING -> ArTrackingState.Tracking
        TrackingState.PAUSED -> ArTrackingState.Paused
        TrackingState.STOPPED -> ArTrackingState.Stopped
    }

    fun anchorTracking(value: TrackingState): ArAnchorTrackingState = when (value) {
        TrackingState.TRACKING -> ArAnchorTrackingState.Tracking
        TrackingState.PAUSED -> ArAnchorTrackingState.Relocalizing
        TrackingState.STOPPED -> ArAnchorTrackingState.Stopped
    }

    fun trackingFailure(value: TrackingFailureReason, state: TrackingState): ArTrackingFailure = when (value) {
        TrackingFailureReason.NONE -> if (state == TrackingState.PAUSED) ArTrackingFailure.Initializing else ArTrackingFailure.None
        TrackingFailureReason.CAMERA_UNAVAILABLE -> ArTrackingFailure.CameraUnavailable
        TrackingFailureReason.EXCESSIVE_MOTION -> ArTrackingFailure.ExcessiveMotion
        TrackingFailureReason.INSUFFICIENT_FEATURES -> ArTrackingFailure.InsufficientFeatures
        TrackingFailureReason.INSUFFICIENT_LIGHT -> ArTrackingFailure.InsufficientLight
        TrackingFailureReason.BAD_STATE -> ArTrackingFailure.Unknown
    }

    fun planeOrientation(value: Plane.Type): ArPlaneOrientation = when (value) {
        Plane.Type.HORIZONTAL_UPWARD_FACING -> ArPlaneOrientation.HorizontalUp
        Plane.Type.HORIZONTAL_DOWNWARD_FACING -> ArPlaneOrientation.HorizontalDown
        Plane.Type.VERTICAL -> ArPlaneOrientation.Vertical
    }
}
