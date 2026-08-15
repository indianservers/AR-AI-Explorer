package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.solver.domain.classifier.SolverProblemClassifier
import com.indianservers.aiexplorer.solver.domain.input.KeyboardSolverInputSource
import com.indianservers.aiexplorer.solver.domain.model.BinaryOperator
import com.indianservers.aiexplorer.solver.domain.model.ExpressionPath
import com.indianservers.aiexplorer.solver.domain.model.InequalityOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.ProblemClassification
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.SolutionStep
import com.indianservers.aiexplorer.solver.domain.model.SolverExpressionRenderer
import com.indianservers.aiexplorer.solver.domain.model.SolverInput
import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult
import com.indianservers.aiexplorer.solver.domain.model.SolverOperation
import com.indianservers.aiexplorer.solver.domain.model.SolverParseResult
import com.indianservers.aiexplorer.solver.domain.model.SolverSolution
import com.indianservers.aiexplorer.solver.domain.model.SourceSpan
import com.indianservers.aiexplorer.solver.domain.model.StepDetail
import com.indianservers.aiexplorer.solver.domain.model.StepOperation
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import com.indianservers.aiexplorer.solver.domain.model.variables
import com.indianservers.aiexplorer.solver.domain.parser.SolverParser
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import com.indianservers.aiexplorer.solver.domain.verification.SolverVerifier
import java.math.BigInteger

class Phase1SolverEngine(
    private val parser: SolverParser = SolverParser(),
    private val classifier: SolverProblemClassifier = SolverProblemClassifier(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
) {
    fun solve(text: String, operation: SolverOperation = SolverOperation.Solve): SolverSolution {
        val input = when (val result = KeyboardSolverInputSource(text).getExpression()) {
            is SolverInputResult.Success -> result.input
            is SolverInputResult.Error -> return emptyFailure(text, result.message)
        }
        return when (val parsed = parser.parse(input.original)) {
            is SolverParseResult.Error -> SolverSolution(
                input = input.copy(normalized = parsed.normalized),
                expression = null,
                classification = unsupportedClassification("Parsing stopped at ${parsed.error.span.start}."),
                steps = emptyList(),
                finalExpression = null,
                finalAnswer = null,
                verification = SolverVerifier.unsupported("Verification cannot run until the syntax is valid."),
                supported = false,
                message = parsed.error.message,
                parseError = parsed.error,
            )
            is SolverParseResult.Success -> {
                val effectiveInput = input.copy(normalized = parsed.normalized)
                val classification = classifier.classify(input.original, parsed.expression)
                solveParsed(effectiveInput, parsed.expression, classification, operation)
            }
        }
    }

    private fun solveParsed(
        input: SolverInput,
        expression: MathExpression,
        classification: ProblemClassification,
        operation: SolverOperation,
    ): SolverSolution = when (classification.type) {
        ProblemType.ArithmeticExpression,
        ProblemType.FractionSimplification,
        ProblemType.PercentageProblem -> solveArithmetic(input, expression, classification)
        ProblemType.RatioOrProportion -> if (expression is MathExpression.Equation) solveProportion(input, expression, classification) else solveRatio(input, expression, classification)
        ProblemType.LinearEquation -> (expression as? MathExpression.Equation)?.let { solveEquation(input, it, classification) }
            ?: unsupported(input, expression, classification, "A linear equation needs an equals sign.")
        ProblemType.LinearInequality -> (expression as? MathExpression.Inequality)?.let { solveInequality(input, it, classification) }
            ?: unsupported(input, expression, classification, "A linear inequality relation is required.")
        ProblemType.SimultaneousLinearEquations -> (expression as? MathExpression.SystemOfEquations)?.let { solveSystem(input, it, classification) }
            ?: unsupported(input, expression, classification, "Separate two equations with a semicolon.")
        ProblemType.Factorisation -> solveSymbolic(input, expression, classification, SymbolicAction.Factor)
        ProblemType.Expansion -> solveSymbolic(input, expression, classification, if (operation == SolverOperation.Simplify) SymbolicAction.Simplify else SymbolicAction.Expand)
        ProblemType.AlgebraicSimplification,
        ProblemType.PolynomialOperation -> solveSymbolic(input, expression, classification, SymbolicAction.Simplify)
        ProblemType.QuadraticEquation,
        ProblemType.RationalOrRadicalEquation,
        ProblemType.FunctionAnalysis,
        ProblemType.TrigonometricProblem,
        ProblemType.SequenceOrSeries,
        ProblemType.CoordinateGeometry,
        ProblemType.MatrixOperation,
        ProblemType.NumberTheory,
        ProblemType.Calculus,
        ProblemType.ComplexNumbers,
        ProblemType.Probability,
        ProblemType.Statistics,
        ProblemType.UnsupportedOrAmbiguous -> unsupported(input, expression, classification, classification.evidence.lastOrNull() ?: "This problem is outside Phase 1.")
    }

    private fun solveArithmetic(input: SolverInput, expression: MathExpression, classification: ProblemClassification): SolverSolution {
        val value = SolverExactMath.evaluate(expression).getOrElse {
            return unsupported(input, expression, classification, it.message ?: "This exact arithmetic form is not supported.")
        }
        val result = MathExpression.Number(value, expression.span)
        val rule = when (classification.type) {
            ProblemType.FractionSimplification -> SolverRuleRegistry.FRACTION_REDUCTION
            ProblemType.PercentageProblem -> SolverRuleRegistry.PERCENT_CONVERSION
            else -> SolverRuleRegistry.ORDER_OF_OPERATIONS
        }
        val verification = SolverVerifier.arithmetic(expression, result)
        return success(
            input,
            expression,
            classification,
            listOf(step("calculate", expression, result, StepOperation.Calculate, rule, listOf(ExpressionPath(emptyList())))),
            result,
            value.toString(),
            verification,
        )
    }

    private fun solveRatio(input: SolverInput, expression: MathExpression, classification: ProblemClassification): SolverSolution {
        val ratio = expression as? MathExpression.Function
        if (ratio?.name != "ratio" || ratio.arguments.size != 2) {
            return unsupported(input, expression, classification, "Use a numeric ratio such as 12:18.")
        }
        val left = SolverExactMath.evaluate(ratio.arguments[0]).getOrNull()
        val right = SolverExactMath.evaluate(ratio.arguments[1]).getOrNull()
        if (left == null || right == null || left.denominator != BigInteger.ONE || right.denominator != BigInteger.ONE) {
            return unsupported(input, expression, classification, "Phase 1 reduces ratios with integer parts.")
        }
        val gcd = left.numerator.abs().gcd(right.numerator.abs())
        if (gcd == BigInteger.ZERO) return unsupported(input, expression, classification, "The ratio 0:0 is undefined.")
        val a = ExactRational.of(left.numerator / gcd, BigInteger.ONE)
        val b = ExactRational.of(right.numerator / gcd, BigInteger.ONE)
        val result = MathExpression.Function(
            "ratio",
            listOf(MathExpression.Number(a, expression.span), MathExpression.Number(b, expression.span)),
            expression.span,
        )
        val verification = SolverVerifier.equivalent(
            MathExpression.Fraction(ratio.arguments[0], ratio.arguments[1], expression.span),
            MathExpression.Fraction(result.arguments[0], result.arguments[1], expression.span),
        )
        return success(
            input, expression, classification,
            listOf(step("reduce-ratio", expression, result, StepOperation.Simplify, SolverRuleRegistry.RATIO_REDUCTION, listOf(ExpressionPath(emptyList())), listOf(StepDetail("GCD", gcd.toString())))),
            result, "$a:$b", verification,
        )
    }

    private fun solveProportion(
        input: SolverInput,
        equation: MathExpression.Equation,
        classification: ProblemClassification,
    ): SolverSolution {
        val leftRatio = ratioParts(equation.left)
        val rightRatio = ratioParts(equation.right)
        if (leftRatio == null && rightRatio == null) return solveEquation(input, equation, classification)
        if (leftRatio == null || rightRatio == null) {
            return unsupported(input, equation, classification, "A colon proportion needs one ratio on each side of the equals sign.")
        }
        val crossProducts = MathExpression.Equation(
            MathExpression.BinaryOperation(BinaryOperator.Multiply, leftRatio.first, rightRatio.second, equation.span),
            MathExpression.BinaryOperation(BinaryOperator.Multiply, rightRatio.first, leftRatio.second, equation.span),
            equation.span,
        )
        val crossStep = step(
            "cross-multiply",
            equation,
            crossProducts,
            StepOperation.MultiplyBothSides,
            SolverRuleRegistry.PROPORTION_CROSS_PRODUCTS,
            listOf(ExpressionPath(listOf(0)), ExpressionPath(listOf(1))),
            listOf(StepDetail("Restriction", "Ratio denominators must be non-zero")),
        )
        val solved = solveEquation(input, crossProducts, classification)
        return solved.copy(expression = equation, steps = listOf(crossStep) + solved.steps)
    }

    private fun solveSymbolic(
        input: SolverInput,
        expression: MathExpression,
        classification: ProblemClassification,
        action: SymbolicAction,
    ): SolverSolution {
        val source = SolverExpressionRenderer.render(expression)
        val row = when (action) {
            SymbolicAction.Simplify -> cas.simplify(source)
            SymbolicAction.Expand -> cas.expand(source)
            SymbolicAction.Factor -> cas.factor(source)
        }
        if (!row.supported) return unsupported(input, expression, classification, row.steps.lastOrNull()?.explanation ?: "This symbolic form is not supported.")
        val result = parseExpression(row.exact) ?: return unsupported(input, expression, classification, "The exact CAS result could not be represented safely.")
        val rule = when (action) {
            SymbolicAction.Simplify -> SolverRuleRegistry.COMBINE_LIKE_TERMS
            SymbolicAction.Expand -> SolverRuleRegistry.DISTRIBUTIVE
            SymbolicAction.Factor -> SolverRuleRegistry.ZERO_PRODUCT
        }
        val operation = when (action) {
            SymbolicAction.Simplify -> StepOperation.CombineLikeTerms
            SymbolicAction.Expand -> StepOperation.Expand
            SymbolicAction.Factor -> StepOperation.Factor
        }
        val verification = SolverVerifier.equivalent(expression, result)
        return success(
            input, expression, classification,
            listOf(step(action.name.lowercase(), expression, result, operation, rule, listOf(ExpressionPath(emptyList())))),
            result, SolverExpressionRenderer.render(result), verification,
        )
    }

    private fun solveEquation(input: SolverInput, equation: MathExpression.Equation, classification: ProblemClassification): SolverSolution {
        val difference = (linearForm(equation.left) ?: return unsupported(input, equation, classification, "The left side is not linear in Phase 1")) -
            (linearForm(equation.right) ?: return unsupported(input, equation, classification, "The right side is not linear in Phase 1"))
        if (difference.coefficients.isEmpty()) {
            val verification = SolverVerifier.relation(equation)
            val answer = if (difference.constant.isZero) "True" else "False"
            val final = MathExpression.Number(if (difference.constant.isZero) ExactRational.ONE else ExactRational.ZERO, equation.span)
            return success(
                input,
                equation,
                classification,
                listOf(step("check-proportion", equation, final, StepOperation.Verify, SolverRuleRegistry.EXACT_ARITHMETIC, listOf(ExpressionPath(emptyList())))),
                final,
                answer,
                verification,
            )
        }
        if (difference.coefficients.size == 2) {
            return solveLinearFamily(input, equation, classification, difference)
        }
        if (difference.coefficients.size != 1) return unsupported(input, equation, classification, "Enter a linear equation with one or two variables.")
        val (variable, coefficient) = difference.coefficients.entries.single()
        if (coefficient.isZero) {
            val answer = if (difference.constant.isZero) "All real values of $variable" else "No solution"
            return SolverSolution(
                input, equation, classification, emptyList(), null, answer,
                SolverVerifier.unsupported("The variable cancels; the remaining constant statement determines the result."),
                true, answer,
            )
        }
        val rightValue = -difference.constant
        val solution = rightValue / coefficient
        val collected = parseExpression("$coefficient*$variable=$rightValue") as MathExpression.Equation
        val final = parseExpression("$variable=$solution") as MathExpression.Equation
        val steps = buildList {
            if (collected != equation) add(step("collect", equation, collected, StepOperation.CombineLikeTerms, SolverRuleRegistry.COMBINE_LIKE_TERMS, listOf(ExpressionPath(listOf(0)), ExpressionPath(listOf(1)))))
            add(step("divide", collected, final, StepOperation.DivideBothSides, SolverRuleRegistry.DIVISION_EQUALITY, listOf(ExpressionPath(emptyList())), listOf(StepDetail("Non-zero divisor", coefficient.toString()))))
        }
        return success(input, equation, classification, steps, final, "$variable = $solution", SolverVerifier.equation(equation, variable, solution))
    }

    private fun solveLinearFamily(
        input: SolverInput,
        equation: MathExpression.Equation,
        classification: ProblemClassification,
        difference: LinearForm,
    ): SolverSolution {
        val target = difference.coefficients.keys.firstOrNull { it == "x" }
            ?: difference.coefficients.keys.sorted().first()
        val targetCoefficient = difference.coefficients.getValue(target)
        val freeCoefficients = difference.coefficients.filterKeys { it != target }
        val rightText = linearRightSideText(-difference.constant, freeCoefficients.mapValues { -it.value })
        val collected = parseExpression("$targetCoefficient*$target=$rightText") as? MathExpression.Equation
            ?: return unsupported(input, equation, classification, "The linear solution family could not be rendered.")
        val final = parseExpression("$target=($rightText)/($targetCoefficient)") as? MathExpression.Equation
            ?: return unsupported(input, equation, classification, "The isolated-variable form could not be rendered.")
        val freeVariables = freeCoefficients.keys.sorted()
        val steps = buildList {
            if (collected != equation) {
                add(
                    step(
                        "collect-family",
                        equation,
                        collected,
                        StepOperation.CombineLikeTerms,
                        SolverRuleRegistry.COMBINE_LIKE_TERMS,
                        listOf(ExpressionPath(listOf(0)), ExpressionPath(listOf(1))),
                        listOf(StepDetail("Free variable", freeVariables.joinToString())),
                    ),
                )
            }
            add(
                step(
                    "divide-family",
                    collected,
                    final,
                    StepOperation.DivideBothSides,
                    SolverRuleRegistry.DIVISION_EQUALITY,
                    listOf(ExpressionPath(emptyList())),
                    listOf(
                        StepDetail("Non-zero divisor", targetCoefficient.toString()),
                        StepDetail("Solution type", "Infinitely many ordered pairs"),
                    ),
                ),
            )
        }
        val verification = SolverVerifier.parametricEquation(equation, final, target, freeVariables)
        return success(
            input,
            equation,
            classification,
            steps,
            final,
            SolverExpressionRenderer.render(final),
            verification,
        )
    }

    private fun linearRightSideText(
        constant: ExactRational,
        coefficients: Map<String, ExactRational>,
    ): String {
        val terms = mutableListOf<Pair<ExactRational, String?>>()
        if (!constant.isZero) terms += constant to null
        coefficients.toSortedMap().forEach { (variable, coefficient) ->
            if (!coefficient.isZero) terms += coefficient to variable
        }
        if (terms.isEmpty()) return "0"
        return buildString {
            terms.forEachIndexed { index, (coefficient, variable) ->
                val negative = coefficient < ExactRational.ZERO
                val magnitude = if (negative) -coefficient else coefficient
                if (index > 0) append(if (negative) "-" else "+") else if (negative) append("-")
                if (variable == null) {
                    append(magnitude)
                } else {
                    if (magnitude != ExactRational.ONE) append(magnitude).append("*")
                    append(variable)
                }
            }
        }
    }

    private fun solveInequality(input: SolverInput, inequality: MathExpression.Inequality, classification: ProblemClassification): SolverSolution {
        val difference = (linearForm(inequality.left) ?: return unsupported(input, inequality, classification, "The left side is not linear in Phase 1")) -
            (linearForm(inequality.right) ?: return unsupported(input, inequality, classification, "The right side is not linear in Phase 1"))
        if (difference.coefficients.size != 1) return unsupported(input, inequality, classification, "Phase 1 solves one-variable linear inequalities.")
        val (variable, coefficient) = difference.coefficients.entries.single()
        if (coefficient.isZero) return unsupported(input, inequality, classification, "The variable cancels; use Check to inspect the constant statement.")
        val boundary = -difference.constant / coefficient
        val effectiveOperator = if (coefficient < ExactRational.ZERO) flip(inequality.operator) else inequality.operator
        val collected = parseExpression("$coefficient*$variable ${inequality.operator.symbol} ${-difference.constant}") as MathExpression.Inequality
        val final = parseExpression("$variable ${effectiveOperator.symbol} $boundary") as MathExpression.Inequality
        val steps = mutableListOf(
            step("collect-inequality", inequality, collected, StepOperation.CombineLikeTerms, SolverRuleRegistry.COMBINE_LIKE_TERMS, listOf(ExpressionPath(emptyList()))),
        )
        if (coefficient < ExactRational.ZERO) {
            steps += step("reverse-inequality", collected, final, StepOperation.ReverseInequality, SolverRuleRegistry.INEQUALITY_NEGATIVE, listOf(ExpressionPath(emptyList())), listOf(StepDetail("Divisor", coefficient.toString())))
        } else {
            steps += step("divide-inequality", collected, final, StepOperation.DivideBothSides, SolverRuleRegistry.DIVISION_EQUALITY, listOf(ExpressionPath(emptyList())), listOf(StepDetail("Positive divisor", coefficient.toString())))
        }
        return success(input, inequality, classification, steps, final, SolverExpressionRenderer.render(final), SolverVerifier.inequality(inequality, variable, boundary, effectiveOperator))
    }

    private fun solveSystem(input: SolverInput, system: MathExpression.SystemOfEquations, classification: ProblemClassification): SolverSolution {
        if (system.equations.size != 2) return unsupported(input, system, classification, "Phase 1 supports two equations.")
        val forms = system.equations.map { equation ->
            (linearForm(equation.left) ?: return unsupported(input, system, classification, "Each side must be linear.")) -
                (linearForm(equation.right) ?: return unsupported(input, system, classification, "Each side must be linear."))
        }
        val variables = forms.flatMap { it.coefficients.keys }.distinct().sorted()
        if (variables.size != 2) return unsupported(input, system, classification, "Exactly two variables are required.")
        val xName = variables[0]
        val yName = variables[1]
        val a1 = forms[0].coefficients[xName] ?: ExactRational.ZERO
        val b1 = forms[0].coefficients[yName] ?: ExactRational.ZERO
        val c1 = -forms[0].constant
        val a2 = forms[1].coefficients[xName] ?: ExactRational.ZERO
        val b2 = forms[1].coefficients[yName] ?: ExactRational.ZERO
        val c2 = -forms[1].constant
        val determinant = a1 * b2 - a2 * b1
        if (determinant.isZero) {
            val consistent = a1 * c2 == a2 * c1 && b1 * c2 == b2 * c1
            val answer = if (consistent) "Infinitely many solutions" else "No solution"
            return SolverSolution(input, system, classification, emptyList(), null, answer, SolverVerifier.unsupported("The determinant is zero."), true, answer)
        }
        val x = (c1 * b2 - c2 * b1) / determinant
        val y = (a1 * c2 - a2 * c1) / determinant
        val final = MathExpression.ExpressionList(
            listOf(
                parseExpression("$xName=$x") as MathExpression.Equation,
                parseExpression("$yName=$y") as MathExpression.Equation,
            ),
            system.span,
        )
        val step = step(
            "eliminate",
            system,
            final,
            StepOperation.Substitute,
            SolverRuleRegistry.ELIMINATION,
            listOf(ExpressionPath(listOf(0)), ExpressionPath(listOf(1))),
            listOf(StepDetail("Determinant", determinant.toString())),
        )
        val values = mapOf(xName to x, yName to y)
        return success(input, system, classification, listOf(step), final, "$xName = $x, $yName = $y", SolverVerifier.system(system, values))
    }

    private fun step(
        id: String,
        before: MathExpression,
        after: MathExpression,
        operation: StepOperation,
        ruleId: String,
        affectedTerms: List<ExpressionPath>,
        details: List<StepDetail> = emptyList(),
    ): SolutionStep {
        val rule = SolverRuleRegistry.get(ruleId)
        return SolutionStep(
            id = id,
            before = before,
            after = after,
            operation = operation,
            ruleId = ruleId,
            explanationKey = ruleId,
            explanation = rule.explanation,
            affectedTerms = affectedTerms,
            optionalDetails = details,
            reversible = rule.preservesEquivalence,
        )
    }

    private fun success(
        input: SolverInput,
        expression: MathExpression,
        classification: ProblemClassification,
        steps: List<SolutionStep>,
        final: MathExpression,
        answer: String,
        verification: com.indianservers.aiexplorer.solver.domain.model.VerificationResult,
    ) = SolverSolution(
        input, expression, classification, steps, final, answer, verification,
        supported = verification.status != VerificationStatus.Failed,
        message = if (verification.status == VerificationStatus.Failed) "Verification failed; the result is not presented as correct." else "Solved fully offline.",
    )

    private fun unsupported(input: SolverInput, expression: MathExpression, classification: ProblemClassification, message: String) =
        SolverSolution(input, expression, classification, emptyList(), null, null, SolverVerifier.unsupported(message), false, message)

    private fun emptyFailure(text: String, message: String) = SolverSolution(
        SolverInput(text, ""),
        null,
        unsupportedClassification(message),
        emptyList(),
        null,
        null,
        SolverVerifier.unsupported(message),
        false,
        message,
    )

    private fun parseExpression(text: String): MathExpression? = when (val result = parser.parse(text)) {
        is SolverParseResult.Success -> result.expression
        is SolverParseResult.Error -> null
    }

    private fun unsupportedClassification(reason: String) =
        ProblemClassification(ProblemType.UnsupportedOrAmbiguous, 1f, listOf(reason))

    private fun flip(operator: InequalityOperator) = when (operator) {
        InequalityOperator.Less -> InequalityOperator.Greater
        InequalityOperator.LessOrEqual -> InequalityOperator.GreaterOrEqual
        InequalityOperator.Greater -> InequalityOperator.Less
        InequalityOperator.GreaterOrEqual -> InequalityOperator.LessOrEqual
    }

    private fun ratioParts(expression: MathExpression): Pair<MathExpression, MathExpression>? = when (expression) {
        is MathExpression.Fraction -> expression.numerator to expression.denominator
        is MathExpression.Function -> expression.arguments.takeIf { expression.name == "ratio" && it.size == 2 }?.let { it[0] to it[1] }
        else -> null
    }

    private enum class SymbolicAction { Simplify, Expand, Factor }
}
