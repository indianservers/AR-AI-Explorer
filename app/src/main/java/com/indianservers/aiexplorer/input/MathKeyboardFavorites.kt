package com.indianservers.aiexplorer.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun MathFavoritesDialog(ids: List<String>, onChange: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var search by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Favorite math keys (${ids.size}/8)") },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ids.forEachIndexed { index, id ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(mathFavoriteCatalog.getValue(id).label, Modifier.weight(1f))
                        TextButton(enabled = index > 0, modifier = Modifier.testTag("math.favorite.up.$id"), onClick = {
                            onChange(ids.toMutableList().apply { add(index - 1, removeAt(index)) })
                        }) { Text("↑") }
                        TextButton(enabled = index < ids.lastIndex, modifier = Modifier.testTag("math.favorite.down.$id"), onClick = {
                            onChange(ids.toMutableList().apply { add(index + 1, removeAt(index)) })
                        }) { Text("↓") }
                        TextButton(modifier = Modifier.testTag("math.favorite.remove.$id"), onClick = { onChange(ids - id) }) { Text("Remove") }
                    }
                }
                OutlinedTextField(search, { search = it }, label = { Text("Find a math key") }, singleLine = true, modifier = Modifier.testTag("math.favorite.search"))
                mathFavoriteCatalog.entries.filter { (id, key) -> id !in ids && (search.isBlank() || key.description.contains(search, true) || key.label.contains(search, true) || key.insertion.contains(search, true)) }
                    .take(40).forEach { (id, key) ->
                        TextButton(enabled = ids.size < 8, modifier = Modifier.testTag("math.favorite.add.$id"), onClick = { onChange(ids + id) }) { Text("Add ${key.label} — ${key.description}") }
                    }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
