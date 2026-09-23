package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType

private val PropertyInk = Color(0xFFF1EEFF)
private val PropertyMuted = Color(0xFFACB6CD)

@Composable
internal fun GeometryPropertiesPanel(
    solid: Solid?, modifier: Modifier, onClose: () -> Unit, onChange: (Solid) -> Unit,
    material: @Composable () -> Unit, visibility: @Composable () -> Unit,
    advanced: @Composable ColumnScope.() -> Unit,
) {
    GlassPanel(modifier, maxPanelHeight = 760.dp) {
        PanelHeader("Properties", onClose, PropertyInk, icon = "settings")
        if (solid != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GeometryShapeArtwork(solid, Color(0xFFFFD83D), Modifier.size(38.dp))
                Text(solid.type.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " "), color = PropertyInk, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            GeometryPropertyGroup("Dimensions", initiallyExpanded = true) {
                val measure = Geometry3D.measure(solid)
                if (solid.type == SolidType.Cube) {
                    GeometryNumberRow("Side length (a)", solid.width) { value -> onChange(solid.copy(width = value.coerceAtLeast(.1), height = value.coerceAtLeast(.1), depth = value.coerceAtLeast(.1))) }
                } else {
                    GeometryNumberRow("Width", solid.width) { onChange(solid.copy(width = it.coerceAtLeast(.1))) }
                    GeometryNumberRow("Height", solid.height) { onChange(solid.copy(height = it.coerceAtLeast(.1))) }
                    GeometryNumberRow("Depth", solid.depth) { onChange(solid.copy(depth = it.coerceAtLeast(.1))) }
                    GeometryNumberRow("Radius", solid.radius) { onChange(solid.copy(radius = it.coerceAtLeast(.1))) }
                    if (solid.type == SolidType.Frustum) GeometryNumberRow("Top radius", solid.topRadius) { onChange(solid.copy(topRadius = it.coerceAtLeast(.05))) }
                }
                GeometryPropertyRow("Volume (V)", String.format(java.util.Locale.US, "%.3f", measure.volume))
                GeometryPropertyRow("Surface area (A)", String.format(java.util.Locale.US, "%.3f", measure.surfaceArea))
            }
            GeometryPropertyGroup("Position (Center)", initiallyExpanded = true) {
                GeometryNumberRow("●  X", solid.position.x, Color(0xFFFF4A68)) { onChange(solid.copy(position = solid.position.copy(x = it))) }
                GeometryNumberRow("●  Y", solid.position.y, Color(0xFF27B5FF)) { onChange(solid.copy(position = solid.position.copy(y = it))) }
                GeometryNumberRow("●  Z", solid.position.z, Color(0xFF3DE681)) { onChange(solid.copy(position = solid.position.copy(z = it))) }
            }
            GeometryPropertyGroup("✥   Transform") {
                GeometryNumberRow("Rotate X", solid.rotation.x, step = 15.0) { onChange(solid.copy(rotation = solid.rotation.copy(x = it))) }
                GeometryNumberRow("Rotate Y", solid.rotation.y, step = 15.0) { onChange(solid.copy(rotation = solid.rotation.copy(y = it))) }
                GeometryNumberRow("Rotate Z", solid.rotation.z, step = 15.0) { onChange(solid.copy(rotation = solid.rotation.copy(z = it))) }
            }
            GeometryPropertyGroup("●   Color & Material") { material() }
            GeometryPropertyGroup("◉   Visibility & Style") { visibility() }
        } else Text("Select an object to edit its properties.", color = PropertyMuted)
        GeometryPropertyGroup("☷   Advanced", content = advanced)
    }
}

@Composable
internal fun GeometryPropertyGroup(title: String, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF111B2B), Color(0xFF08111F))))
        .border(1.dp, Color(0xFF293345), RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = PropertyInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
            Text(if (expanded) "⌃" else "⌄", color = PropertyMuted)
        }
        if (expanded) content()
    }
}

@Composable
internal fun GeometryPropertyRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = PropertyMuted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = PropertyInk, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun GeometryNumberRow(label: String, value: Double, accent: Color = PropertyMuted, step: Double = .1, onChange: (Double) -> Unit) {
    var draft by remember(value) { mutableStateOf(String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = accent, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
        androidx.compose.foundation.text.BasicTextField(
            value = draft, onValueChange = { text -> draft = text; text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let(onChange) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = PropertyInk, fontSize = 13.sp, lineHeight = 17.sp),
            modifier = Modifier.width(62.dp).background(Color(0xFF050D19), RoundedCornerShape(7.dp)).border(1.dp, Color(0xFF283345), RoundedCornerShape(7.dp)).padding(8.dp),
        )
        listOf("−" to -step, "+" to step).forEach { (text, delta) ->
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1B273B)).clickable { onChange(value + delta) }, contentAlignment = Alignment.Center) {
                Text(text, color = PropertyInk, fontSize = 21.sp)
            }
        }
    }
}
