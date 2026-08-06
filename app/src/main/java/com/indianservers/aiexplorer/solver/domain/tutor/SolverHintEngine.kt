package com.indianservers.aiexplorer.solver.domain.tutor

import com.indianservers.aiexplorer.solver.domain.model.ExplanationProfile
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry

object SolverHintEngine {
    fun hints(
        solution: SolverSolution,
        stepIndex: Int,
        profile: ExplanationProfile,
    ): List<SolverHint> {
        if (!solution.supported || solution.steps.isEmpty()) return emptyList()
        val step = solution.steps[stepIndex.coerceIn(0, solution.steps.lastIndex)]
        val rule = SolverRuleRegistry.get(step.ruleId)
        val before = SolverExpressionRenderer.render(step.before)
        val after = SolverExpressionRenderer.render(step.after)
        val target = when {
            solution.classification.type.name.contains("Equation") -> "isolate the unknown while keeping both sides equivalent"
            solution.classification.type.name.contains("Inequality") -> "isolate the unknown and preserve the correct order"
            solution.classification.type.name.contains("Calculus") -> "match the next part of the expression to a valid calculus rule"
            solution.classification.type.name.contains("Complex") -> "separate the real, imaginary, modulus and argument information"
            else -> "reduce the expression without changing its value"
        }
        return listOf(
            hint(HintLevel.ProblemType, profile, step.id, false,
                simple(profile, "This is a ${readable(solution.classification.type.name)} problem.", "The deterministic classifier identifies ${readable(solution.classification.type.name)}.")),
            hint(HintLevel.RelevantConcept, profile, step.id, false,
                simple(profile, "The useful idea is ${rule.name.lowercase()}.", "Use ${rule.name}: ${rule.explanation}")),
            hint(HintLevel.NextTarget, profile, step.id, false,
                simple(profile, "Aim to $target.", "The next invariant-preserving target is to $target.")),
            hint(HintLevel.SuggestedOperation, profile, step.id, false,
                simple(profile, "Look at $before. Which part matches ${rule.name.lowercase()}?", "Apply ${rule.name} to the affected subexpression only.")),
            hint(HintLevel.PartialNextStep, profile, step.id, false,
                "Start from $before, apply ${rule.name.lowercase()}, and leave the final simplification blank."),
            hint(HintLevel.RevealedNextStep, profile, step.id, true,
                "$after. ${step.explanation}"),
        )
    }

    private fun hint(
        level: HintLevel,
        profile: ExplanationProfile,
        stepId: String,
        reveals: Boolean,
        text: String,
    ) = SolverHint(level, text, reveals, stepId, profile)

    private fun simple(profile: ExplanationProfile, child: String, formal: String): String = when (profile) {
        ExplanationProfile.ChildFriendly -> child
        ExplanationProfile.SchoolExamination -> formal
        ExplanationProfile.University -> "$formal Track the domain and the transformation invariant."
        ExplanationProfile.Rigorous -> "$formal State every condition under which the implication is reversible."
    }

    private fun readable(value: String) = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()
}

