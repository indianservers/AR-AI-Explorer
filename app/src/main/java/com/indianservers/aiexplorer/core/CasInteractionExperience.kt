package com.indianservers.aiexplorer.core

enum class CasSolutionMethod(val label: String, val explanation: String) {
    Auto("Best method", "Select a deterministic method from the interpreted operation."),
    Direct("Direct rules", "Apply structural identities directly to the symbolic tree."),
    ExpandFirst("Expand first", "Expand products and powers before the requested operation."),
    FactorFirst("Factor first", "Expose polynomial factors before simplifying or solving."),
    Elimination("Elimination", "Use exact row operations for a system or matrix."),
    Substitution("Substitution", "Isolate a variable and substitute it into the remaining relations."),
    Verify("Verified result", "Compute exactly, then attach an independent verification strategy."),
}

enum class CasKeyboardLayer(val label: String) {
    Basic("123"), Algebra("Algebra"), Calculus("Calculus"), Matrix("Matrix"), Advanced("Advanced"),
    Structures("Structures"), Units("Units"), Relations("Relations"), Greek("Greek")
}
data class CasKeyboardKey(val label: String, val insertion: String, val cursorBack: Int = 0, val description: String = label)

object CasKeyboardCatalog {
    val layers: Map<CasKeyboardLayer, List<CasKeyboardKey>> = mapOf(
        CasKeyboardLayer.Basic to listOf(
            CasKeyboardKey("7", "7"), CasKeyboardKey("8", "8"), CasKeyboardKey("9", "9"), CasKeyboardKey("÷", "/"),
            CasKeyboardKey("4", "4"), CasKeyboardKey("5", "5"), CasKeyboardKey("6", "6"), CasKeyboardKey("×", "*"),
            CasKeyboardKey("1", "1"), CasKeyboardKey("2", "2"), CasKeyboardKey("3", "3"), CasKeyboardKey("−", "-"),
            CasKeyboardKey("0", "0"), CasKeyboardKey(".", "."), CasKeyboardKey("+", "+"), CasKeyboardKey("=", "="),
            CasKeyboardKey("(", "("), CasKeyboardKey(")", ")"), CasKeyboardKey("π", "pi"), CasKeyboardKey("e", "e"),
        ),
        CasKeyboardLayer.Algebra to listOf(
            CasKeyboardKey("x", "x"), CasKeyboardKey("y", "y"), CasKeyboardKey("a", "a"), CasKeyboardKey("b", "b"),
            CasKeyboardKey("x²", "^2"), CasKeyboardKey("xⁿ", "^()", 1), CasKeyboardKey("√", "sqrt()", 1), CasKeyboardKey("|x|", "abs()", 1),
            CasKeyboardKey("factor", "factor "), CasKeyboardKey("expand", "expand "), CasKeyboardKey("simplify", "simplify "),
            CasKeyboardKey("fraction", "()/()", 4), CasKeyboardKey("system", "{x+y=1; x-y=3}", 0),
        ),
        CasKeyboardLayer.Calculus to listOf(
            CasKeyboardKey("d/dx", "derivative "), CasKeyboardKey("∂/∂x", "partial derivative "),
            CasKeyboardKey("∫", "integral "), CasKeyboardKey("∫ₐᵇ", "integral  from 0 to 1", 12),
            CasKeyboardKey("lim", "limit  as x -> 0", 10), CasKeyboardKey("Σ", "sum(,x,1,n)", 8), CasKeyboardKey("Π", "product(,x,1,n)", 8),
            CasKeyboardKey("sin", "sin()", 1), CasKeyboardKey("cos", "cos()", 1), CasKeyboardKey("tan", "tan()", 1),
            CasKeyboardKey("ln", "ln()", 1), CasKeyboardKey("exp", "exp()", 1), CasKeyboardKey("ODE", "y'=a*y+b"),
            CasKeyboardKey("Laplace", "laplace "), CasKeyboardKey("L^-1", "inverse laplace "), CasKeyboardKey("Z", "z transform "),
        ),
        CasKeyboardLayer.Matrix to listOf(
            CasKeyboardKey("2×2", "[[1,0],[0,1]]"), CasKeyboardKey("3×3", "[[1,0,0],[0,1,0],[0,0,1]]"),
            CasKeyboardKey("det", "determinant "), CasKeyboardKey("rref", "rref "), CasKeyboardKey("A⁻¹", "inverse "),
            CasKeyboardKey("eigen", "eigenvalues "), CasKeyboardKey("LU", "lu "), CasKeyboardKey("QR", "qr "), CasKeyboardKey("Chol", "cholesky "),
            CasKeyboardKey("transpose", "transpose "), CasKeyboardKey("·", "*"),
            CasKeyboardKey("[", "["), CasKeyboardKey("]", "]"), CasKeyboardKey(",", ","), CasKeyboardKey(";", ";"),
        ),
        CasKeyboardLayer.Advanced to listOf(
            CasKeyboardKey("series", "series(,x,0,6)", 8), CasKeyboardKey("asymptotic", "asymptotic "),
            CasKeyboardKey("Fourier", "fourier "), CasKeyboardKey("Z", "z transform "),
            CasKeyboardKey("residue", "residue(,z,0)", 5), CasKeyboardKey("contour", "contour integral residue(,z,0)", 5),
            CasKeyboardKey("Gamma", "gamma()", 1), CasKeyboardKey("zeta", "zeta()", 1),
            CasKeyboardKey("gcd", "gcd(,)", 2), CasKeyboardKey("mod inverse", "modInverse(,)", 2),
            CasKeyboardKey("recurrence", "a(n)=r*a(n-1)+b, a(0)=c"), CasKeyboardKey("PDE", "u_xx+u_yy=0"),
            CasKeyboardKey("optimize", "minimize(a*x^2+b*x+c)"), CasKeyboardKey("RootOf", "exact roots "),
            CasKeyboardKey("Jordan", "jordan form "), CasKeyboardKey("SVD", "svd "),
        ),
        CasKeyboardLayer.Structures to listOf(
            CasKeyboardKey("piecewise", "piecewise({condition:value, otherwise:value})", 30),
            CasKeyboardKey("2x2 matrix", "[[a,b],[c,d]]"), CasKeyboardKey("3x3 matrix", "[[a,b,c],[d,e,f],[g,h,i]]"),
            CasKeyboardKey("vector", "<x,y,z>"), CasKeyboardKey("system", "{equation1; equation2}", 12),
            CasKeyboardKey("sum", "sum(expression,k,1,n)", 17), CasKeyboardKey("product", "product(expression,k,1,n)", 17),
            CasKeyboardKey("limit", "limit(expression,x,0)", 15), CasKeyboardKey("integral", "integral(expression,x)", 2),
            CasKeyboardKey("def integral", "integral(expression,x,a,b)", 4), CasKeyboardKey("derivative", "derivative(expression,x)", 2),
            CasKeyboardKey("cases row", "condition:value"), CasKeyboardKey("otherwise", "otherwise:value"),
        ),
        CasKeyboardLayer.Units to listOf(
            CasKeyboardKey("m", " m"), CasKeyboardKey("cm", " cm"), CasKeyboardKey("km", " km"), CasKeyboardKey("s", " s"),
            CasKeyboardKey("min", " min"), CasKeyboardKey("h", " h"), CasKeyboardKey("kg", " kg"), CasKeyboardKey("g", " g"),
            CasKeyboardKey("mol", " mol"), CasKeyboardKey("K", " K"), CasKeyboardKey("rad", " rad"), CasKeyboardKey("deg", " deg"),
            CasKeyboardKey("m/s", " m/s"), CasKeyboardKey("m^2", " m^2"), CasKeyboardKey("m^3", " m^3"), CasKeyboardKey("convert", "convert  to ", 4),
        ),
        CasKeyboardLayer.Relations to listOf(
            CasKeyboardKey("=", "="), CasKeyboardKey("≠", "!="), CasKeyboardKey("<", "<"), CasKeyboardKey(">", ">"),
            CasKeyboardKey("≤", "<="), CasKeyboardKey("≥", ">="), CasKeyboardKey("→", "->"), CasKeyboardKey("∞", "infinity"),
            CasKeyboardKey("∈", " in "), CasKeyboardKey("and", " and "), CasKeyboardKey("or", " or "), CasKeyboardKey("assuming", " assuming "),
        ),
        CasKeyboardLayer.Greek to (
            "αβγδεζηθικλμνξοπρστυφχψω".map { CasKeyboardKey(it.toString(), it.toString()) } +
                listOf(CasKeyboardKey("Γ", "Γ"), CasKeyboardKey("Δ", "Δ"), CasKeyboardKey("Θ", "Θ"), CasKeyboardKey("Λ", "Λ"), CasKeyboardKey("Σ", "Σ"), CasKeyboardKey("Ω", "Ω"))
        ),
    )
}

data class CasInterpretationPreview(
    val original: String,
    val expression: String,
    val operation: String,
    val variable: String,
    val intent: MathInputIntent,
    val confidence: Double,
    val assumptionsUsed: List<String>,
    val explanation: String,
    val warnings: List<String>,
)

data class CasAssumptionDraft(
    val variable: String = "x",
    val domain: MathNumberDomain = MathNumberDomain.Real,
    val positive: Boolean = false,
    val nonNegative: Boolean = false,
    val nonZero: Boolean = false,
    val minimum: String = "",
    val maximum: String = "",
) {
    fun build(): VariableAssumption {
        val cleanVariable = variable.trim()
        val cleanMinimum = minimum.trim(); val cleanMaximum = maximum.trim()
        val parsedMinimum = cleanMinimum.takeIf(String::isNotEmpty)?.toDoubleOrNull()
        val parsedMaximum = cleanMaximum.takeIf(String::isNotEmpty)?.toDoubleOrNull()
        require(cleanVariable.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) { "Use a variable such as x, y or theta." }
        require(cleanMinimum.isEmpty() || parsedMinimum != null) { "Minimum must be numeric." }
        require(cleanMaximum.isEmpty() || parsedMaximum != null) { "Maximum must be numeric." }
        require(parsedMinimum == null || parsedMaximum == null || parsedMinimum <= parsedMaximum) { "Minimum cannot exceed maximum." }
        require(!(positive && nonNegative)) { "Choose positive or non-negative, not both." }
        require(domain != MathNumberDomain.Complex || (!positive && !nonNegative && parsedMinimum == null && parsedMaximum == null)) {
            "Complex variables do not have order, so sign and range assumptions are unavailable."
        }
        require(!positive || parsedMaximum == null || parsedMaximum > 0.0) { "A positive variable needs a maximum above zero." }
        require(!nonNegative || parsedMaximum == null || parsedMaximum >= 0.0) { "A non-negative variable needs a maximum of at least zero." }
        return VariableAssumption(cleanVariable, domain, positive, nonNegative, nonZero, parsedMinimum, parsedMaximum)
    }
}

class CasInteractionEngine(private val cas: SymbolicCasEngine = SymbolicCasEngine()) {
    fun interpret(source: String, selectedOperation: String? = null, assumptions: MathAssumptionSet = MathAssumptionSet()): CasInterpretationPreview {
        val analysis = MathInputIntelligence.analyze(source); val trimmed = source.trim(); val firstWord = trimmed.substringBefore(' ').lowercase()
        val recognizedPrefix = operationPrefixes.sortedByDescending(String::length).firstOrNull { prefix ->
            trimmed.equals(prefix, ignoreCase = true) || trimmed.startsWith("$prefix ", ignoreCase = true)
        }
        val operation = recognizedPrefix?.let { aliases[it] ?: it }
            ?: selectedOperation?.takeIf { it.isNotBlank() }
            ?: operationFor(firstWord, analysis.intent)
        val expression = recognizedPrefix?.let { trimmed.drop(it.length).trim() }?.takeIf(String::isNotBlank) ?: trimmed
        val variables = analysis.variables.filterNot { it.lowercase() in MathInputIntelligence.units }.toList()
        val variable = when { "x" in variables -> "x"; variables.isNotEmpty() -> variables.first(); else -> "x" }
        val warnings = buildList {
            if (!analysis.validBrackets) add(analysis.message)
            if (analysis.intent == MathInputIntent.NaturalLanguage && recognizedPrefix == null) add("Natural language remains editable; confirm the operation before evaluating.")
            if (operation == "limit" && !trimmed.contains("->") && !trimmed.contains("approach", true)) add("No approach value was found; the current CAS default is 0.")
        }
        return CasInterpretationPreview(
            source, expression, operation, variable, analysis.intent, analysis.confidence, assumptions.descriptions,
            "Interpret ‘$expression’ as ${analysis.intent.label.lowercase()} and apply $operation with respect to $variable.", warnings,
        )
    }

    fun availableMethods(operation: String): List<CasSolutionMethod> = when (operation.lowercase()) {
        "simplify" -> listOf(CasSolutionMethod.Auto, CasSolutionMethod.Direct, CasSolutionMethod.ExpandFirst, CasSolutionMethod.FactorFirst, CasSolutionMethod.Verify)
        "factor", "partial fractions" -> listOf(CasSolutionMethod.Auto, CasSolutionMethod.Direct, CasSolutionMethod.ExpandFirst, CasSolutionMethod.Verify)
        "system", "nonlinear system", "inequalities" -> listOf(CasSolutionMethod.Auto, CasSolutionMethod.Elimination, CasSolutionMethod.Substitution, CasSolutionMethod.Verify)
        "determinant", "rref", "rank", "nullspace", "matrix inverse", "transpose", "eigenvalues", "eigenvectors", "jordan", "jordan form", "svd", "lu", "qr", "cholesky" -> listOf(CasSolutionMethod.Auto, CasSolutionMethod.Elimination, CasSolutionMethod.Verify)
        else -> listOf(CasSolutionMethod.Auto, CasSolutionMethod.Direct, CasSolutionMethod.Verify)
    }

    fun evaluate(preview: CasInterpretationPreview, assumptions: MathAssumptionSet, method: CasSolutionMethod): CasRow {
        val chosen = if (method == CasSolutionMethod.Auto) defaultMethod(preview.operation) else method
        val prepared = when (chosen) {
            CasSolutionMethod.ExpandFirst -> cas.expand(preview.expression).takeIf { it.supported }?.exact ?: preview.expression
            CasSolutionMethod.FactorFirst -> cas.factor(preview.expression, preview.variable).takeIf { it.supported }?.exact ?: preview.expression
            else -> preview.expression
        }
        var result = when (preview.operation.lowercase()) {
            "simplify" -> when (chosen) {
                CasSolutionMethod.FactorFirst -> cas.factor(preview.expression, preview.variable)
                else -> cas.simplify(prepared, assumptions)
            }
            else -> cas.casRow(prepared, preview.operation)
        }
        val methodStep = CasStep("Method", chosen.label, chosen.explanation)
        val assumptionStep = assumptions.descriptions.takeIf { it.isNotEmpty() }?.let { CasStep("Assumptions", it.joinToString(), "These domain facts are available to guarded symbolic identities.") }
        val verificationStep = if (chosen == CasSolutionMethod.Verify) CasStep("Independent check", result.exact, "Substitute, differentiate, expand or row-check the result according to the operation before accepting it.") else null
        result = result.copy(
            assumptions = (result.assumptions + assumptions.descriptions).distinct(),
            steps = listOfNotNull(methodStep, assumptionStep) + result.steps + listOfNotNull(verificationStep),
        )
        return result
    }

    private fun defaultMethod(operation: String) = when (operation.lowercase()) {
        "system", "nonlinear system", "inequalities", "determinant", "rref", "eigenvalues", "lu", "qr", "cholesky" -> CasSolutionMethod.Elimination
        "factor", "partial fractions" -> CasSolutionMethod.ExpandFirst
        else -> CasSolutionMethod.Direct
    }

    private fun operationFor(firstWord: String, intent: MathInputIntent): String = when {
        firstWord in operationPrefixes -> aliases[firstWord] ?: firstWord
        intent == MathInputIntent.Calculus -> "derivative"
        intent == MathInputIntent.LinearAlgebra -> "simplify"
        intent == MathInputIntent.Inequality -> "inequalities"
        else -> "simplify"
    }

    private companion object {
        val aliases = mapOf("differentiate" to "derivative", "integrate" to "integral", "solve" to "simplify", "det" to "determinant", "ilaplace" to "inverse laplace")
        val operationPrefixes = setOf("simplify", "expand", "factor", "partial fractions", "differentiate", "derivative", "integrate", "integral", "limit", "determinant", "det", "rref", "rank", "nullspace", "matrix inverse", "transpose", "eigenvalues", "eigenvectors", "jordan form", "svd", "ode", "nonlinear ode", "higher ode", "nonlinear system", "pde", "series", "asymptotic", "laplace", "inverse laplace", "ilaplace", "fourier", "inverse fourier", "z transform", "sum", "product", "residue", "contour integral", "special functions", "number theory", "finite algebra", "optimization", "recurrence", "exact roots", "domain", "branch analysis", "lu", "plu", "qr", "cholesky")
    }
}
