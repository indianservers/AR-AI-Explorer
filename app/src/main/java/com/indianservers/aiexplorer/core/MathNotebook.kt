package com.indianservers.aiexplorer.core

import java.util.Locale
import kotlin.math.abs

enum class NotebookCellKind(val label: String) {
    Scalar("Scalar"), Function("Function"), Calculation("Calculation")
}

data class NotebookCell(
    val id: String,
    val input: String,
    val kind: NotebookCellKind,
    val symbol: String? = null,
    val expression: String = input,
    val dependencies: Set<String> = emptySet(),
    val exactOutput: String = "",
    val decimalOutput: String? = null,
    val graphExpression: String? = null,
    val error: String? = null,
)

data class MathNotebookDocument(
    val cells: List<NotebookCell> = emptyList(),
    val revision: Int = 0,
) {
    val hasErrors: Boolean get() = cells.any { it.error != null }
    fun symbolNames(): List<String> = cells.mapNotNull { it.symbol }.distinct()
}

/**
 * A deterministic notebook dependency engine. Assignments use `:=` so equations
 * remain unambiguous. Redefining a symbol replaces its defining cell and
 * atomically recomputes every dependent cell.
 */
class MathNotebookEngine(
    private val expressions: ExpressionEngine = ExpressionEngine(),
    private val solver: MathProblemSolver = MathProblemSolver(expressions),
) {
    fun submit(document: MathNotebookDocument, rawInput: String): MathNotebookDocument {
        val input = rawInput.trim()
        require(input.isNotBlank()) { "Notebook input cannot be blank." }
        val parsed = parseCell(input, nextId(document))
        val updated = if (parsed.symbol == null) {
            document.cells + parsed
        } else {
            val existing = document.cells.indexOfFirst { it.symbol == parsed.symbol }
            if (existing < 0) document.cells + parsed
            else document.cells.mapIndexed { index, cell -> if (index == existing) parsed.copy(id = cell.id) else cell }
        }
        return rebuild(MathNotebookDocument(updated, document.revision + 1))
    }

    fun remove(document: MathNotebookDocument, id: String): MathNotebookDocument =
        rebuild(MathNotebookDocument(document.cells.filterNot { it.id == id }, document.revision + 1))

    fun clear() = MathNotebookDocument()

    fun exportJson(document: MathNotebookDocument): String = buildString {
        append("{\"schemaVersion\":1,\"revision\":${document.revision},\"cells\":[")
        append(document.cells.joinToString(",") { cell ->
            "{\"id\":\"${cell.id.escape()}\",\"input\":\"${cell.input.escape()}\",\"kind\":\"${cell.kind}\",\"symbol\":${cell.symbol?.let { "\"${it.escape()}\"" } ?: "null"},\"exactOutput\":\"${cell.exactOutput.escape()}\",\"error\":${cell.error?.let { "\"${it.escape()}\"" } ?: "null"}}"
        })
        append("]}")
    }

    private fun rebuild(document: MathNotebookDocument): MathNotebookDocument {
        val definitions = document.cells.mapNotNull { cell -> cell.symbol?.let { it to cell } }.toMap()
        val resolved = mutableMapOf<String, ResolvedSymbol>()

        fun resolve(name: String, stack: Set<String>): ResolvedSymbol {
            resolved[name]?.let { return it }
            if (name in stack) error("Circular dependency: ${(stack + name).joinToString(" → ")}")
            val cell = definitions[name] ?: error("Unknown symbol '$name'")
            val nextStack = stack + name
            val dependencyValues = cell.dependencies.associateWith { resolve(it, nextStack) }
            val resolvedSymbol = when (cell.kind) {
                NotebookCellKind.Scalar -> {
                    val substituted = substituteScalars(cell.expression, dependencyValues)
                    val numeric = expressions.compile(substituted).eval()
                    require(numeric.isFinite()) { "'$name' does not have a finite real value." }
                    val exact = solver.solve("Calculate $substituted").takeIf { it.supported }?.answer ?: format(numeric)
                    ResolvedSymbol(exact, numeric, null)
                }
                NotebookCellKind.Function -> {
                    val substituted = substituteScalars(cell.expression, dependencyValues)
                    val check = expressions.compile(substituted).eval(mapOf("x" to .417))
                    require(check.isFinite()) { "Function '$name' is not finite at the validation point." }
                    ResolvedSymbol("$name(x) = $substituted", null, substituted)
                }
                NotebookCellKind.Calculation -> error("Calculations cannot define symbols.")
            }
            resolved[name] = resolvedSymbol
            return resolvedSymbol
        }

        definitions.keys.forEach { name -> runCatching { resolve(name, emptySet()) } }
        val rebuilt = mutableListOf<NotebookCell>()
        document.cells.forEach { cell ->
            val evaluated = runCatching {
                if (cell.symbol != null) {
                    val value = resolve(cell.symbol, emptySet())
                    cell.copy(
                        exactOutput = value.exact,
                        decimalOutput = value.numeric?.let(::format),
                        graphExpression = value.graphExpression,
                        error = null,
                    )
                } else {
                    val referenced = substituteCellReferences(cell.expression, rebuilt)
                    val dependencies = dependenciesOf(referenced)
                    val values = dependencies.associateWith { resolve(it, emptySet()) }
                    val substituted = substituteScalars(referenced, values)
                    val solution = solver.solve(substituted)
                    if (!solution.supported) error(solution.verification)
                    val numeric = runCatching { expressions.compile(substituted).eval() }.getOrNull()?.takeIf { it.isFinite() }
                    cell.copy(
                        dependencies = dependencies,
                        exactOutput = solution.answer,
                        decimalOutput = numeric?.let(::format),
                        error = null,
                    )
                }
            }.getOrElse { failure ->
                cell.copy(exactOutput = "", decimalOutput = null, graphExpression = null, error = failure.message ?: "Unable to evaluate cell.")
            }
            rebuilt += evaluated
        }
        return document.copy(cells = rebuilt)
    }

    private fun parseCell(input: String, id: String): NotebookCell {
        val assignment = Regex("^([A-Za-z][A-Za-z0-9_]*)(?:\\(\\s*x\\s*\\))?\\s*:=\\s*(.+)$").find(input)
        if (assignment == null) return NotebookCell(id, input, NotebookCellKind.Calculation, expression = input)
        val symbol = assignment.groupValues[1]
        require(symbol.lowercase() !in reserved) { "'$symbol' is reserved by the maths engine." }
        val expression = assignment.groupValues[2].trim()
        val isFunction = input.substringBefore(":=").contains('(')
        val dependencies = dependenciesOf(expression).filterNot { isFunction && it == "x" }.toSet()
        return NotebookCell(id, input, if (isFunction) NotebookCellKind.Function else NotebookCellKind.Scalar, symbol, expression, dependencies)
    }

    private fun dependenciesOf(source: String): Set<String> = identifier.findAll(source)
        .map { it.value }
        .filterNot { it.lowercase() in reserved }
        .toSet()

    private fun substituteScalars(source: String, values: Map<String, ResolvedSymbol>): String {
        var result = source
        values.entries.sortedByDescending { it.key.length }.forEach { (name, value) ->
            val exactScalar = value.exact.takeIf { it.matches(Regex("-?\\d+(?:/\\d+)?(?:\\.\\d+)?")) }
            val replacement = exactScalar?.let { "($it)" } ?: value.numeric?.let { "(${format(it)})" }
                ?: error("Function '$name' cannot be used as a scalar value yet.")
            result = result.replace(Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])"), replacement)
        }
        return result
    }

    private fun substituteCellReferences(source: String, cells: List<NotebookCell>): String =
        cellReference.replace(source) { match ->
            val reference = match.groupValues[1].toInt() - 1
            require(reference in cells.indices) { "#${reference + 1} must refer to an earlier cell." }
            val cell = cells[reference]
            require(cell.error == null && cell.exactOutput.isNotBlank()) { "#${reference + 1} has no usable result." }
            val value = cell.decimalOutput ?: cell.exactOutput.takeIf { it.matches(Regex("-?\\d+(?:/\\d+)?(?:\\.\\d+)?")) }
                ?: error("#${reference + 1} is not a scalar result.")
            "($value)"
        }

    private fun nextId(document: MathNotebookDocument) = "cell-${(document.cells.mapNotNull { it.id.substringAfterLast('-').toIntOrNull() }.maxOrNull() ?: 0) + 1}"
    private fun format(value: Double): String {
        if (abs(value) < 1e-12) return "0"
        val whole = value.toLong()
        return if (abs(value - whole) < 1e-10) whole.toString()
        else String.format(Locale.US, "%.10f", value).trimEnd('0').trimEnd('.')
    }

    private data class ResolvedSymbol(val exact: String, val numeric: Double?, val graphExpression: String?)

    private companion object {
        val identifier = Regex("[A-Za-z][A-Za-z0-9_]*")
        val cellReference = Regex("#(\\d+)")
        val reserved = setOf("x", "y", "z", "t", "pi", "e", "sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "abs", "exp", "ln", "log", "min", "max", "if", "calculate", "evaluate", "simplify", "expand", "factor", "differentiate", "integrate", "solve")
    }
}

private fun String.escape(): String = buildString {
    this@escape.forEach { character -> append(when (character) { '\\' -> "\\\\"; '"' -> "\\\""; '\n' -> "\\n"; '\r' -> "\\r"; '\t' -> "\\t"; else -> character }) }
}
