package com.indianservers.aiexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.workspace.Shape2D

enum class Transform2DMode { Select, Move, Resize, Rotate }
enum class SpatialEditMode { Select, Move, Resize, Rotate }

private val ControlSurface = Color(0xF20A1522)
private val ControlInk = Color(0xFFEAF7FF)
private val ControlMuted = Color(0xFF91A4B5)
private val ControlCyan = Color(0xFF20D9FF)
private val ControlViolet = Color(0xFF9B6CFF)
private val ControlGreen = Color(0xFF48E0A4)

@Composable
private fun CanvasChip(label: String, selected: Boolean, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = if (selected) "• $label" else label,
        color = if (selected) ControlInk else ControlMuted,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .background(if (selected) accent.copy(.2f) else Color.Transparent, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) accent.copy(.65f) else ControlMuted.copy(.2f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
    )
}

@Composable
fun GeometryManipulationBar(
    current: Transform2DMode,
    selectedAvailable: Boolean = true,
    onSelect: (Transform2DMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.alpha(if (selectedAvailable) .96f else .22f)
            .background(ControlSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ControlCyan.copy(.42f), RoundedCornerShape(16.dp)).padding(5.dp),
    ) {
        Transform2DMode.entries.forEach { mode ->
            CanvasChip(
                mode.name,
                current == mode,
                ControlCyan,
                enabled = selectedAvailable || mode == Transform2DMode.Select,
            ) { onSelect(mode) }
        }
    }
}

@Composable
fun GeometryLayerPanel(
    shapes: List<Shape2D>,
    selected: Set<Int>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.widthIn(min = 130.dp, max = 220.dp)
            .background(ControlSurface, RoundedCornerShape(15.dp))
            .border(1.dp, ControlViolet.copy(.48f), RoundedCornerShape(15.dp))
            .animateContentSize().padding(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text("Objects ${shapes.size}", color = ControlInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (expanded) "▲" else "▼", color = ControlViolet, fontSize = 10.sp)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 4.dp)) {
                if (shapes.isEmpty()) Text("No objects", color = ControlMuted, fontSize = 10.sp)
                shapes.forEachIndexed { index, shape ->
                    Row(
                        Modifier.fillMaxWidth().background(
                            if (index in selected) ControlViolet.copy(.22f) else Color.Transparent,
                            RoundedCornerShape(9.dp),
                        ).clickable { onSelect(index) }.padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    ) {
                        Text(shape.name, color = if (index in selected) ControlInk else ControlMuted, fontSize = 10.sp, maxLines = 1)
                        Text(if (shape.locked) "🔒" else if (!shape.visible) "○" else "●", color = if (shape.visible) ControlCyan else ControlMuted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SpatialManipulationBar(
    current: SpatialEditMode,
    selectedAvailable: Boolean,
    onSelect: (SpatialEditMode) -> Unit,
    modifier: Modifier = Modifier,
    formulasVisible: Boolean = false,
    onFormulas: (() -> Unit)? = null,
) {
    Row(
        modifier.alpha(if (selectedAvailable) .96f else .22f).background(ControlSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ControlViolet.copy(.46f), RoundedCornerShape(16.dp)).padding(5.dp),
    ) {
        SpatialEditMode.entries.forEach { mode ->
            CanvasChip(mode.name, current == mode, ControlViolet, enabled = selectedAvailable || mode == SpatialEditMode.Select) { onSelect(mode) }
        }
        if (onFormulas != null) {
            CanvasChip("Formulas", formulasVisible, ControlGreen, enabled = selectedAvailable) { onFormulas() }
        }
    }
}

@Composable
fun SolidObjectCombo(
    solids: List<Solid>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = solids.getOrNull(selectedIndex)
    Column(
        modifier.fillMaxWidth().background(ControlSurface, RoundedCornerShape(12.dp))
            .border(1.dp, ControlCyan.copy(.38f), RoundedCornerShape(12.dp)).padding(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = solids.isNotEmpty()) { expanded = !expanded }.padding(3.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(selected?.let { "Object ${selectedIndex + 1}: ${it.type.name}" } ?: "Select an object", color = ControlInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(if (expanded) "▲" else "▼", color = ControlCyan, fontSize = 10.sp)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 4.dp)) {
                solids.forEachIndexed { index, solid ->
                    Text(
                        "${index + 1}. ${solid.type.name}",
                        color = if (index == selectedIndex) ControlCyan else ControlMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth().background(
                            if (index == selectedIndex) ControlCyan.copy(.12f) else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        ).clickable { onSelect(index); expanded = false }.padding(horizontal = 7.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedSolidDetails(solid: Solid, modifier: Modifier = Modifier) {
    val measurements = Geometry3D.measure(solid)
    fun number(value: Double) = String.format(java.util.Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    Column(
        modifier.fillMaxWidth().background(ControlSurface.copy(.92f), RoundedCornerShape(12.dp))
            .border(1.dp, ControlViolet.copy(.34f), RoundedCornerShape(12.dp)).padding(8.dp),
    ) {
        Text("${solid.type.name} formulas", color = ControlViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Geometry3D.formulas(solid.type).forEach { (name, formula) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(name, color = ControlMuted, fontSize = 9.sp)
                Text(formula, color = ControlInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("Calculated properties", color = ControlGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
        Text("Volume ${number(measurements.volume)} · Area ${number(measurements.surfaceArea)}", color = ControlInk, fontSize = 9.sp)
        Text("Width ${number(solid.width)} · Height ${number(solid.height)} · Depth ${number(solid.depth)} · Radius ${number(solid.radius)}", color = ControlMuted, fontSize = 9.sp)
        Text("Position (${number(solid.position.x)}, ${number(solid.position.y)}, ${number(solid.position.z)})", color = ControlMuted, fontSize = 9.sp)
        Text("Rotation (${number(solid.rotation.x)}°, ${number(solid.rotation.y)}°, ${number(solid.rotation.z)}°)", color = ControlMuted, fontSize = 9.sp)
        Text("${measurements.faces} faces · ${measurements.edges} edges · ${measurements.vertices} vertices", color = ControlMuted, fontSize = 9.sp)
    }
}

/**
 * A selected object never opens a blocking property sheet by itself. The compact
 * strip stays unobtrusive until the user explicitly asks for controls.
 */
@Composable
fun SmartSelectionHud(
    title: String,
    instruction: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    selectionKey: Any? = title,
    actions: @Composable () -> Unit,
) {
    var expanded by remember(selectionKey) { mutableStateOf(initiallyExpanded) }
    Column(
        modifier
            .widthIn(max = 560.dp)
            .alpha(if (expanded) .98f else .72f)
            .background(
                Brush.linearGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))),
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, Cyan.copy(alpha = if (expanded) .55f else .30f), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics {
                contentDescription =
                    if (expanded) "Selected object controls for $title" else "Open selected object controls for $title"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Text(
                if (expanded) "Controls open" else "Controls",
                color = Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Green.copy(.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, Green.copy(.42f), RoundedCornerShape(10.dp))
                    .clickable(enabled = !expanded) { expanded = true }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
        AnimatedVisibility(expanded) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(instruction, color = Muted, fontSize = 10.sp, maxLines = 2)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) { actions() }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Cyan.copy(.08f), RoundedCornerShape(10.dp))
                        .clickable { expanded = false }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Collapse controls ▲", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SpatialLayerPanel(
    solids: List<Solid>,
    vectors: List<Vector3D>,
    selectedSolids: Set<Int>,
    selectedVector: Int,
    locked: Set<Int>,
    hidden: Set<Int>,
    groups: List<Set<Int>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectSolid: (Int) -> Unit,
    onSelectVector: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.widthIn(min = 145.dp, max = 230.dp)
            .background(ControlSurface, RoundedCornerShape(15.dp))
            .border(1.dp, ControlCyan.copy(.45f), RoundedCornerShape(15.dp))
            .animateContentSize().padding(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { onExpandedChange(!expanded) },
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text("Scene ${solids.size + vectors.size}", color = ControlInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (expanded) "▲" else "▼", color = ControlCyan, fontSize = 10.sp)
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 4.dp)) {
                if (solids.isEmpty() && vectors.isEmpty()) Text("No scene objects", color = ControlMuted, fontSize = 10.sp)
                groups.forEachIndexed { index, group ->
                    Text("▾ Group ${index + 1} · ${group.size}", color = ControlViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                solids.forEachIndexed { index, solid ->
                    val displayName = solid.type.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                    Row(
                        Modifier.fillMaxWidth().background(
                            if (index in selectedSolids) ControlCyan.copy(.18f) else Color.Transparent,
                            RoundedCornerShape(9.dp),
                        ).clickable { onSelectSolid(index) }.padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    ) {
                        Text("├ $displayName", color = if (index in selectedSolids) ControlInk else ControlMuted, fontSize = 10.sp, maxLines = 1)
                        Text(if (index in locked) "🔒" else if (index in hidden) "○" else "●", color = ControlCyan, fontSize = 9.sp)
                    }
                }
                vectors.forEachIndexed { index, vector ->
                    Text(
                        "└ ${vector.name}",
                        color = if (index == selectedVector) ControlGreen else ControlMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth().background(Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { onSelectVector(index) }.padding(horizontal = 6.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}
