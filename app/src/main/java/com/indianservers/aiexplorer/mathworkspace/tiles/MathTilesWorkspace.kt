package com.indianservers.aiexplorer.mathworkspace.tiles

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.FloatingWorkspaceTool
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import com.indianservers.aiexplorer.mathworkspace.components.NumericField
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceAction
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceFloatingTools
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSectionTitle
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MathTilesWorkspace(viewModel: MathTilesViewModel, onBack: () -> Unit) {
    val vm = viewModel
    val stop by vm.sheetStop
    MathWorkspaceShell(
        title = "Math Tiles",
        subtitle = "Build and simplify algebra by moving tiles",
        onBack = onBack,
        sheetTitle = "Tiles · ${vm.expression}",
        sheetStop = stop,
        onSheetStopChange = { vm.sheetStop.value = it },
        canvas = { MathTileCanvas(vm, Modifier.fillMaxSize()) },
        tools = {
            WorkspaceFloatingTools(
                listOf(
                    FloatingWorkspaceTool("Select multiple", if (vm.multiSelect.value) "✓" else "Select", vm.multiSelect.value) { vm.multiSelect.value = !vm.multiSelect.value },
                    FloatingWorkspaceTool("Zoom in", "+") { vm.zoom.value = (vm.zoom.value * 1.2f).coerceAtMost(100f) },
                    FloatingWorkspaceTool("Zoom out", "−") { vm.zoom.value = (vm.zoom.value / 1.2f).coerceAtLeast(.02f) },
                    FloatingWorkspaceTool("Reset view", "◎") { vm.resetView() },
                ),
                Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            )
        },
        sheet = { MathTilesPanel(vm) },
    )
}

@Composable
private fun MathTileCanvas(vm: MathTilesViewModel, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val zoom = vm.zoom.value
    val pan = vm.pan.value
    val unitDp = 54.dp * zoom
    val unitPx = with(density) { unitDp.toPx() }
    val tiles = vm.tiles.toList()
    val pairIds = remember(vm.revealPairs.value, vm.tiles.toList()) {
        if (vm.revealPairs.value) AlgebraTileEngine.zeroPairs(tiles).flatMap { listOf(it.positiveId, it.negativeId) }.toSet() else emptySet()
    }
    BoxWithConstraints(modifier.background(P.snow).pointerInput(vm) {
        detectTransformGestures { centroid, panDelta, zoomDelta, _ ->
            vm.zoom.value = (vm.zoom.value * zoomDelta).coerceIn(.02f, 100f)
            val focal = centroid - Offset(size.width / 2f, size.height / 2f) - vm.pan.value
            vm.pan.value = vm.pan.value + panDelta + focal * (1f - zoomDelta)
        }
    }) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val spacing = (unitDp.toPx()).coerceAtLeast(12f)
            val countX = (size.width / spacing).toInt().coerceIn(1, 220)
            val countY = (size.height / spacing).toInt().coerceIn(1, 220)
            val origin = Offset(size.width / 2f + pan.x, size.height / 2f + pan.y)
            for (i in -countX..countX) {
                val x = origin.x + i * spacing
                drawLine(P.border.copy(alpha = if (i == 0) .95f else .5f), Offset(x, 0f), Offset(x, size.height), if (i == 0) 1.5f else 1f)
            }
            for (i in -countY..countY) {
                val y = origin.y + i * spacing
                drawLine(P.border.copy(alpha = if (i == 0) .95f else .5f), Offset(0f, y), Offset(size.width, y), if (i == 0) 1.5f else 1f)
            }
        }
        tiles.forEach { tile ->
            val square = tile.kind.family == TileFamily.Square
            val unitTile = tile.kind.family == TileFamily.Unit
            val tileWidth = unitDp * if (square) 1.2f else if (unitTile) .36f else 1.2f
            val tileHeight = unitDp * if (square) 1.2f else if (unitTile) .36f else .36f
            val centerX = widthPx / 2f + pan.x + tile.x * unitPx
            val centerY = heightPx / 2f + pan.y + tile.y * unitPx
            val color = when (tile.kind) {
                TileKind.XSquared -> P.blue
                TileKind.PositiveX -> P.green
                TileKind.NegativeX -> P.red
                TileKind.PositiveOne -> Color(0xFFCAE0FF)
                TileKind.NegativeOne -> P.amber
            }
            val pairGlow by animateFloatAsState(if (tile.id in vm.cancellingIds) .04f else if (tile.id in pairIds) 1.04f else 1f, animationSpec = tween(240), label = "zero pair emphasis")
            val selected = tile.id in vm.selectedIds
            val background by animateColorAsState(if (tile.id in pairIds) Color(0xFFFFE8A8) else color, label = "tile highlight")
            val position = Modifier.offset {
                IntOffset((centerX - with(density) { tileWidth.toPx() } / 2f).roundToInt(), (centerY - with(density) { tileHeight.toPx() } / 2f).roundToInt())
            }.size(tileWidth, tileHeight).scale(pairGlow)
                .clip(RoundedCornerShape(if (square || unitTile) 10.dp else 8.dp))
                .background(background)
                .border(if (selected) 3.dp else if (tile.id in pairIds) 2.dp else 1.dp, if (selected) P.violet else color.copy(alpha = .65f), RoundedCornerShape(9.dp))
                .semantics { contentDescription = "${tile.kind.label} algebra tile${if (selected) ", selected" else ""}" }
                .clickable { vm.select(tile.id) }
                .pointerInput(tile.id, unitPx, vm.multiSelect.value) {
                    detectDragGestures(
                        onDragStart = { vm.prepareDrag(tile.id); vm.beginDrag() },
                        onDrag = { change, delta -> change.consume(); vm.move(tile.id, delta.x / unitPx, delta.y / unitPx) },
                        onDragEnd = vm::endDrag,
                        onDragCancel = vm::endDrag,
                    )
                }
            Box(position, contentAlignment = Alignment.Center) {
                Text(tile.kind.label, color = if (tile.kind == TileKind.PositiveOne) P.ink else Color.White, fontSize = if (square) 24.sp else 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MathTilesPanel(vm: MathTilesViewModel) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WorkspaceSectionTitle("Expression · ${vm.expression}") {
            WorkspaceAction("Undo", "Undo tile change", vm::undo, enabled = vm.canUndo)
            WorkspaceAction("Redo", "Redo tile change", vm::redo, enabled = vm.canRedo)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            NumericField("Enter polynomial", vm.expressionInput.value, { vm.expressionInput.value = it }, Modifier.weight(1f))
            WorkspaceAction("Build", "Build tiles from polynomial", vm::buildFromInput)
        }
        if (vm.inputError.value.isNotBlank()) Text(vm.inputError.value, color = P.red, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TileKind.entries.forEach { kind ->
                PaletteTile(kind, vm)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            WorkspaceAction(if (vm.revealPairs.value) "Hide zero pairs" else "Reveal zero pairs", "Reveal matching positive and negative tiles", { vm.revealPairs.value = !vm.revealPairs.value }, vm.revealPairs.value)
            WorkspaceAction("Remove pairs · ${vm.pairs.size}", "Cancel zero pairs", {
                vm.beginZeroPairCancellation()
                scope.launch { delay(240); vm.removeZeroPairs() }
            }, enabled = vm.pairs.isNotEmpty())
            WorkspaceAction("Duplicate", "Duplicate selected tiles", vm::duplicateSelected, enabled = vm.selectedIds.isNotEmpty())
            WorkspaceAction("Group", "Group selected tiles", vm::groupSelected, enabled = vm.selectedIds.size > 1)
            WorkspaceAction("Ungroup", "Ungroup selected tiles", vm::ungroupSelected, enabled = vm.selectedIds.isNotEmpty())
            WorkspaceAction("Snap", "Align selected tiles to grid", vm::snapSelected, enabled = vm.selectedIds.isNotEmpty())
            WorkspaceAction("Delete", "Delete selected tiles", vm::deleteSelected, enabled = vm.selectedIds.isNotEmpty())
            WorkspaceAction("Clear", "Clear all tiles", vm::clear, enabled = vm.tiles.isNotEmpty())
        }
        Text("Drag a tile from the palette into the canvas. Select several tiles to group, duplicate, snap, or delete them.", color = P.muted, fontSize = 11.sp)
    }
}

@Composable
private fun PaletteTile(kind: TileKind, vm: MathTilesViewModel) {
    val color = when (kind) {
        TileKind.XSquared -> P.blue
        TileKind.PositiveX -> P.green
        TileKind.NegativeX -> P.red
        TileKind.PositiveOne -> Color(0xFFCAE0FF)
        TileKind.NegativeOne -> P.amber
    }
    val unitPx = with(LocalDensity.current) { 54.dp.toPx() * vm.zoom.value }
    Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(11.dp)).background(P.panelBlue).border(1.dp, P.border, RoundedCornerShape(11.dp))
            .clickable { vm.addTile(kind) }
            .pointerInput(kind, unitPx) {
                var draggedId: String? = null
                detectDragGestures(
                    onDragStart = { draggedId = vm.beginPaletteDrag(kind) },
                    onDrag = { change, delta ->
                        change.consume()
                        draggedId?.let { vm.move(it, delta.x / unitPx, delta.y / unitPx) }
                    },
                    onDragEnd = { vm.endDrag(); draggedId = null },
                    onDragCancel = { vm.endDrag(); draggedId = null },
                )
            }
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(if (kind.family == TileFamily.Square) 38.dp else if (kind.family == TileFamily.Unit) 28.dp else 46.dp, if (kind.family == TileFamily.Linear) 24.dp else if (kind.family == TileFamily.Square) 38.dp else 28.dp)
                .clip(RoundedCornerShape(5.dp)).background(color),
            contentAlignment = Alignment.Center,
        ) { Text(kind.label, color = if (kind == TileKind.PositiveOne) P.ink else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Text("Tap or drag", color = P.muted, fontSize = 9.sp)
    }
}
