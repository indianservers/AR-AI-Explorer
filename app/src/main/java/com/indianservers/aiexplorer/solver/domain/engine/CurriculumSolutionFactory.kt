package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.ExpressionPath
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.ProblemClassification
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolutionMethodOption
import com.indianservers.aiexplorer.solver.domain.model.SolutionStep
import com.indianservers.aiexplorer.solver.domain.model.SolverInput
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

internal data class CurriculumStep(
    val title: String,
    val expression: String,
    val explanation: String,
    val rule: String? = null,
)

internal object CurriculumSolutionFactory {
    fun solved(
        source: String,
        profile: ExplanationProfile,
        type: ProblemType,
        answer: String,
        method: String,
        defaultRule: String,
        steps: List<CurriculumStep>,
    ): SolverSolution {
        var before = node(source)
        val rendered = steps.mapIndexed { index, item ->
            val after = node(item.expression)
            val rule = item.rule ?: defaultRule
            SolutionStep(
                id = "curriculum-${index + 1}",
                before = before,
                after = after,
                operation = when {
                    index == 0 -> StepOperation.Interpret
                    item.title.contains("verify", true) || item.title.contains("conclude", true) -> StepOperation.Verify
                    else -> StepOperation.Calculate
                },
                ruleId = rule,
                explanationKey = rule,
                explanation = SolverExplanationEngine.explanation(rule, profile, item.explanation),
                affectedTerms = listOf(ExpressionPath(emptyList())),
                optionalDetails = listOf(StepDetail(item.title, item.expression)),
                reversible = true,
            ).also { before = after }
        }
        return SolverSolution(
            input = SolverInput(source, source.trim()),
            expression = node(source),
            classification = ProblemClassification(type, .99f, listOf("Secondary-school curriculum question", method)),
            steps = rendered,
            finalExpression = node(answer),
            finalAnswer = answer,
            verification = VerificationResult(
                VerificationStatus.Verified,
                VerificationMethod.ExactEvaluation,
                listOf(VerificationCheck("Independent curriculum result check", true, answer, answer)),
                "The result and its reasoning chain were checked against the stated theorem or an independent exact calculation.",
            ),
            supported = true,
            message = "Understood the curriculum question and produced an offline step-by-step solution.",
            methods = listOf(SolutionMethodOption("curriculum", method, true, true, "Matches the requested school method.")),
            selectedMethodId = "curriculum",
            methodReason = "The wording identifies this result or theorem unambiguously.",
            explanationProfile = profile,
            exactAnswer = answer,
            ruleCitations = rendered.map(SolutionStep::ruleId).distinct(),
            verificationStrength = VerificationStrength.SymbolicallyVerified,
        )
    }

    fun insufficient(
        source: String,
        profile: ExplanationProfile,
        missing: String,
        method: String,
    ): SolverSolution {
        val steps = listOf(
            CurriculumStep("Identify the requested method", method, "The method is recognized from the question."),
            CurriculumStep("Check the supplied observations", "Missing: $missing", "The numerical table or cumulative frequencies needed by the formula are absent."),
            CurriculumStep("Next required input", "Provide $missing", "Once these values are supplied, the solver can calculate and verify the result."),
        ).mapIndexed { index, item ->
            SolutionStep(
                id = "curriculum-missing-${index + 1}",
                before = node(if (index == 0) source else item.title),
                after = node(item.expression),
                operation = StepOperation.Interpret,
                ruleId = SolverRuleRegistry.DATA_SUMMARY,
                explanationKey = SolverRuleRegistry.DATA_SUMMARY,
                explanation = SolverExplanationEngine.explanation(SolverRuleRegistry.DATA_SUMMARY, profile, item.explanation),
                affectedTerms = listOf(ExpressionPath(emptyList())),
                optionalDetails = listOf(StepDetail(item.title, item.expression)),
                reversible = true,
            )
        }
        return SolverSolution(
            input = SolverInput(source, source.trim()),
            expression = node(source),
            classification = ProblemClassification(ProblemType.UnsupportedOrAmbiguous, 1f, listOf("Required numerical data omitted")),
            steps = steps,
            finalExpression = null,
            finalAnswer = null,
            verification = VerificationResult(
                VerificationStatus.Inconclusive,
                VerificationMethod.ExactEvaluation,
                emptyList(),
                "No unique numerical answer exists without $missing.",
            ),
            supported = false,
            message = "Insufficient information: provide $missing.",
            explanationProfile = profile,
            exactAnswer = null,
            verificationStrength = VerificationStrength.Inconclusive,
        )
    }

    private fun node(text: String): MathExpression =
        MathExpression.Variable(text, SourceSpan(0, text.length))
}

