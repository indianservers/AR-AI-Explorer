package com.indianservers.aiexplorer.solver.domain.practice

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeProblem
import java.util.UUID
import kotlin.math.abs

class SolverPracticeGenerator(
    private val engine: Phase3SolverEngine = Phase3SolverEngine(),
) {
    fun generate(
        sourceSolution: SolverSolution,
        mode: PracticeMode,
        seed: Int = sourceSolution.input.normalized.hashCode(),
    ): PracticeProblem? {
        if (!sourceSolution.supported) return null
        val candidate = candidate(sourceSolution, mode, seed)
        val validated = engine.solve(candidate.prompt)
        if (!validated.supported || validated.finalAnswer.isNullOrBlank() || validated.verification.status == VerificationStatus.Failed) return null
        if (candidate.expectedType != null && validated.classification.type != candidate.expectedType) return null
        val answer = validated.exactAnswer ?: validated.finalAnswer ?: return null
        val difficulty = SolverDifficultyModel.assess(candidate.prompt, validated, mode == PracticeMode.ExplainTheRule)
        return PracticeProblem(
            id = UUID.nameUUIDFromBytes("${candidate.prompt}|$mode|$seed".toByteArray()).toString(),
            prompt = candidate.displayPrompt ?: candidate.prompt,
            expectedAnswer = if (mode == PracticeMode.CorrectTheMistake) candidate.correction ?: answer else answer,
            skill = validated.classification.type.name,
            mode = mode,
            difficulty = difficulty,
            hint = candidate.hint,
            validationMessage = "Validated offline: ${validated.verification.message}",
            choices = candidate.choices,
        )
    }

    private fun candidate(solution: SolverSolution, mode: PracticeMode, seed: Int): Candidate {
        val magnitude = when (mode) {
            PracticeMode.EasierNumbers -> 2
            PracticeMode.HarderNumbers -> 9
            else -> 5
        }
        val n = abs(seed % 7) + magnitude
        val m = abs((seed / 7) % 6) + 2
        return when (solution.classification.type) {
            ProblemType.ArithmeticExpression, ProblemType.FractionSimplification, ProblemType.PercentageProblem ->
                arithmetic(mode, n, m)
            ProblemType.LinearEquation, ProblemType.LinearInequality ->
                linear(mode, n, m)
            ProblemType.QuadraticEquation, ProblemType.PolynomialOperation, ProblemType.Factorisation, ProblemType.Expansion ->
                quadratic(mode, n.coerceIn(2, 9), m.coerceIn(1, 7))
            ProblemType.TrigonometricProblem ->
                Candidate("sin(${listOf(0, 30, 45, 60, 90)[abs(seed) % 5]}deg)", ProblemType.TrigonometricProblem, "Use the unit circle.")
            ProblemType.SequenceOrSeries ->
                Candidate("arithmeticterm($n,$m,8)", ProblemType.SequenceOrSeries, "Use a_n=a+(n-1)d.")
            ProblemType.CoordinateGeometry ->
                Candidate("distance(0,0,$n,$m)", ProblemType.CoordinateGeometry, "Make a right triangle from coordinate differences.")
            ProblemType.MatrixOperation ->
                Candidate("matrixadd([[$n,1],[2,$m]],[[1,2],[3,4]])", ProblemType.MatrixOperation, "Add corresponding entries.")
            ProblemType.NumberTheory ->
                Candidate("gcd(${n * m},${m * (m + 1)})", ProblemType.NumberTheory, "Use decreasing Euclidean remainders.")
            ProblemType.Calculus ->
                Candidate("differentiate ${n}*x^${m.coerceAtMost(5)}", ProblemType.Calculus, "Apply the power rule.")
            ProblemType.ComplexNumbers ->
                Candidate("complex ${n}+${m}i", ProblemType.ComplexNumbers, "Separate real and imaginary coordinates.")
            else -> Candidate("${n}+${m}", ProblemType.ArithmeticExpression, "Apply exact arithmetic.")
        }
    }

    private fun arithmetic(mode: PracticeMode, n: Int, m: Int): Candidate = when (mode) {
        PracticeMode.CorrectTheMistake -> Candidate(
            "$n + $m",
            ProblemType.ArithmeticExpression,
            "Recalculate in the correct operation order.",
            displayPrompt = "Correct the mistake: $n + $m = ${n + m + 1}",
            correction = "${n + m}",
        )
        PracticeMode.FillMissingStep -> Candidate(
            "$n + $m",
            ProblemType.ArithmeticExpression,
            "Evaluate the addition.",
            displayPrompt = "Fill the missing result: $n + $m = ___",
        )
        PracticeMode.ChooseNextStep -> Candidate(
            "$n + $m*2",
            ProblemType.ArithmeticExpression,
            "Multiplication comes before addition.",
            choices = listOf("Add $n+$m first", "Multiply $m*2 first", "Square $m"),
        )
        PracticeMode.VerificationChallenge -> Candidate(
            "$n*$m",
            ProblemType.ArithmeticExpression,
            "Verify by repeated addition or division.",
            displayPrompt = "Verify the result of $n*$m using a reverse operation.",
        )
        PracticeMode.ExplainTheRule -> Candidate(
            "$n+$m*2",
            ProblemType.ArithmeticExpression,
            "Name the order-of-operations rule.",
            displayPrompt = "Explain why multiplication is evaluated first in $n+$m*2.",
        )
        PracticeMode.DifferentRepresentation -> Candidate("$n/$m + 1/$m", ProblemType.FractionSimplification, "The denominator already matches.")
        PracticeMode.MixedReview -> Candidate("${n * m}% of 100", ProblemType.PercentageProblem, "A percent means divide by 100.")
        else -> Candidate("$n + $m*2", ProblemType.ArithmeticExpression, "Use multiplication before addition.")
    }

    private fun linear(mode: PracticeMode, n: Int, m: Int): Candidate {
        val rhs = n * m + m
        return when (mode) {
            PracticeMode.CorrectTheMistake -> Candidate(
                "${n}x + $m = $rhs",
                ProblemType.LinearEquation,
                "Apply the same subtraction to both sides.",
                displayPrompt = "Correct the mistake: ${n}x+$m=$rhs, so ${n}x=$rhs.",
                correction = "${n}x=${rhs - m}",
            )
            PracticeMode.FillMissingStep -> Candidate(
                "${n}x + $m = $rhs",
                ProblemType.LinearEquation,
                "Undo the added constant first.",
                displayPrompt = "Fill the next step: ${n}x+$m=$rhs -> ${n}x=___",
            )
            PracticeMode.ChooseNextStep -> Candidate(
                "${n}x + $m = $rhs",
                ProblemType.LinearEquation,
                "Subtract $m from both sides.",
                choices = listOf("Subtract $m from both sides", "Divide only the left side by $n", "Add $m to the left side"),
            )
            PracticeMode.DifferentRepresentation -> Candidate("$n*(x+$m)=${n * (m + 2)}", ProblemType.LinearEquation, "Distribute or divide by the non-zero coefficient.")
            PracticeMode.ExplainTheRule -> Candidate(
                "${n}x+$m=$rhs",
                ProblemType.LinearEquation,
                "Explain the balance property.",
                displayPrompt = "Explain why subtracting $m from both sides preserves the solution.",
            )
            else -> Candidate("${n}x + $m = $rhs", ProblemType.LinearEquation, "Keep both sides balanced.")
        }
    }

    private fun quadratic(mode: PracticeMode, first: Int, second: Int): Candidate {
        val sum = first + second
        val product = first * second
        return when (mode) {
            PracticeMode.CorrectTheMistake -> Candidate(
                "x^2-${sum}x+$product=0",
                ProblemType.QuadraticEquation,
                "Both factor roots change sign when solving each factor.",
                displayPrompt = "Correct the mistake: (x-$first)(x-$second)=0 gives x=-$first,-$second.",
                correction = "x=$first or x=$second",
            )
            PracticeMode.FillMissingStep -> Candidate(
                "x^2-${sum}x+$product=0",
                ProblemType.QuadraticEquation,
                "Find two numbers with sum $sum and product $product.",
                displayPrompt = "Fill the factors: x^2-${sum}x+$product=(x-___)(x-___).",
            )
            PracticeMode.DifferentRepresentation -> Candidate("(x-$first)*(x-$second)=0", ProblemType.QuadraticEquation, "Use the zero-product property.")
            PracticeMode.ExplainTheRule -> Candidate(
                "x^2-${sum}x+$product=0",
                ProblemType.QuadraticEquation,
                "Explain the zero-product property.",
                displayPrompt = "Explain why each factor can be set to zero.",
            )
            else -> Candidate("x^2-${sum}x+$product=0", ProblemType.QuadraticEquation, "Try factorisation before the general formula.")
        }
    }

    private data class Candidate(
        val prompt: String,
        val expectedType: ProblemType?,
        val hint: String,
        val displayPrompt: String? = null,
        val correction: String? = null,
        val choices: List<String> = emptyList(),
    )
}

