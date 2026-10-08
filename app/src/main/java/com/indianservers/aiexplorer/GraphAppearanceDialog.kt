package com.indianservers.aiexplorer

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.indianservers.aiexplorer.core.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GraphAppearanceDialog(function: FunctionDefinition, onClose: () -> Unit, onChange: (FunctionDefinition) -> Unit) {
    val context = LocalContext.current
    var custom by remember(function.id) { mutableStateOf(function.colorKey.takeIf { it.startsWith("#") } ?: "#30D9FF") }
    var imageError by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val readable = runCatching { context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.Options().let { options -> options.inJustDecodeBounds = true; android.graphics.BitmapFactory.decodeStream(it, null, options); options.outWidth > 0 && options.outHeight > 0 } } == true }.getOrDefault(false)
            if (readable) onChange(function.copy(appearance = function.appearance.copy(fill = GraphFill.Image, imageUri = uri.toString())))
            else imageError = "Could not open this image. Choose another image."
        }
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.widthIn(max = 360.dp).heightIn(max = 460.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Graph appearance", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onClose) { Text("Done") }
                }
                Text("Color")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    graphColorKeys.forEach { key ->
                        FilterChip(selected = function.colorKey == key, onClick = { onChange(function.copy(colorKey = key)) }, label = { Text(key) }, leadingIcon = { Box(Modifier.size(12.dp).background(graphColor(key))) })
                    }
                }
                Row {
                    OutlinedTextField(custom, { custom = it.take(7) }, Modifier.weight(1f), label = { Text("Hex color") }, singleLine = true)
                    TextButton(enabled = custom.matches(Regex("#[0-9A-Fa-f]{6}")), onClick = { onChange(function.copy(colorKey = custom.uppercase())) }) { Text("Apply") }
                }
                Text("Fill")
                Text("Fills shade the area to the x-axis for functions, or the solution region for inequalities.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphFill.entries.forEach { fill -> FilterChip(selected = function.appearance.fill == fill, onClick = { onChange(function.copy(appearance = function.appearance.copy(fill = fill))) }, label = { Text(fill.name) }) }
                }
                if (function.appearance.fill == GraphFill.Gradient) {
                    Text("Gradient end color")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        graphColorKeys.forEach { key -> FilterChip(selected = function.appearance.secondaryColor == key, onClick = { onChange(function.copy(appearance = function.appearance.copy(secondaryColor = key))) }, label = { Text(key) }) }
                    }
                }
                if (function.appearance.fill == GraphFill.Image) {
                    TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) { Text(if (function.appearance.imageUri.isEmpty()) "Choose image" else "Replace image") }
                    if (imageError.isNotEmpty()) Text(imageError)
                }
                Text("Line style")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraphLineStyle.entries.forEach { line -> FilterChip(selected = function.appearance.line == line, onClick = { onChange(function.copy(appearance = function.appearance.copy(line = line))) }, label = { Text(line.name) }) }
                }
                Text("Thickness: ${function.appearance.width.toInt()}")
                Slider(function.appearance.width, { onChange(function.copy(appearance = function.appearance.copy(width = it))) }, valueRange = 1f..12f)
                Text("Opacity: ${(function.appearance.opacity * 100).toInt()}%")
                Slider(function.appearance.opacity, { onChange(function.copy(appearance = function.appearance.copy(opacity = it))) }, valueRange = .1f..1f)
                TextButton(onClick = { onChange(function.copy(colorKey = "cyan", appearance = GraphAppearance())) }) { Text("Reset appearance") }
            }
        }
    }
}
