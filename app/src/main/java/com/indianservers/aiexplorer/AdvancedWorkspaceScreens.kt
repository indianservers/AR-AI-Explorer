package com.indianservers.aiexplorer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.MathSpreadsheet
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.ExactMatrix
import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.LinearSystemSolution
import com.indianservers.aiexplorer.core.MatrixLabEngine
import com.indianservers.aiexplorer.core.SpreadsheetAddress
import com.indianservers.aiexplorer.workspace.MathModule
import java.math.BigInteger
import java.util.Locale
import kotlin.math.abs

private val LabCyan get() = themedColor(Color(0xFF31D7FF), Cyan)
private val LabViolet get() = themedColor(Color(0xFFA878FF), Violet)
private val LabGreen get() = themedColor(Color(0xFF55E6A5), Green)
private val LabAmber get() = themedColor(Color(0xFFFFC857), Amber)

@Composable
internal fun MatricesLinearTransformationsWorkspace(vm: ExplorerViewModel) {
    var dimensionText by rememberLabText(vm, MathModule.MatricesLinearTransformations, "dimension", "2")
    var matrixAText by rememberLabText(vm, MathModule.MatricesLinearTransformations, "matrixA", "1,0;0,1")
    var matrixBText by rememberLabText(vm, MathModule.MatricesLinearTransformations, "matrixB", "1,0;0,1")
    var constantsText by rememberLabText(vm, MathModule.MatricesLinearTransformations, "constants", "1,1")
    val dimension = dimensionText.toIntOrNull()?.coerceIn(2, 3) ?: 2
    val matrixA = decodeMatrixCells(matrixAText, dimension)
    val matrixB = decodeMatrixCells(matrixBText, dimension)
    val constants = decodeVectorCells(constantsText, dimension)
    var operation by remember { mutableStateOf(MatrixLabOperation.Add) }
    var showDecimals by remember { mutableStateOf(false) }
    val parsedA = runCatching { ExactMatrix.parse(matrixA) }
    val parsedB = runCatching { ExactMatrix.parse(matrixB) }
    val result = runCatching {
        val a = parsedA.getOrThrow(); val b = parsedB.getOrThrow()
        when (operation) {
            MatrixLabOperation.Add -> MatrixLabEngine.add(a, b)
            MatrixLabOperation.Subtract -> MatrixLabEngine.subtract(a, b)
            MatrixLabOperation.Multiply -> MatrixLabEngine.multiply(a, b)
            MatrixLabOperation.Transpose -> MatrixLabEngine.transpose(a)
            MatrixLabOperation.Square -> MatrixLabEngine.power(a, 2)
            MatrixLabOperation.Rref -> MatrixLabEngine.rref(a).matrix
            MatrixLabOperation.Inverse -> MatrixLabEngine.inverse(a).matrix
        }
    }
    val determinant = parsedA.mapCatching(MatrixLabEngine::determinant)
    val trace = parsedA.mapCatching(MatrixLabEngine::trace)
    val reduction = parsedA.mapCatching(MatrixLabEngine::rref)
    val solve = runCatching { MatrixLabEngine.solve(parsedA.getOrThrow(), constants.map { ExactRational.parse(it.ifBlank { "0" }) }) }
    val eigenvalues = if (dimension == 2) parsedA.mapCatching(MatrixLabEngine::eigenvalues2x2) else null

    fun resize(size: Int) {
        dimensionText = size.toString()
        matrixAText = encodeMatrixCells(matrixCells(size, identity = true))
        matrixBText = encodeMatrixCells(matrixCells(size, identity = true))
        constantsText = List(size) { "1" }.joinToString(",")
    }
    fun preset(values: List<List<String>>, label: String) {
        matrixAText = encodeMatrixCells(values)
        vm.reportStatus("$label matrix loaded")
    }

    WorkspacePage("Matrix Lab", "Compute exactly, inspect row operations, solve systems, and see a 2D transformation geometrically.", LabViolet) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { resize(2) }, enabled = dimension != 2) { Text("2 × 2") }
            Button(onClick = { resize(3) }, enabled = dimension != 3) { Text("3 × 3") }
            Button(onClick = { showDecimals = !showDecimals }) { Text(if (showDecimals) "Show exact" else "Show decimals") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MatrixEditor("MATRIX A", matrixA, { row, column, value -> matrixAText = encodeMatrixCells(updateMatrixCell(matrixA, row, column, value)) }, Modifier.weight(1f))
            MatrixEditor("MATRIX B", matrixB, { row, column, value -> matrixBText = encodeMatrixCells(updateMatrixCell(matrixB, row, column, value)) }, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatrixLabOperation.entries.forEach { choice ->
                Button(onClick = { operation = choice }) { Text(choice.label) }
            }
        }

        Text("${operation.expression} =", color = LabViolet, fontWeight = FontWeight.Bold)
        Text(
            result.fold(onSuccess = { it.render(showDecimals) }, onFailure = { it.message ?: "Invalid matrix" }),
            color = if (result.isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 16.sp,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("det(A)", determinant.fold({ if (showDecimals) formatLab(it.toDouble()) else it.toString() }, { "—" }), LabAmber, Modifier.weight(1f))
            MetricCard("tr(A)", trace.fold({ if (showDecimals) formatLab(it.toDouble()) else it.toString() }, { "—" }), LabViolet, Modifier.weight(1f))
            MetricCard("rank(A)", reduction.fold({ it.rank.toString() }, { "—" }), LabGreen, Modifier.weight(1f))
            MetricCard("invertible", determinant.fold({ if (it.isZero) "No" else "Yes" }, { "—" }), LabCyan, Modifier.weight(1f))
        }
        parsedA.getOrNull()?.let { matrix ->
            Button(onClick = {
                repeat(matrix.columnCount) { column ->
                    val components = com.indianservers.aiexplorer.core.Vec3(
                        matrix[0, column].toDouble(),
                        matrix.rows.getOrNull(1)?.get(column)?.toDouble() ?: 0.0,
                        matrix.rows.getOrNull(2)?.get(column)?.toDouble() ?: 0.0,
                    )
                    vm.addVector3D("A${column + 1}", com.indianservers.aiexplorer.core.Vec3(0.0, 0.0, 0.0), components, "matrix column")
                }
                vm.open(MathModule.VectorLab)
            }) { Text("Send columns to Vector Lab") }
        }

        Text("SOLVE A x = b", color = LabViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            constants.forEachIndexed { index, value ->
                CompactTextField(value, { next -> constantsText = constants.toMutableList().also { it[index] = next }.joinToString(",") }, "b${index + 1}", Modifier.weight(1f))
            }
        }
        Text(solve.fold(::renderSystemSolution, { it.message ?: "Enter valid values" }), color = LabGreen, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

        if (dimension == 2) {
            val a = parsedA.getOrNull()
            val av = a?.rows?.map { row -> row.map(ExactRational::toDouble) }
            if (av != null) MatrixVectorCanvas(av[0][0].toFloat(), av[0][1].toFloat(), av[1][0].toFloat(), av[1][1].toFloat())
            Text("Eigenvalues: ${eigenvalues?.fold({ it.exact.joinToString(", ") }, { "—" }) ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Characteristic polynomial: ${a?.let { MatrixLabEngine.characteristicPolynomial2x2(it) } ?: "—"}", color = LabViolet, fontFamily = FontFamily.Monospace)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { preset(listOf(listOf("0", "-1"), listOf("1", "0")), "90 degree rotation") }) { Text("Rotate 90°") }
                Button(onClick = { preset(listOf(listOf("2", "0"), listOf("0", "1/2")), "non-uniform scale") }) { Text("Scale") }
                Button(onClick = { preset(listOf(listOf("1", "1"), listOf("0", "1")), "horizontal shear") }) { Text("Shear") }
                Button(onClick = { preset(listOf(listOf("1", "0"), listOf("0", "-1")), "reflection") }) { Text("Reflect") }
            }
        }

        Text("ROW-REDUCTION STEPS", color = LabViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        reduction.getOrNull()?.steps?.forEachIndexed { index, step ->
            Text("${index + 1}. $step", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

private enum class MatrixLabOperation(val label: String, val expression: String) {
    Add("A + B", "A + B"), Subtract("A − B", "A − B"), Multiply("A × B", "A × B"),
    Transpose("Aᵀ", "Aᵀ"), Square("A²", "A²"), Rref("RREF", "rref(A)"), Inverse("A⁻¹", "A⁻¹"),
}

@Composable
private fun MatrixEditor(
    title: String,
    cells: List<List<String>>,
    onChange: (Int, Int, String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = LabViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        cells.forEachIndexed { row, values ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                values.forEachIndexed { column, value ->
                    CompactTextField(value, { onChange(row, column, it) }, "${row + 1},${column + 1}", Modifier.weight(1f))
                }
            }
        }
    }
}

private fun matrixCells(size: Int, identity: Boolean): List<List<String>> = List(size) { row ->
    List(size) { column -> if (identity && row == column) "1" else "0" }
}

private fun encodeMatrixCells(cells: List<List<String>>): String = cells.joinToString(";") { it.joinToString(",") }

private fun decodeMatrixCells(source: String, size: Int): List<List<String>> {
    val rows = source.split(';').map { it.split(',') }
    return List(size) { row -> List(size) { column -> rows.getOrNull(row)?.getOrNull(column) ?: if (row == column) "1" else "0" } }
}

private fun decodeVectorCells(source: String, size: Int): List<String> =
    source.split(',').let { values -> List(size) { values.getOrNull(it) ?: "1" } }

private fun updateMatrixCell(cells: List<List<String>>, row: Int, column: Int, value: String): List<List<String>> =
    cells.mapIndexed { rowIndex, values -> if (rowIndex == row) values.mapIndexed { columnIndex, old -> if (columnIndex == column) value else old } else values }

private fun renderSystemSolution(solution: LinearSystemSolution): String = when (solution) {
    is LinearSystemSolution.Unique -> solution.values.mapIndexed { index, value -> "x${index + 1} = $value" }.joinToString(",  ")
    is LinearSystemSolution.Infinite -> "Infinitely many solutions; free: ${solution.freeVariables.joinToString { "x${it + 1}" }}"
    LinearSystemSolution.Inconsistent -> "No solution (inconsistent system)"
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
private fun MatrixVectorCanvas(m00: Float, m01: Float, m10: Float, m11: Float) {
    Canvas(Modifier.fillMaxWidth().height(260.dp).border(1.dp, LabViolet.copy(.4f), RoundedCornerShape(16.dp)).padding(8.dp)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val scale = (size.minDimension / 10f).coerceAtLeast(1f)
        for (grid in -5..5) {
            drawLine(Color.Gray.copy(.14f), Offset(0f, centre.y + grid * scale), Offset(size.width, centre.y + grid * scale), 1f)
            drawLine(Color.Gray.copy(.14f), Offset(centre.x + grid * scale, 0f), Offset(centre.x + grid * scale, size.height), 1f)
        }
        drawLine(Color.Gray.copy(.55f), Offset(0f, centre.y), Offset(size.width, centre.y), 1.5f)
        drawLine(Color.Gray.copy(.55f), Offset(centre.x, 0f), Offset(centre.x, size.height), 1.5f)
        fun endpoint(vx: Float, vy: Float) = Offset(centre.x + vx * scale, centre.y - vy * scale)
        val origin = endpoint(0f, 0f)
        val first = endpoint(m00, m10)
        val second = endpoint(m01, m11)
        val corner = endpoint(m00 + m01, m10 + m11)
        drawLine(LabViolet.copy(.75f), origin, first, 5f)
        drawLine(LabViolet.copy(.75f), first, corner, 5f)
        drawLine(LabViolet.copy(.75f), corner, second, 5f)
        drawLine(LabViolet.copy(.75f), second, origin, 5f)
        drawLine(LabCyan, centre, first, 7f, StrokeCap.Round)
        drawLine(LabGreen, centre, second, 7f, StrokeCap.Round)
        drawCircle(LabCyan, 7f, first); drawCircle(LabGreen, 7f, second)
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
