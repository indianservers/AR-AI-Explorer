package com.indianservers.aiexplorer

import androidx.compose.animation.AnimatedVisibility
import com.indianservers.aiexplorer.core.Vec2
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Geometry2DResizePolicy { Free, Proportional }

@Composable
internal fun Geometry2DObjectBar(
    name: String,
    objectCount: Int,
    detailsExpanded: Boolean,
    onDetailsToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))))
            .border(1.dp, Violet.copy(.48f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = "$name selected. $objectCount objects on canvas" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Violet, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$objectCount object${if (objectCount == 1) "" else "s"}", color = Muted, fontSize = 8.sp)
        }
        Text(
            if (detailsExpanded) "Hide" else "Details",
            color = Green,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onDetailsToggle)
                .padding(horizontal = 9.dp, vertical = 7.dp)
                .semantics { contentDescription = if (detailsExpanded) "Hide object details" else "Show object details" },
        )
    }
}

@Composable
internal fun Geometry2DBottomDock(
    mode: Transform2DMode,
    selected: Boolean,
    locked: Boolean,
    canClear: Boolean,
    rotationAngle: Double,
    resizePolicy: Geometry2DResizePolicy,
    onMode: (Transform2DMode) -> Unit,
    onAdd: () -> Unit,
    onLock: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onClearAll: () -> Unit,
    onNudge: (Vec2) -> Unit,
    onScale: (Double) -> Unit,
    onRotateBy: (Double) -> Unit,
    onResetRotation: () -> Unit,
    onResizePolicy: (Geometry2DResizePolicy) -> Unit,
    onFit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    Column(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF50C1627), Color(0xF507101D))))
            .border(1.dp, Cyan.copy(.42f), RoundedCornerShape(18.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .semantics { contentDescription = if (expanded) "Collapse 2D geometry tools" else "Expand 2D geometry tools" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (selected) "${mode.name} tool" else "2D geometry tools", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(if (expanded) "Tap to collapse" else "Tap to open controls", color = Muted, fontSize = 8.sp)
            }
            Geometry2DContextButton("+ Add", Modifier.width(76.dp)) { onAdd() }
            Geometry2DContextButton("Clear all", Modifier.width(86.dp), destructive = true, enabled = canClear) { onClearAll() }
            Text(if (expanded) "  ^" else "  v", color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        AnimatedVisibility(selected) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Geometry2DCompactActionButton("Move", mode == Transform2DMode.Move, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Move) }
                Geometry2DCompactActionButton("Size", mode == Transform2DMode.Resize, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Resize) }
                Geometry2DCompactActionButton("Rot", mode == Transform2DMode.Rotate, Violet, Modifier.weight(1f)) { onMode(Transform2DMode.Rotate) }
                Geometry2DCompactActionButton("Copy", false, Violet, Modifier.weight(1f), onClick = onDuplicate)
                Geometry2DCompactActionButton(if (locked) "Unlock" else "Lock", locked, Amber, Modifier.weight(1f), onClick = onLock)
                Geometry2DCompactActionButton("Del", false, Color(0xFFFF6688), Modifier.weight(1f), onClick = onDelete)
            }
        }

        AnimatedVisibility(selected) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Geometry2DCompactActionButton("←", false, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Move); onNudge(Vec2(-.25, 0.0)) }
                    Geometry2DCompactActionButton("↑", false, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Move); onNudge(Vec2(0.0, .25)) }
                    Geometry2DCompactActionButton("↓", false, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Move); onNudge(Vec2(0.0, -.25)) }
                    Geometry2DCompactActionButton("→", false, Cyan, Modifier.weight(1f)) { onMode(Transform2DMode.Move); onNudge(Vec2(.25, 0.0)) }
                    Geometry2DCompactActionButton("- Size", false, Cyan, Modifier.weight(1.35f)) { onMode(Transform2DMode.Resize); onScale(.9) }
                    Geometry2DCompactActionButton("+ Size", false, Cyan, Modifier.weight(1.35f)) { onMode(Transform2DMode.Resize); onScale(1.1) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Geometry2DCompactActionButton("-15°", false, Violet, Modifier.weight(1f)) { onMode(Transform2DMode.Rotate); onRotateBy(-15.0) }
                    Geometry2DCompactActionButton("+15°", false, Violet, Modifier.weight(1f)) { onMode(Transform2DMode.Rotate); onRotateBy(15.0) }
                    Geometry2DCompactActionButton(if (resizePolicy == Geometry2DResizePolicy.Proportional) "Ratio" else "Free", resizePolicy == Geometry2DResizePolicy.Proportional, Cyan, Modifier.weight(1f)) {
                        onMode(Transform2DMode.Resize)
                        onResizePolicy(if (resizePolicy == Geometry2DResizePolicy.Proportional) Geometry2DResizePolicy.Free else Geometry2DResizePolicy.Proportional)
                    }
                    Geometry2DCompactActionButton("Clear", false, Color(0xFFFF6688), Modifier.weight(1f), onClick = onClearAll)
                }
            }
        }

        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Transform2DMode.entries.forEach { tool ->
                        Geometry2DToolButton(
                            icon = when (tool) {
                                Transform2DMode.Select -> "S"
                                Transform2DMode.Move -> "MOVE"
                                Transform2DMode.Resize -> "SIZE"
                                Transform2DMode.Rotate -> "ROT"
                            },
                            label = tool.name,
                            accent = if (tool == Transform2DMode.Rotate) Violet else Cyan,
                            enabled = selected || tool == Transform2DMode.Select,
                            selected = mode == tool,
                            modifier = Modifier.weight(1f),
                        ) { onMode(tool); overflowOpen = false }
                    }
                    Geometry2DToolButton(if (locked) "U" else "L", if (locked) "Unlock" else "Lock", Amber, selected = locked, enabled = selected, modifier = Modifier.weight(1f), onClick = onLock)
                    Geometry2DToolButton("DEL", "Delete", Color(0xFFFF6688), enabled = selected, selected = false, modifier = Modifier.weight(1f), onClick = onDelete)
                    Geometry2DToolButton("...", "More", Violet, enabled = true, selected = overflowOpen, modifier = Modifier.weight(1f)) { overflowOpen = !overflowOpen }
                }

                AnimatedVisibility(selected && mode == Transform2DMode.Rotate) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Geometry2DContextButton("-15 deg", Modifier.weight(1f)) { onRotateBy(-15.0) }
                        Text("Angle ${formatGeometryAngle(rotationAngle)}", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Geometry2DContextButton("+15 deg", Modifier.weight(1f)) { onRotateBy(15.0) }
                        Geometry2DContextButton("Reset", Modifier.weight(1f), onClick = onResetRotation)
                    }
                }
                AnimatedVisibility(selected && mode == Transform2DMode.Resize) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Resize", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Geometry2DContextButton(if (resizePolicy == Geometry2DResizePolicy.Free) "Free selected" else "Free", Modifier.weight(1f)) { onResizePolicy(Geometry2DResizePolicy.Free) }
                        Geometry2DContextButton(if (resizePolicy == Geometry2DResizePolicy.Proportional) "Proportional selected" else "Proportional", Modifier.weight(1.4f)) { onResizePolicy(Geometry2DResizePolicy.Proportional) }
                    }
                }
                AnimatedVisibility(overflowOpen) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Geometry2DContextButton("Fit view", Modifier.weight(1f)) { onFit(); overflowOpen = false }
                        Geometry2DContextButton("Duplicate", Modifier.weight(1f), enabled = selected) { onDuplicate(); overflowOpen = false }
                        Geometry2DContextButton("Clear all", Modifier.weight(1f), destructive = true, enabled = canClear) { onClearAll(); overflowOpen = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun Geometry2DCompactActionButton(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(accent.copy(if (selected) .20f else .09f))
            .border(1.dp, accent.copy(if (selected) .66f else .28f), RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label selected 2D object" }
            .padding(horizontal = 3.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun Geometry2DToolButton(
    icon: String,
    label: String,
    accent: Color,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) accent.copy(.22f) else Color.Transparent)
            .border(1.dp, if (selected) accent.copy(.72f) else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = "$label 2D object action${if (!enabled) ", unavailable" else ""}" }
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(25.dp).clip(CircleShape).background(accent.copy(if (enabled) .15f else .05f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = accent.copy(if (enabled) 1f else .28f), fontSize = if (icon.length > 1) 7.sp else 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(1.dp))
        Text(label, color = if (enabled) Ink else Muted.copy(.28f), fontSize = 7.sp, maxLines = 1)
    }
}

@Composable
private fun Geometry2DContextButton(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val accent = if (destructive) Color(0xFFFF6688) else Violet
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(if (enabled) .12f else .04f))
            .border(1.dp, accent.copy(if (enabled) .42f else .12f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent.copy(if (enabled) 1f else .3f), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun formatGeometryAngle(value: Double): String {
    val normalized = ((value % 360.0) + 360.0) % 360.0
    return "${"%.1f".format(java.util.Locale.US, normalized).trimEnd('0').trimEnd('.')} deg"
}

internal data class GeometryRotationSnap(val angle: Double, val snapped: Boolean)

internal fun snapGeometryRotation(angle: Double, increment: Double = 15.0, threshold: Double = 2.5): GeometryRotationSnap {
    if (!angle.isFinite() || increment <= 0.0) return GeometryRotationSnap(angle, false)
    val candidate = kotlin.math.round(angle / increment) * increment
    return if (kotlin.math.abs(angle - candidate) <= threshold) GeometryRotationSnap(candidate, true)
    else GeometryRotationSnap(angle, false)
}
