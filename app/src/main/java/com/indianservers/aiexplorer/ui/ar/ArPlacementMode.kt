package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate

internal enum class ArPlacementMode(val label: String, val shortLabel: String) {
    Viewer("3D Viewer", "VIEW"),
    FloorTable("Floor/Table", "FLOOR"),
    Wall("Wall", "WALL"),
}

internal fun ArPlacementMode.accepts(hit: ArHitCandidate): Boolean = when (this) {
    ArPlacementMode.Viewer -> false
    ArPlacementMode.FloorTable -> com.indianservers.aiexplorer.arengine.session.ArSurfaceIntelligence.accepts(hit, com.indianservers.aiexplorer.arengine.session.ArSurfaceTarget.FloorTable)
    ArPlacementMode.Wall -> com.indianservers.aiexplorer.arengine.session.ArSurfaceIntelligence.accepts(hit, com.indianservers.aiexplorer.arengine.session.ArSurfaceTarget.Wall)
}
