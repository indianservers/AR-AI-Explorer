package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.arengine.interaction.ArGizmoMode
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionEngine
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.spatial.SpatialBlendMode
import com.indianservers.aiexplorer.spatial.SpatialGeometry
import com.indianservers.aiexplorer.spatial.SpatialMaterial
import com.indianservers.aiexplorer.spatial.SpatialPrimitive
import com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind
import com.indianservers.aiexplorer.spatial.SpatialRenderScene

internal fun phase4DisplayScene(
    source: SpatialRenderScene,
    selection: ArSelectionState,
    ghost: Boolean,
    solids: List<Solid>,
    gizmoMode: ArGizmoMode,
    hoverObjectId: String?,
): SpatialRenderScene {
    val styled = source.primitives.map { primitive ->
        val selected = primitive.id in selection.objectIds
        val hovered = primitive.id == hoverObjectId && !selected
        val visible = primitive.visible && ArSelectionEngine.isVisible(selection, primitive.id)
        val color = primitive.material.colorRgba.toMutableList().apply {
            while (size < 4) add(1f)
            if (ghost) this[3] = minOf(this[3], .28f)
            if (selected) {
                this[0] = 1f
                this[1] = .68f
                this[2] = .12f
                this[3] = 1f
            } else if (hovered) {
                this[0] = .35f
                this[1] = 1f
                this[2] = .72f
                this[3] = 1f
            }
        }
        primitive.copy(
            visible = visible,
            selectable = primitive.selectable && !ghost,
            material = primitive.material.copy(
                colorRgba = color,
                emissive = if (selected || hovered) maxOf(primitive.material.emissive, if (selected) .3f else .18f) else primitive.material.emissive,
                blendMode = if (ghost) SpatialBlendMode.Transparent else primitive.material.blendMode,
            ),
        )
    }
    if (ghost || selection.objectIds.isEmpty()) return source.copy(primitives = styled)
    val gizmos = selection.objectIds.mapNotNull {
        it.removePrefix("solid-").toIntOrNull()?.let { index -> index to solids.getOrNull(index) }
    }.filter { it.second != null }.flatMap { (index, nullableSolid) ->
        val solid = nullableSolid ?: return@flatMap emptyList()
        val origin = solid.position
        val axes = listOf(
            Triple("x", Vec3(1.0, 0.0, 0.0), listOf(1f, .18f, .22f, 1f)),
            Triple("y", Vec3(0.0, 1.0, 0.0), listOf(.2f, 1f, .4f, 1f)),
            Triple("z", Vec3(0.0, 0.0, 1.0), listOf(.25f, .55f, 1f, 1f)),
        )
        axes.map { (name, axis, color) ->
            val points = when (gizmoMode) {
                ArGizmoMode.Translate, ArGizmoMode.Scale -> listOf(origin, origin + axis * .85)
                ArGizmoMode.Rotate -> (0..40).map { step ->
                    val angle = step * Math.PI * 2.0 / 40.0
                    when (name) {
                        "x" -> origin + Vec3(0.0, kotlin.math.cos(angle), kotlin.math.sin(angle)) * .72
                        "y" -> origin + Vec3(kotlin.math.cos(angle), 0.0, kotlin.math.sin(angle)) * .72
                        else -> origin + Vec3(kotlin.math.cos(angle), kotlin.math.sin(angle), 0.0) * .72
                    }
                }
            }
            SpatialPrimitive(
                id = "gizmo-$index-$name",
                kind = SpatialPrimitiveKind.Curve,
                geometry = SpatialGeometry(
                    vertices = points,
                    lines = if (gizmoMode == ArGizmoMode.Rotate) points.indices.toList().dropLast(1).map { it to it + 1 } else listOf(0 to 1),
                    pointRadius = if (gizmoMode == ArGizmoMode.Scale) .12 else .055,
                ),
                material = SpatialMaterial(
                    name = "$name ${gizmoMode.name.lowercase()} handle",
                    colorRgba = color,
                    roughness = .28f,
                    emissive = .35f,
                ),
                label = "${gizmoMode.name} ${name.uppercase()}",
                selectable = false,
            )
        }
    }
    return source.copy(primitives = styled + gizmos)
}
