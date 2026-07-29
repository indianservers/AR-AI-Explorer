package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import com.indianservers.aiexplorer.spatial.TrackingQuality

internal enum class ArPlacementMode(val label: String, val shortLabel: String) {
    Screen("Screen", "SCR"),
    Instant6Dof("6DoF", "6D"),
    PlaneAnchor("Tap Anchor", "TAP"),
}

internal fun SpatialScenePlacement.instantCameraFrontPlacement(
    frame: ArFrameSnapshot,
    rotationDegrees: Vec3,
    distanceMeters: Double = 1.45,
    nowMillis: Long = System.currentTimeMillis(),
): SpatialScenePlacement {
    val camera = frame.camera.pose
    val forward = camera.orientation.rotate(ArVector3(0.0, 0.0, -1.0))
    val downBias = camera.orientation.rotate(ArVector3(0.0, -0.10, 0.0))
    val position = camera.positionMeters + forward * distanceMeters + downBias
    return copy(
        anchorId = "instant-world-$nowMillis",
        pose = pose.copy(
            positionMeters = Vec3(position.x, position.y, position.z),
            rotationDegrees = rotationDegrees,
        ),
        trackingQuality = if (frame.camera.trackingState == ArTrackingState.Tracking) TrackingQuality.Tracking else TrackingQuality.Limited,
        estimated = true,
        measurementUncertaintyMeters = if (frame.camera.trackingState == ArTrackingState.Tracking) 0.04 else 0.12,
        relocalizationMessage = "Instant 6DoF placement follows ARCore camera tracking; no plane required.",
        placedAt = nowMillis,
    )
}
