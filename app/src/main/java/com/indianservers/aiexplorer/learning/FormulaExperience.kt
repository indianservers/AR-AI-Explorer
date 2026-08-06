package com.indianservers.aiexplorer.learning

import com.indianservers.aiexplorer.core.ExpressionEngine
import kotlin.math.abs

enum class FormulaPurpose(val label: String) {
    Area("Area"),
    Perimeter("Perimeter"),
    Volume("Volume"),
    Distance("Distance"),
    Angle("Angle"),
    Equation("Solve"),
    Rate("Rate"),
    Accumulation("Accumulation"),
    Probability("Probability"),
    Statistics("Statistics"),
    Transform("Transform"),
    Identity("Identity"),
    Other("Other"),
}

enum class FormulaDimension(val label: String, val baseUnit: String) {
    Scalar("Scalar", ""),
    Length("Length", "m"),
    Area("Area", "m^2"),
    Volume("Volume", "m^3"),
    Time("Time", "s"),
    Angle("Angle", "deg"),
    Probability("Probability", ""),
}

data class FormulaVariableSpec(
    val symbol: String,
    val label: String,
    val dimension: FormulaDimension,
    val defaultInput: String,
    val minimum: Double = 0.0,
    val maximum: Double = 100.0,
)

data class FormulaExample(
    val prompt: String,
    val substitution: String,
    val result: String,
)

data class FormulaExperience(
    val formula: FormulaCard,
    val purpose: FormulaPurpose,
    val outputSymbol: String?,
    val outputDimension: FormulaDimension,
    val variables: List<FormulaVariableSpec>,
    val whenToUse: String,
    val assumptions: List<String>,
    val limitations: List<String>,
    val derivation: List<String>,
    val examples: List<FormulaExample>,
    val rearrangements: List<String>,
    val equivalentForms: List<Pair<String, String>>,
    val relatedFormulaIds: List<String>,
    val workspace: MathModuleTarget,
)

data class FormulaFilterSet(
    val purpose: FormulaPurpose? = null,
    val outputSymbol: String? = null,
    val requiredVariable: String? = null,
    val dimension: FormulaDimension? = null,
    val level: KnowledgeLevel? = null,
)

data class FormulaCalculation(
    val valid: Boolean,
    val result: Double? = null,
    val resultUnit: String = "",
    val substitution: String = "",
    val messages: List<String> = emptyList(),
)

object FormulaExperienceEngine {
    fun details(formula: FormulaCard, catalogue: List<FormulaCard> = MathKnowledgeCatalog.formulas): FormulaExperience {
        val purpose = purposeOf(formula)
        val output = equationSide(formula.expression)?.first
        val outputDimension = dimensionForOutput(formula, purpose)
        val variables = formula.variables
            .distinct()
            .filterNot { normalizeSymbol(it) == normalizeSymbol(output.orEmpty()) }
            .map { variableSpec(it, formula) }
        val workspace = when (formula.topic) {
            KnowledgeTopic.Geometry -> if ("3d" in formula.tags || purpose == FormulaPurpose.Volume) MathModuleTarget.Graph3D else MathModuleTarget.Geometry2D
            KnowledgeTopic.Algebra, KnowledgeTopic.Calculus -> MathModuleTarget.Graph2D
            KnowledgeTopic.Probability, KnowledgeTopic.Statistics -> MathModuleTarget.Statistics
        }
        val related = catalogue
            .asSequence()
            .filter { it.id != formula.id }
            .map { candidate ->
                val sharedTags = candidate.tags.intersect(formula.tags.toSet()).size
                val sharedVariables = candidate.variables.intersect(formula.variables.toSet()).size
                candidate to (sharedTags * 3 + sharedVariables + if (candidate.subcategory == formula.subcategory) 4 else 0)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(8)
            .map { it.first.id }
            .toList()
        val assumptions = assumptions(formula, purpose)
        val limitations = limitations(formula, purpose)
        val derivation = derivation(formula, purpose)
        val rearrangements = rearrangements(formula, output)
        val equivalents = buildList {
            add(
            "Readable" to readableFormula(formula.expression),
            )
            add("Exact LaTeX" to formula.expression)
            add("Substitution template" to substitutionTemplate(formula))
            if (formula.category == FormulaCategory.AlgebraFunctions) {
                add("Expanded or factored form" to "Open the linked algebra workspace to expand, factor, and verify equivalence.")
            }
            if (formula.category == FormulaCategory.LinearAlgebraVectors) {
                add("Vector or matrix form" to "Open the linked workspace to switch between component and matrix representations.")
            }
            add("Decimal form" to "Enter values in Calculate to obtain a validated decimal approximation.")
        }
        val provisional = FormulaExperience(
            formula = formula,
            purpose = purpose,
            outputSymbol = output,
            outputDimension = outputDimension,
            variables = variables,
            whenToUse = whenToUse(formula, purpose),
            assumptions = assumptions,
            limitations = limitations,
            derivation = derivation,
            examples = emptyList(),
            rearrangements = rearrangements,
            equivalentForms = equivalents,
            relatedFormulaIds = related,
            workspace = workspace,
        )
        return provisional.copy(examples = examples(provisional))
    }

    fun filter(formulas: List<FormulaCard>, query: String, filters: FormulaFilterSet): List<FormulaCard> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
        return formulas.filter { formula ->
            val detail = details(formula, formulas)
            val haystack = listOf(
                formula.title,
                formula.expression,
                formula.introduction,
                formula.useCase,
                formula.category.label,
                formula.subcategory,
                formula.tags.joinToString(),
                formula.variables.joinToString(),
                detail.purpose.label,
                detail.outputDimension.label,
                detail.whenToUse,
            ).joinToString(" ").lowercase()
            words.all(haystack::contains) &&
                (filters.purpose == null || detail.purpose == filters.purpose) &&
                (filters.outputSymbol == null || normalizeSymbol(detail.outputSymbol.orEmpty()) == normalizeSymbol(filters.outputSymbol)) &&
                (filters.requiredVariable == null || formula.variables.any { normalizeSymbol(it) == normalizeSymbol(filters.requiredVariable) }) &&
                (filters.dimension == null || detail.outputDimension == filters.dimension) &&
                (filters.level == null || formula.level == filters.level)
        }
    }

    fun calculate(experience: FormulaExperience, inputs: Map<String, String>): FormulaCalculation {
        val equation = equationSide(experience.formula.expression)
            ?: return FormulaCalculation(false, messages = listOf("This identity is explored symbolically; use its substitution preview or linked workspace."))
        val parsed = linkedMapOf<String, Double>()
        val issues = mutableListOf<String>()
        experience.variables.forEach { spec ->
            val raw = inputs[spec.symbol].orEmpty()
            val value = FormulaUnits.parse(raw, spec.dimension)
            if (value == null) issues += "${spec.symbol}: enter a number${if (spec.dimension.baseUnit.isBlank()) "" else " with an optional unit"}."
            else parsed[normalizeSymbol(spec.symbol)] = value
        }
        issues += validate(experience, parsed)
        if (issues.isNotEmpty()) return FormulaCalculation(false, messages = issues.distinct())
        val expression = runCatching { latexToExpression(equation.second) }
            .getOrElse { return FormulaCalculation(false, messages = listOf("This formula needs the symbolic solver rather than direct numeric substitution.")) }
        val result = runCatching { ExpressionEngine().compile(expression).eval(parsed) }
            .getOrElse { return FormulaCalculation(false, messages = listOf("Direct calculation is unavailable for this symbolic form. Open its linked workspace to evaluate it.")) }
        if (!result.isFinite()) return FormulaCalculation(false, messages = listOf("These values produce an undefined or infinite result."))
        val substitution = experience.variables.joinToString(", ") { "${it.symbol}=${inputs[it.symbol].orEmpty()}" }
        return FormulaCalculation(
            valid = true,
            result = result,
            resultUnit = experience.outputDimension.baseUnit,
            substitution = "${experience.outputSymbol ?: "result"} = ${format(result)}",
            messages = listOf("Calculated in compatible base units from $substitution."),
        )
    }

    fun practice(experience: FormulaExperience, seed: Int): FormulaExample {
        val values = experience.variables.associate { spec ->
            val value = ((abs(seed + spec.symbol.hashCode()) % 8) + 2).toDouble()
            spec.symbol to "$value${spec.dimension.baseUnit.takeIf(String::isNotBlank)?.let { " $it" }.orEmpty()}"
        }
        val calculation = calculate(experience, values)
        return FormulaExample(
            prompt = "Find ${experience.outputSymbol ?: "the result"} when ${values.entries.joinToString { "${it.key}=${it.value}" }}.",
            substitution = calculation.messages.firstOrNull().orEmpty(),
            result = calculation.substitution.ifBlank { "Use the formula conditions and simplify the symbolic expression." },
        )
    }

    fun exportText(experience: FormulaExperience): String = buildString {
        appendLine(experience.formula.title)
        appendLine(experience.formula.expression)
        appendLine(experience.formula.introduction)
        appendLine("When to use: ${experience.whenToUse}")
        appendLine("Variables: ${experience.variables.joinToString { "${it.symbol} (${it.label}, ${it.dimension.label})" }}")
        appendLine("Assumptions: ${experience.assumptions.joinToString()}")
        appendLine("Limitations: ${experience.limitations.joinToString()}")
        appendLine("Derivation:")
        experience.derivation.forEachIndexed { index, step -> appendLine("${index + 1}. $step") }
        appendLine("Worked examples:")
        experience.examples.forEach { appendLine("- ${it.prompt} ${it.result}") }
    }.trim()

    fun exportMathMl(formula: FormulaCard): String =
        "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mtext>${xmlEscape(readableFormula(formula.expression))}</mtext></math>"

    private fun purposeOf(formula: FormulaCard): FormulaPurpose = when {
        "area" in formula.tags -> FormulaPurpose.Area
        "perimeter" in formula.tags -> FormulaPurpose.Perimeter
        "volume" in formula.tags -> FormulaPurpose.Volume
        "distance" in formula.tags -> FormulaPurpose.Distance
        "angle" in formula.tags -> FormulaPurpose.Angle
        "derivative" in formula.tags -> FormulaPurpose.Rate
        "integral" in formula.tags -> FormulaPurpose.Accumulation
        "probability" in formula.tags -> FormulaPurpose.Probability
        "statistics" in formula.tags -> FormulaPurpose.Statistics
        "transform" in formula.tags || formula.category == FormulaCategory.LinearAlgebraVectors -> FormulaPurpose.Transform
        "identity" in formula.tags -> FormulaPurpose.Identity
        "equation" in formula.tags || '=' in formula.expression -> FormulaPurpose.Equation
        else -> FormulaPurpose.Other
    }

    private fun dimensionForOutput(formula: FormulaCard, purpose: FormulaPurpose) = when (purpose) {
        FormulaPurpose.Area -> FormulaDimension.Area
        FormulaPurpose.Perimeter, FormulaPurpose.Distance -> FormulaDimension.Length
        FormulaPurpose.Volume -> FormulaDimension.Volume
        FormulaPurpose.Angle -> FormulaDimension.Angle
        FormulaPurpose.Probability, FormulaPurpose.Statistics -> FormulaDimension.Probability
        else -> if (formula.variables.any { normalizeSymbol(it) in setOf("r", "h", "l", "d") }) FormulaDimension.Length else FormulaDimension.Scalar
    }

    private fun variableSpec(symbol: String, formula: FormulaCard): FormulaVariableSpec {
        val normalized = normalizeSymbol(symbol)
        val dimension = when {
            normalized in setOf("theta", "alpha", "beta", "gamma", "angle") -> FormulaDimension.Angle
            normalized in setOf("t", "time") -> FormulaDimension.Time
            normalized in setOf("p", "probability") && formula.topic == KnowledgeTopic.Probability -> FormulaDimension.Probability
            normalized in setOf("a", "b", "c", "r", "R", "h", "l", "d", "x", "y", "z", "s", "P") &&
                formula.category in setOf(FormulaCategory.GeometryMensuration, FormulaCategory.Trigonometry, FormulaCategory.CoordinateGeometry3D) -> FormulaDimension.Length
            else -> FormulaDimension.Scalar
        }
        val label = when (normalized.lowercase()) {
            "r" -> "radius"
            "h" -> "height"
            "b" -> "base or coefficient"
            "l", "ell" -> "length"
            "t" -> "time"
            "theta" -> "angle"
            "p" -> "probability or perimeter"
            "n" -> "count"
            else -> symbol
        }
        val default = when (dimension) {
            FormulaDimension.Length -> "5 m"
            FormulaDimension.Time -> "2 s"
            FormulaDimension.Angle -> "30 deg"
            FormulaDimension.Probability -> "0.5"
            else -> "2"
        }
        return FormulaVariableSpec(symbol, label, dimension, default)
    }

    private fun assumptions(formula: FormulaCard, purpose: FormulaPurpose): List<String> = buildList {
        if (formula.variables.isNotEmpty()) add("Every symbol must represent a quantity in a compatible unit system.")
        if (formula.variables.any { normalizeSymbol(it) in setOf("r", "h", "l", "d", "s") }) add("Geometric lengths are non-negative.")
        if (formula.variables.any { normalizeSymbol(it) == "n" }) add("n is an integer in the stated counting or sequence domain.")
        if (purpose == FormulaPurpose.Probability) add("Probabilities lie between 0 and 1 and the stated events define the sample space.")
        if (formula.category == FormulaCategory.Trigonometry) add("Angles use one consistent unit: degrees or radians.")
        if ("right" in formula.title.lowercase() || "pythagorean" in formula.title.lowercase()) add("The triangle is right-angled.")
        if (formula.expression.contains("\\frac")) add("Every denominator must be non-zero.")
        if (isEmpty()) add("The variables satisfy the definition stated by the formula.")
    }

    private fun limitations(formula: FormulaCard, purpose: FormulaPurpose): List<String> = buildList {
        add("Do not apply the formula when its assumptions or variable meanings do not match the problem.")
        if (purpose in setOf(FormulaPurpose.Area, FormulaPurpose.Perimeter, FormulaPurpose.Volume)) add("The model assumes ideal geometric shapes; measured objects may require tolerances.")
        if (formula.level != KnowledgeLevel.School) add("Advanced symbolic forms may require a numerical method or a linked specialist workspace.")
        if (formula.expression.contains("\\approx") || formula.title.contains("estimate", true)) add("This is an approximation whose error depends on the input range and method.")
    }

    private fun derivation(formula: FormulaCard, purpose: FormulaPurpose): List<String> {
        val first = when (formula.category) {
            FormulaCategory.GeometryMensuration, FormulaCategory.CoordinateGeometry3D -> "Start from the defining geometric measure and label every required dimension."
            FormulaCategory.Trigonometry -> "Start from a right-triangle, unit-circle, or triangle-law definition."
            FormulaCategory.AlgebraFunctions, FormulaCategory.ComplexNumbers -> "Start from the defining equation and apply equivalent algebraic transformations."
            FormulaCategory.CalculusAnalysis, FormulaCategory.DifferentialEquations, FormulaCategory.NumericalMethods -> "Start from the relevant limit, derivative, integral, or approximation definition."
            FormulaCategory.LinearAlgebraVectors -> "Start from the component, matrix, or inner-product definition."
            FormulaCategory.ProbabilityCombinatorics, FormulaCategory.StatisticsDistributions -> "Start from the sample space, counting rule, or statistical definition."
            FormulaCategory.NumberTheory -> "Start from the integer definition and preserve divisibility or congruence at each step."
        }
        val second = when (purpose) {
            FormulaPurpose.Area, FormulaPurpose.Volume -> "Decompose or accumulate standard pieces, then combine equal measures."
            FormulaPurpose.Identity -> "Transform one side only using established identities until it matches the other."
            FormulaPurpose.Rate -> "Form a change quotient and pass to the required limiting or differential form."
            FormulaPurpose.Accumulation -> "Partition the quantity, sum contributions, and pass to the integral form."
            else -> "Substitute the definitions, collect like quantities, and isolate the required relationship."
        }
        return listOf(first, second, "Simplify to obtain ${formula.expression}.", "Check dimensions, domain conditions, and a simple known case.")
    }

    private fun rearrangements(formula: FormulaCard, output: String?): List<String> {
        val known = when (formula.title.lowercase()) {
            "circle area" -> listOf("""A=\pi r^{2}""", """r=\sqrt{\frac{A}{\pi}}""")
            "circle circumference" -> listOf("""C=2\pi r""", """r=\frac{C}{2\pi}""")
            "triangle area" -> listOf("""A=\frac{1}{2}bh""", """b=\frac{2A}{h}""", """h=\frac{2A}{b}""")
            "pythagorean theorem" -> listOf("""c=\sqrt{a^{2}+b^{2}}""", """a=\sqrt{c^{2}-b^{2}}""", """b=\sqrt{c^{2}-a^{2}}""")
            "speed" -> listOf("""v=\frac{d}{t}""", """d=vt""", """t=\frac{d}{v}""")
            else -> emptyList()
        }
        return (known + listOfNotNull(output?.let { "Solve symbolically for $it from ${formula.expression}" })).distinct()
    }

    private fun examples(experience: FormulaExperience): List<FormulaExample> {
        val values = experience.variables.associate { it.symbol to it.defaultInput }
        val calculation = calculate(experience, values)
        val first = FormulaExample(
            prompt = "Use standard values: ${values.entries.joinToString { "${it.key}=${it.value}" }}.",
            substitution = calculation.messages.firstOrNull().orEmpty(),
            result = calculation.substitution.ifBlank { "Substitute the values and simplify while preserving the stated conditions." },
        )
        return listOf(
            first,
            FormulaExample(
                prompt = "Real-world use",
                substitution = experience.formula.useCase,
                result = "Identify the known quantities, convert units, then apply ${experience.formula.title}.",
            ),
        )
    }

    private fun whenToUse(formula: FormulaCard, purpose: FormulaPurpose) =
        "Use this ${purpose.label.lowercase()} relationship when the known quantities match ${formula.variables.joinToString().ifBlank { "its symbolic definitions" }} and ${formula.useCase.replaceFirstChar(Char::lowercase)}"

    private fun validate(experience: FormulaExperience, values: Map<String, Double>): List<String> = buildList {
        values.forEach { (symbol, value) ->
            val spec = experience.variables.firstOrNull { normalizeSymbol(it.symbol) == symbol }
            if (spec?.dimension in setOf(FormulaDimension.Length, FormulaDimension.Area, FormulaDimension.Volume, FormulaDimension.Time) && value < 0.0) {
                add("$symbol cannot be negative for this formula.")
            }
            if (spec?.dimension == FormulaDimension.Probability && value !in 0.0..1.0) add("$symbol must be between 0 and 1.")
            if (symbol == "n" && value != value.toInt().toDouble()) add("n must be an integer.")
        }
        if (experience.formula.expression.contains("\\frac") && values.values.any { abs(it) < 1e-12 }) {
            add("A zero input may make a denominator zero; check the formula before continuing.")
        }
    }

    private fun equationSide(expression: String): Pair<String, String>? {
        val index = expression.indexOf('=')
        if (index <= 0 || index == expression.lastIndex || expression.contains("\\equiv")) return null
        val left = readableFormula(expression.substring(0, index)).trim()
        if (!Regex("[A-Za-zΑ-ω][A-Za-z0-9_]*").matches(left)) return null
        return normalizeSymbol(left) to expression.substring(index + 1)
    }

    private fun latexToExpression(source: String): String {
        var text = source
            .replace("\\left", "")
            .replace("\\right", "")
            .replace("\\,", "")
            .replace("\\mathrm", "")
            .replace("\\pi", "pi")
            .replace("\\theta", "theta")
            .replace("\\cdot", "*")
            .replace("\\times", "*")
            .replace("\\sin", "sin")
            .replace("\\cos", "cos")
            .replace("\\tan", "tan")
        repeat(12) {
            text = Regex("""\\frac\{([^{}]+)}\{([^{}]+)}""").replace(text) { "((${it.groupValues[1]})/(${it.groupValues[2]}))" }
            text = Regex("""\\sqrt\{([^{}]+)}""").replace(text) { "sqrt(${it.groupValues[1]})" }
        }
        text = text
            .replace(Regex("""([A-Za-z])_\{?([A-Za-z0-9]+)}?""")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .replace(Regex("""\^\{([^{}]+)}""")) { "^(${it.groupValues[1]})" }
            .replace("{", "(")
            .replace("}", ")")
            .replace(Regex("""\\[A-Za-z]+"""), "")
        if ('\\' in text || "\\sum" in source || "\\int" in source || "\\prod" in source) error("Symbolic operator")
        return text
    }

    private fun substitutionTemplate(formula: FormulaCard) =
        formula.variables.joinToString(prefix = "${readableFormula(formula.expression)} where ", separator = ", ") { "$it = ?" }

    private fun readableFormula(source: String) = source
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\frac", "fraction")
        .replace("\\sqrt", "sqrt")
        .replace("\\pi", "pi")
        .replace("\\theta", "theta")
        .replace("\\cdot", "*")
        .replace("\\times", "*")
        .replace("\\", "")
        .replace("{", "(")
        .replace("}", ")")

    private fun normalizeSymbol(symbol: String) = symbol
        .replace("θ", "theta")
        .replace("\\theta", "theta")
        .replace(Regex("[^A-Za-z0-9_]"), "")

    private fun format(value: Double): String =
        if (abs(value - value.toLong()) < 1e-9) value.toLong().toString() else "%.6f".format(value).trimEnd('0').trimEnd('.')

    private fun xmlEscape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

object FormulaUnits {
    private val units = mapOf(
        "mm" to (FormulaDimension.Length to .001),
        "cm" to (FormulaDimension.Length to .01),
        "m" to (FormulaDimension.Length to 1.0),
        "km" to (FormulaDimension.Length to 1000.0),
        "s" to (FormulaDimension.Time to 1.0),
        "min" to (FormulaDimension.Time to 60.0),
        "h" to (FormulaDimension.Time to 3600.0),
        "deg" to (FormulaDimension.Angle to 1.0),
        "rad" to (FormulaDimension.Angle to (180.0 / Math.PI)),
        "%" to (FormulaDimension.Probability to .01),
    )

    fun parse(source: String, expected: FormulaDimension): Double? {
        val match = Regex("""^\s*([-+]?(?:\d+(?:\.\d*)?|\.\d+))\s*([A-Za-z%]+)?\s*$""").matchEntire(source) ?: return null
        val number = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].lowercase()
        if (unit.isBlank()) return number
        val definition = units[unit] ?: return null
        if (expected != FormulaDimension.Scalar && expected != FormulaDimension.Probability && definition.first != expected) return null
        return number * definition.second
    }
}
