package com.indianservers.aiexplorer.input

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun HandwritingMathInput(onInsert: (String) -> Unit, modifier: Modifier = Modifier) {
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    val recognition = remember(strokes) { LocalHandwritingMathRecognizer.recognize(strokes.map { stroke -> stroke.map { InkPoint(it.x, it.y) } }) }
    LaunchedEffect(recognition.primary) { if (selected.isBlank() && recognition.primary.isNotBlank()) selected = recognition.primary }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Handwriting input · draw one symbol, verify it, then insert")
        Canvas(
            Modifier.fillMaxWidth().height(180.dp).background(Color(0xFF07101A), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF20D9FF), RoundedCornerShape(12.dp))
                .semantics { contentDescription = "Handwriting maths canvas" }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { point -> current = listOf(point) },
                        onDrag = { change, _ -> current = current + change.position },
                        onDragEnd = { if (current.size > 1) strokes = strokes + listOf(current); current = emptyList(); selected = "" },
                    )
                },
        ) {
            (strokes + listOf(current)).forEach { stroke -> stroke.zipWithNext().forEach { (a,b) -> drawLine(Color(0xFFEAF5FF),a,b,8f,StrokeCap.Round) } }
        }
        Text(if (recognition.primary.isBlank()) recognition.explanation else "Recognized: ${recognition.primary} · ${(recognition.confidence*100).toInt()}% · ${recognition.explanation}")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            (listOf(recognition.primary) + recognition.alternatives + listOf("0","1","2","3","4","5","6","7","8","9","x","y","+","-","=","/","^","(",")")).filter { it.isNotBlank() }.distinct().forEach { token ->
                OutlinedButton({ selected = token }) { Text(if (selected == token) "• $token" else token) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button({ if (selected.isNotBlank()) { onInsert(selected); strokes = emptyList(); current = emptyList(); selected = "" } }, enabled = selected.isNotBlank()) { Text("Insert symbol") }
            OutlinedButton({ strokes = emptyList(); current = emptyList(); selected = "" }) { Text("Clear ink") }
            OutlinedButton({ strokes = strokes.dropLast(1); selected = "" }, enabled = strokes.isNotEmpty()) { Text("Undo stroke") }
        }
    }
}
