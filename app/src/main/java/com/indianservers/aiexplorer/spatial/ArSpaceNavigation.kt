package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.workspace.MathModule

/** Legacy project routes remain readable, while all AR launches use one workspace. */
object ArSpaceNavigation {
    val legacyRoutes = setOf(MathModule.ARGraph2D, MathModule.ARGraph3D, MathModule.ARGeometry2D, MathModule.ARGeometry3D, MathModule.ARCoordinatePlane, MathModule.ARVectorLab)
    fun canonicalRoute(module: MathModule): MathModule = when {
        module in legacyRoutes -> MathModule.SpatialAR
        module == MathModule.CoordinatePlane -> MathModule.Geometry2D
        else -> module
    }
}
