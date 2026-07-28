package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArSmartEnhancementsPanel(
    vm: ExplorerViewModel,
    arWorkspaceMode: ArMathWorkspaceMode,
    linkedStatus: String,
    diagnostics: List<String>,
    visualizedObjectCount: Int,
    arSelection: ArSelectionState,
    onSelectionChange: (ArSelectionState) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    arVisualTheme: ArVisualTheme,
    onVisualTheme: (ArVisualTheme) -> Unit,
    arObjectOpacity: Float,
    onObjectOpacity: (Float) -> Unit,
    arShowWireframe: Boolean,
    onShowWireframe: (Boolean) -> Unit,
    arShowGrid: Boolean,
    onShowGrid: (Boolean) -> Unit,
    arShowAxes: Boolean,
    onShowAxes: (Boolean) -> Unit,
    arSurfaceQuality: ArSurfaceQuality,
    onSurfaceQuality: (ArSurfaceQuality) -> Unit,
    onOverlayScale: (Float) -> Unit,
    onFitDisplay: () -> Unit,
    arOverlayRotationX: Float,
    onOverlayRotationX: (Float) -> Unit,
    arOverlayRotationY: Float,
    onOverlayRotationY: (Float) -> Unit,
    arOverlayRotationZ: Float,
    onOverlayRotationZ: (Float) -> Unit,
) {
    Insight("Workspace", arWorkspaceMode.label, Cyan)
    Insight("Linked scene", linkedStatus, if (visualizedObjectCount > 0) Green else Amber)
    diagnostics.firstOrNull()?.let { Text(it, color = Amber, fontSize = 10.sp) }

    Text("Objects", color = Ink, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        when (arWorkspaceMode) {
            ArMathWorkspaceMode.Geometry2D -> {
                vm.state.shapes.forEachIndexed { index, shape ->
                    GlowButton(if (vm.selectedShape == index) "* ${shape.name.take(10)}" else shape.name.take(10)) {
                        vm.selectShape(index)
                    }
                }
                if (vm.state.shapes.isEmpty()) Text("No 2D shapes yet. Tap +.", color = Muted, fontSize = 11.sp)
            }
            ArMathWorkspaceMode.Geometry3D -> {
                vm.state.solids.forEachIndexed { index, solid ->
                    GlowButton(if ("solid-$index" == arSelection.primaryObjectId) "* ${solid.type.name.take(8)}" else solid.type.name.take(8)) {
                        vm.selectSolid(index)
                        onSelectionChange(ArSelectionState(setOf("solid-$index"), "solid-$index"))
                    }
                }
                vm.state.vectors3D.forEachIndexed { index, vector ->
                    GlowButton(if ("vector-$index" == arSelection.primaryObjectId) "* ${vector.name.take(8)}" else vector.name.take(8)) {
                        vm.selectVector3D(index)
                        onSelectionChange(ArSelectionState(setOf("vector-$index"), "vector-$index"))
                    }
                }
                if (vm.state.solids.isEmpty() && vm.state.vectors3D.isEmpty()) Text("No 3D objects yet. Tap +.", color = Muted, fontSize = 11.sp)
            }
            ArMathWorkspaceMode.Graph2D -> {
                vm.state.functions.forEachIndexed { index, function ->
                    GlowButton(if (function.visible) function.name.take(10) else "Hide ${function.name.take(6)}") {
                        vm.updateFunction(index) { it.copy(visible = !it.visible) }
                    }
                }
                if (vm.state.functions.isEmpty()) Text("No graphs yet. Tap +.", color = Muted, fontSize = 11.sp)
            }
            ArMathWorkspaceMode.Graph3D -> GlowButton(vm.state.surfaceExpression.take(18), onClick = onAdd)
            ArMathWorkspaceMode.CAS -> GlowButton("Notebook") { vm.openMathNotebook() }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GlowButton("+ Add ${arWorkspaceMode.shortLabel}", icon = "+", onClick = onAdd)
        GlowButton("- Delete ${arWorkspaceMode.shortLabel}", icon = "-", onClick = onDelete)
    }

    Text("AR style", color = Ink, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        ArVisualTheme.entries.forEach { theme ->
            GlowButton(if (arVisualTheme == theme) "* ${theme.label}" else theme.label) { onVisualTheme(theme) }
        }
    }
    AxisSlider("Opacity", arObjectOpacity, .18f..1f, onObjectOpacity)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TogglePill("Wireframe", arShowWireframe, onShowWireframe)
        TogglePill("Grid", arShowGrid, onShowGrid)
        TogglePill("Axes", arShowAxes, onShowAxes)
    }

    if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D) {
        Text("3D Graph quality", color = Ink, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ArSurfaceQuality.entries.forEach { quality ->
                GlowButton(if (arSurfaceQuality == quality) "* ${quality.label}" else quality.label) { onSurfaceQuality(quality) }
            }
        }
    }

    Text("Screen display", color = Ink, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlowButton("Small") { onOverlayScale(.28f) }
        GlowButton("Medium") { onOverlayScale(.42f) }
        GlowButton("Large") { onOverlayScale(.64f) }
        GlowButton("Fit", onClick = onFitDisplay)
    }
    AxisSlider("Rotate X", arOverlayRotationX, -90f..90f, onOverlayRotationX)
    AxisSlider("Rotate Y", arOverlayRotationY, -180f..180f, onOverlayRotationY)
    AxisSlider("Rotate Z", arOverlayRotationZ, -180f..180f, onOverlayRotationZ)
}
