package com.indianservers.aiexplorer.core

import java.util.Locale

enum class CasInputModality { Keyboard, NaturalLanguage, Handwriting, Photo, Voice, Csv, StructuredFile }
enum class CasVariableScope { Row, FollowingRows, Notebook }
enum class CasPodKind(val label: String) {
    Exact("Exact result"), Decimal("Decimal result"), Plot("Plot"), Domain("Domain"),
    Alternate("Alternate forms"), Verification("Verification"), Conditions("Conditions and parameters"), Geometry("Geometric interpretation")
}
enum class CasTargetAction { Simplify, Factor, Substitute, Differentiate }
enum class CasExportFormat { Latex, MathMl, Svg, Png, Json, Csv }

data class CasInterpretationOption(
    val id: String,
    val label: String,
    val expression: String,
    val operation: String,
    val variable: String = "x",
    val confidence: Double = 1.0,
    val explanation: String = "",
)

data class CasCapturedInput(
    val modality: CasInputModality,
    val recognizedText: String,
    val confidence: Double,
    val alternatives: List<String> = emptyList(),
) {
    init { require(confidence in 0.0..1.0) }
}

data class CasScopedVariable(
    val name: String,
    val expression: String,
    val scope: CasVariableScope = CasVariableScope.FollowingRows,
    val rowId: String? = null,
) {
    init { require(name.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) }
}

data class CasImportedData(
    val name: String,
    val matrixExpression: String,
    val rows: Int,
    val columns: Int,
    val headers: List<String> = emptyList(),
    val source: CasInputModality = CasInputModality.Csv,
)
data class CasStructuredFileResult(val data: CasImportedData, val format: String, val warnings: List<String>, val preview: List<List<String>>)

data class CasResultPod(
    val kind: CasPodKind,
    val title: String,
    val primary: String,
    val details: List<String> = emptyList(),
    val expandable: Boolean = true,
)

data class CasProgressiveHint(val level: Int, val title: String, val text: String)
enum class CasStepDisclosureLevel(val label: String) { Hint("Hint"), Rule("Rule"), Worked("Worked step"), Verify("Check") }
data class CasStepDisclosure(val level: CasStepDisclosureLevel, val title: String, val content: String, val revealsIntermediate: Boolean = false)
data class CasStepAssumptionWarning(val stepIndex: Int, val assumptions: List<String>, val message: String)
data class CasSubexpressionTarget(val id: String, val text: String, val start: Int, val endExclusive: Int, val actions: Set<CasTargetAction>)
data class CasExpressionTransfer(val sourceRowId: String, val expression: String, val mimeType: String = "application/x-aiexplorer-cas")
data class CasSolutionStructure(val parameters: List<String>, val conditions: List<String>, val branches: List<String>, val exactAlgebraic: Boolean)

object CasSolutionStructureAnalyzer {
    fun analyze(result: CasRow): CasSolutionStructure = CasSolutionStructure(
        parameters = Regex("\\bC\\d+\\b").findAll(result.exact).map { it.value }.distinct().toList(),
        conditions = result.assumptions.distinct(),
        branches = result.exact.split(';').map(String::trim).filter { it.isNotBlank() }.takeIf { it.size > 1 } ?: emptyList(),
        exactAlgebraic = result.exact.contains("RootOf("),
    )
}

data class CasInteractiveRow(
    val id: String,
    val source: String,
    val interpretation: CasInterpretationOption,
    val method: CasSolutionMethod,
    val result: CasRow,
    val pods: List<CasResultPod>,
    val hints: List<CasProgressiveHint>,
    val assumptionWarnings: List<CasStepAssumptionWarning>,
    val pinned: Boolean = false,
    val annotation: String = "",
)

data class CasNotebookSession(
    val assumptions: MathAssumptionSet = MathAssumptionSet(),
    val variables: List<CasScopedVariable> = emptyList(),
    val rows: List<CasInteractiveRow> = emptyList(),
    val imports: List<CasImportedData> = emptyList(),
    val compareRowIds: Set<String> = emptySet(),
) {
    fun visibleVariables(rowId: String? = null): Map<String, String> {
        val rowIndex = rows.indexOfFirst { it.id == rowId }.takeIf { it >= 0 } ?: rows.size
        return variables.filter { variable ->
            when (variable.scope) {
                CasVariableScope.Notebook -> true
                CasVariableScope.Row -> variable.rowId == rowId
                CasVariableScope.FollowingRows -> {
                    val definitionIndex = rows.indexOfFirst { it.id == variable.rowId }
                    definitionIndex < 0 || definitionIndex < rowIndex
                }
            }
        }.associate { it.name to it.expression }
    }
}

object CasInterpretationResolver {
    fun options(source: String, selectedOperation: String? = null): List<CasInterpretationOption> {
        val clean = source.trim()
        if (clean.isEmpty()) return listOf(CasInterpretationOption("empty", "Enter a question", "", selectedOperation ?: "simplify", confidence = 0.0))
        val natural = normalizeNaturalLanguage(clean, selectedOperation)
        val candidates = mutableListOf(natural)
        Regex("(?i)^sin\\s+([A-Za-z])\\^2$").matchEntire(clean)?.let { match ->
            val v = match.groupValues[1]
            candidates += CasInterpretationOption("sin-square", "Square of sine", "sin($v)^2", "simplify", v, .76, "Interpret the exponent as applying to sin($v).")
            candidates += CasInterpretationOption("sin-argument-square", "Sine of a square", "sin($v^2)", "simplify", v, .76, "Interpret the exponent as part of the function argument.")
        }
        Regex("^(-?\\d+(?:\\.\\d+)?)/(-?\\d+(?:\\.\\d+)?)([A-Za-z])$").matchEntire(clean.replace(" ", ""))?.let { match ->
            val (a, b, v) = match.destructured
            candidates += CasInterpretationOption("fraction-times", "Fraction times variable", "($a/$b)*$v", selectedOperation ?: "simplify", v, .72, "Multiplication follows the fraction.")
            candidates += CasInterpretationOption("fraction-denominator", "Variable in denominator", "$a/($b*$v)", selectedOperation ?: "simplify", v, .72, "The whole product is the denominator.")
        }
        return candidates.distinctBy { it.expression to it.operation }
    }

    private fun normalizeNaturalLanguage(source: String, selectedOperation: String?): CasInterpretationOption {
        val patterns = listOf(
            Regex("(?i)^(?:what is |please )?(?:the )?derivative of (.+?)(?: with respect to ([A-Za-z][A-Za-z0-9_]*))?$") to "derivative",
            Regex("(?i)^(?:what is |please )?(?:the )?integral of (.+?)(?: with respect to ([A-Za-z][A-Za-z0-9_]*))?$") to "integral",
            Regex("(?i)^(?:please )?factor (.+)$") to "factor",
            Regex("(?i)^(?:please )?expand (.+)$") to "expand",
            Regex("(?i)^(?:please )?simplify (.+)$") to "simplify",
            Regex("(?i)^(?:find |compute )?the limit of (.+?)(?: as ([A-Za-z]) (?:approaches|->) (.+))?$") to "limit",
        )
        patterns.forEachIndexed { index, (pattern, operation) ->
            pattern.matchEntire(source)?.let { match ->
                val expression = match.groupValues.getOrElse(1) { source }.trim()
                val variable = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: inferVariable(expression)
                return CasInterpretationOption("nl-$index", "$operation $expression", expression, operation, variable, .96, "Translated the question into a symbolic $operation request.")
            }
        }
        val operation = selectedOperation ?: "simplify"
        return CasInterpretationOption("literal", "$operation as typed", source, operation, inferVariable(source), .84, "Keep the expression editable and evaluate it literally.")
    }

    private fun inferVariable(expression: String) = Regex("[A-Za-z][A-Za-z0-9_]*").findAll(expression)
        .map { it.value }.firstOrNull { it.lowercase() !in setOf("sin", "cos", "tan", "sqrt", "ln", "exp", "pi") } ?: "x"
}

object CasStepDisclosureEngine {
    fun disclosures(step: CasStep, index: Int, totalSteps: Int, assumptions: List<String> = emptyList()): List<CasStepDisclosure> = listOf(
        CasStepDisclosure(
            CasStepDisclosureLevel.Hint,
            "What should I notice?",
            hint(step.title, index),
        ),
        CasStepDisclosure(
            CasStepDisclosureLevel.Rule,
            "Why is this valid?",
            step.explanation.ifBlank { "Apply the ${step.title.lowercase(Locale.ROOT)} rule to the current expression." },
        ),
        CasStepDisclosure(
            CasStepDisclosureLevel.Worked,
            "Intermediate expression",
            step.expression,
            revealsIntermediate = true,
        ),
        CasStepDisclosure(
            CasStepDisclosureLevel.Verify,
            "How can I check it?",
            buildString {
                append(if (index == totalSteps - 1) "Substitute or reverse the operation to check the final result." else "Compare this line with the previous line and verify that only the stated rule changed it.")
                if (assumptions.isNotEmpty()) append(" Check under ${assumptions.joinToString()}.")
            },
        ),
    )

    private fun hint(title: String, index: Int) = when {
        title.contains("method", true) -> "Look at the expression's outermost structure before choosing a technique."
        title.contains("assumption", true) -> "Ask which signs, domains or excluded values make the next identity safe."
        title.contains("factor", true) -> "Search for a common factor or two terms whose product and sum match the coefficients."
        title.contains("expand", true) -> "Distribute one factor at a time and combine only like terms."
        title.contains("deriv", true) -> "Identify the outermost operation, then choose its derivative rule."
        title.contains("integr", true) -> "Look for a reverse derivative pattern before applying a general rule."
        title.contains("row", true) || title.contains("elimin", true) -> "Choose a row operation that creates a useful zero without changing the solution set."
        index == 0 -> "Classify the problem and identify the first reversible transformation."
        else -> "Use the previous line and change one mathematical idea at a time."
    }
}

object CasCaptureNormalizer {
    fun normalize(modality: CasInputModality, candidates: List<Pair<String, Double>>): CasCapturedInput {
        require(modality in setOf(CasInputModality.Handwriting, CasInputModality.Photo, CasInputModality.Voice))
        require(candidates.isNotEmpty())
        val cleaned = candidates.map { (text, confidence) -> normalizeMathSpeech(text) to confidence.coerceIn(0.0, 1.0) }
            .filter { it.first.isNotBlank() }.sortedByDescending { it.second }
        require(cleaned.isNotEmpty())
        return CasCapturedInput(modality, cleaned.first().first, cleaned.first().second, cleaned.drop(1).map { it.first }.distinct())
    }

    private fun normalizeMathSpeech(text: String) = text.trim()
        .replace(Regex("(?i)\\b(open|left) parenthesis\\b"), "(")
        .replace(Regex("(?i)\\b(close|right) parenthesis\\b"), ")")
        .replace(Regex("(?i)\\bplus\\b"), "+").replace(Regex("(?i)\\bminus\\b"), "-")
        .replace(Regex("(?i)\\btimes|multiplied by\\b"), "*").replace(Regex("(?i)\\bdivided by\\b"), "/")
        .replace(Regex("(?i)\\bsquared\\b"), "^2").replace(Regex("(?i)\\bcubed\\b"), "^3")
        .replace(Regex("\\s+"), " ").trim()
}

object CasStructuredImportEngine {
    fun structured(name: String, source: String, mimeType: String? = null): CasStructuredFileResult {
        require(source.length <= 2_000_000) { "Import is too large." }
        val extension = name.substringAfterLast('.', "").lowercase()
        return when {
            extension == "json" || mimeType?.contains("json", true) == true -> json(name, source)
            extension in setOf("mtx", "mm") || source.lineSequence().firstOrNull()?.startsWith("%%MatrixMarket") == true -> matrixMarket(name, source)
            extension == "tsv" || mimeType?.contains("tab-separated", true) == true -> csvResult(name, source, '\t', "TSV")
            extension == "csv" || mimeType?.contains("csv", true) == true -> csvResult(name, source, ',', "CSV")
            else -> plainMatrix(name, source)
        }
    }

    fun csv(name: String, source: String, delimiter: Char? = null): CasImportedData {
        require(source.length <= 2_000_000) { "Import is too large." }
        val lines = source.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "The file is empty." }
        val separator = delimiter ?: if (lines.first().count { it == '\t' } > lines.first().count { it == ',' }) '\t' else ','
        val parsed = lines.map { parseDelimitedRow(it, separator) }
        val width = parsed.maxOf { it.size }
        require(width in 1..500 && parsed.size <= 10000) { "CAS imports support up to 10,000 rows and 500 columns." }
        require(parsed.all { it.size == width }) { "Every imported row must have the same number of columns." }
        val hasHeader = parsed.first().any { it.toDoubleOrNull() == null }
        val headers = if (hasHeader) parsed.first() else emptyList()
        val values = if (hasHeader) parsed.drop(1) else parsed
        require(values.isNotEmpty() && values.flatten().all { it.toDoubleOrNull() != null }) { "Matrix cells must contain numbers; use the first row for optional headings." }
        val matrix = values.joinToString(separator = ",", prefix = "[", postfix = "]") { row -> row.joinToString(separator = ",", prefix = "[", postfix = "]") }
        return CasImportedData(safeName(name), matrix, values.size, width, headers, if (separator == '\t') CasInputModality.StructuredFile else CasInputModality.Csv)
    }

    private fun csvResult(name: String, source: String, delimiter: Char, format: String): CasStructuredFileResult {
        val data = csv(name, source, delimiter)
        val rows = data.matrixExpression.removePrefix("[[").removeSuffix("]]").split("],[").take(5).map { it.split(',') }
        return CasStructuredFileResult(data, format, if (data.rows > 5) listOf("Preview shows 5 of ${data.rows} rows.") else emptyList(), rows)
    }

    private fun json(name: String, source: String): CasStructuredFileResult {
        val clean = source.replace(Regex("\\s+"), "")
        require(clean.startsWith("[[") && clean.endsWith("]]")) { "JSON CAS import must be a two-dimensional numeric array." }
        val rows = clean.substring(2, clean.length - 2).split(Regex("\\],\\[")).map { row -> row.split(',') }
        require(rows.isNotEmpty() && rows.all { it.size == rows.first().size } && rows.flatten().all { it.toDoubleOrNull() != null }) { "JSON matrix rows must be rectangular and numeric." }
        require(rows.size <= 10000 && rows.first().size <= 500)
        val matrix = rows.joinToString(separator = ",", prefix = "[", postfix = "]") { it.joinToString(separator = ",", prefix = "[", postfix = "]") }
        val data = CasImportedData(safeName(name), matrix, rows.size, rows.first().size, source = CasInputModality.StructuredFile)
        return CasStructuredFileResult(data, "JSON matrix", emptyList(), rows.take(5))
    }

    private fun plainMatrix(name: String, source: String): CasStructuredFileResult {
        val clean = source.trim()
        if (clean.startsWith("[[")) return json(name, clean).copy(format = "Bracket matrix")
        val normalized = clean.lineSequence().filter { it.isNotBlank() && !it.trimStart().startsWith('#') }
            .joinToString("\n") { it.trim().split(Regex("\\s+")).joinToString(",") }
        return csvResult(name, normalized, ',', "Whitespace matrix")
    }

    private fun matrixMarket(name: String, source: String): CasStructuredFileResult {
        val lines = source.lineSequence().map(String::trim).filter { it.isNotBlank() && !it.startsWith('%') }.toList()
        require(lines.isNotEmpty()) { "Matrix Market data is empty." }
        val dimensions = lines.first().split(Regex("\\s+")).mapNotNull(String::toIntOrNull)
        require(dimensions.size >= 2) { "Matrix Market dimensions are missing." }
        val rowCount = dimensions[0]; val columnCount = dimensions[1]
        require(rowCount in 1..10000 && columnCount in 1..500)
        val matrix = MutableList(rowCount) { MutableList(columnCount) { "0" } }
        val coordinate = dimensions.size >= 3
        if (coordinate) {
            lines.drop(1).forEach { line ->
                val fields = line.split(Regex("\\s+")); require(fields.size >= 3)
                val row = fields[0].toInt() - 1; val column = fields[1].toInt() - 1; val value = fields[2]
                require(row in matrix.indices && column in matrix[0].indices && value.toDoubleOrNull() != null)
                matrix[row][column] = value
            }
        } else {
            val values = lines.drop(1).flatMap { it.split(Regex("\\s+")).filter(String::isNotBlank) }
            require(values.size == rowCount * columnCount && values.all { it.toDoubleOrNull() != null })
            values.forEachIndexed { index, value -> matrix[index / columnCount][index % columnCount] = value }
        }
        val expression = matrix.joinToString(separator = ",", prefix = "[", postfix = "]") { it.joinToString(separator = ",", prefix = "[", postfix = "]") }
        val data = CasImportedData(safeName(name), expression, rowCount, columnCount, source = CasInputModality.StructuredFile)
        return CasStructuredFileResult(data, if (coordinate) "Matrix Market coordinate" else "Matrix Market array", listOf("Missing coordinate entries were filled with exact zero."), matrix.take(5))
    }

    private fun parseDelimitedRow(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>(); val cell = StringBuilder(); var quoted = false; var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && line.getOrNull(index + 1) == '"' -> { cell.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> { cells += cell.toString().trim(); cell.clear() }
                else -> cell.append(char)
            }
            index++
        }
        require(!quoted) { "Unclosed quoted field in imported data." }
        cells += cell.toString().trim(); return cells
    }

    private fun safeName(name: String) = name.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_]"), "_").ifBlank { "data" }
}

data class CasDimensionReport(val valid: Boolean, val dimension: String?, val message: String)

object CasDimensionalAnalyzer {
    private val dimensions = mapOf(
        "m" to "length", "cm" to "length", "km" to "length", "s" to "time", "min" to "time", "h" to "time",
        "kg" to "mass", "g" to "mass", "rad" to "angle", "deg" to "angle", "k" to "temperature", "mol" to "amount",
    )

    fun analyze(source: String): CasDimensionReport {
        val additiveParts = source.lowercase().split(Regex("(?<![eE])[+-]")).map { part ->
            Regex("(?<![A-Za-z])(?:m|cm|km|s|min|h|kg|g|rad|deg|k|mol)(?![A-Za-z])").find(part)?.value
        }.filterNotNull()
        if (additiveParts.size < 2) {
            val unit = Regex("(?<![A-Za-z])(?:m|cm|km|s|min|h|kg|g|rad|deg|k|mol)(?![A-Za-z])").find(source.lowercase())?.value
            return CasDimensionReport(true, unit?.let(dimensions::get), unit?.let { "Dimension: ${dimensions[it]}" } ?: "Dimensionless or symbolic input.")
        }
        val found = additiveParts.mapNotNull(dimensions::get).distinct()
        return if (found.size <= 1) CasDimensionReport(true, found.firstOrNull(), "Additive terms are dimensionally compatible.")
        else CasDimensionReport(false, null, "Cannot add or subtract ${found.joinToString(" and ")} quantities.")
    }
}

class CasNotebookInteractionEngine(
    private val interaction: CasInteractionEngine = CasInteractionEngine(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
) {
    fun evaluate(
        session: CasNotebookSession,
        source: String,
        interpretation: CasInterpretationOption,
        method: CasSolutionMethod,
        rowId: String = "cas-${session.rows.size + 1}",
    ): CasNotebookSession {
        require(session.rows.none { it.id == rowId })
        val substituted = substituteScoped(interpretation.expression, session.visibleVariables())
        val resolved = interpretation.copy(expression = substituted)
        val preview = interaction.interpret(resolved.expression, resolved.operation, session.assumptions)
            .copy(operation = resolved.operation, variable = resolved.variable, expression = resolved.expression)
        val evaluated = interaction.evaluate(preview, session.assumptions, method)
        val domainReport = CasDomainBranchAnalyzer.analyze(resolved.expression, session.assumptions)
        val domainSteps = buildList {
            if (domainReport.excluded.isNotEmpty()) add(CasStep("Domain exclusions", domainReport.excluded.joinToString(), "Values that make a denominator zero remain excluded throughout the notebook."))
            domainReport.branches.forEach { branch -> add(CasStep("Branch analysis", branch.expression, "${branch.condition}: ${branch.consequence}")) }
        }
        val result = evaluated.copy(
            assumptions = (evaluated.assumptions + domainReport.descriptions).distinct(),
            steps = evaluated.steps + domainSteps,
        )
        val row = CasInteractiveRow(
            rowId, source, resolved, method, result, CasPodBuilder.build(result, resolved),
            result.steps.mapIndexed { index, step -> CasProgressiveHint(index + 1, "Hint ${index + 1}", hintFor(step, index)) },
            result.steps.mapIndexedNotNull { index, step ->
                session.assumptions.descriptions.takeIf { it.isNotEmpty() && (step.title == "Assumptions" || step.explanation.contains("assum", true)) }
                    ?.let { CasStepAssumptionWarning(index, it, "This transformation depends on ${it.joinToString()}.") }
            },
        )
        return session.copy(rows = session.rows + row)
    }

    fun define(session: CasNotebookSession, variable: CasScopedVariable) = session.copy(variables = session.variables.filterNot { it.name == variable.name && it.scope == variable.scope && it.rowId == variable.rowId } + variable)
    fun import(session: CasNotebookSession, data: CasImportedData) = define(session.copy(imports = session.imports + data), CasScopedVariable(data.name, data.matrixExpression, CasVariableScope.Notebook))
    fun pin(session: CasNotebookSession, rowId: String) = session.copy(rows = session.rows.map { if (it.id == rowId) it.copy(pinned = !it.pinned) else it })
    fun annotate(session: CasNotebookSession, rowId: String, note: String) = session.copy(rows = session.rows.map { if (it.id == rowId) it.copy(annotation = note.take(2000)) else it })
    fun compare(session: CasNotebookSession, rowId: String) = session.copy(compareRowIds = (session.compareRowIds xor setOf(rowId)).take(4).toSet())
    fun transfer(session: CasNotebookSession, transfer: CasExpressionTransfer, targetRowId: String?): String {
        require(session.rows.any { it.id == transfer.sourceRowId })
        require(targetRowId == null || session.rows.any { it.id == targetRowId })
        return transfer.expression
    }

    fun subexpressions(source: String): List<CasSubexpressionTarget> {
        val targets = mutableListOf<CasSubexpressionTarget>(); val stack = ArrayDeque<Int>()
        source.forEachIndexed { index, char ->
            if (char == '(') stack.addLast(index)
            if (char == ')' && stack.isNotEmpty()) {
                val start = stack.removeLast(); if (index > start + 1) targets += target(source, start + 1, index)
            }
        }
        Regex("[A-Za-z][A-Za-z0-9_]*(?:\\^[0-9]+)?|(?:\\d+(?:\\.\\d+)?)").findAll(source).forEach { targets += target(source, it.range.first, it.range.last + 1) }
        return targets.distinctBy { it.start to it.endExclusive }.sortedWith(compareBy({ it.start }, { it.endExclusive - it.start }))
    }

    fun applyTarget(source: String, target: CasSubexpressionTarget, action: CasTargetAction, variable: String = "x", replacement: String = "0"): String {
        require(target.start >= 0 && target.endExclusive <= source.length && source.substring(target.start, target.endExclusive) == target.text)
        val transformed = when (action) {
            CasTargetAction.Simplify -> cas.simplify(target.text).exact
            CasTargetAction.Factor -> cas.factor(target.text, variable).exact
            CasTargetAction.Substitute -> cas.substitute(target.text, mapOf(variable to replacement)).exact
            CasTargetAction.Differentiate -> cas.derivative(target.text, variable).exact
        }
        return source.replaceRange(target.start, target.endExclusive, transformed)
    }

    private fun substituteScoped(source: String, variables: Map<String, String>): String = variables.entries.fold(source) { current, (name, expression) ->
        current.replace(Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])"), "($expression)")
    }

    private fun target(source: String, start: Int, end: Int) = CasSubexpressionTarget(
        "expr-$start-$end", source.substring(start, end), start, end,
        setOf(CasTargetAction.Simplify, CasTargetAction.Factor, CasTargetAction.Substitute, CasTargetAction.Differentiate),
    )

    private fun hintFor(step: CasStep, index: Int) = when (index) {
        0 -> "Choose a rule that matches the outermost structure; the method is ${step.expression}."
        else -> "Next, ${step.title.lowercase(Locale.ROOT)} without changing the expression's domain."
    }
}

object CasPodBuilder {
    fun build(result: CasRow, interpretation: CasInterpretationOption): List<CasResultPod> = buildList {
        add(CasResultPod(CasPodKind.Exact, "Exact result", result.exact))
        result.decimal?.let { add(CasResultPod(CasPodKind.Decimal, "Decimal approximation", it)) }
        add(CasResultPod(CasPodKind.Domain, "Domain and assumptions", result.assumptions.joinToString().ifBlank { "No additional assumptions" }, listOf("Variable: ${interpretation.variable}")))
        add(CasResultPod(CasPodKind.Alternate, "Alternate forms", result.exact, alternateForms(result.exact)))
        add(CasResultPod(CasPodKind.Verification, "Verification", if (result.supported) "Symbolic evaluation completed" else "Not verified", result.steps.takeLast(2).map { it.explanation }))
        CasSolutionStructureAnalyzer.analyze(result).let { structure ->
            val details = buildList {
                if (structure.parameters.isNotEmpty()) add("Free parameters: ${structure.parameters.joinToString()}")
                addAll(structure.conditions)
                if (structure.branches.isNotEmpty()) addAll(structure.branches.mapIndexed { index, branch -> "Branch ${index + 1}: $branch" })
                if (structure.exactAlgebraic) add("RootOf objects preserve exact algebraic values without decimalizing them.")
            }
            if (details.isNotEmpty()) add(CasResultPod(CasPodKind.Conditions, "Conditions and solution families", details.first(), details.drop(1)))
        }
        if (interpretation.variable.isNotBlank() && (interpretation.expression.contains(interpretation.variable) || result.exact.contains(interpretation.variable))) {
            add(CasResultPod(CasPodKind.Plot, "Automatic plot", "y = ${result.exact}", listOf("Independent variable: ${interpretation.variable}", "Open beside the symbolic result")))
            add(CasResultPod(CasPodKind.Geometry, "Geometric interpretation", geometryLabel(interpretation, result)))
        }
    }

    private fun alternateForms(exact: String) = listOf(exact.replace("*", " · "), exact.replace("^2", "²")).distinct()
    private fun geometryLabel(interpretation: CasInterpretationOption, result: CasRow) = when (interpretation.operation) {
        "derivative" -> "Slope field / tangent function: ${result.exact}"
        "integral" -> "Signed area represented by: ${result.exact}"
        "system", "nonlinear system" -> "Intersections of the system's curves"
        else -> "Graph of ${result.exact}"
    }
}

object CasExportEngine {
    fun export(row: CasInteractiveRow, format: CasExportFormat): String = when (format) {
        CasExportFormat.Latex -> toLatex(row.result.exact)
        CasExportFormat.MathMl -> "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mtext>${xml(row.result.exact)}</mtext></math>"
        CasExportFormat.Svg -> """<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="300" viewBox="0 0 1200 300"><rect width="1200" height="300" rx="28" fill="#101824"/><text x="56" y="82" fill="#67e8f9" font-family="sans-serif" font-size="30">${xml(row.interpretation.operation)}</text><text x="56" y="178" fill="#f8fafc" font-family="serif" font-size="56">${xml(row.result.exact)}</text><text x="56" y="245" fill="#a78bfa" font-family="sans-serif" font-size="22">${xml(row.result.assumptions.joinToString().ifBlank { "No assumptions" })}</text></svg>"""
        CasExportFormat.Png -> "CAS PNG render: ${row.result.exact}"
        CasExportFormat.Json -> "{\"id\":\"${json(row.id)}\",\"input\":\"${json(row.source)}\",\"operation\":\"${json(row.result.operation)}\",\"exact\":\"${json(row.result.exact)}\",\"decimal\":${row.result.decimal?.let { "\"${json(it)}\"" } ?: "null"},\"assumptions\":[${row.result.assumptions.joinToString { "\"${json(it)}\"" }}]}"
        CasExportFormat.Csv -> "field,value\ninput,\"${csv(row.source)}\"\noperation,\"${csv(row.result.operation)}\"\nexact,\"${csv(row.result.exact)}\"\ndecimal,\"${csv(row.result.decimal.orEmpty())}\""
    }

    private fun toLatex(source: String): String {
        var value = source.replace("*", " \\cdot ").replace(Regex("sqrt\\(([^()]*)\\)"), "\\\\sqrt{$1}")
        value = value.replace(Regex("([A-Za-z0-9)]+)\\^([A-Za-z0-9+-]+)"), "$1^{$2}")
        return value
    }
    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun json(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    private fun csv(value: String) = value.replace("\"", "\"\"")
}

private infix fun <T> Set<T>.xor(other: Set<T>): Set<T> = (this - other) + (other - this)
