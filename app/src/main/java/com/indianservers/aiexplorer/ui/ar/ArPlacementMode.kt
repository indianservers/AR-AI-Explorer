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
    if (hit.type != ArHitType.Plane) return false
    val normalY = abs(hit.pose.orientation.rotate(ArVector3.Up).y)
    return when (this) {
        ArPlacementMode.FloorTable -> normalY >= .7
        ArPlacementMode.Wall -> normalY < .7
        ArPlacementMode.Viewer -> false
    }
}
