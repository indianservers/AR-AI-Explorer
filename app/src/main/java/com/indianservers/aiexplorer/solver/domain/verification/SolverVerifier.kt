package com.indianservers.aiexplorer.solver.domain.verification

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.engine.SolverExactMath
import com.indianservers.aiexplorer.solver.domain.model.InequalityOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.VerificationCheck
import com.indianservers.aiexplorer.solver.domain.model.VerificationMethod
import com.indianservers.aiexplorer.solver.domain.model.VerificationResult
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.variables

internal object SolverVerifier {
    fun arithmetic(original: MathExpression, result: MathExpression): VerificationResult {
        val expected = SolverExactMath.evaluate(original)
        val actual = SolverExactMath.evaluate(result)
        val passed = expected.isSuccess && actual.isSuccess && expected.getOrThrow() == actual.getOrThrow()
        return verification(
            VerificationMethod.ExactEvaluation,
            listOf(VerificationCheck("Exact re-evaluation", passed, expected.getOrNull()?.toString() ?: "exact value", actual.getOrNull()?.toString() ?: "not exact")),
            if (passed) "The final value independently matches the original exact expression." else "Exact re-evaluation did not match.",
        )
    }

    fun equation(original: MathExpression.Equation, variable: String, solution: ExactRational): VerificationResult {
        val values = mapOf(variable to solution)
        val left = SolverExactMath.evaluate(original.left, values)
        val right = SolverExactMath.evaluate(original.right, values)
        val passed = left.isSuccess && right.isSuccess && left.getOrThrow() == right.getOrThrow()
        return verification(
            VerificationMethod.Substitution,
            listOf(VerificationCheck("Substitute $variable=$solution", passed, left.getOrNull()?.toString() ?: "defined", right.getOrNull()?.toString() ?: "undefined")),
            if (passed) "Substitution makes both sides exactly equal." else "The proposed solution does not make both sides equal.",
        )
    }

    fun relation(original: MathExpression.Equation): VerificationResult {
        val left = SolverExactMath.evaluate(original.left)
        val right = SolverExactMath.evaluate(original.right)
        val evaluated = left.isSuccess && right.isSuccess
        val relationIsTrue = evaluated && left.getOrThrow() == right.getOrThrow()
        return verification(
            VerificationMethod.ExactEvaluation,
            listOf(
                VerificationCheck(
                    "Exact relation decision",
                    evaluated,
                    if (relationIsTrue) "true" else "false",
                    if (relationIsTrue) "true" else "false",
                ),
            ),
            if (!evaluated) "The relation could not be evaluated exactly."
            else if (relationIsTrue) "Both sides have the same exact value, so the relation is true."
            else "The exact values differ, so the relation is false.",
        )
    }

    fun system(original: MathExpression.SystemOfEquations, values: Map<String, ExactRational>): VerificationResult {
        val checks = original.equations.mapIndexed { index, equation ->
            val left = SolverExactMath.evaluate(equation.left, values)
            val right = SolverExactMath.evaluate(equation.right, values)
            VerificationCheck(
                "Equation ${index + 1}",
                left.isSuccess && right.isSuccess && left.getOrThrow() == right.getOrThrow(),
                left.getOrNull()?.toString() ?: "defined",
                right.getOrNull()?.toString() ?: "undefined",
            )
        }
        return verification(
            VerificationMethod.SystemSubstitution,
            checks,
            if (checks.all { it.passed }) "Both original equations are satisfied exactly." else "At least one original equation is not satisfied.",
        )
    }

    fun equivalent(original: MathExpression, result: MathExpression): VerificationResult {
        val variables = (original.variables() + result.variables()).sorted()
        if (variables.isEmpty()) return arithmetic(original, result)
        val samples = listOf(-3L, -1L, 1L, 2L, 5L)
        val checks = samples.mapNotNull { sample ->
            val values = variables.associateWith { ExactRational.of(sample + variables.indexOf(it)) }
            val left = SolverExactMath.evaluate(original, values)
            val right = SolverExactMath.evaluate(result, values)
            if (left.isFailure || right.isFailure) null else VerificationCheck(
                variables.joinToString { "$it=${values.getValue(it)}" },
                left.getOrThrow() == right.getOrThrow(),
                left.getOrThrow().toString(),
                right.getOrThrow().toString(),
            )
        }
        if (checks.isEmpty()) return VerificationResult(
            VerificationStatus.Inconclusive,
            VerificationMethod.SampledEquivalence,
            emptyList(),
            "No safe exact sample points were available.",
        )
        return verification(
            VerificationMethod.SampledEquivalence,
            checks,
            if (checks.all { it.passed }) "The forms agree at every safe exact sample." else "The forms disagree at a sampled value.",
        )
    }

    fun inequality(original: MathExpression.Inequality, variable: String, boundary: ExactRational, resultOperator: InequalityOperator): VerificationResult {
        val offsets = listOf(ExactRational.of(-2), ExactRational.of(-1), ExactRational.ZERO, ExactRational.ONE, ExactRational.of(2))
        val checks = offsets.mapNotNull { offset ->
            val value = boundary + offset
            val left = SolverExactMath.evaluate(original.left, mapOf(variable to value)).getOrNull() ?: return@mapNotNull null
            val right = SolverExactMath.evaluate(original.right, mapOf(variable to value)).getOrNull() ?: return@mapNotNull null
            val expected = compare(left, original.operator, right)
            val actual = compare(value, resultOperator, boundary)
            VerificationCheck("$variable=$value", expected == actual, expected.toString(), actual.toString())
        }
        return verification(
            VerificationMethod.BoundarySampling,
            checks,
            if (checks.isNotEmpty() && checks.all { it.passed }) "Representative values and the boundary match the original inequality." else "Boundary sampling did not confirm the result.",
        )
    }

    fun unsupported(message: String) = VerificationResult(
        VerificationStatus.NotApplicable,
        VerificationMethod.ExactEvaluation,
        emptyList(),
        message,
    )

    private fun compare(left: ExactRational, operator: InequalityOperator, right: ExactRational): Boolean = when (operator) {
        InequalityOperator.Less -> left < right
        InequalityOperator.LessOrEqual -> left <= right
        InequalityOperator.Greater -> left > right
        InequalityOperator.GreaterOrEqual -> left >= right
    }

    private fun verification(method: VerificationMethod, checks: List<VerificationCheck>, message: String): VerificationResult {
        val passed = checks.isNotEmpty() && checks.all { it.passed }
        return VerificationResult(if (passed) VerificationStatus.Verified else VerificationStatus.Failed, method, checks, message)
    }
}
