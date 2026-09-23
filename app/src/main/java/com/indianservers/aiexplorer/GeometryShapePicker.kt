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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.SolidType

private val PickerInk = Color(0xFFEFEDFF)
private val PickerMuted = Color(0xFFA9B5D0)
private val PickerLine = Color(0xFF29364D)

@Composable
internal fun GeometryShapePicker(
    modifier: Modifier,
    onDismiss: () -> Unit,
    onAdd: (SolidType) -> Unit,
    tools: List<Pair<String, () -> Unit>>,
    thumbnail: @Composable (SolidType, Modifier) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(setOf("Basic Shapes")) }
    val basic = listOf(SolidType.Cube, SolidType.Cuboid, SolidType.Sphere, SolidType.Hemisphere, SolidType.Cylinder, SolidType.Cone, SolidType.Torus, SolidType.Pyramid)
    val prisms = listOf(SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.OctagonalPrism)
    val polyhedra = listOf(SolidType.Tetrahedron, SolidType.TriangularPyramid, SolidType.Octahedron, SolidType.Wedge)
    val groups = listOf("Basic Shapes" to basic, "Prisms" to prisms, "Polyhedra" to polyhedra,
        "Curved Solids" to SolidType.entries.filter { it !in basic && it !in prisms && it !in polyhedra })
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.padding(8.dp).shadow(10.dp, shape, ambientColor = Color(0xFF6F4CFF), spotColor = Color(0xFF4885FF))
            .clip(shape).background(Brush.linearGradient(listOf(Color(0xFF101B31), Color(0xFF030D1B), Color(0xFF020916))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF96B8FF), Color(0xFF476CCB), Color(0xFF7553D9))), shape)
            .clickable(enabled = false) {}.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            thumbnail(SolidType.Cube, Modifier.size(34.dp))
            Text("Add to 3D Space", color = PickerInk, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp).background(Color(0xFF132039), RoundedCornerShape(10.dp))) {
                Text("×", color = PickerInk, fontSize = 25.sp, modifier = Modifier.semantics { contentDescription = "Close shape library" })
            }
        }
        OutlinedTextField(
            value = search, onValueChange = { search = it }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Search solids, tools, or elements…", fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PickerInk, unfocusedTextColor = PickerInk,
                focusedBorderColor = Color(0xFF779AFF), unfocusedBorderColor = PickerLine,
                focusedContainerColor = Color(0xFF061120), unfocusedContainerColor = Color(0xFF061120),
                unfocusedPlaceholderColor = PickerMuted, focusedPlaceholderColor = PickerMuted),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            var matches = 0
            groups.forEachIndexed { index, (label, types) ->
                val filtered = types.filter { search.isBlank() || it.name.contains(search.trim().replace(" ", ""), true) || label.contains(search.trim(), true) }
                if (filtered.isNotEmpty()) {
                    matches += filtered.size
                    val open = label in expanded || search.isNotBlank()
                    PickerCategory(label, open, onClick = { expanded = if (label in expanded) expanded - label else expanded + label }) {
                        thumbnail(listOf(SolidType.Cuboid, SolidType.TriangularPrism, SolidType.Octahedron, SolidType.Sphere)[index], Modifier.size(28.dp))
                    }
                    if (open) filtered.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { type ->
                                val name = type.name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
                                Column(
                                    Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(11.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF0E1D33), Color(0xFF061221))))
                                        .border(1.dp, PickerLine, RoundedCornerShape(11.dp))
                                        .clickable { onAdd(type) }.semantics { contentDescription = "Add $name" }
                                        .padding(horizontal = 3.dp, vertical = 7.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    thumbnail(type, Modifier.fillMaxWidth().height(56.dp))
                                    Text(name, color = PickerInk, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            val filteredTools = tools.filter { search.isBlank() || it.first.contains(search.trim(), true) }
            if (filteredTools.isNotEmpty()) {
                val open = "Tools & Elements" in expanded || search.isNotBlank()
                PickerCategory("Tools & Elements", open, onClick = { expanded = if (open) expanded - "Tools & Elements" else expanded + "Tools & Elements" }) {
                    Text("↗", color = Color(0xFF43C8FF), fontSize = 26.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                }
                if (open) filteredTools.forEach { (name, action) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF0B1728)).clickable(onClick = action).padding(14.dp)) {
                        Text(name, color = PickerInk, modifier = Modifier.weight(1f))
                        Text("+", color = Color(0xFF5EDEBB))
                    }
                }
            } else if (matches == 0) Text("No matching shapes or tools", color = PickerMuted, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun PickerCategory(label: String, expanded: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xFF121B30), Color(0xFF081322))))
        .border(1.dp, PickerLine, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        icon()
        Text(label, color = PickerInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(if (expanded) "⌃" else "⌄", color = PickerMuted, fontSize = 19.sp)
    }
}
