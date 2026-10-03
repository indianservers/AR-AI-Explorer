package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.core.Vec3

internal fun SpatialScenePlacement.anchoredPosition(anchor: ArAnchorHandle?): Vec3 {
    val origin = anchorReferencePositionMeters
    if (anchor == null || origin == null) return pose.positionMeters
    val tracked = anchor.pose.positionMeters
    return Vec3(tracked.x, tracked.y, tracked.z) + (pose.positionMeters - origin)
}
