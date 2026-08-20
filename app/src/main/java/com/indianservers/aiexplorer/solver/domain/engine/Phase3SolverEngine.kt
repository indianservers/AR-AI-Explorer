package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.AdvancedCalculatorResult
import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.CasDomainBranchAnalyzer
import com.indianservers.aiexplorer.solver.domain.input.SolverInputIntentGuard
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
import com.indianservers.aiexplorer.solver.domain.visualisation.SolverFormulaUnderstanding
import com.indianservers.aiexplorer.solver.domain.visualisation.SolverVisualisationGenerator
import com.indianservers.aiexplorer.solver.domain.verification.SolverAdvancedVerifier

class Phase3SolverEngine(
    private val phase2: Phase2SolverEngine = Phase2SolverEngine(),
    private val calculator: AdvancedScientificCalculator = AdvancedScientificCalculator(),
    private val visualisations: SolverVisualisationGenerator = SolverVisualisationGenerator(),
    private val schoolQuestions: SolverSchoolQuestionEngine = SolverSchoolQuestionEngine(phase2),
    private val advancedVerifier: SolverAdvancedVerifier = SolverAdvancedVerifier(),
    private val probabilityStatistics: SolverProbabilityStatisticsEngine = SolverProbabilityStatisticsEngine(),
    private val typedWordProblems: SolverTypedWordProblemEngine = SolverTypedWordProblemEngine(),
) {
    fun solve(
        text: String,
        operation: SolverOperation = SolverOperation.Solve,
        profile: ExplanationProfile = ExplanationProfile.SchoolExamination,
        requestedMethodId: String? = null,
    ): SolverSolution {
        if (text.trim().startsWith("normal probability", true)) {
            return unsupported(text, profile, "Use the verified structured form 'normal pdf x mean m sd s'; interval probability is not implemented yet.", ProblemType.Probability)
        }
        // Curriculum questions can contain words such as "tangent", "normal", or
        // "mode" without being Phase 3 commands. Prefer their exact, verified
        // schemas before considering the explicit advanced-command grammar.
        schoolQuestions.solve(text, profile)?.let { return it }
        val advancedRequest = isPhase3Request(text)
        if (!advancedRequest) {
            typedWordProblems.solve(text, profile)?.let { return it }
            probabilityStatistics.solve(text, profile)?.let { solution ->
                if (!solution.supported) return solution
                val specs = runCatching { visualisations.generate(solution) }.getOrDefault(emptyList())
                return solution.copy(visualisations = specs, visualVerification = runCatching { visualisations.verification(solution, specs) }.getOrNull())
            }
        }
        if (!advancedRequest) {
            SolverInputIntentGuard.rejectionReason(text)?.let { reason ->
                return unsupported(text, profile, reason, ProblemType.UnsupportedOrAmbiguous)
            }
        }
        val base = if (advancedRequest) {
            solveAdvanced(text, profile) ?: unsupported(
                text,
                profile,
                "The Phase 3 command was recognized, but no verified local calculus or complex-number strategy matched its arguments.",
            )
        } else {
            phase2.solve(text, operation, profile, requestedMethodId)
        }
        if (!base.supported) return base
        val specs = runCatching { visualisations.generate(base) }.getOrDefault(emptyList())
        val visualVerification = runCatching { visualisations.verification(base, specs) }.getOrNull()
        return base.copy(
            visualisations = specs,
            visualVerification = visualVerification,
            formulaUnderstanding = runCatching { SolverFormulaUnderstanding.forSolution(base, specs) }.getOrNull(),
        )
    }

    private fun solveAdvanced(text: String, profile: ExplanationProfile): SolverSolution? {
        val clean = text.trim()
        if (clean.matches(Regex("""(?i)(differentiate|derivative|derivative of|integrate|integral|integral of|limit|complex)"""))) {
            return unsupported(text, profile, "Enter the expression and every required bound, value, or root order.")
        }
        val result = runCatching { calculator.evaluate(text) }.getOrNull() ?: return null
        if (result.primary.contains("unsupported", true) || result.primary.contains("not solved", true)) {
            return unsupported(text, profile, result.warning ?: result.verification)
        }
        val complex = text.trim().startsWith("complex", true)
        val type = if (complex) ProblemType.ComplexNumbers else ProblemType.Calculus
        val rule = ruleFor(text, complex)
        var before: MathExpression = node(text)
        val steps = result.steps.mapIndexed { index, description ->
            val afterText = if (index == result.steps.lastIndex) result.exact ?: result.primary else description.substringAfter(':', description)
            val after = node(afterText)
            SolutionStep(
                id = "phase3-${index + 1}",
                before = before,
                after = after,
                operation = when {
                    text.contains("integr", true) -> StepOperation.Calculate
                    text.contains("limit", true) -> StepOperation.Simplify
                    complex -> StepOperation.Calculate
                    else -> StepOperation.Simplify
                },
                ruleId = rule,
                explanationKey = rule,
                explanation = SolverExplanationEngine.explanation(rule, profile, description),
                affectedTerms = listOf(ExpressionPath(emptyList())),
                optionalDetails = listOf(StepDetail("Phase 3 local kernel", description)) +
                    if (index == result.steps.lastIndex) result.alternatives.map { StepDetail(it.first, it.second) } else emptyList(),
                reversible = !text.contains("square both", true),
            ).also { before = after }
        }.ifEmpty {
            listOf(
                SolutionStep(
                    "phase3-result", node(text), node(result.primary), StepOperation.Calculate, rule, rule,
                    SolverExplanationEngine.explanation(rule, profile, "Apply the deterministic offline rule."),
                    listOf(ExpressionPath(emptyList())), reversible = true,
                ),
            )
        }
        val exact = result.exact ?: result.primary
        val approximate = result.primary.takeIf { it != exact }
        val branchReport = CasDomainBranchAnalyzer.analyze(text)
        val restrictions = branchReport.domain.map {
            DomainRestriction(text, it, "Required by the real-domain operation.")
        } + branchReport.excluded.map {
            DomainRestriction(text, "exclude $it", "The original expression is undefined there.", listOf(it))
        }
        val assumptions = buildList {
            branchReport.warnings.forEach { add(SolverAssumption(it, "Principal-value or branch convention.")) }
            if (complex) {
                add(SolverAssumption("Principal argument lies in (-pi, pi].", "Fixes a reproducible polar representation."))
                if (text.contains("roots", true)) add(SolverAssumption("All root branches k=0,...,n-1 are retained.", "Complex roots are multi-valued."))
            }
            if (text.contains("integr", true) && !Regex("""(?i)\bfrom\b.+\bto\b""").containsMatchIn(text)) {
                add(SolverAssumption("C is an arbitrary constant.", "Indefinite antiderivatives form a family."))
            }
        }
        val method = methodFor(text)
        val evidence = advancedVerifier.verify(text, exact, result.verification)
        return SolverSolution(
            input = SolverInput(text, text.trim()),
            expression = node(text),
            classification = ProblemClassification(
                type, .97f,
                listOf(if (complex) "complex-number operation" else "calculus operation", "deterministic offline Phase 3 kernel"),
            ),
            steps = steps,
            finalExpression = node(exact),
            finalAnswer = exact,
            verification = evidence.result,
            supported = true,
            message = "Solved and visualised fully offline.",
            methods = listOf(SolutionMethodOption(method.first, method.second, true, true, method.third)),
            selectedMethodId = method.first,
            methodReason = method.third,
            explanationProfile = profile,
            assumptions = assumptions,
            restrictions = restrictions,
            exactAnswer = exact,
            approximateAnswer = approximate,
            ruleCitations = listOf(rule),
            verificationStrength = evidence.strength,
        )
    }

    private fun unsupported(
        text: String,
        profile: ExplanationProfile,
        message: String,
        type: ProblemType = if (text.trim().startsWith("complex", true)) {
            ProblemType.ComplexNumbers
        } else {
            ProblemType.Calculus
        },
    ): SolverSolution {
        val source = node(text)
        return SolverSolution(
            input = SolverInput(text, text.trim()),
            expression = source,
            classification = ProblemClassification(
                type,
                .95f,
                listOf("Phase 3 request recognized", "No verified local strategy matched"),
            ),
            steps = emptyList(),
            finalExpression = null,
            finalAnswer = null,
            verification = VerificationResult(VerificationStatus.NotApplicable, VerificationMethod.ExactEvaluation, emptyList(), message),
            supported = false,
            message = message,
            explanationProfile = profile,
        )
    }

    private fun isPhase3Request(text: String): Boolean {
        val lower = text.trim().lowercase().replaceFirst(Regex("""^\d+\.\s*"""), "")
        return Regex(
            """^(?:complex|differentiate|derivative(?:\s+of)?|partial\s+derivative|gradient\s+|""" +
                """directional\s+derivative|divergence|curl|tangent\s+plane|jacobian|hessian|""" +
                """integrate|integral\s+of|improper\s+integrate|double\s+integrate|triple\s+integrate|""" +
                """parameter\s+integral|limit|continuity|tangent\s+|normal\s+|derivative\s+analysis|""" +
                """ode|ode\s+series|linear\s+ivp|logistic|second\s+order\s+ivp|system\s+rk4|rk4|laplace|""" +
                """lagrange|line\s+integral|work\s+integral|surface\s+flux|green|gauss|stokes)\b""",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(lower)
    }

    private fun ruleFor(text: String, complex: Boolean): String {
        val lower = text.lowercase()
        return when {
            complex && "root" in lower -> SolverRuleRegistry.DE_MOIVRE
            complex && ("polar" in lower || !lower.contains("multiply")) -> SolverRuleRegistry.COMPLEX_POLAR
            complex -> SolverRuleRegistry.COMPLEX_RECTANGULAR
            "limit" in lower || "continuity" in lower -> SolverRuleRegistry.LIMIT_LAW
            "gradient" in lower || "jacobian" in lower || "hessian" in lower || "partial derivative" in lower || "directional derivative" in lower || "divergence" in lower || "curl" in lower || "tangent plane" in lower -> SolverRuleRegistry.DERIVATIVE_POWER
            lower.startsWith("green ") || lower.startsWith("gauss ") || lower.startsWith("stokes ") -> SolverRuleRegistry.INTEGRATION_POWER
            "integr" in lower && ("parts" in lower || Regex("""\bx\s*\*?\s*(?:sin|cos|ln|exp)""").containsMatchIn(lower)) -> SolverRuleRegistry.INTEGRATION_PARTS
            "integr" in lower && Regex("""(?:sin|cos|exp|sqrt)\([^)]*[+\-*/][^)]*\)""").containsMatchIn(lower) -> SolverRuleRegistry.INTEGRATION_SUBSTITUTION
            "integr" in lower -> SolverRuleRegistry.INTEGRATION_POWER
            "different" in lower && '/' in lower -> SolverRuleRegistry.QUOTIENT_RULE
            "different" in lower && '*' in lower -> SolverRuleRegistry.PRODUCT_RULE
            "different" in lower && Regex("""(?:sin|cos|tan|ln|exp|sqrt)\([^)]*[+\-*/^][^)]*\)""").containsMatchIn(lower) -> SolverRuleRegistry.CHAIN_RULE
            else -> SolverRuleRegistry.DERIVATIVE_POWER
        }
    }

    private fun methodFor(text: String): Triple<String, String, String> {
        val lower = text.lowercase()
        return when {
            lower.startsWith("complex") && "root" in lower ->
                Triple("de-moivre", "De Moivre", "Polar form exposes every root branch without discarding solutions.")
            lower.startsWith("complex") ->
                Triple("rectangular-polar", "Rectangular and polar", "Separates exact components and fixes the principal argument.")
            "limit" in lower -> Triple("limit-laws", "Limit laws", "Transforms only on a punctured neighbourhood and checks the approach.")
            "jacobian" in lower -> Triple("jacobian", "Jacobian matrix", "Computes every first partial and evaluates the complete derivative map at the requested point.")
            "hessian" in lower -> Triple("hessian", "Hessian matrix", "Computes all second partials and checks mixed-partial symmetry numerically.")
            lower.startsWith("lagrange ") -> Triple("lagrange", "Lagrange multipliers", "Solves stationarity and constraint equations together and reports both residuals.")
            lower.startsWith("line integral") -> Triple("scalar-line-integral", "Scalar line integral", "Includes arc-length scaling and preserves the declared parameter interval.")
            lower.startsWith("work integral") -> Triple("work-line-integral", "Oriented work integral", "Uses F(r(t)) dot r'(t), so reversing the curve reverses the result.")
            lower.startsWith("surface flux") -> Triple("surface-flux", "Oriented surface flux", "Uses r_u cross r_v to make the normal orientation explicit.")
            lower.startsWith("green ") -> Triple("green-certificate", "Green theorem certificate", "Compares closed-boundary circulation with the double integral of planar curl.")
            lower.startsWith("gauss ") -> Triple("gauss-certificate", "Gauss theorem certificate", "Compares outward surface flux with the volume integral of divergence.")
            lower.startsWith("stokes ") -> Triple("stokes-certificate", "Stokes theorem certificate", "Compares oriented boundary circulation with surface curl flux.")
            lower.startsWith("derivative analysis") -> Triple("derivative-analysis", "Derivative sign analysis", "Combines stationary points, monotonic intervals and endpoint comparison on a closed interval.")
            lower.startsWith("tangent ") -> Triple("tangent-line", "Tangent line", "Evaluates the derivative at the requested point and uses point-slope form.")
            lower.startsWith("normal ") -> Triple("normal-line", "Normal line", "Uses the negative reciprocal of the tangent slope.")
            lower.startsWith("linear ivp") -> Triple("linear-ivp", "Linear IVP", "Uses the closed-form constant-coefficient solution and reports its residual.")
            lower.startsWith("rk4") -> Triple("rk4", "Adaptive RK4", "Compares refined numerical trajectories to expose an integration error estimate.")
            lower.startsWith("ode series") -> Triple("power-series", "Power series", "Matches coefficients and reports the first un-cancelled residual order.")
            lower.startsWith("logistic ") -> Triple("logistic", "Logistic closed form", "Separates the nonlinear growth equation and verifies the differentiated result.")
            lower.startsWith("second order ivp") -> Triple("characteristic-equation", "Characteristic equation", "Selects the root family and applies both initial conditions.")
            lower.startsWith("system rk4") -> Triple("coupled-rk4", "Coupled RK4", "Advances every component from the same Runge-Kutta stage state.")
            lower.startsWith("laplace ") -> Triple("laplace-transform", "Laplace transform", "Uses the verified local transform table and preserves convergence conditions.")
            lower.startsWith("improper integrate") -> Triple("improper-integral", "Improper integral", "Classifies convergence before reporting a finite value.")
            lower.startsWith("double integrate") -> Triple("iterated-integral", "Double integral", "Integrates over both declared bounds and checks refinement error.")
            lower.startsWith("triple integrate") -> Triple("triple-integral", "Triple integral", "Propagates a certified error envelope through three nested bounds.")
            lower.startsWith("parameter integral") -> Triple("parameter-integral", "Parameterized integral", "Certifies each parameter sample independently.")
            "integr" in lower && " from " in lower -> Triple("adaptive-definite", "Adaptive definite integration", "Refines curved regions and checks reversed orientation.")
            "integr" in lower -> Triple("symbolic-antiderivative", "Symbolic antiderivative", "Uses a reversible rule and differentiates back.")
            else -> Triple("symbolic-derivative", "Symbolic differentiation", "Applies rules to the expression tree and verifies with finite differences.")
        }
    }

    private fun node(text: String): MathExpression =
        MathExpression.Variable(text, SourceSpan(0, text.length))
}
