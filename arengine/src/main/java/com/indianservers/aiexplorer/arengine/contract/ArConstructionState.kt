package com.indianservers.aiexplorer.arengine.contract

enum class ArConstructionObjectType { PointSet, Curve, Surface, Solid, VectorSet, Plane, Formula }

data class ArFormulaMetadata(
    val title: String,
    val expression: String,
    val derivedValues: Map<String, Double> = emptyMap(),
)

data class ArConstructionState(
    val id: String,
    val labId: String,
    val objectType: ArConstructionObjectType,
    val parameters: Map<String, Double> = emptyMap(),
    val transform: ArLocalTransform = ArLocalTransform(),
    val metersPerMathUnit: Double = 0.1,
    val color: ArColor = ArColor(.3f, .65f, 1f),
    val opacity: Float = .8f,
    val wireframe: Boolean = true,
    val labelsVisible: Boolean = true,
    val selectedPointIds: Set<String> = emptySet(),
    val formula: ArFormulaMetadata? = null,
    val labConfiguration: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank() && labId.isNotBlank())
        require(parameters.values.all(Double::isFinite))
        require(metersPerMathUnit.isFinite() && metersPerMathUnit > 0.0)
        require(opacity in 0f..1f)
    }

    fun toPersistedMap(): Map<String, String> = buildMap {
        put("id", id)
        put("labId", labId)
        put("objectType", objectType.name)
        put("metersPerMathUnit", metersPerMathUnit.toString())
        put("opacity", opacity.toString())
        put("wireframe", wireframe.toString())
        put("labelsVisible", labelsVisible.toString())
        put("color", listOf(color.red, color.green, color.blue, color.alpha).joinToString(","))
        put("offset", listOf(transform.offsetMeters.x, transform.offsetMeters.y, transform.offsetMeters.z).joinToString(","))
        put("orientation", listOf(transform.orientation.x, transform.orientation.y, transform.orientation.z, transform.orientation.w).joinToString(","))
        put("scale", transform.uniformScale.toString())
        parameters.forEach { (key, value) -> put("parameter.$key", value.toString()) }
        labConfiguration.forEach { (key, value) -> put("config.$key", value) }
    }

    companion object {
        fun fromPersistedMap(values: Map<String, String>): ArConstructionState {
            fun vector(key: String, count: Int) = values.getValue(key).split(',').map(String::toDouble).also { require(it.size == count) }
            val color = vector("color", 4)
            val offset = vector("offset", 3)
            val orientation = vector("orientation", 4)
            return ArConstructionState(
                id = values.getValue("id"),
                labId = values.getValue("labId"),
                objectType = ArConstructionObjectType.valueOf(values.getValue("objectType")),
                parameters = values.filterKeys { it.startsWith("parameter.") }.mapKeys { it.key.removePrefix("parameter.") }.mapValues { it.value.toDouble() },
                transform = ArLocalTransform(
                    offsetMeters = ArVector3(offset[0], offset[1], offset[2]),
                    orientation = ArQuaternion(orientation[0], orientation[1], orientation[2], orientation[3]),
                    uniformScale = values.getValue("scale").toDouble(),
                ),
                metersPerMathUnit = values.getValue("metersPerMathUnit").toDouble(),
                color = ArColor(color[0].toFloat(), color[1].toFloat(), color[2].toFloat(), color[3].toFloat()),
                opacity = values.getValue("opacity").toFloat(),
                wireframe = values.getValue("wireframe").toBooleanStrict(),
                labelsVisible = values.getValue("labelsVisible").toBooleanStrict(),
                labConfiguration = values.filterKeys { it.startsWith("config.") }.mapKeys { it.key.removePrefix("config.") },
            )
        }
    }
}
