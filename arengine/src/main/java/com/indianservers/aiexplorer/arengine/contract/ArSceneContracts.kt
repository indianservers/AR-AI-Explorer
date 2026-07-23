package com.indianservers.aiexplorer.arengine.contract

enum class ArObjectKind {
    Point,
    Curve,
    Surface,
    Solid,
    Vector,
    Plane,
    Annotation,
    Measurement,
}

enum class ArBlendMode { Opaque, Transparent, Additive }

data class ArColor(val red: Float, val green: Float, val blue: Float, val alpha: Float = 1f) {
    init {
        require(listOf(red, green, blue, alpha).all { it in 0f..1f }) { "Colour channels must be in [0, 1]." }
    }
}

data class ArMaterial(
    val color: ArColor,
    val metallic: Float = 0f,
    val roughness: Float = 0.65f,
    val emissive: Float = 0f,
    val blendMode: ArBlendMode = if (color.alpha < 1f) ArBlendMode.Transparent else ArBlendMode.Opaque,
) {
    init {
        require(metallic in 0f..1f)
        require(roughness in 0f..1f)
        require(emissive >= 0f && emissive.isFinite())
    }
}

/**
 * Renderer-neutral indexed geometry. Vertices remain in mathematical scene-local units.
 * The AR placement boundary performs the units-to-metres conversion.
 */
data class ArMesh(
    val vertices: List<ArVector3>,
    val triangleIndices: List<Int> = emptyList(),
    val lineIndices: List<Int> = emptyList(),
    val pointRadiusUnits: Double = 0.05,
) {
    init {
        require(triangleIndices.size % 3 == 0) { "Triangle indices must be grouped in threes." }
        require(lineIndices.size % 2 == 0) { "Line indices must be grouped in pairs." }
        require((triangleIndices + lineIndices).all { it in vertices.indices }) { "Mesh index is outside the vertex list." }
        require(pointRadiusUnits.isFinite() && pointRadiusUnits >= 0.0)
    }
}

data class ArSceneObject(
    val id: String,
    val kind: ArObjectKind,
    val label: String,
    val mesh: ArMesh,
    val material: ArMaterial,
    val localTransform: ArLocalTransform = ArLocalTransform(),
    val visible: Boolean = true,
    val selectable: Boolean = true,
    val dependencyIds: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank())
        require(label.isNotBlank())
        require(id !in dependencyIds) { "A scene object cannot depend on itself." }
    }
}

data class ArAnnotation(
    val id: String,
    val objectId: String?,
    val positionUnits: ArVector3,
    val text: String,
    val color: ArColor = ArColor(1f, 1f, 1f),
) {
    init {
        require(id.isNotBlank())
        require(text.isNotBlank())
    }
}

enum class ArMeasurementKind { Distance, Angle, Area, Volume, SectionPerimeter }

data class ArMeasurement(
    val id: String,
    val kind: ArMeasurementKind,
    val objectIds: Set<String>,
    val anchorPointsUnits: List<ArVector3>,
    val value: Double,
    val unit: String,
    val uncertainty: Double = 0.0,
    val environmentalEstimate: Boolean = false,
) {
    init {
        require(id.isNotBlank())
        require(value.isFinite())
        require(unit.isNotBlank())
        require(uncertainty.isFinite() && uncertainty >= 0.0)
    }
}

data class ArScene(
    val id: String,
    val revision: Long,
    val objects: List<ArSceneObject>,
    val annotations: List<ArAnnotation> = emptyList(),
    val measurements: List<ArMeasurement> = emptyList(),
    val placement: ArScenePlacement = ArScenePlacement(),
    val environmentIntensity: Float = 1f,
    val depthOcclusionEnabled: Boolean = false,
) {
    init {
        require(id.isNotBlank())
        require(revision >= 0L)
        require(environmentIntensity.isFinite() && environmentIntensity >= 0f)
        val objectIds = objects.map { it.id }
        require(objectIds.size == objectIds.toSet().size) { "Scene object IDs must be unique." }
        require(annotations.map { it.id }.distinct().size == annotations.size) { "Annotation IDs must be unique." }
        require(measurements.map { it.id }.distinct().size == measurements.size) { "Measurement IDs must be unique." }
    }
}

/** Implemented by app-side adapters over existing Graph, Geometry, 3D, CAS and Algebra engines. */
fun interface ArSceneSource {
    fun snapshot(): ArScene
}
