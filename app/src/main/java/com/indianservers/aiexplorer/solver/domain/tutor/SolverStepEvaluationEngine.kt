package com.indianservers.aiexplorer.solver.domain.tutor

import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.solver.domain.engine.linearForm
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverParseResult
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.parser.SolverParser
import com.indianservers.aiexplorer.solver.domain.steps.MathRuleKnowledgeBase
import kotlin.math.abs

class SolverStepEvaluationEngine(
    private val parser: SolverParser = SolverParser(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
) {
    fun evaluate(solution: SolverSolution, stepIndex: Int, learnerInput: String): LearnerStepEvaluation {
        if (!solution.supported || solution.steps.isEmpty()) return result(
            LearnerStepStatus.UnsupportedTransformation, EquivalenceStatus.Unknown,
            "Tutor unavailable", "The original problem has no verified structured route.", "Return to a supported problem.",
        )
        if (learnerInput.isBlank()) return result(
            LearnerStepStatus.IncompleteStep, EquivalenceStatus.Unknown,
            "The step is empty", "Enter the complete next mathematical line.", "Start by copying the current line, then make one justified change.",
        )
        val parsed = parser.parse(learnerInput)
        if (parsed is SolverParseResult.Error) return result(
            LearnerStepStatus.AmbiguousInput, EquivalenceStatus.Unknown,
            "The notation is incomplete", "${parsed.error.message} at position ${parsed.error.span.start + 1}.",
            "Close grouping symbols and include the relation sign when working with an equation.",
        )

        detectDomainViolation(learnerInput)?.let { return it }
        detectMisconception(learnerInput)?.let { return it }

        val index = stepIndex.coerceIn(0, solution.steps.lastIndex)
        val expectedStep = solution.steps[index]
        val expected = SolverExpressionRenderer.render(expectedStep.after)
        val current = SolverExpressionRenderer.render(expectedStep.before)
        val normalizedLearner = render(parsed)
        val normalizedExpected = render(parser.parse(expected))
        val normalizedCurrent = render(parser.parse(current))
        val rule = MathRuleKnowledgeBase.get(expectedStep.ruleId)

        if (same(normalizedLearner, normalizedExpected)) {
            return LearnerStepEvaluation(
                LearnerStepStatus.CorrectNextStep, EquivalenceStatus.Equivalent, rule,
                solution.selectedMethodId,
                TutorFeedback("Correct next step", "Your line matches the verified next transformation and preserves the required relationship.", "Continue to the following structured step.", true),
                null, null,
            )
        }
        val finalAnswer = solution.exactAnswer ?: solution.finalAnswer.orEmpty()
        if (same(normalizedLearner, finalAnswer) || equivalent(normalizedLearner, finalAnswer)) {
            return LearnerStepEvaluation(
                LearnerStepStatus.CorrectLargeJump, EquivalenceStatus.Equivalent, rule,
                solution.selectedMethodId,
                TutorFeedback("Correct, with a large jump", "The result is equivalent to the verified final answer, but intermediate justification was skipped.", "Explain which rule connects the current line to your answer.", true),
                null, RecoveryAction("Explain skipped transformation", expectedStep.ruleId),
            )
        }
        if (same(normalizedLearner, normalizedCurrent) || equivalent(normalizedLearner, normalizedCurrent)) {
            return LearnerStepEvaluation(
                LearnerStepStatus.EquivalentReformatting, EquivalenceStatus.Equivalent, null,
                solution.selectedMethodId,
                TutorFeedback("Equivalent reformatting", "The notation changed, but the mathematical state did not move toward the next target.", "Apply the next rule after this formatting change.", true),
                null, RecoveryAction("Use next rule", expectedStep.ruleId),
            )
        }
        if (equivalent(normalizedLearner, normalizedExpected)) {
            return LearnerStepEvaluation(
                LearnerStepStatus.CorrectAlternativeMethod, EquivalenceStatus.Equivalent, rule,
                solution.selectedMethodId,
                TutorFeedback("Valid alternative step", "Your expression is mathematically equivalent to the verified next state even though its form differs.", "Keep the same domain restrictions as the original problem.", true),
                null, null,
            )
        }
        if (parsed is SolverParseResult.Success && expectedStep.before is MathExpression.Equation && parsed.expression is MathExpression.Equation) {
            if (linearEquationsEquivalent(expectedStep.before, parsed.expression)) {
                return LearnerStepEvaluation(
                    LearnerStepStatus.CorrectAlternativeMethod, EquivalenceStatus.Equivalent, rule,
                    solution.selectedMethodId,
                    TutorFeedback("Valid equation transformation", "Both equations have the same solution set; you used a different non-zero scaling or collection order.", "Continue from your equivalent equation.", true),
                    null, null,
                )
            }
            val oneSide = oneSideOnlyChanged(expectedStep.before, parsed.expression)
            if (oneSide) return result(
                LearnerStepStatus.RuleMisuse, EquivalenceStatus.NotEquivalent,
                "The equation became unbalanced", "Only one side was changed. Equality is preserved only when the same valid operation is applied to both sides.",
                "Repeat the operation on the unchanged side.",
                TutorMisconception("one-sided-operation", "Equality", learnerInput, "One-sided operations generally change the solution set."),
            )
        }
        return result(
            LearnerStepStatus.NonEquivalentTransformation, EquivalenceStatus.NotEquivalent,
            "The transformation changes the mathematics",
            "Independent symbolic checks do not match this line to the current state or the verified next state.",
            "Make one smaller change and name the rule that justifies it.",
        )
    }

    private fun detectDomainViolation(input: String): LearnerStepEvaluation? {
        val compact = input.replace(" ", "")
        if (Regex("""/[A-Za-z](?![A-Za-z0-9_])""").containsMatchIn(compact) && !Regex("""[A-Za-z]\s*(?:!=|not=)\s*0""").containsMatchIn(input)) {
            return result(
                LearnerStepStatus.DomainViolation, EquivalenceStatus.ConditionallyEquivalent,
                "A zero branch may be lost", "Dividing by a variable is valid only after handling the case where that variable equals zero.",
                "State the non-zero condition and inspect the zero case separately.",
                TutorMisconception("divide-by-variable", "Domain", input, "Division by a possibly zero expression is not an equivalence."),
            )
        }
        if (Regex("""(?i)ln\(\s*(?:0|-)|log\(\s*(?:0|-)""").containsMatchIn(compact)) {
            return result(
                LearnerStepStatus.DomainViolation, EquivalenceStatus.NotEquivalent,
                "The logarithm is outside its real domain", "A real logarithm requires a positive argument.", "Restrict the argument to values greater than zero.",
            )
        }
        return null
    }

    private fun detectMisconception(input: String): LearnerStepEvaluation? {
        val compact = input.replace(" ", "")
        val equalityParts = compact.split('=', limit = 2)
        val looksLikeSquaredBinomial = equalityParts.size == 2 &&
            Regex("""^\([^()]+[+-][^()]+\)\^2$""").matches(equalityParts[0])
        val resultTermSeparators = equalityParts.getOrNull(1)
            ?.drop(1)
            ?.count { it == '+' || it == '-' }
            ?: Int.MAX_VALUE
        if (looksLikeSquaredBinomial && resultTermSeparators < 2) {
            return result(
                LearnerStepStatus.RuleMisuse, EquivalenceStatus.NotEquivalent,
                "The middle product is missing", "Squaring a sum produces two cross-products, so (a+b)^2=a^2+2ab+b^2.",
                "Expand the two identical brackets term by term.",
                TutorMisconception("square-of-sum", "Expansion", input, "The distributive property creates the 2ab term."),
            )
        }
        if (Regex("""\([^)]*[+-][^)]*\)/[A-Za-z0-9]+=""").containsMatchIn(compact) && "cancel" in compact.lowercase()) {
            return result(
                LearnerStepStatus.RuleMisuse, EquivalenceStatus.NotEquivalent,
                "Cancellation cannot cross addition", "Only common factors of the entire numerator and denominator can cancel.",
                "Factor the complete numerator first.",
                TutorMisconception("cancel-across-sum", "Fractions", input, "Addends are not multiplicative factors."),
            )
        }
        return null
    }

    private fun equivalent(first: String, second: String): Boolean {
        if (first.isBlank() || second.isBlank() || '=' in first || '=' in second || '<' in first || '>' in first || '<' in second || '>' in second) return false
        return runCatching {
            val row = cas.simplify("($first)-($second)")
            row.supported && row.exact.replace(" ", "") in setOf("0", "0/1")
        }.getOrDefault(false)
    }

    private fun linearEquationsEquivalent(first: MathExpression.Equation, second: MathExpression.Equation): Boolean {
        val a = linearFormDifference(first) ?: return false
        val b = linearFormDifference(second) ?: return false
        val keys = a.coefficients.keys + b.coefficients.keys
        val pairs = keys.map { (a.coefficients[it] ?: com.indianservers.aiexplorer.core.ExactRational.ZERO) to (b.coefficients[it] ?: com.indianservers.aiexplorer.core.ExactRational.ZERO) } +
            listOf(a.constant to b.constant)
        val pivot = pairs.firstOrNull { !it.first.isZero || !it.second.isZero } ?: return true
        if (pivot.first.isZero || pivot.second.isZero) return false
        val scale = pivot.second / pivot.first
        return !scale.isZero && pairs.all { (x, y) -> y == x * scale }
    }

    private fun linearFormDifference(equation: MathExpression.Equation): com.indianservers.aiexplorer.solver.domain.engine.LinearForm? {
        val left = linearForm(equation.left) ?: return null
        val right = linearForm(equation.right) ?: return null
        return left - right
    }

    private fun oneSideOnlyChanged(first: MathExpression.Equation, second: MathExpression.Equation): Boolean {
        val leftSame = same(SolverExpressionRenderer.render(first.left), SolverExpressionRenderer.render(second.left))
        val rightSame = same(SolverExpressionRenderer.render(first.right), SolverExpressionRenderer.render(second.right))
        return leftSame.xor(rightSame)
    }

    private fun render(result: SolverParseResult): String = when (result) {
        is SolverParseResult.Success -> SolverExpressionRenderer.render(result.expression)
        is SolverParseResult.Error -> result.normalized
    }

    private fun same(first: String, second: String) = first.replace(" ", "") == second.replace(" ", "")

    private fun result(
        status: LearnerStepStatus,
        equivalence: EquivalenceStatus,
        headline: String,
        explanation: String,
        clue: String,
        misconception: TutorMisconception? = null,
    ) = LearnerStepEvaluation(
        status, equivalence, null, null,
        TutorFeedback(headline, explanation, clue, status in setOf(LearnerStepStatus.CorrectNextStep, LearnerStepStatus.CorrectAlternativeMethod, LearnerStepStatus.CorrectLargeJump, LearnerStepStatus.EquivalentReformatting)),
        misconception,
        RecoveryAction("Use a smaller justified step", clue),
    )
}
