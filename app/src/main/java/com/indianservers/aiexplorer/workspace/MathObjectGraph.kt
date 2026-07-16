package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.CurveSample
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import kotlin.math.abs
import kotlin.math.round

enum class MathGraphRowKind { Expression, Parameter, Table, Folder, Note }

data class MathGraphRowMetadata(
    val colorKey: String = "cyan",
    val visible: Boolean = true,
    val collapsed: Boolean = false,
    val folderId: String? = null,
    val note: String = "",
    val error: String? = null,
)

data class MathExpressionRow(
    val id: String,
    val name: String,
    val expression: String,
    val metadata: MathGraphRowMetadata,
)

data class MathParameterRow(
    val id: String,
    val name: String,
    val value: Double,
    val min: Double = -10.0,
    val max: Double = 10.0,
    val step: Double = 0.1,
    val playing: Boolean = false,
    val animationMode: SliderAnimationMode = SliderAnimationMode.Loop,
)

enum class SliderAnimationMode { Loop, Bounce }

data class MathGeneratedTableRow(
    val input: Double,
    val outputs: Map<String, Double>,
)

data class MathObjectAlgebra(
    val canonicalExpression: String,
    val classification: String,
    val facts: List<String>,
)

data class MathGraphObject(
    val rowId: String,
    val name: String,
    val sourceExpression: String,
    val resolvedExpression: String,
    val algebra: MathObjectAlgebra,
    val sample: CurveSample,
    val roots: List<Double>,
    val extrema: List<Vec2>,
    val table: List<LinkedTableRow>,
)

data class MathObjectGraphSnapshot(
    val universalDocument: UniversalMathDocument,
    val expressionRows: List<MathExpressionRow>,
    val parameterRows: List<MathParameterRow>,
    val graphObjects: List<MathGraphObject>,
    val generatedTable: List<MathGeneratedTableRow>,
    val diagnostics: List<String>,
) {
    fun parameter(name: String): MathParameterRow? = parameterRows.firstOrNull { it.name == name }
    fun graphObject(name: String): MathGraphObject? = graphObjects.firstOrNull { it.name == name }
}

class MathObjectGraph(
    private val expressionEngine: ExpressionEngine = ExpressionEngine(),
    private val graphAnalysis: GraphAnalysis = GraphAnalysis(expressionEngine),
) {
    fun snapshot(
        state: WorkspaceState,
        parameterValues: Map<String, Double> = emptyMap(),
        tableInputs: List<Double> = (-4..4).map { it.toDouble() },
    ): MathObjectGraphSnapshot {
        val universalDocument = UniversalWorkspaceBridge.fromWorkspace(state)
        val functionsById = state.functions.associateBy { it.id }
        val initialRows = universalDocument.objects.values.filter { it.kind == UniversalMathKind.Function }.mapNotNull { objectValue ->
            val symbolic = objectValue.payload as? UniversalMathPayload.Symbolic ?: return@mapNotNull null
            val function = functionsById[objectValue.id]
            MathExpressionRow(
                id = objectValue.id,
                name = objectValue.name,
                expression = symbolic.source,
                metadata = MathGraphRowMetadata(
                    colorKey = function?.colorKey ?: "cyan",
                    visible = function?.visible ?: true,
                ),
            )
        }
        val parameterRows = detectParameters(initialRows, parameterValues)
        val parameterMap = parameterRows.associate { it.name to it.value }
        val expressionRows = initialRows.map { row ->
            row.copy(metadata = row.metadata.copy(error = compileDiagnostic(row.expression, parameterMap)))
        }
        val diagnostics = mutableListOf<String>()
        val graphObjects = expressionRows.filter { it.metadata.visible }.mapNotNull { row ->
            runCatching { graphObject(row, parameterMap, tableInputs) }
                .onFailure { diagnostics += "${row.name}: ${it.message ?: "could not recompute"}" }
                .getOrNull()
        }
        val generatedTable = tableInputs.map { x ->
            MathGeneratedTableRow(
                input = x,
                outputs = graphObjects.associate { graphObject ->
                    graphObject.name to (graphObject.table.firstOrNull { abs(it.input.decimal - x) < 1e-9 }?.output?.decimal ?: Double.NaN)
                },
            )
        }
        return MathObjectGraphSnapshot(universalDocument, expressionRows, parameterRows, graphObjects, generatedTable, diagnostics)
    }

    fun withParameter(snapshot: MathObjectGraphSnapshot, name: String, value: Double): Map<String, Double> =
        snapshot.parameterRows.associate { it.name to if (it.name == name) value else it.value }

    private fun graphObject(row: MathExpressionRow, parameterValues: Map<String, Double>, tableInputs: List<Double>): MathGraphObject {
        val source = stripEquation(row.expression)
        val resolved = substituteParameters(source, parameterValues)
        val compiled = expressionEngine.compile(resolved)
        val table = tableInputs.map { x ->
            LinkedTableRow(
                input = exactNumber(x, "generated table input"),
                output = exactNumber(compiled.eval(mapOf("x" to x, "t" to x)), "evaluated ${row.name} with ${parameterValues.describe()}"),
            )
        }
        return MathGraphObject(
            rowId = row.id,
            name = row.name,
            sourceExpression = source,
            resolvedExpression = resolved,
            algebra = algebra(resolved, source, parameterValues),
            sample = graphAnalysis.sampleDefinition(resolved, -6.0, 6.0, 180),
            roots = graphAnalysis.roots(resolved, -10.0, 10.0),
            extrema = graphAnalysis.extrema(resolved, -6.0, 6.0),
            table = table,
        )
    }

    private fun detectParameters(rows: List<MathExpressionRow>, parameterValues: Map<String, Double>): List<MathParameterRow> {
        val names = rows.flatMap { identifiers(stripEquation(it.expression)) }
            .filterNot { it in reservedIdentifiers }
            .distinct()
            .sorted()
        return names.map { name ->
            val default = parameterValues[name] ?: defaultParameterValue(name)
            MathParameterRow(
                id = "param-$name",
                name = name,
                value = default,
                min = if (name == "a") -8.0 else -10.0,
                max = if (name == "a") 8.0 else 10.0,
                step = if (abs(default) >= 10.0) 1.0 else 0.1,
            )
        }
    }

    private fun compileDiagnostic(expression: String, parameterValues: Map<String, Double>): String? =
        runCatching { expressionEngine.compile(substituteParameters(stripEquation(expression), parameterValues)).eval(mapOf("x" to 0.0, "t" to 0.0)) }
            .exceptionOrNull()
            ?.message

    private fun substituteParameters(expression: String, parameterValues: Map<String, Double>): String {
        var resolved = expression
        parameterValues.forEach { (name, value) ->
            if (name !in reservedIdentifiers) {
                resolved = resolved.replace(Regex("\\b${Regex.escape(name)}\\b"), "(${trim(value)})")
            }
        }
        return resolved
    }

    private fun algebra(resolved: String, source: String, parameterValues: Map<String, Double>): MathObjectAlgebra {
        val canonical = resolved.replace(Regex("\\s+"), "")
        val coefficients = quadraticCoefficients(canonical)
        val facts = mutableListOf<String>()
        if (parameterValues.isNotEmpty()) facts += "parameters=${parameterValues.describe()}"
        if (coefficients != null) {
            val (a, b, c) = coefficients
            val discriminant = b * b - 4.0 * a * c
            facts += listOf(
                "leadingCoefficient=${exactNumber(a, "coefficient").exact}",
                "linearCoefficient=${exactNumber(b, "coefficient").exact}",
                "constant=${exactNumber(c, "coefficient").exact}",
                "discriminant=${exactNumber(discriminant, "discriminant").exact}",
            )
            return MathObjectAlgebra(canonical, "quadratic polynomial", facts)
        }
        return MathObjectAlgebra(
            canonicalExpression = canonical,
            classification = if (source == resolved) "shared expression" else "parameterized expression",
            facts = facts.ifEmpty { listOf("graph and table share this resolved expression") },
        )
    }

    private fun quadraticCoefficients(expression: String): Triple<Double, Double, Double>? {
        if (!expression.contains("x")) return null
        val values = listOf(0.0, 1.0, 2.0).map { x ->
            runCatching { expressionEngine.compile(expression).eval(mapOf("x" to x)) }.getOrNull() ?: return null
        }
        val c = values[0]
        val a = (values[2] - 2.0 * values[1] + values[0]) / 2.0
        val b = values[1] - a - c
        val checkX = -1.5
        val check = runCatching { expressionEngine.compile(expression).eval(mapOf("x" to checkX)) }.getOrNull() ?: return null
        val predicted = a * checkX * checkX + b * checkX + c
        return if (abs(check - predicted) < 1e-7 && abs(a) > 1e-12) Triple(a, b, c) else null
    }

    private fun identifiers(expression: String): List<String> =
        Regex("[A-Za-z][A-Za-z0-9_]*").findAll(expression).map { it.value }.toList()

    private fun defaultParameterValue(name: String): Double = when (name) {
        "a" -> 1.0
        "b" -> 0.0
        "c" -> 1.0
        else -> 1.0
    }

    private fun exactNumber(value: Double, provenance: String): ExactNumericValue {
        val clean = if (abs(value) < 1e-12) 0.0 else value
        val whole = round(clean)
        if (abs(clean - whole) < 1e-10) return ExactNumericValue(whole.toLong().toString(), clean, provenance)
        return ExactNumericValue(trim(clean), clean, provenance)
    }

    private fun Map<String, Double>.describe(): String =
        entries.sortedBy { it.key }.joinToString { "${it.key}=${trim(it.value)}" }.ifBlank { "no parameters" }

    private companion object {
        val reservedIdentifiers = setOf(
            "x", "y", "t", "pi", "e",
            "sin", "cos", "tan", "sec", "csc", "cot",
            "sinh", "cosh", "tanh", "asin", "acos", "atan",
            "sqrt", "abs", "exp", "ln", "log", "floor", "ceil",
            "sign", "min", "max", "if", "r",
        )
    }
}
