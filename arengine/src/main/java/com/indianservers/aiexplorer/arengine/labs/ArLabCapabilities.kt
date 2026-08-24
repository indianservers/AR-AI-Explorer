package com.indianservers.aiexplorer.arengine.labs

data class ArLabCapabilities(
    val supportsPlacement: Boolean = true,
    val supportsMeasurement: Boolean = false,
    val supportsPointSelection: Boolean = false,
    val supportsPrimitiveFitting: Boolean = false,
    val supportsDepth: Boolean = true,
    val supportsOcclusion: Boolean = true,
    val supportsFormulaPanel: Boolean = true,
    val supportsCrossSection: Boolean = false,
    val supportsScaleCalibration: Boolean = true,
    val supports3DToAR: Boolean = true,
    val supportsPersistence: Boolean = true,
)

data class ArLabDefinition(
    val id: String,
    val title: String,
    val capabilities: ArLabCapabilities,
)

object ArLabRegistry {
    val definitions = listOf(
        lab("ar-3d-graph", "AR 3D Graph", crossSection = true),
        lab("ar-3d-shapes", "AR 3D Shapes", measurement = true, points = true, fitting = true, crossSection = true),
        lab("ar-2d-shapes", "AR 2D Shapes", measurement = true, points = true),
        lab("ar-coordinate-plane", "AR Coordinate Plane", measurement = true, points = true, fitting = false, crossSection = false),
        lab("ar-vector-lab", "AR Vector Lab", measurement = true, points = true),
        lab("ar-trigonometry", "AR Trigonometry Lab", measurement = true, points = true),
        lab("ar-calculus", "AR Calculus Lab", measurement = true, points = true, crossSection = true),
        lab("ar-geometry-construction", "AR Geometry Construction", measurement = true, points = true),
        lab("ar-solids-dissection", "AR Solids Dissection", measurement = true, fitting = true, crossSection = true),
        lab("ar-volume-explorer", "AR Volume Explorer", measurement = true, fitting = true),
        lab("ar-transformation", "AR Transformation Lab", measurement = true, points = true),
        lab("ar-physics-math", "AR Physics-Math Workspace", measurement = true, points = true),
        lab("ar-statistics", "AR Statistics Workspace", measurement = false, points = true, depth = false, occlusion = false, calibration = false),
        lab("ar-number-line", "AR Number Line", measurement = true, points = true, depth = false, occlusion = false),
        lab("ar-function-machine", "AR Function Machine", depth = false, occlusion = false, calibration = false),
        lab("ar-mathematical-art", "AR Mathematical Art", crossSection = true),
        lab("ar-formula-universe", "AR Formula Universe", measurement = true),
        lab("ar-math-museum", "AR Math Museum", measurement = false, points = false, fitting = false, calibration = false),
    )

    fun require(id: String): ArLabDefinition = definitions.firstOrNull { it.id == id }
        ?: throw IllegalArgumentException("Unknown AR lab: $id")

    private fun lab(
        id: String,
        title: String,
        measurement: Boolean = false,
        points: Boolean = false,
        fitting: Boolean = false,
        depth: Boolean = true,
        occlusion: Boolean = true,
        crossSection: Boolean = false,
        calibration: Boolean = true,
    ) = ArLabDefinition(
        id,
        title,
        ArLabCapabilities(
            supportsMeasurement = measurement,
            supportsPointSelection = points,
            supportsPrimitiveFitting = fitting,
            supportsDepth = depth,
            supportsOcclusion = occlusion,
            supportsCrossSection = crossSection,
            supportsScaleCalibration = calibration,
        ),
    )
}
