package com.indianservers.aiexplorer.core

import kotlin.math.abs

enum class MissingDataPolicy { Skip, Zero, Error }
data class SpreadsheetAddress(val column: Int, val row: Int) {
    init { require(column >= 0 && row >= 0) }
    val a1: String get() = columnName(column) + (row + 1)
}
data class SpreadsheetColumn(val name: String, val listName: String = name)
data class SpreadsheetCell(val input: String = "", val value: Double? = null, val error: String? = null)
data class MathSpreadsheet(
    val columns: List<SpreadsheetColumn> = listOf(SpreadsheetColumn("A"), SpreadsheetColumn("B")),
    val cells: Map<SpreadsheetAddress, String> = emptyMap(),
    val revision: Int = 0,
)
data class SpreadsheetSnapshot(
    val document: MathSpreadsheet,
    val evaluated: Map<SpreadsheetAddress, SpreadsheetCell>,
    val namedLists: Map<String, List<Double>>,
)
data class LinkedDataSeries(val xName: String, val yName: String, val points: List<Vec2>, val revision: Int)

class MathSpreadsheetEngine(private val expressions: ExpressionEngine = ExpressionEngine()) {
    fun setCell(document: MathSpreadsheet, address: SpreadsheetAddress, input: String): MathSpreadsheet =
        document.copy(cells = document.cells + (address to input), revision = document.revision + 1)

    fun renameColumn(document: MathSpreadsheet, column: Int, name: String): MathSpreadsheet {
        require(column in document.columns.indices && name.matches(Regex("[A-Za-z][A-Za-z0-9_ ]{0,31}")))
        return document.copy(columns = document.columns.mapIndexed { index, value -> if (index == column) SpreadsheetColumn(value.name, name.trim().replace(' ', '_')) else value }, revision = document.revision + 1)
    }

    fun evaluate(document: MathSpreadsheet, missing: MissingDataPolicy = MissingDataPolicy.Skip): SpreadsheetSnapshot {
        val cache = mutableMapOf<SpreadsheetAddress, SpreadsheetCell>()
        fun eval(address: SpreadsheetAddress, stack: Set<SpreadsheetAddress>): SpreadsheetCell {
            cache[address]?.let { return it }
            if (address in stack) return SpreadsheetCell(document.cells[address].orEmpty(), error = "Circular reference")
            val input = document.cells[address].orEmpty().trim()
            if (input.isBlank() || input.equals("NA", true) || input == "-") {
                val value = when (missing) { MissingDataPolicy.Zero -> 0.0; MissingDataPolicy.Skip -> null; MissingDataPolicy.Error -> null }
                return SpreadsheetCell(input, value, if (missing == MissingDataPolicy.Error) "Missing value" else null).also { cache[address] = it }
            }
            val result = runCatching {
                if (!input.startsWith("=")) input.toDouble() else evaluateFormula(input.drop(1), stack + address, ::eval)
            }.fold(
                onSuccess = { SpreadsheetCell(input, it.takeIf(Double::isFinite), if (it.isFinite()) null else "Non-finite result") },
                onFailure = { SpreadsheetCell(input, error = it.message ?: "Invalid formula") },
            )
            cache[address] = result
            return result
        }
        document.cells.keys.forEach { eval(it, emptySet()) }
        val named = document.columns.mapIndexed { index, column ->
            val lastRow = document.cells.keys.filter { it.column == index }.maxOfOrNull { it.row } ?: -1
            column.listName to (0..lastRow).mapNotNull { eval(SpreadsheetAddress(index, it), emptySet()).value }
        }.toMap()
        return SpreadsheetSnapshot(document, cache, named)
    }

    fun linkedSeries(snapshot: SpreadsheetSnapshot, xColumn: Int, yColumn: Int): LinkedDataSeries {
        require(xColumn in snapshot.document.columns.indices && yColumn in snapshot.document.columns.indices)
        val rows = snapshot.evaluated.keys.maxOfOrNull { it.row } ?: -1
        val points = (0..rows).mapNotNull { row ->
            val x = snapshot.evaluated[SpreadsheetAddress(xColumn, row)]?.value
            val y = snapshot.evaluated[SpreadsheetAddress(yColumn, row)]?.value
            if (x == null || y == null) null else Vec2(x, y)
        }
        return LinkedDataSeries(snapshot.document.columns[xColumn].listName, snapshot.document.columns[yColumn].listName, points, snapshot.document.revision)
    }

    fun importCsv(csv: String, hasHeader: Boolean = true): MathSpreadsheet {
        val rows = csv.lineSequence().filter { it.isNotBlank() }.map(::parseCsvRow).toList()
        if (rows.isEmpty()) return MathSpreadsheet()
        val width = rows.maxOf { it.size }
        val headers = if (hasHeader) rows.first() else (0 until width).map(::columnName)
        val data = if (hasHeader) rows.drop(1) else rows
        val columns = (0 until width).map { index ->
            val name = headers.getOrNull(index)?.ifBlank { columnName(index) } ?: columnName(index)
            SpreadsheetColumn(columnName(index), name.replace(Regex("[^A-Za-z0-9_]"), "_").ifBlank { columnName(index) })
        }
        val cells = buildMap {
            data.forEachIndexed { row, values -> values.forEachIndexed { column, value -> if (value.isNotBlank()) put(SpreadsheetAddress(column, row), value) } }
        }
        return MathSpreadsheet(columns, cells, 1)
    }

    fun exportCsv(snapshot: SpreadsheetSnapshot, includeHeader: Boolean = true): String {
        val width = snapshot.document.columns.size
        val rows = snapshot.document.cells.keys.maxOfOrNull { it.row } ?: -1
        return buildList {
            if (includeHeader) add(snapshot.document.columns.joinToString(",") { csvEscape(it.listName) })
            (0..rows).forEach { row ->
                add((0 until width).joinToString(",") { column ->
                    val cell = snapshot.evaluated[SpreadsheetAddress(column, row)]
                    csvEscape(cell?.value?.let(::format) ?: snapshot.document.cells[SpreadsheetAddress(column, row)].orEmpty())
                })
            }
        }.joinToString("\n")
    }

    private fun evaluateFormula(
        source: String,
        stack: Set<SpreadsheetAddress>,
        eval: (SpreadsheetAddress, Set<SpreadsheetAddress>) -> SpreadsheetCell,
    ): Double {
        var formula = source
        val aggregate = Regex("(?i)(SUM|MEAN|AVERAGE|MIN|MAX|COUNT)\\(([A-Z]+\\d+):([A-Z]+\\d+)\\)")
        formula = aggregate.replace(formula) { match ->
            val values = addresses(match.groupValues[2], match.groupValues[3]).mapNotNull { eval(it, stack).value }
            require(values.isNotEmpty() || match.groupValues[1].equals("COUNT", true)) { "Range has no numeric values" }
            val value = when (match.groupValues[1].uppercase()) {
                "SUM" -> values.sum(); "MEAN", "AVERAGE" -> values.average(); "MIN" -> values.min(); "MAX" -> values.max(); else -> values.size.toDouble()
            }
            "(" + format(value) + ")"
        }
        formula = Regex("(?i)\\b([A-Z]+\\d+)\\b").replace(formula) { match ->
            val cell = eval(parseAddress(match.groupValues[1]), stack)
            require(cell.error == null) { cell.error.orEmpty() }
            "(" + format(cell.value ?: 0.0) + ")"
        }
        return expressions.compile(formula).eval()
    }

    private fun addresses(first: String, last: String): List<SpreadsheetAddress> {
        val a = parseAddress(first); val b = parseAddress(last)
        return (minOf(a.row, b.row)..maxOf(a.row, b.row)).flatMap { row ->
            (minOf(a.column, b.column)..maxOf(a.column, b.column)).map { column -> SpreadsheetAddress(column, row) }
        }
    }

    private fun parseAddress(value: String): SpreadsheetAddress {
        val match = Regex("(?i)^([A-Z]+)(\\d+)$").matchEntire(value) ?: error("Invalid cell reference '$value'")
        var column = 0
        match.groupValues[1].uppercase().forEach { column = column * 26 + (it - 'A' + 1) }
        return SpreadsheetAddress(column - 1, match.groupValues[2].toInt() - 1)
    }

    private fun parseCsvRow(row: String): List<String> {
        val result = mutableListOf<String>(); val value = StringBuilder(); var quoted = false; var index = 0
        while (index < row.length) {
            val character = row[index]
            when {
                character == '"' && quoted && row.getOrNull(index + 1) == '"' -> { value.append('"'); index++ }
                character == '"' -> quoted = !quoted
                character == ',' && !quoted -> { result += value.toString(); value.clear() }
                else -> value.append(character)
            }
            index++
        }
        result += value.toString()
        return result
    }

    private fun csvEscape(value: String) = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
    private fun format(value: Double) = if (abs(value - value.toLong()) < 1e-10) value.toLong().toString() else String.format(java.util.Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
}

private fun columnName(index: Int): String {
    var value = index + 1; val result = StringBuilder()
    while (value > 0) { value--; result.append(('A'.code + value % 26).toChar()); value /= 26 }
    return result.reverse().toString()
}
