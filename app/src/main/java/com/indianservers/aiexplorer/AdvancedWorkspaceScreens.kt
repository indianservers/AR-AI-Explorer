package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.MathSpreadsheet
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.SpreadsheetAddress
import java.math.BigInteger
import java.util.Locale
import kotlin.math.abs

private val LabCyan = Color(0xFF31D7FF)
private val LabViolet = Color(0xFFA878FF)
private val LabGreen = Color(0xFF55E6A5)
private val LabAmber = Color(0xFFFFC857)

@Composable
internal fun MatricesLinearTransformationsWorkspace(vm: ExplorerViewModel) {
    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("0") }
    var c by remember { mutableStateOf("0") }
    var d by remember { mutableStateOf("1") }
    var x by remember { mutableStateOf("1") }
    var y by remember { mutableStateOf("1") }
    val av = a.toDoubleOrNull() ?: 0.0
    val bv = b.toDoubleOrNull() ?: 0.0
    val cv = c.toDoubleOrNull() ?: 0.0
    val dv = d.toDoubleOrNull() ?: 0.0
    val xv = x.toDoubleOrNull() ?: 0.0
    val yv = y.toDoubleOrNull() ?: 0.0
    val tx = av * xv + bv * yv
    val ty = cv * xv + dv * yv
    val determinant = av * dv - bv * cv

    WorkspacePage("Matrices & Linear Transformations", "Transform vectors and connect matrix arithmetic to geometry.", LabViolet) {
        Text("TRANSFORMATION MATRIX", color = LabViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactNumberField(a, { a = it }, "a", Modifier.weight(1f)); CompactNumberField(b, { b = it }, "b", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactNumberField(c, { c = it }, "c", Modifier.weight(1f)); CompactNumberField(d, { d = it }, "d", Modifier.weight(1f))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactNumberField(x, { x = it }, "vector x", Modifier.fillMaxWidth())
                CompactNumberField(y, { y = it }, "vector y", Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("det(A)", formatLab(determinant), LabAmber, Modifier.weight(1f))
            MetricCard("A · v", "(${formatLab(tx)}, ${formatLab(ty)})", LabGreen, Modifier.weight(1f))
        }
        MatrixVectorCanvas(xv.toFloat(), yv.toFloat(), tx.toFloat(), ty.toFloat())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { a = "0"; b = "-1"; c = "1"; d = "0"; vm.reportStatus("90 degree rotation matrix loaded") }) { Text("Rotate 90°") }
            Button(onClick = { a = "2"; b = "0"; c = "0"; d = "0.5"; vm.reportStatus("Non-uniform scale matrix loaded") }) { Text("Scale") }
            Button(onClick = { a = "1"; b = "1"; c = "0"; d = "1"; vm.reportStatus("Horizontal shear matrix loaded") }) { Text("Shear") }
        }
    }
}

@Composable
internal fun DataSpreadsheetWorkspace(vm: ExplorerViewModel) {
    val engine = remember { MathSpreadsheetEngine() }
    val inputs = remember {
        mutableStateMapOf(
            SpreadsheetAddress(0, 0) to "1", SpreadsheetAddress(1, 0) to "2", SpreadsheetAddress(2, 0) to "=A1+B1",
            SpreadsheetAddress(0, 1) to "2", SpreadsheetAddress(1, 1) to "4", SpreadsheetAddress(2, 1) to "=A2+B2",
            SpreadsheetAddress(0, 2) to "3", SpreadsheetAddress(1, 2) to "6", SpreadsheetAddress(2, 2) to "=SUM(A1:B3)",
        )
    }
    val document = MathSpreadsheet().let { base ->
        base.copy(
            columns = listOf("A", "B", "C", "D").map { com.indianservers.aiexplorer.core.SpreadsheetColumn(it) },
            cells = inputs.toMap(),
            revision = inputs.hashCode(),
        )
    }
    val snapshot = remember(document) { engine.evaluate(document) }

    WorkspacePage("Data Table & Spreadsheet", "Use values, cell references and live aggregate formulas.", LabCyan) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("", "A", "B", "C", "D").forEachIndexed { index, label ->
                Text(label, color = LabCyan, fontWeight = FontWeight.Bold, modifier = Modifier.weight(if (index == 0) .35f else 1f))
            }
        }
        repeat(6) { row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${row + 1}", color = LabCyan, modifier = Modifier.weight(.35f))
                repeat(4) { column ->
                    val address = SpreadsheetAddress(column, row)
                    OutlinedTextField(
                        value = inputs[address].orEmpty(),
                        onValueChange = { inputs[address] = it },
                        label = { Text(address.a1, fontSize = 9.sp) },
                        supportingText = snapshot.evaluated[address]?.let { cell ->
                            { Text(cell.error ?: cell.value?.let(::formatLab).orEmpty(), fontSize = 8.sp, color = if (cell.error == null) LabGreen else MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text("Try: =A1+B1, =SUM(A1:B3), =MEAN(A1:A3), =MIN(A1:A3), =MAX(A1:A3)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Button(onClick = { inputs.clear(); vm.reportStatus("Spreadsheet cleared") }) { Text("Clear table") }
    }
}

@Composable
internal fun NumberTheoryWorkspace(vm: ExplorerViewModel) {
    var first by remember { mutableStateOf("84") }
    var second by remember { mutableStateOf("30") }
    var modulus by remember { mutableStateOf("7") }
    val a = first.toBigIntegerOrNull()
    val b = second.toBigIntegerOrNull()
    val m = modulus.toBigIntegerOrNull()?.takeIf { it > BigInteger.ZERO }
    val gcd = if (a != null && b != null) a.abs().gcd(b.abs()) else null
    val lcm = if (a != null && b != null && gcd != null && gcd != BigInteger.ZERO) (a / gcd * b).abs() else BigInteger.ZERO

    WorkspacePage("Number Theory Lab", "Investigate factors, divisibility, greatest common divisors and residues.", LabAmber) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactTextField(first, { first = it }, "First integer", Modifier.weight(1f))
            CompactTextField(second, { second = it }, "Second integer", Modifier.weight(1f))
            CompactTextField(modulus, { modulus = it }, "Modulus", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("gcd", gcd?.toString() ?: "—", LabGreen, Modifier.weight(1f))
            MetricCard("lcm", lcm.toString(), LabViolet, Modifier.weight(1f))
            MetricCard("a mod m", if (a != null && m != null) a.mod(m).toString() else "—", LabCyan, Modifier.weight(1f))
        }
        Text("PRIME FACTORISATION", color = LabAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(a?.let(::primeFactorText) ?: "Enter an integer", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
        Text("EUCLIDEAN ALGORITHM", color = LabAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        EuclideanSteps(a, b).forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
        Button(onClick = { first = "97"; second = "35"; modulus = "12"; vm.reportStatus("Number theory example loaded") }) { Text("Load example") }
    }
}

@Composable
private fun WorkspacePage(title: String, subtitle: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, color = accent, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) =
    CompactTextField(value, onValueChange, label, modifier)

@Composable
private fun CompactTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, onValueChange, modifier, label = { Text(label) }, singleLine = true)
}

@Composable
private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier) {
    Column(modifier.border(1.dp, accent.copy(alpha = .55f), RoundedCornerShape(14.dp)).background(accent.copy(alpha = .08f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MatrixVectorCanvas(x: Float, y: Float, tx: Float, ty: Float) {
    Canvas(Modifier.fillMaxWidth().height(260.dp).border(1.dp, LabViolet.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val scale = (size.minDimension / 10f).coerceAtLeast(1f)
        drawLine(Color.Gray.copy(.45f), Offset(0f, centre.y), Offset(size.width, centre.y), 1.5f)
        drawLine(Color.Gray.copy(.45f), Offset(centre.x, 0f), Offset(centre.x, size.height), 1.5f)
        fun endpoint(vx: Float, vy: Float) = Offset(centre.x + vx * scale, centre.y - vy * scale)
        drawLine(LabCyan, centre, endpoint(x, y), 7f, StrokeCap.Round)
        drawLine(LabGreen, centre, endpoint(tx, ty), 7f, StrokeCap.Round)
        drawCircle(LabCyan, 7f, endpoint(x, y)); drawCircle(LabGreen, 7f, endpoint(tx, ty))
    }
}

private fun formatLab(value: Double): String = if (!value.isFinite()) "—" else if (abs(value - value.toLong()) < 1e-9) value.toLong().toString() else String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')

private fun primeFactorText(value: BigInteger): String {
    val two = BigInteger.valueOf(2)
    if (value.abs() < two) return value.toString()
    var remaining = value.abs()
    var divisor = two
    val factors = mutableListOf<BigInteger>()
    while (divisor * divisor <= remaining) {
        while (remaining.mod(divisor) == BigInteger.ZERO) { factors += divisor; remaining /= divisor }
        divisor += BigInteger.ONE
    }
    if (remaining > BigInteger.ONE) factors += remaining
    return (if (value.signum() < 0) listOf(BigInteger.valueOf(-1)) + factors else factors).joinToString(" × ")
}

private fun EuclideanSteps(first: BigInteger?, second: BigInteger?): List<String> {
    if (first == null || second == null) return listOf("Enter two integers")
    var a = first.abs(); var b = second.abs(); val steps = mutableListOf<String>()
    while (b != BigInteger.ZERO) { val quotient = a / b; val remainder = a.mod(b); steps += "$a = $b × $quotient + $remainder"; a = b; b = remainder }
    steps += "gcd = $a"
    return steps
}
