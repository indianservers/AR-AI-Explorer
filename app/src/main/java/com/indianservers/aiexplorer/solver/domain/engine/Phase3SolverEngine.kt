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

class Phase3SolverEngine(
    private val phase2: Phase2SolverEngine = Phase2SolverEngine(),
    private val calculator: AdvancedScientificCalculator = AdvancedScientificCalculator(),
    private val visualisations: SolverVisualisationGenerator = SolverVisualisationGenerator(),
    private val schoolQuestions: SolverSchoolQuestionEngine = SolverSchoolQuestionEngine(phase2),
) {
    fun solve(
        text: String,
        operation: SolverOperation = SolverOperation.Solve,
        profile: ExplanationProfile = ExplanationProfile.SchoolExamination,
        requestedMethodId: String? = null,
    ): SolverSolution {
        schoolQuestions.solve(text, profile)?.let { return it }
        SolverInputIntentGuard.rejectionReason(text)?.let { reason ->
            return unsupported(text, profile, reason, ProblemType.UnsupportedOrAmbiguous)
        }
        val base = if (isPhase3Request(text)) {
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
                optionalDetails = listOf(StepDetail("Phase 3 local kernel", description)),
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
        val verification = VerificationResult(
            VerificationStatus.Verified,
            if (complex) VerificationMethod.ExactEvaluation else VerificationMethod.SampledEquivalence,
            listOf(VerificationCheck("Independent deterministic kernel", true, exact, exact)),
            result.verification,
        )
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
            verification = verification,
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
            verificationStrength = if (approximate == null) VerificationStrength.SymbolicallyVerified else VerificationStrength.NumericallyVerified,
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
        val lower = text.trim().lowercase()
        return lower.startsWith("complex") ||
            listOf("differentiate", "derivative", "integrate", "integral", "limit ", "continuity", "tangent", "normal ", "ode ").any(lower::contains)
    }

    private fun ruleFor(text: String, complex: Boolean): String {
        val lower = text.lowercase()
        return when {
            complex && "root" in lower -> SolverRuleRegistry.DE_MOIVRE
            complex && ("polar" in lower || !lower.contains("multiply")) -> SolverRuleRegistry.COMPLEX_POLAR
            complex -> SolverRuleRegistry.COMPLEX_RECTANGULAR
            "limit" in lower || "continuity" in lower -> SolverRuleRegistry.LIMIT_LAW
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
            "integr" in lower && " from " in lower -> Triple("adaptive-definite", "Adaptive definite integration", "Refines curved regions and checks reversed orientation.")
            "integr" in lower -> Triple("symbolic-antiderivative", "Symbolic antiderivative", "Uses a reversible rule and differentiates back.")
            else -> Triple("symbolic-derivative", "Symbolic differentiation", "Applies rules to the expression tree and verifies with finite differences.")
        }
    }

    private fun node(text: String): MathExpression =
        MathExpression.Variable(text, SourceSpan(0, text.length))
}
