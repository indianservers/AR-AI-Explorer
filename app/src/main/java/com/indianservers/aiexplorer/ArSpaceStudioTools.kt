package com.indianservers.aiexplorer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.indianservers.aiexplorer.core.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArSpaceCoordinateTools(vm: ExplorerViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableIntStateOf(vm.selectedPoint) }
    var first by rememberSaveable { mutableIntStateOf(0) }
    var second by rememberSaveable { mutableIntStateOf(1) }
    val point = parseArSpaceCoordinates(input, 2)?.let { Vec2(it[0], it[1]) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Point x, y") }, placeholder = { Text("Enter coordinates") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Plot point", enabled = point != null) { point?.let { vm.addPoint(it); selected = vm.state.points.lastIndex } }
            GlowButton("Move selected", enabled = point != null && selected in vm.state.points.indices) { point?.let { vm.movePoint(selected, it) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.state.points.forEachIndexed { index, p ->
                GlowButton("${if (selected == index) "• " else ""}P${index + 1} (${trim(p.x)}, ${trim(p.y)})") { selected = index; input = "${trim(p.x)}, ${trim(p.y)}"; vm.selectCoordinatePoint(index) }
            }
        }
        if (vm.state.points.size >= 2) {
            Text("Measure / construct using two points")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.state.points.indices.forEach { index ->
                    GlowButton("${if (first == index) "• " else ""}A=P${index + 1}") { first = index }
                    GlowButton("${if (second == index) "• " else ""}B=P${index + 1}") { second = index }
                }
            }
            val a = vm.state.points.getOrNull(first)
            val b = vm.state.points.getOrNull(second)
            if (a != null && b != null && first != second) {
                val measure = Geometry2D.segment(a, b)
                Text("Distance ${trim(measure.distance)} · slope ${measure.slope?.let(::trim) ?: "undefined"}")
                Text("Midpoint (${trim(measure.midpoint.x)}, ${trim(measure.midpoint.y)})")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Draw line") { vm.applyContextualGeometryTool("Line", listOf(first, second)) }
                    GlowButton("Midpoint") { vm.applyContextualGeometryTool("Midpoint", listOf(first, second)) }
                }
            }
        }
        Text(vm.status)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArSpaceVectorTools(vm: ExplorerViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableIntStateOf(vm.selectedVector3D) }
    var first by rememberSaveable { mutableIntStateOf(0) }
    var second by rememberSaveable { mutableIntStateOf(1) }
    val components = parseArSpaceCoordinates(input, 3)?.let { Vec3(it[0], it[1], it[2]) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("Vector x, y, z") }, placeholder = { Text("Enter components") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Add vector", enabled = components != null) { components?.let { vm.addVector3D(start = Vec3(0.0, 0.0, 0.0), end = it); selected = vm.state.vectors3D.lastIndex } }
            GlowButton("Update selected", enabled = components != null && selected in vm.state.vectors3D.indices) { components?.let { value -> vm.transformVector3D(selected) { it.copy(end = it.start + value) } } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.state.vectors3D.forEachIndexed { index, vector ->
                GlowButton("${if (selected == index) "• " else ""}${vector.name} |v|=${trim(vector.magnitude)}") {
                    selected = index; vm.selectVector3D(index)
                    val c = vector.components; input = "${trim(c.x)}, ${trim(c.y)}, ${trim(c.z)}"
                }
            }
        }
        if (vm.state.vectors3D.size >= 2) {
            Text("Select vectors A and B")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.state.vectors3D.forEachIndexed { index, vector ->
                    GlowButton("${if (first == index) "• " else ""}A=${vector.name}") { first = index }
                    GlowButton("${if (second == index) "• " else ""}B=${vector.name}") { second = index }
                }
            }
            val a = vm.state.vectors3D.getOrNull(first)?.components
            val b = vm.state.vectors3D.getOrNull(second)?.components
            if (a != null && b != null && first != second) {
                val cross = arSpaceCrossProduct(a, b)
                Text("A · B = ${trim(a.dot(b))}")
                Text("A × B = (${trim(cross.x)}, ${trim(cross.y)}, ${trim(cross.z)})")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Show A+B") { vm.addVector3D(namePrefix = "sum", start = Vec3(0.0, 0.0, 0.0), end = a + b) }
                    GlowButton("Show A−B") { vm.addVector3D(namePrefix = "difference", start = Vec3(0.0, 0.0, 0.0), end = a - b) }
                    GlowButton("Show A×B") { vm.addVector3D(namePrefix = "cross", start = Vec3(0.0, 0.0, 0.0), end = cross) }
                }
            }
        }
        Text(vm.status)
    }
}

internal fun parseArSpaceCoordinates(source: String, dimensions: Int): List<Double>? {
    val values = source.split(',').map { it.trim().toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null }
    return values.takeIf { it.size == dimensions }
}
internal fun arSpaceCrossProduct(a: Vec3, b: Vec3) = Vec3(a.y*b.z-a.z*b.y, a.z*b.x-a.x*b.z, a.x*b.y-a.y*b.x)

@Composable
internal fun ArSpaceCasTools(vm: ExplorerViewModel) {
    var input by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(input, { input = it }, Modifier.fillMaxWidth(), label = { Text("CAS / algebra expression") }, placeholder = { Text("f(x) := x^2") })
        GlowButton("Evaluate", enabled = input.isNotBlank()) { vm.submitNotebook(input) }
        Text(vm.status)
        vm.notebookDocument.cells.takeLast(6).forEach { cell ->
            Text(cell.input)
            GlowButton("Remove") { vm.removeNotebookCell(cell.id) }
        }
        Text("Graphable functions, points and vectors appear in the AR scene.")
    }
}
