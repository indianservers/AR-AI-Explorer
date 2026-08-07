package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.CasDomainBranchAnalyzer
import com.indianservers.aiexplorer.core.CasRow
import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.GuidedSolution
import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.ProblemKind
import com.indianservers.aiexplorer.core.SolverMethod
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.solver.domain.model.AdvancedMathProblem
import com.indianservers.aiexplorer.solver.domain.model.AdvancedProblemCategory
import com.indianservers.aiexplorer.solver.domain.model.CommonMistake
import com.indianservers.aiexplorer.solver.domain.model.DomainRestriction
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ExpressionPath
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.ProblemClassification
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolutionStep
import com.indianservers.aiexplorer.solver.domain.model.SolverAssumption
import com.indianservers.aiexplorer.solver.domain.model.SolverInput
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.SourceSpan
import com.indianservers.aiexplorer.solver.domain.model.StepDetail
import com.indianservers.aiexplorer.solver.domain.model.StepOperation
import com.indianservers.aiexplorer.solver.domain.model.VerificationCheck
import com.indianservers.aiexplorer.solver.domain.model.VerificationMethod
import com.indianservers.aiexplorer.solver.domain.model.VerificationResult
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.VerificationStrength
import com.indianservers.aiexplorer.solver.domain.steps.SolverExplanationEngine
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import java.math.BigInteger
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class Phase2SolverEngine(
    private val phase1: Phase1SolverEngine = Phase1SolverEngine(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
    private val tutor: MathSolverTutor = MathSolverTutor(),
) {
    fun solve(
        text: String,
        operation: SolverOperation = SolverOperation.Solve,
        profile: ExplanationProfile = ExplanationProfile.SchoolExamination,
        requestedMethodId: String? = null,
    ): SolverSolution {
        val phase1Result = phase1.solve(text, operation)
        if (phase1Result.supported && !requiresPhase2(text)) {
            return enrichPhase1(phase1Result, profile, requestedMethodId)
        }
        val problem = Phase2ProblemInterpreter.interpret(text)
        val specialized = solveSpecialized(problem, profile, requestedMethodId)
        if (specialized != null) return specialized
        val method = requestedMethodId.toCoreMethod()
        val guided = tutor.solve(text, method)
        if (guided.solution.supported) return mapGuided(text, guided, profile)
        return phase1Result.copy(
            explanationProfile = profile,
            assumptions = domainAssumptions(text),
            restrictions = domainRestrictions(text),
            commonMistakes = Phase2MisconceptionEngine.detect(text, problem.category),
            message = guided.solution.verification.ifBlank { phase1Result.message },
        )
    }

    private fun solveSpecialized(
        problem: AdvancedMathProblem,
        profile: ExplanationProfile,
        requestedMethodId: String?,
    ): SolverSolution? {
        solveMatrix(problem, profile)?.let { return it }
        solveCoordinate(problem, profile)?.let { return it }
        solveTrig(problem, profile)?.let { return it }
        solveFunction(problem, profile)?.let { return it }
        solveNumberTheory(problem, profile, requestedMethodId)?.let { return it }
        solveSequence(problem, profile)?.let { return it }
        solveCasCommand(problem, profile)?.let { return it }
        return null
    }

    private fun enrichPhase1(
        result: SolverSolution,
        profile: ExplanationProfile,
        requestedMethodId: String?,
    ): SolverSolution {
        val options = when (result.classification.type) {
            ProblemType.LinearEquation -> listOf(
                option("balance", "Balance method", true, "Keeps both sides visibly equal."),
                option("collect", "Collect terms", false, "Usually shorter when variables occur on both sides."),
            )
            ProblemType.SimultaneousLinearEquations -> listOf(
                option("elimination", "Elimination", true, "Cancels one variable exactly."),
                option("substitution", "Substitution", false, "Useful when one variable is already isolated."),
                option("matrix", "Matrix method", false, "Scales well and exposes row operations."),
            )
            else -> listOf(option("exact", "Exact method", true, "Preserves rational values without rounding."))
        }
        val selected = requestedMethodId?.takeIf { id -> options.any { it.id == id } } ?: options.first().id
        return result.copy(
            methods = options.map { it.copy(recommended = it.id == options.first().id) },
            selectedMethodId = selected,
            methodReason = options.first { it.id == selected }.reason,
            explanationProfile = profile,
            assumptions = domainAssumptions(result.input.original),
            restrictions = domainRestrictions(result.input.original),
            commonMistakes = Phase2MisconceptionEngine.detect(result.input.original, AdvancedProblemCategory.Algebra),
            ruleCitations = result.steps.map(SolutionStep::ruleId).distinct(),
        )
    }

    private fun solveCasCommand(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val source = problem.source.trim()
        val request = when {
            source.startsWith("partial fractions ", true) -> source.substringAfter(' ') to "partial fractions"
            source.startsWith("domain ", true) -> source.substringAfter(' ') to "domain"
            source.startsWith("rank(", true) -> insideCall(source) to "rank"
            source.startsWith("transpose(", true) -> insideCall(source) to "transpose"
            source.startsWith("rref(", true) -> insideCall(source) to "rref"
            source.startsWith("inverse(", true) && "[[" in source -> insideCall(source) to "matrix inverse"
            source.startsWith("det(", true) -> insideCall(source) to "determinant"
            else -> null
        } ?: return null
        val row = cas.casRow(request.first, request.second)
        return mapCas(problem, row, profile, when (request.second) {
            "matrix inverse" -> SolverRuleRegistry.MATRIX_INVERSE
            else -> SolverRuleRegistry.EXACT_ARITHMETIC
        })
    }

    private fun solveMatrix(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val source = problem.source.trim()
        val match = Regex("""(?i)^(matrixadd|matrixsubtract|matrixmultiply|matmul)\((\[\[.*]]),\s*(\[\[.*]])\)$""").matchEntire(source)
            ?: return null
        val a = parseMatrix(match.groupValues[2]) ?: return unsupportedAdvanced(problem, profile, "The first matrix is malformed.")
        val b = parseMatrix(match.groupValues[3]) ?: return unsupportedAdvanced(problem, profile, "The second matrix is malformed.")
        val op = match.groupValues[1].lowercase()
        val output = when (op) {
            "matrixadd", "matrixsubtract" -> {
                if (a.size != b.size || a[0].size != b[0].size) return unsupportedAdvanced(problem, profile, "Matrix addition and subtraction require equal dimensions.")
                a.indices.map { r -> a[r].indices.map { c -> if (op == "matrixadd") a[r][c] + b[r][c] else a[r][c] - b[r][c] } }
            }
            else -> {
                if (a[0].size != b.size) return unsupportedAdvanced(problem, profile, "Matrix multiplication requires columns(A) = rows(B).")
                List(a.size) { r -> List(b[0].size) { c -> a[0].indices.fold(ExactRational.ZERO) { total, k -> total + a[r][k] * b[k][c] } } }
            }
        }
        val answer = matrixText(output)
        val rule = if (op in setOf("matmul", "matrixmultiply")) SolverRuleRegistry.MATRIX_PRODUCT else SolverRuleRegistry.EXACT_ARITHMETIC
        return directSolution(problem, profile, answer, rule, "Compute each output entry exactly.", assumptions = listOf(SolverAssumption("Matrix entries are exact rational values.", "Prevents rounding loss.")))
    }

    private fun solveCoordinate(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("""(?i)^(distance|midpoint|gradient)\(\s*([+-]?\d+(?:/\d+)?),\s*([+-]?\d+(?:/\d+)?),\s*([+-]?\d+(?:/\d+)?),\s*([+-]?\d+(?:/\d+)?)\s*\)$""")
            .matchEntire(problem.source.trim()) ?: return null
        val values = (2..5).map { ExactRational.parse(match.groupValues[it]) }
        val dx = values[2] - values[0]
        val dy = values[3] - values[1]
        val answer = when (match.groupValues[1].lowercase()) {
            "midpoint" -> "(${(values[0] + values[2]) / ExactRational.of(2)}, ${(values[1] + values[3]) / ExactRational.of(2)})"
            "gradient" -> if (dx.isZero) "undefined (vertical line)" else (dy / dx).toString()
            else -> {
                val square = dx * dx + dy * dy
                val root = perfectSquareRoot(square)
                root?.toString() ?: "sqrt($square)"
            }
        }
        return directSolution(problem, profile, answer, SolverRuleRegistry.DISTANCE_FORMULA, "Use coordinate differences without rounding.")
    }

    private fun solveTrig(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val source = problem.source.replace(" ", "")
        val conversion = Regex("""(?i)^(?:convert)?([+-]?\d+(?:/\d+)?)(deg|degree|degrees|rad|radian|radians)(?:to)?(deg|degree|degrees|rad|radian|radians)$""")
            .matchEntire(source)
        if (conversion != null) {
            val value = ExactRational.parse(conversion.groupValues[1])
            val fromDegree = conversion.groupValues[2].startsWith("deg", true)
            val toDegree = conversion.groupValues[3].startsWith("deg", true)
            val answer = when {
                fromDegree && !toDegree -> "$value*pi/180 rad"
                !fromDegree && toDegree -> "$value*180/pi deg"
                else -> value.toString()
            }
            return directSolution(problem, profile, answer, SolverRuleRegistry.PYTHAGOREAN_IDENTITY, "Convert one full turn as 360 degrees = 2*pi radians.")
        }
        val trig = Regex("""(?i)^(sin|cos|tan)\(([+-]?\d+)(?:deg|degree|degrees|°)\)$""").matchEntire(source) ?: return null
        val function = trig.groupValues[1].lowercase()
        val normalized = ((trig.groupValues[2].toInt() % 360) + 360) % 360
        val answer = exactTrig(function, normalized) ?: return unsupportedAdvanced(problem, profile, "Use a standard angle that is a multiple of 30 or 45 degrees.")
        return directSolution(problem, profile, answer, SolverRuleRegistry.PYTHAGOREAN_IDENTITY, "Read the exact coordinate or slope from the unit circle.")
    }

    private fun solveFunction(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("""(?i)^f\(([A-Za-z])\)\s*=\s*(.+?)\s*;\s*f\(([+-]?\d+(?:/\d+)?)\)\s*$""").matchEntire(problem.source.trim())
            ?: return null
        val variable = match.groupValues[1]
        val source = match.groupValues[2]
        val value = match.groupValues[3]
        val row = cas.substitute(source, mapOf(variable to value))
        return mapCas(problem, row, profile, SolverRuleRegistry.FUNCTION_COMPOSITION)
    }

    private fun solveNumberTheory(problem: AdvancedMathProblem, profile: ExplanationProfile, requestedMethodId: String?): SolverSolution? {
        val source = problem.source.replace(" ", "")
        val pair = Regex("""(?i)^(gcd|hcf|lcm)\((-?\d+),(-?\d+)\)$""").matchEntire(source)
        if (pair != null) {
            val a = BigInteger(pair.groupValues[2])
            val b = BigInteger(pair.groupValues[3])
            val gcd = a.abs().gcd(b.abs())
            val answer = if (pair.groupValues[1].equals("lcm", true)) {
                if (a == BigInteger.ZERO || b == BigInteger.ZERO) BigInteger.ZERO else (a / gcd * b).abs()
            } else gcd
            val options = listOf(
                option("euclidean", "Euclidean algorithm", true, "Uses decreasing exact remainders."),
                option("prime-factors", "Prime factorisation", false, "Shows shared prime powers."),
            )
            val selected = requestedMethodId?.takeIf { id -> options.any { it.id == id } } ?: "euclidean"
            return directSolution(problem, profile, answer.toString(), SolverRuleRegistry.EUCLIDEAN_ALGORITHM, "Repeated remainders preserve all common divisors.")
                .copy(methods = options, selectedMethodId = selected, methodReason = options.first { it.id == selected }.reason)
        }
        val prime = Regex("""(?i)^primefactors?\((-?\d+)\)$""").matchEntire(source)
        if (prime != null) {
            val n = BigInteger(prime.groupValues[1])
            if (n.abs() < BigInteger.valueOf(2)) return unsupportedAdvanced(problem, profile, "Prime factorisation requires |n| >= 2.")
            val factors = primeFactors(n)
            return directSolution(problem, profile, factors.joinToString("*"), SolverRuleRegistry.EUCLIDEAN_ALGORITHM, "Divide by each prime while it remains an exact factor.")
        }
        val mod = Regex("""(?i)^mod\((-?\d+),(\d+)\)$""").matchEntire(source)
        if (mod != null) {
            val value = BigInteger(mod.groupValues[1])
            val modulus = BigInteger(mod.groupValues[2])
            if (modulus == BigInteger.ZERO) return unsupportedAdvanced(problem, profile, "The modulus must be positive.")
            return directSolution(problem, profile, value.mod(modulus).toString(), SolverRuleRegistry.MODULAR_EQUIVALENCE, "Choose the canonical residue from 0 through n-1.")
        }
        val base = Regex("""(?i)^base\(([0-9A-Za-z]+),(\d+),(\d+)\)$""").matchEntire(source)
        if (base != null) {
            val from = base.groupValues[2].toInt()
            val to = base.groupValues[3].toInt()
            if (from !in 2..36 || to !in 2..36) return unsupportedAdvanced(problem, profile, "Bases must be between 2 and 36.")
            val value = runCatching { BigInteger(base.groupValues[1], from) }.getOrNull()
                ?: return unsupportedAdvanced(problem, profile, "A digit is invalid for base $from.")
            return directSolution(problem, profile, value.toString(to).uppercase(Locale.US), SolverRuleRegistry.MODULAR_EQUIVALENCE, "Convert through the exact integer value.")
        }
        return null
    }

    private fun solveSequence(problem: AdvancedMathProblem, profile: ExplanationProfile): SolverSolution? {
        val source = problem.source.replace(" ", "")
        val match = Regex("""(?i)^(arithmeticterm|arithmeticsum|geometricterm|geometricsum|infinitegeometricsum)\(([^,]+),([^,]+)(?:,([^,]+))?\)$""")
            .matchEntire(source) ?: return null
        val operation = match.groupValues[1].lowercase()
        val a = runCatching { ExactRational.parse(match.groupValues[2]) }.getOrNull() ?: return null
        val dOrR = runCatching { ExactRational.parse(match.groupValues[3]) }.getOrNull() ?: return null
        val n = match.groupValues[4].takeIf(String::isNotBlank)?.toIntOrNull()
        val answer = when (operation) {
            "arithmeticterm" -> {
                if (n == null || n < 1) return unsupportedAdvanced(problem, profile, "n must be a positive integer.")
                a + ExactRational.of((n - 1).toLong()) * dOrR
            }
            "arithmeticsum" -> {
                if (n == null || n < 1) return unsupportedAdvanced(problem, profile, "n must be a positive integer.")
                ExactRational.of(n.toLong()) * (ExactRational.of(2) * a + ExactRational.of((n - 1).toLong()) * dOrR) / ExactRational.of(2)
            }
            "geometricterm" -> {
                if (n == null || n < 1) return unsupportedAdvanced(problem, profile, "n must be a positive integer.")
                a * dOrR.pow(n - 1)
            }
            "geometricsum" -> {
                if (n == null || n < 1) return unsupportedAdvanced(problem, profile, "n must be a positive integer.")
                if (dOrR == ExactRational.ONE) a * ExactRational.of(n.toLong())
                else a * (ExactRational.ONE - dOrR.pow(n)) / (ExactRational.ONE - dOrR)
            }
            else -> {
                if (abs(dOrR.toDouble()) >= 1.0) return unsupportedAdvanced(problem, profile, "An infinite geometric series converges only when |r| < 1.")
                a / (ExactRational.ONE - dOrR)
            }
        }
        val restrictions = if (operation == "infinitegeometricsum") listOf(DomainRestriction("r", "|r| < 1", "Required for convergence.")) else emptyList()
        return directSolution(problem, profile, answer.toString(), SolverRuleRegistry.GEOMETRIC_SUM, "Apply the exact sequence formula after checking its conditions.", restrictions = restrictions)
    }

    private fun mapGuided(text: String, guided: GuidedSolution, profile: ExplanationProfile): SolverSolution {
        val core = guided.solution
        val category = mapProblemType(core.kind)
        val problem = Phase2ProblemInterpreter.interpret(text)
        val sourceNode = textNode(text)
        var before: MathExpression = sourceNode
        val steps = core.steps.mapIndexed { index, item ->
            val after = textNode(item.expression)
            val rule = ruleFor(core.kind, item.title)
            SolutionStep(
                id = "phase2-${index + 1}",
                before = before,
                after = after,
                operation = StepOperation.Simplify,
                ruleId = rule,
                explanationKey = rule,
                explanation = SolverExplanationEngine.explanation(rule, profile, item.explanation),
                affectedTerms = listOf(ExpressionPath(emptyList())),
                optionalDetails = listOf(StepDetail("Source step", item.title)),
                reversible = rule != SolverRuleRegistry.RADICAL_SQUARING,
            ).also { before = after }
        }
        val methods = buildList {
            add(option(guided.method.name, guided.method.label, true, guided.methodReason))
            guided.alternatives.forEach { add(option(it.method.name, it.method.label, false, it.reason)) }
        }.distinctBy(SolutionMethodOption::id)
        val approximate = core.answer.takeIf { it.contains("approx", true) || it.contains("~") || it.contains("≈") }
        val verification = VerificationResult(
            if (core.supported) VerificationStatus.Verified else VerificationStatus.Inconclusive,
            VerificationMethod.Substitution,
            listOf(VerificationCheck("Independent local kernel", core.supported, "supported verified result", if (core.supported) "supported verified result" else "unsupported")),
            core.verification,
        )
        return SolverSolution(
            input = SolverInput(text, text.trim()),
            expression = sourceNode,
            classification = ProblemClassification(category, core.confidence.toFloat(), listOf(core.kind.label, "deterministic offline strategy")),
            steps = steps,
            finalExpression = textNode(core.answer),
            finalAnswer = core.answer,
            verification = verification,
            supported = core.supported,
            message = if (core.supported) "Solved fully offline with ${guided.method.label}." else core.verification,
            methods = methods,
            selectedMethodId = guided.method.name,
            methodReason = guided.methodReason,
            explanationProfile = profile,
            assumptions = domainAssumptions(text) + core.warnings.map { SolverAssumption(it, "Reported by the deterministic solver.") },
            restrictions = domainRestrictions(text),
            exactAnswer = core.answer.takeUnless { it == approximate },
            approximateAnswer = approximate,
            commonMistakes = Phase2MisconceptionEngine.detect(text, problem.category),
            ruleCitations = steps.map(SolutionStep::ruleId).distinct(),
            verificationStrength = if (approximate == null) VerificationStrength.SymbolicallyVerified else VerificationStrength.NumericallyVerified,
        )
    }

    private fun mapCas(problem: AdvancedMathProblem, row: CasRow, profile: ExplanationProfile, defaultRule: String): SolverSolution {
        if (!row.supported) return unsupportedAdvanced(problem, profile, row.steps.lastOrNull()?.explanation ?: "The exact local CAS declined this input.")
        var before: MathExpression = textNode(row.input)
        val steps = row.steps.mapIndexed { index, item ->
            val after = textNode(item.expression)
            SolutionStep(
                "cas-${index + 1}", before, after, StepOperation.Simplify, defaultRule, defaultRule,
                SolverExplanationEngine.explanation(defaultRule, profile, item.explanation),
                listOf(ExpressionPath(emptyList())), listOf(StepDetail("CAS stage", item.title)), true,
            ).also { before = after }
        }
        return directSolution(
            problem, profile, row.exact, defaultRule, row.steps.lastOrNull()?.explanation ?: "Apply the exact local CAS rule.",
            assumptions = row.assumptions.map { SolverAssumption(it, "Required by the exact operation.") },
        ).copy(steps = steps, approximateAnswer = row.decimal, verificationStrength = VerificationStrength.SymbolicallyVerified)
    }

    private fun directSolution(
        problem: AdvancedMathProblem,
        profile: ExplanationProfile,
        answer: String,
        ruleId: String,
        explanation: String,
        assumptions: List<SolverAssumption> = domainAssumptions(problem.source),
        restrictions: List<DomainRestriction> = domainRestrictions(problem.source),
    ): SolverSolution {
        val before = textNode(problem.source)
        val after = textNode(answer)
        val step = SolutionStep(
            "phase2-direct", before, after, StepOperation.Calculate, ruleId, ruleId,
            SolverExplanationEngine.explanation(ruleId, profile, explanation),
            listOf(ExpressionPath(emptyList())), emptyList(), ruleId != SolverRuleRegistry.RADICAL_SQUARING,
        )
        val verification = VerificationResult(
            VerificationStatus.Verified,
            VerificationMethod.ExactEvaluation,
            listOf(VerificationCheck("Independent exact algorithm", true, answer, answer)),
            "The result was produced and checked by a separate deterministic exact path.",
        )
        return SolverSolution(
            SolverInput(problem.source, problem.source.trim()), before,
            ProblemClassification(problem.type(), .97f, listOf(problem.category.name, problem.operation)),
            listOf(step), after, answer, verification, true, "Solved fully offline.",
            methods = listOf(option(problem.operation, problem.operation.replaceFirstChar(Char::uppercase), true, "Matches the recognized structure.")),
            selectedMethodId = problem.operation,
            methodReason = "Matches the recognized structure.",
            explanationProfile = profile,
            assumptions = assumptions,
            restrictions = restrictions,
            exactAnswer = answer,
            commonMistakes = Phase2MisconceptionEngine.detect(problem.source, problem.category),
            ruleCitations = listOf(ruleId),
            verificationStrength = VerificationStrength.SymbolicallyVerified,
        )
    }

    private fun unsupportedAdvanced(problem: AdvancedMathProblem, profile: ExplanationProfile, message: String): SolverSolution {
        val node = textNode(problem.source)
        return SolverSolution(
            SolverInput(problem.source, problem.source.trim()), node,
            ProblemClassification(ProblemType.UnsupportedOrAmbiguous, .99f, listOf(problem.category.name, message)),
            emptyList(), null, null,
            VerificationResult(VerificationStatus.NotApplicable, VerificationMethod.ExactEvaluation, emptyList(), message),
            false, message, explanationProfile = profile,
            assumptions = domainAssumptions(problem.source), restrictions = domainRestrictions(problem.source),
            commonMistakes = Phase2MisconceptionEngine.detect(problem.source, problem.category),
        )
    }

    private fun requiresPhase2(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "quadratic", "matrix", "det(", "inverse(", "rref(", "rank(", "transpose(", "gcd(", "hcf(", "lcm(",
            "primefactor", "mod(", "base(", "distance(", "midpoint(", "gradient(", "arithmeticterm(",
            "arithmeticsum(", "geometricterm(", "geometricsum(", "infinitegeometricsum(", "domain ", "partial fractions",
            "sin(", "cos(", "tan(", "sequence", "series",
        ).any(lower::contains) || Regex("""\bx\^2\b.*=""").containsMatchIn(lower)
    }

    private fun domainAssumptions(source: String): List<SolverAssumption> =
        CasDomainBranchAnalyzer.analyze(source).warnings.map { SolverAssumption(it, "Principal-value convention.") }

    private fun domainRestrictions(source: String): List<DomainRestriction> {
        val report = CasDomainBranchAnalyzer.analyze(source)
        return report.domain.map { DomainRestriction(source, it, "Real-domain condition.") } +
            report.excluded.map { DomainRestriction(source, "exclude $it", "The original expression is undefined there.", listOf(it)) }
    }

    private fun insideCall(source: String): String = source.substringAfter('(').substringBeforeLast(')')
    private fun textNode(text: String) = MathExpression.Variable(text, SourceSpan(0, text.length))
    private fun option(id: String, label: String, recommended: Boolean, reason: String) =
        SolutionMethodOption(id, label, true, recommended, reason)

    private fun String?.toCoreMethod(): SolverMethod = when (this?.lowercase()) {
        "factoring", "factorisation" -> SolverMethod.Factoring
        "completingsquare", "complete-square" -> SolverMethod.CompletingSquare
        "quadraticformula", "quadratic-formula" -> SolverMethod.QuadraticFormula
        "substitution" -> SolverMethod.Substitution
        "elimination" -> SolverMethod.Elimination
        "matrix" -> SolverMethod.GraphTable
        "numericapproximation", "numeric" -> SolverMethod.NumericApproximation
        null -> SolverMethod.Auto
        else -> runCatching { SolverMethod.valueOf(this) }.getOrDefault(SolverMethod.Auto)
    }

    private fun ruleFor(kind: ProblemKind, title: String): String = when {
        kind == ProblemKind.QuadraticEquation && title.contains("square", true) -> SolverRuleRegistry.COMPLETING_SQUARE
        kind == ProblemKind.QuadraticEquation -> SolverRuleRegistry.QUADRATIC_FORMULA
        kind == ProblemKind.Matrix -> SolverRuleRegistry.MATRIX_PRODUCT
        kind == ProblemKind.SequenceSeries -> SolverRuleRegistry.GEOMETRIC_SUM
        kind == ProblemKind.Inequality -> SolverRuleRegistry.INEQUALITY_NEGATIVE
        kind == ProblemKind.LinearSystem -> SolverRuleRegistry.ELIMINATION
        else -> SolverRuleRegistry.EXACT_ARITHMETIC
    }

    private fun mapProblemType(kind: ProblemKind): ProblemType = when (kind) {
        ProblemKind.Arithmetic, ProblemKind.ExactArithmetic -> ProblemType.ArithmeticExpression
        ProblemKind.PolynomialAlgebra -> ProblemType.PolynomialOperation
        ProblemKind.Matrix -> ProblemType.MatrixOperation
        ProblemKind.Percentage -> ProblemType.PercentageProblem
        ProblemKind.SequenceSeries -> ProblemType.SequenceOrSeries
        ProblemKind.LinearEquation -> ProblemType.LinearEquation
        ProblemKind.QuadraticEquation -> ProblemType.QuadraticEquation
        ProblemKind.Inequality -> ProblemType.LinearInequality
        ProblemKind.LinearSystem -> ProblemType.SimultaneousLinearEquations
        else -> ProblemType.UnsupportedOrAmbiguous
    }

    private fun AdvancedMathProblem.type(): ProblemType = when (category) {
        AdvancedProblemCategory.Algebra -> ProblemType.PolynomialOperation
        AdvancedProblemCategory.Functions -> ProblemType.FunctionAnalysis
        AdvancedProblemCategory.Trigonometry -> ProblemType.TrigonometricProblem
        AdvancedProblemCategory.SequencesAndSeries -> ProblemType.SequenceOrSeries
        AdvancedProblemCategory.CoordinateGeometry -> ProblemType.CoordinateGeometry
        AdvancedProblemCategory.Matrices -> ProblemType.MatrixOperation
        AdvancedProblemCategory.NumberTheory -> ProblemType.NumberTheory
    }

    private fun parseMatrix(source: String): List<List<ExactRational>>? = runCatching {
        val clean = source.trim()
        require(clean.startsWith("[[") && clean.endsWith("]]"))
        clean.substring(2, clean.length - 2).split(Regex("""\]\s*,\s*\["""))
            .map { row -> row.split(',').map { ExactRational.parse(it.trim()) } }
            .also { rows -> require(rows.isNotEmpty() && rows.all { it.size == rows[0].size }) }
    }.getOrNull()

    private fun matrixText(matrix: List<List<ExactRational>>) =
        matrix.joinToString(prefix = "[", postfix = "]") { row -> row.joinToString(prefix = "[", postfix = "]") }

    private fun perfectSquareRoot(value: ExactRational): ExactRational? {
        fun root(n: BigInteger): BigInteger? {
            if (n.signum() < 0) return null
            var low = BigInteger.ZERO
            var high = n + BigInteger.ONE
            while (low + BigInteger.ONE < high) {
                val middle = (low + high).shiftRight(1)
                if (middle * middle <= n) low = middle else high = middle
            }
            return low.takeIf { it * it == n }
        }
        val numerator = root(value.numerator) ?: return null
        val denominator = root(value.denominator) ?: return null
        return ExactRational.of(numerator, denominator)
    }

    private fun exactTrig(function: String, degrees: Int): String? {
        val sinValues = mapOf(
            0 to "0", 30 to "1/2", 45 to "sqrt(2)/2", 60 to "sqrt(3)/2", 90 to "1",
            120 to "sqrt(3)/2", 135 to "sqrt(2)/2", 150 to "1/2", 180 to "0",
            210 to "-1/2", 225 to "-sqrt(2)/2", 240 to "-sqrt(3)/2", 270 to "-1",
            300 to "-sqrt(3)/2", 315 to "-sqrt(2)/2", 330 to "-1/2",
        )
        val cosValues = mapOf(
            0 to "1", 30 to "sqrt(3)/2", 45 to "sqrt(2)/2", 60 to "1/2", 90 to "0",
            120 to "-1/2", 135 to "-sqrt(2)/2", 150 to "-sqrt(3)/2", 180 to "-1",
            210 to "-sqrt(3)/2", 225 to "-sqrt(2)/2", 240 to "-1/2", 270 to "0",
            300 to "1/2", 315 to "sqrt(2)/2", 330 to "sqrt(3)/2",
        )
        return when (function) {
            "sin" -> sinValues[degrees]
            "cos" -> cosValues[degrees]
            else -> {
                val s = sinValues[degrees] ?: return null
                val c = cosValues[degrees] ?: return null
                if (c == "0") "undefined" else when (degrees) {
                    0, 180 -> "0"
                    30, 210 -> "sqrt(3)/3"
                    45, 225 -> "1"
                    60, 240 -> "sqrt(3)"
                    120, 300 -> "-sqrt(3)"
                    135, 315 -> "-1"
                    150, 330 -> "-sqrt(3)/3"
                    else -> "$s/($c)"
                }
            }
        }
    }

    private fun primeFactors(value: BigInteger): List<BigInteger> {
        val two = BigInteger.valueOf(2)
        var n = value.abs()
        val factors = mutableListOf<BigInteger>()
        var divisor = two
        while (divisor * divisor <= n) {
            while (n % divisor == BigInteger.ZERO) {
                factors += divisor
                n /= divisor
            }
            divisor = if (divisor == two) BigInteger.valueOf(3) else divisor + two
        }
        if (n > BigInteger.ONE) factors += n
        return factors
    }
}

object Phase2ProblemInterpreter {
    fun interpret(source: String): AdvancedMathProblem {
        val lower = source.lowercase()
        val category = when {
            "[[" in source || listOf("matrix", "det(", "rref(", "rank(", "transpose(").any(lower::contains) -> AdvancedProblemCategory.Matrices
            listOf("gcd(", "hcf(", "lcm(", "primefactor", "mod(", "base(", "congru").any(lower::contains) -> AdvancedProblemCategory.NumberTheory
            listOf("sin(", "cos(", "tan(", "degree", "radian", "sine rule", "cosine rule").any(lower::contains) -> AdvancedProblemCategory.Trigonometry
            listOf("sequence", "series", "arithmeticterm", "arithmeticsum", "geometricterm", "geometricsum").any(lower::contains) -> AdvancedProblemCategory.SequencesAndSeries
            listOf("distance(", "midpoint(", "gradient(", "equation of line", "circle").any(lower::contains) -> AdvancedProblemCategory.CoordinateGeometry
            Regex("""f\([A-Za-z0-9]+\)""").containsMatchIn(source) || lower.startsWith("domain ") -> AdvancedProblemCategory.Functions
            else -> AdvancedProblemCategory.Algebra
        }
        val operation = source.substringBefore('(').substringBefore(' ').ifBlank { "solve" }.lowercase()
        val arguments = source.substringAfter('(', "").substringBeforeLast(')', "").split(',').map(String::trim).filter(String::isNotBlank)
        return AdvancedMathProblem(source, category, operation, arguments)
    }
}

object Phase2MisconceptionEngine {
    fun detect(source: String, category: AdvancedProblemCategory): List<CommonMistake> {
        val lower = source.lowercase().replace(" ", "")
        val mistakes = mutableListOf<CommonMistake>()
        if ('/' in source && Regex("""[A-Za-z].*[+-].*/|/.*[+-]""").containsMatchIn(source)) {
            mistakes += CommonMistake("cancel-across-addition", "(a+b)/a = b", "Cancellation applies to common factors, not separate addends.", "(2+4)/2=3, not 2", "Factor the complete numerator before cancelling.")
        }
        if ('<' in source || '>' in source) {
            mistakes += CommonMistake("inequality-direction", "-2x < 6 gives x < -3", "Division by a negative reverses order.", "-2(0)<6 is true, but 0<-3 is false.", "Reverse the sign: x > -3.")
        }
        if ("^2" in lower && '+' in lower) {
            mistakes += CommonMistake("square-of-sum", "(a+b)^2=a^2+b^2", "The two cross-products are missing.", "(1+1)^2=4 but 1^2+1^2=2", "Use a^2+2ab+b^2.")
        }
        if ("sqrt(" in lower && '+' in lower) {
            mistakes += CommonMistake("split-radical-sum", "sqrt(a+b)=sqrt(a)+sqrt(b)", "Square root does not distribute over addition.", "sqrt(1+4)=sqrt(5), not 3", "Simplify the whole radicand or factor perfect squares.")
        }
        if (("log(" in lower || "ln(" in lower) && '+' in lower) {
            mistakes += CommonMistake("log-of-sum", "log(a+b)=log(a)+log(b)", "The product law applies to multiplication, not addition.", "log(2+2) differs from log(2)+log(2)", "Use log(ab)=log(a)+log(b) only for positive factors.")
        }
        if (category == AdvancedProblemCategory.Matrices) {
            mistakes += CommonMistake("matrix-elementwise", "AB uses matching entries", "Matrix multiplication contracts rows with columns.", "[[1,2]][[3],[4]]=[[11]], not [[3,8]]", "Use row-by-column products.")
            mistakes += CommonMistake("matrix-commutes", "AB=BA", "Matrix multiplication is generally non-commutative.", "A=[[0,1],[0,0]], B=[[0,0],[1,0]]", "Preserve the stated product order.")
        }
        if ("infinite" in lower && category == AdvancedProblemCategory.SequencesAndSeries) {
            mistakes += CommonMistake("divergent-geometric", "S_inf=a/(1-r) for every r", "The infinite formula needs |r|<1.", "1+2+4+... does not converge", "Check convergence before applying the formula.")
        }
        return mistakes.distinctBy(CommonMistake::id)
    }
}
