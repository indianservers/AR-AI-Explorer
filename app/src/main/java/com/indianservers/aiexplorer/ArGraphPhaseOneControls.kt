package com.indianservers.aiexplorer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.indianservers.aiexplorer.arengine.interaction.ArHandTool

enum class ArGraphToolbarState {
    Expanded, Compact, Immersive;
    fun collapse() = when (this) { Expanded -> Compact; Compact, Immersive -> Immersive }
    fun expand() = when (this) { Immersive -> Compact; Compact, Expanded -> Expanded }
}
enum class ArGraphInteractionMode { Touch, Gesture, Hybrid }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArGraphPhaseOneToolbar(
    modifier: Modifier, state: ArGraphToolbarState, onState: (ArGraphToolbarState) -> Unit,
    handsEnabled: Boolean, onHands: () -> Unit, canUndo: Boolean, canRedo: Boolean,
    onUndo: () -> Unit, onRedo: () -> Unit, onBack: () -> Unit,
    onCreate: () -> Unit, onEquation: () -> Unit, onTool: (ArHandTool) -> Unit,
    onSelect: () -> Unit, onArControls: () -> Unit, onMore: () -> Unit,
) {
    var drag by remember { mutableFloatStateOf(0f) }
    val panel = modifier.widthIn(max = 360.dp).animateContentSize()
        .background(Color(0xED131D2D), RoundedCornerShape(16.dp))
        .pointerInput(state) {
            detectVerticalDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                if (drag < -32.dp.toPx()) onState(state.collapse())
                if (drag > 32.dp.toPx()) onState(state.expand())
            }) { change, dy -> change.consume(); drag += dy }
        }
    if (state == ArGraphToolbarState.Immersive) {
        Row(panel.padding(4.dp)) {
            TextButton(onClick = { onState(state.expand()) }, modifier = Modifier.semantics { contentDescription = "Reveal graph toolbar" }) { Text("⌄") }
            if (handsEnabled) HandControl(true, onHands)
        }
        return
    }
    Column(panel.padding(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            if (state == ArGraphToolbarState.Expanded) {
                TextButton(onClick = onBack) { Text("Back", fontSize = 12.sp) }
                TextButton(onClick = onCreate) { Text("Create", fontSize = 12.sp) }
                TextButton(onClick = onEquation) { Text("Equation", fontSize = 12.sp) }
                TextButton(onClick = onSelect) { Text("Select", fontSize = 12.sp) }
                ArHandTool.entries.forEach { tool -> TextButton(onClick = { onTool(tool) }) { Text(tool.name, fontSize = 12.sp) } }
                TextButton(onClick = onArControls) { Text("AR controls", fontSize = 12.sp) }
            } else {
                TextButton(onClick = onMore) { Text("Select / Transform", fontSize = 11.sp) }
            }
            TextButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.semantics { contentDescription = "Undo graph action" }) { Text("↶") }
            TextButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.semantics { contentDescription = "Redo graph action" }) { Text("↷") }
            HandControl(handsEnabled, onHands)
            if (state == ArGraphToolbarState.Expanded) TextButton(onClick = onMore) { Text("More", fontSize = 12.sp) }
            TextButton(onClick = { onState(if (state == ArGraphToolbarState.Expanded) state.collapse() else state.expand()) }, modifier = Modifier.semantics { contentDescription = if (state == ArGraphToolbarState.Expanded) "Compact graph toolbar" else "Expand graph toolbar" }) { Text(if (state == ArGraphToolbarState.Expanded) "⌃" else "⌄") }
        }
    }
}

@Composable
private fun HandControl(enabled: Boolean, onClick: () -> Unit) {
    Canvas(Modifier.size(48.dp).semantics { contentDescription = if (enabled) "Hand gestures on" else "Hand gestures off" }.clickable(onClick = onClick)) {
        val path = Path().apply {
            moveTo(size.width*.31f, size.height*.76f)
            lineTo(size.width*.17f, size.height*.49f); lineTo(size.width*.25f, size.height*.43f)
            lineTo(size.width*.36f, size.height*.53f); lineTo(size.width*.36f, size.height*.23f)
            lineTo(size.width*.43f, size.height*.23f); lineTo(size.width*.43f, size.height*.45f)
            lineTo(size.width*.48f, size.height*.18f); lineTo(size.width*.55f, size.height*.18f)
            lineTo(size.width*.55f, size.height*.45f); lineTo(size.width*.60f, size.height*.23f)
            lineTo(size.width*.67f, size.height*.23f); lineTo(size.width*.67f, size.height*.49f)
            lineTo(size.width*.72f, size.height*.34f); lineTo(size.width*.79f, size.height*.34f)
            lineTo(size.width*.77f, size.height*.67f); lineTo(size.width*.65f, size.height*.79f); close()
        }
        drawPath(path, Color(0xFFBFEAF5), style = Stroke(1.7.dp.toPx()))
        if (enabled) drawCircle(Color(0xFF68EB9B), 3.dp.toPx(), Offset(size.width*.84f, size.height*.21f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArGraphPhaseOneSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val scroll = remember(title) { ScrollState(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF131D2D)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).background(Color(0xFF131D2D), RoundedCornerShape(20.dp)).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Column(Modifier.fillMaxWidth().weight(1f,fill=false).verticalScroll(scroll)) { content() }
        }
    }
}
