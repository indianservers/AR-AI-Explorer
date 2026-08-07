package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.SpatialMaterial
import com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.SurfaceMesh

internal enum class WorkspacePaletteId {
    Spectral,
    Aurora,
    Ember,
    Ice,
    CyberNeon,
    Abyss,
    EmeraldGold,
    Porcelain,
    Topographic,
}
internal enum class WorkspaceTexture { Smooth, Mesh, Contour, Faceted }

internal data class WorkspaceAxisStyle(
    val x: Color,
    val y: Color,
    val z: Color,
    val grid: Color,
    val label: Color = Color.White,
)

internal data class WorkspacePalette(
    val id: WorkspacePaletteId,
    val label: String,
    val background: Color,
    val colors: List<Color>,
    val axes: WorkspaceAxisStyle,
    val defaultMaterial: SpatialMaterial = SpatialMaterial.Gloss,
    val defaultTexture: WorkspaceTexture = WorkspaceTexture.Mesh,
    val defaultGlow: Boolean = true,
)

internal data class WorkspaceAppearance(
    val paletteId: WorkspacePaletteId = WorkspacePaletteId.Spectral,
    val colorIndex: Int = 0,
    val material: SpatialMaterial = SpatialMaterial.Gloss,
    val texture: WorkspaceTexture = WorkspaceTexture.Mesh,
    val glow: Boolean = true,
) {
    val palette: WorkspacePalette get() = WorkspaceVisualStyles.palette(paletteId)
    val color: Color get() = palette.colors[colorIndex.mod(palette.colors.size)]
}

internal data class StyledSurfaceMesh(
    val mesh: SurfaceMesh,
    val appearance: WorkspaceAppearance,
    val opacity: Float = 1f,
    val renderMode: SpatialSurfaceRenderMode = SpatialSurfaceRenderMode.SurfaceMesh,
)

internal object WorkspaceVisualStyles {
    // Default colors are deliberately limited to the supplied reference artwork.
    val ReferenceNavy = Color(0xFF020817)
    val ReferenceCyan = Color(0xFF00E5FF)
    val ReferenceBlue = Color(0xFF087CFF)
    val ReferenceViolet = Color(0xFF7A2CFF)
    val ReferenceMagenta = Color(0xFFF000D9)
    val ReferenceCoral = Color(0xFFFF4B83)
    val ReferenceOrange = Color(0xFFFF7A2F)
    val ReferenceYellow = Color(0xFFFFD93D)

    val Spectral = WorkspacePalette(
        id = WorkspacePaletteId.Spectral,
        label = "Spectral",
        background = ReferenceNavy,
        colors = listOf(
            ReferenceCyan,
            ReferenceBlue,
            ReferenceViolet,
            ReferenceMagenta,
            ReferenceCoral,
            ReferenceOrange,
            ReferenceYellow,
        ),
        axes = WorkspaceAxisStyle(
            x = ReferenceBlue,
            y = ReferenceMagenta,
            z = ReferenceCyan,
            grid = ReferenceBlue.copy(alpha = .22f),
        ),
    )
    val Aurora = WorkspacePalette(
        WorkspacePaletteId.Aurora,
        "Aurora",
        Color(0xFF03120F),
        listOf(Color(0xFF2AF5C8), Color(0xFF43C8FF), Color(0xFFA274FF), Color(0xFFFF6EC7)),
        WorkspaceAxisStyle(Color(0xFF43C8FF), Color(0xFFFF6EC7), Color(0xFF2AF5C8), Color(0xFF43C8FF).copy(.2f)),
    )
    val Ember = WorkspacePalette(
        WorkspacePaletteId.Ember,
        "Ember",
        Color(0xFF120706),
        listOf(Color(0xFFFFD166), Color(0xFFFF8C42), Color(0xFFFF4D6D), Color(0xFFB537F2)),
        WorkspaceAxisStyle(Color(0xFFFF8C42), Color(0xFFFF4D6D), Color(0xFFFFD166), Color(0xFFFF8C42).copy(.2f)),
    )
    val Ice = WorkspacePalette(
        WorkspacePaletteId.Ice,
        "Ice",
        Color(0xFF031018),
        listOf(Color(0xFFE8FBFF), Color(0xFF8AE8FF), Color(0xFF36A9FF), Color(0xFF6D7CFF)),
        WorkspaceAxisStyle(Color(0xFF36A9FF), Color(0xFF8AE8FF), Color(0xFFE8FBFF), Color(0xFF36A9FF).copy(.18f)),
    )
    val CyberNeon = WorkspacePalette(
        WorkspacePaletteId.CyberNeon,
        "Cyber Neon",
        Color(0xFF010107),
        listOf(Color(0xFF00E5FF), Color(0xFF087CFF), Color(0xFF7A2CFF), Color(0xFFF000D9), Color(0xFFFF4BCA)),
        WorkspaceAxisStyle(
            x = Color(0xFF00B8FF),
            y = Color(0xFFF000D9),
            z = Color(0xFF00E5FF),
            grid = Color(0xFF2752D7).copy(alpha = .28f),
        ),
        defaultMaterial = SpatialMaterial.Gloss,
        defaultTexture = WorkspaceTexture.Mesh,
        defaultGlow = true,
    )
    val Abyss = WorkspacePalette(
        WorkspacePaletteId.Abyss,
        "Abyss",
        Color(0xFF001329),
        listOf(Color(0xFF00F0E4), Color(0xFF00B7FF), Color(0xFF087CFF), Color(0xFF5B4BFF), Color(0xFF8D5CFF)),
        WorkspaceAxisStyle(
            x = Color(0xFF72E8FF),
            y = Color(0xFF00D5CC),
            z = Color(0xFF5DA8FF),
            grid = Color(0xFF087CFF).copy(alpha = .22f),
        ),
        defaultMaterial = SpatialMaterial.Glass,
        defaultTexture = WorkspaceTexture.Mesh,
        defaultGlow = true,
    )
    val EmeraldGold = WorkspacePalette(
        WorkspacePaletteId.EmeraldGold,
        "Emerald Gold",
        Color(0xFF06100B),
        listOf(Color(0xFF0B704F), Color(0xFF15956A), Color(0xFF2EC98F), Color(0xFFD7AE45), Color(0xFFFFD76A)),
        WorkspaceAxisStyle(
            x = Color(0xFFD7AE45),
            y = Color(0xFF2EC98F),
            z = Color(0xFFFFD76A),
            grid = Color(0xFFD7AE45).copy(alpha = .22f),
            label = Color(0xFFFFE4A3),
        ),
        defaultMaterial = SpatialMaterial.Metal,
        defaultTexture = WorkspaceTexture.Faceted,
        defaultGlow = true,
    )
    val Porcelain = WorkspacePalette(
        WorkspacePaletteId.Porcelain,
        "Porcelain",
        Color(0xFFF7FAFF),
        listOf(Color(0xFFB8A8FF), Color(0xFF8EDBFF), Color(0xFF9DEBE4), Color(0xFFFFD2B8), Color(0xFFE4CCFF)),
        WorkspaceAxisStyle(
            x = Color(0xFF203553),
            y = Color(0xFF477B9E),
            z = Color(0xFF162843),
            grid = Color(0xFF7AA6CE).copy(alpha = .24f),
            label = Color(0xFF162843),
        ),
        defaultMaterial = SpatialMaterial.Glass,
        defaultTexture = WorkspaceTexture.Smooth,
        defaultGlow = false,
    )
    val Topographic = WorkspacePalette(
        WorkspacePaletteId.Topographic,
        "Topographic",
        Color(0xFF080D18),
        listOf(Color(0xFF32E6E2), Color(0xFF35B9FF), Color(0xFF7857D8), Color(0xFFFFD76A), Color(0xFFF89B5B)),
        WorkspaceAxisStyle(
            x = Color(0xFF6DEBF0),
            y = Color(0xFFFFD76A),
            z = Color(0xFF8ABEFF),
            grid = Color(0xFF8ABEFF).copy(alpha = .19f),
        ),
        defaultMaterial = SpatialMaterial.Matte,
        defaultTexture = WorkspaceTexture.Mesh,
        defaultGlow = false,
    )

    val palettes = listOf(Spectral, Aurora, Ember, Ice, CyberNeon, Abyss, EmeraldGold, Porcelain, Topographic)
    fun palette(id: WorkspacePaletteId): WorkspacePalette = palettes.first { it.id == id }
    fun spectralColor(index: Int): Color = Spectral.colors[index.mod(Spectral.colors.size)]
}

internal fun WorkspaceAppearance.switchPalette(palette: WorkspacePalette): WorkspaceAppearance = copy(
    paletteId = palette.id,
    colorIndex = colorIndex.mod(palette.colors.size),
    material = palette.defaultMaterial,
    texture = palette.defaultTexture,
    glow = palette.defaultGlow,
)

internal fun SpatialSurfaceLayer.workspaceAppearance(): WorkspaceAppearance {
    val paletteId = when (paletteKey.lowercase()) {
        "aurora" -> WorkspacePaletteId.Aurora
        "ember" -> WorkspacePaletteId.Ember
        "ice" -> WorkspacePaletteId.Ice
        "cyberneon" -> WorkspacePaletteId.CyberNeon
        "abyss" -> WorkspacePaletteId.Abyss
        "emeraldgold" -> WorkspacePaletteId.EmeraldGold
        "porcelain" -> WorkspacePaletteId.Porcelain
        "topographic" -> WorkspacePaletteId.Topographic
        else -> WorkspacePaletteId.Spectral
    }
    val texture = when (textureKey.lowercase()) {
        "smooth" -> WorkspaceTexture.Smooth
        "contour" -> WorkspaceTexture.Contour
        "faceted" -> WorkspaceTexture.Faceted
        else -> WorkspaceTexture.Mesh
    }
    return WorkspaceAppearance(paletteId, colorIndex, material, texture, glow)
}

internal fun SpatialSurfaceLayer.withWorkspaceAppearance(appearance: WorkspaceAppearance): SpatialSurfaceLayer = copy(
    paletteKey = appearance.paletteId.name.lowercase(),
    colorIndex = appearance.colorIndex,
    material = appearance.material,
    textureKey = appearance.texture.name.lowercase(),
    glow = appearance.glow,
)

@Composable
internal fun WorkspaceAppearancePicker(
    appearance: WorkspaceAppearance,
    onChange: (WorkspaceAppearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GlowButton(appearance.palette.label) {
            val palettes = WorkspaceVisualStyles.palettes
            val next = palettes[(palettes.indexOfFirst { it.id == appearance.paletteId } + 1).mod(palettes.size)]
            onChange(appearance.switchPalette(next))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.widthIn(max = 250.dp),
        ) {
            appearance.palette.colors.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .size(28.dp)
                        .background(color, CircleShape)
                        .border(
                            if (index == appearance.colorIndex) 3.dp else 1.dp,
                            if (index == appearance.colorIndex) Color.White else color.copy(alpha = .45f),
                            CircleShape,
                        )
                        .clickable { onChange(appearance.copy(colorIndex = index)) }
                        .semantics { contentDescription = "Object color ${index + 1}" },
                )
            }
        }
        GlowButton(appearance.material.name) {
            val entries = SpatialMaterial.entries
            onChange(appearance.copy(material = entries[(appearance.material.ordinal + 1) % entries.size]))
        }
        GlowButton(appearance.texture.name) {
            val entries = WorkspaceTexture.entries
            onChange(appearance.copy(texture = entries[(appearance.texture.ordinal + 1) % entries.size]))
        }
        GlowButton(if (appearance.glow) "Glow" else "No glow") {
            onChange(appearance.copy(glow = !appearance.glow))
        }
    }
}

@Composable
internal fun WorkspaceThemeButton(
    appearance: WorkspaceAppearance,
    onSelect: (WorkspacePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.semantics { contentDescription = "Change 3D theme" }) {
        GlowButton(
            label = "3D theme",
            icon = "◈",
            iconOnly = true,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            WorkspaceVisualStyles.palettes.forEach { palette ->
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                palette.colors.take(3).forEach { color ->
                                    Box(Modifier.size(12.dp).background(color, CircleShape))
                                }
                            }
                            Text(
                                palette.label,
                                color = if (palette.id == appearance.paletteId) palette.axes.z else Color.Unspecified,
                                fontSize = 13.sp,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(palette)
                    },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun WorkspaceAxisPicker(
    axes: WorkspaceAxisStyle,
    palette: WorkspacePalette,
    onChange: (WorkspaceAxisStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            Triple("X", axes.x) { color: Color -> onChange(axes.copy(x = color)) },
            Triple("Y", axes.y) { color: Color -> onChange(axes.copy(y = color)) },
            Triple("Z", axes.z) { color: Color -> onChange(axes.copy(z = color)) },
        ).forEach { (label, color, update) ->
            Row(
                Modifier
                    .clickable {
                        val current = palette.colors.indexOf(color).takeIf { it >= 0 } ?: 0
                        update(palette.colors[(current + 1) % palette.colors.size])
                    }
                    .semantics { contentDescription = "Change $label axis color" },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = color, fontSize = 11.sp)
                Box(Modifier.size(22.dp).background(color, CircleShape).border(1.dp, Color.White.copy(.7f), CircleShape))
            }
        }
    }
}
