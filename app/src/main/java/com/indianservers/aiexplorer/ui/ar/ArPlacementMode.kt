package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import kotlin.math.abs

internal enum class ArPlacementMode(val label: String, val shortLabel: String) {
    Viewer("3D Viewer", "VIEW"),
    FloorTable("Floor/Table", "FLOOR"),
    Wall("Wall", "WALL"),
}

internal fun ArPlacementMode.accepts(hit: ArHitCandidate): Boolean {
    return when (this) {
        ArPlacementMode.Viewer -> false
        ArPlacementMode.FloorTable -> when (hit.type) {
            ArHitType.InstantPlacement -> true
            ArHitType.Plane, ArHitType.Depth, ArHitType.OrientedPoint -> {
                val normalY = abs(hit.pose.orientation.rotate(ArVector3.Up).y)
                normalY >= .55
            }
            ArHitType.Simulator -> true
        }
        ArPlacementMode.Wall -> when (hit.type) {
            ArHitType.Plane, ArHitType.Depth, ArHitType.OrientedPoint -> {
                val normalY = abs(hit.pose.orientation.rotate(ArVector3.Up).y)
                normalY < .55
            }
            ArHitType.InstantPlacement, ArHitType.Simulator -> false
        }
    }
}
