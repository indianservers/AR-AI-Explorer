package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode
import com.indianservers.aiexplorer.spatial.SpatialBlendMode
import com.indianservers.aiexplorer.spatial.SpatialMaterial
import com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind
import com.indianservers.aiexplorer.spatial.SpatialRenderScene

internal enum class ArVisualTheme(val label: String, val palette: List<List<Float>>) {
    NeonGlass(
        "Neon glass",
        listOf(
            listOf(.10f, .95f, 1f, 1f),
            listOf(.72f, .34f, 1f, 1f),
            listOf(.25f, 1f, .72f, 1f),
            listOf(1f, .72f, .18f, 1f),
        ),
    ),
    Heatmap(
        "Heatmap",
        listOf(
            listOf(.10f, .86f, 1f, 1f),
            listOf(.30f, 1f, .62f, 1f),
            listOf(1f, .78f, .12f, 1f),
            listOf(1f, .22f, .34f, 1f),
        ),
    ),
    VioletCyan(
        "Cyan violet",
        listOf(
            listOf(.14f, .92f, 1f, 1f),
            listOf(.44f, .58f, 1f, 1f),
            listOf(.86f, .30f, 1f, 1f),
            listOf(.32f, 1f, .82f, 1f),
        ),
    ),
    Whiteboard(
        "Whiteboard",
        listOf(
            listOf(1f, 1f, 1f, 1f),
            listOf(.12f, .95f, 1f, 1f),
            listOf(.96f, .86f, .24f, 1f),
            listOf(.30f, 1f, .70f, 1f),
        ),
    ),
}

internal enum class ArSurfaceQuality(val label: String, val density: Int) {
    Battery("Battery", 14),
    Balanced("Balanced", 24),
    High("High", 38),
}

internal fun SpatialRenderScene.smartArStyle(
    mode: ArMathWorkspaceMode,
    theme: ArVisualTheme,
    opacity: Float,
    showWireframe: Boolean,
    showGrid: Boolean,
    showAxes: Boolean,
): SpatialRenderScene {
    val tunedOpacity = opacity.coerceIn(.18f, 1f)
    val styled = primitives.mapIndexedNotNull { index, primitive ->
        val isAxis = primitive.id.startsWith("axis-")
        val isGrid = primitive.id.contains("grid", ignoreCase = true)
        val isWire = primitive.id.contains("wire", ignoreCase = true)
        when {
            isAxis && !showAxes -> null
            isGrid && !showGrid -> null
            isWire && !showWireframe -> null
            else -> {
                val rgba = when {
                    isAxis -> listOf(1f, 1f, 1f, .95f)
                    isGrid -> listOf(.76f, .92f, 1f, .18f)
                    isWire -> theme.palette[(index + 1) % theme.palette.size].withAlpha(.86f)
                    primitive.kind == SpatialPrimitiveKind.Surface && mode == ArMathWorkspaceMode.Graph3D ->
                        theme.palette[index % theme.palette.size].withAlpha(tunedOpacity.coerceAtMost(.78f))
                    primitive.kind == SpatialPrimitiveKind.Surface ->
                        theme.palette[index % theme.palette.size].withAlpha((tunedOpacity * .72f).coerceIn(.20f, .72f))
                    primitive.kind == SpatialPrimitiveKind.Curve ->
                        theme.palette[index % theme.palette.size].withAlpha(tunedOpacity.coerceAtLeast(.72f))
                    primitive.kind == SpatialPrimitiveKind.Point ->
                        theme.palette[index % theme.palette.size].withAlpha(1f)
                    else -> theme.palette[index % theme.palette.size].withAlpha(tunedOpacity)
                }
                primitive.copy(
                    material = SpatialMaterial(
                        name = "${theme.label} ${primitive.material.name}",
                        colorRgba = rgba,
                        metallic = primitive.material.metallic,
                        roughness = if (primitive.kind == SpatialPrimitiveKind.Surface) .2f else primitive.material.roughness,
                        emissive = when {
                            primitive.kind == SpatialPrimitiveKind.Surface -> .46f
                            primitive.kind == SpatialPrimitiveKind.Curve -> .58f
                            else -> primitive.material.emissive.coerceAtLeast(.34f)
                        },
                        blendMode = SpatialBlendMode.Transparent,
                    ),
                )
            }
        }
    }
    return copy(primitives = styled)
}

private fun List<Float>.withAlpha(alpha: Float): List<Float> =
    listOf(getOrElse(0) { 1f }, getOrElse(1) { 1f }, getOrElse(2) { 1f }, alpha.coerceIn(0f, 1f))
