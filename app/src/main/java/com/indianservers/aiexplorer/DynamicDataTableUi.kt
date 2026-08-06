package com.indianservers.aiexplorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.ProfessionalGraphTable
import com.indianservers.aiexplorer.core.trim
import com.indianservers.aiexplorer.workspace.MathGeneratedTableRow

@Composable
internal fun FunctionDataTablePanel(
    rows: List<MathGeneratedTableRow>,
    start: String,
    end: String,
    step: String,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    onStep: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Function table", color = Green, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            CompactNumberField("Start", start, onStart, Modifier.weight(1f))
            CompactNumberField("End", end, onEnd, Modifier.weight(1f))
            CompactNumberField("Step", step, onStep, Modifier.weight(1f))
        }
        if (rows.isEmpty() || rows.all { it.outputs.isEmpty() }) {
            Text("Use a valid range and at least one function.", color = Amber, fontSize = 11.sp)
        } else {
            GeneratedDataGrid(
                headers = listOf("x") + rows.flatMap { it.outputs.keys }.distinct(),
                rows = rows.map { row -> listOf(trim(row.input)) + row.outputs.values.map(::trim) },
                totalRows = rows.size,
            )
            Text("Values update with equations and parameter sliders.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
internal fun CsvDataImportPanel(
    source: String,
    hasHeader: Boolean,
    table: ProfessionalGraphTable?,
    selectedX: String?,
    selectedY: String?,
    message: String,
    onSource: (String) -> Unit,
    onToggleHeader: () -> Unit,
    onChooseFile: () -> Unit,
    onParse: () -> Unit,
    onSelectX: (String) -> Unit,
    onSelectY: (String) -> Unit,
    onPlot: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0x2200CFE8), RoundedCornerShape(8.dp))
            .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("CSV data", color = Cyan, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Choose CSV", icon = "file", onClick = onChooseFile)
            GlowButton(if (hasHeader) "Headers: ON" else "Headers: OFF", onClick = onToggleHeader)
            GlowButton("Read data", onClick = onParse)
        }
        OutlinedTextField(
            value = source,
            onValueChange = onSource,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Paste CSV or TSV") },
            minLines = 3,
            maxLines = 7,
        )
        Text(message, color = if (table == null) Muted else Green, fontSize = 10.sp)
        table?.let { parsed ->
            val numeric = parsed.columns.filter { column -> column.values.any { it != null } }
            GeneratedDataGrid(
                headers = parsed.columns.map { it.name },
                rows = (0 until minOf(parsed.rowCount, 8)).map { row ->
                    parsed.columns.map { column -> column.values[row]?.let(::trim) ?: "—" }
                },
                totalRows = parsed.rowCount,
            )
            Text("X column", color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                numeric.forEach { column ->
                    GlowButton(if (column.name == selectedX) "• ${column.name}" else column.name) { onSelectX(column.name) }
                }
            }
            Text("Y column", color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                numeric.forEach { column ->
                    GlowButton(if (column.name == selectedY) "• ${column.name}" else column.name) { onSelectY(column.name) }
                }
            }
            GlowButton(
                "Plot ${selectedX ?: "x"} vs ${selectedY ?: "y"}",
                icon = "plot",
                enabled = selectedX != null && selectedY != null && selectedX != selectedY,
                onClick = onPlot,
            )
        }
    }
}

@Composable
private fun CompactNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= 12 && candidate.matches(Regex("-?\\d*(?:\\.\\d*)?"))) onValueChange(candidate)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun GeneratedDataGrid(headers: List<String>, rows: List<List<String>>, totalRows: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color(0xAA07111C), RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        DataGridRow(headers, header = true)
        rows.take(8).forEach { DataGridRow(it, header = false) }
        if (totalRows > 8) Text("+${totalRows - 8} more rows", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun DataGridRow(values: List<String>, header: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { value ->
            Text(
                text = value,
                color = if (header) Cyan else Ink,
                fontSize = 10.sp,
                fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(92.dp)
                    .background(if (header) Cyan.copy(alpha = .1f) else Color.Transparent, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 4.dp),
            )
        }
    }
}
