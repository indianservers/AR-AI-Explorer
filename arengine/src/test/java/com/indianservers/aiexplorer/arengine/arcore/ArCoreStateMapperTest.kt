package com.indianservers.aiexplorer.arengine.arcore

import com.google.ar.core.ArCoreApk
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.indianservers.aiexplorer.arengine.contract.ArPlaneOrientation
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingFailure
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArCoreStateMapperTest {
    @Test
    fun availabilityStatesKeepInstallUpdateUnsupportedAndTransientDistinct() {
        assertTrue(ArCoreStateMapper.availability(ArCoreApk.Availability.SUPPORTED_INSTALLED) is ArRuntimeState.Ready)
        val install = ArCoreStateMapper.availability(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED)
        val update = ArCoreStateMapper.availability(ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD)
        assertTrue(install is ArRuntimeState.InstallRequired && !install.update)
        assertTrue(update is ArRuntimeState.InstallRequired && update.update)
        assertTrue(ArCoreStateMapper.availability(ArCoreApk.Availability.UNKNOWN_CHECKING) is ArRuntimeState.Checking)
        assertTrue(ArCoreStateMapper.availability(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) is ArRuntimeState.Unsupported)
        assertTrue(ArCoreStateMapper.availability(ArCoreApk.Availability.UNKNOWN_TIMED_OUT) is ArRuntimeState.RecoverableError)
    }

    @Test
    fun trackingFailureGuidanceAndPlaneOrientationAreLossless() {
        assertEquals(ArTrackingState.Tracking, ArCoreStateMapper.tracking(TrackingState.TRACKING))
        assertEquals(ArTrackingFailure.Initializing, ArCoreStateMapper.trackingFailure(TrackingFailureReason.NONE, TrackingState.PAUSED))
        assertEquals(ArTrackingFailure.ExcessiveMotion, ArCoreStateMapper.trackingFailure(TrackingFailureReason.EXCESSIVE_MOTION, TrackingState.PAUSED))
        assertEquals(ArTrackingFailure.InsufficientFeatures, ArCoreStateMapper.trackingFailure(TrackingFailureReason.INSUFFICIENT_FEATURES, TrackingState.PAUSED))
        assertEquals(ArTrackingFailure.InsufficientLight, ArCoreStateMapper.trackingFailure(TrackingFailureReason.INSUFFICIENT_LIGHT, TrackingState.PAUSED))
        assertEquals(ArTrackingFailure.CameraUnavailable, ArCoreStateMapper.trackingFailure(TrackingFailureReason.CAMERA_UNAVAILABLE, TrackingState.PAUSED))
        assertEquals(ArPlaneOrientation.HorizontalUp, ArCoreStateMapper.planeOrientation(Plane.Type.HORIZONTAL_UPWARD_FACING))
        assertEquals(ArPlaneOrientation.HorizontalDown, ArCoreStateMapper.planeOrientation(Plane.Type.HORIZONTAL_DOWNWARD_FACING))
        assertEquals(ArPlaneOrientation.Vertical, ArCoreStateMapper.planeOrientation(Plane.Type.VERTICAL))
    }
}
