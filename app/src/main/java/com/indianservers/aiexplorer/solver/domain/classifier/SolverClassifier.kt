package com.indianservers.aiexplorer.solver.domain.classifier

import com.indianservers.aiexplorer.solver.domain.model.BinaryOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.ProblemClassification
import com.indianservers.aiexplorer.solver.domain.model.ProblemType
import com.indianservers.aiexplorer.solver.domain.model.containsFraction
import com.indianservers.aiexplorer.solver.domain.model.containsPower
import com.indianservers.aiexplorer.solver.domain.model.variables

class SolverProblemClassifier {
    fun classify(originalInput: String, expression: MathExpression): ProblemClassification {
        val source = originalInput.trim().lowercase()
        val variables = expression.variables()
        val evidence = mutableListOf<String>()
        fun result(type: ProblemType, confidence: Float, vararg reasons: String): ProblemClassification =
            ProblemClassification(type, confidence, evidence + reasons)

        val unsupportedFunctions = expression.functionNames() - setOf("abs", "ratio")
        if (unsupportedFunctions.isNotEmpty()) {
            return result(
                ProblemType.UnsupportedOrAmbiguous,
                .99f,
                "Phase 1 does not solve function(s): ${unsupportedFunctions.sorted().joinToString()}",
            )
        }
        if (expression is MathExpression.SystemOfEquations) {
            evidence += "${expression.equations.size} equations were separated by semicolons"
            return if (expression.equations.size == 2 && variables.size == 2) {
                result(ProblemType.SimultaneousLinearEquations, .99f, "two variables were detected")
            } else result(ProblemType.UnsupportedOrAmbiguous, .98f, "Phase 1 supports exactly two linear equations in two variables")
        }
        if (expression is MathExpression.Inequality) {
            return if (variables.size == 1) result(ProblemType.LinearInequality, .98f, "one inequality relation and one variable were detected")
            else result(ProblemType.UnsupportedOrAmbiguous, .96f, "Phase 1 inequalities require one variable")
        }
        if (expression is MathExpression.Equation) {
            val numericFractionEquality =
                variables.isEmpty() && (expression.left.containsFraction() || expression.right.containsFraction())
            if (containsRatio(expression) || numericFractionEquality || "proportion" in source || "ratio" in source) {
                return result(ProblemType.RatioOrProportion, .96f, "an equality between ratios or fractions was detected")
            }
            return if (variables.size == 1) result(ProblemType.LinearEquation, .98f, "one equality and one variable were detected")
            else result(ProblemType.UnsupportedOrAmbiguous, .95f, "Phase 1 equations require one variable unless entered as a two-equation system")
        }
        if ('%' in originalInput) return result(ProblemType.PercentageProblem, .99f, "the percent symbol was detected")
        if (containsRatio(expression) || ':' in originalInput) return result(ProblemType.RatioOrProportion, .99f, "ratio notation was detected")
        if (variables.isEmpty()) {
            return if (expression.containsFraction()) result(ProblemType.FractionSimplification, .96f, "exact fraction nodes were detected")
            else result(ProblemType.ArithmeticExpression, .98f, "the expression contains no variables")
        }
        if (source.startsWith("factor") || source.startsWith("factorise") || source.startsWith("factorize")) {
            return result(ProblemType.Factorisation, .99f, "an explicit factorisation command was detected")
        }
        if (source.startsWith("expand") || containsExpandableProduct(expression)) {
            return result(ProblemType.Expansion, if (source.startsWith("expand")) .99f else .86f, "a product or power of a sum was detected")
        }
        if (expression.containsPower()) return result(ProblemType.PolynomialOperation, .91f, "variable powers were detected")
        return result(ProblemType.AlgebraicSimplification, .90f, "variables occur without an equation relation")
    }

    private fun containsRatio(expression: MathExpression): Boolean =
        (expression is MathExpression.Function && expression.name == "ratio") ||
            expression.childrenForClassifier().any(::containsRatio)

    private fun containsExpandableProduct(expression: MathExpression): Boolean = when (expression) {
        is MathExpression.BinaryOperation ->
            expression.operator == BinaryOperator.Multiply &&
                (isSum(expression.left) || isSum(expression.right)) ||
                containsExpandableProduct(expression.left) ||
                containsExpandableProduct(expression.right)
        is MathExpression.Power -> isSum(expression.base)
        else -> expression.childrenForClassifier().any(::containsExpandableProduct)
    }

    private fun isSum(expression: MathExpression): Boolean =
        expression is MathExpression.BinaryOperation &&
            expression.operator in setOf(BinaryOperator.Add, BinaryOperator.Subtract)
}

private fun MathExpression.childrenForClassifier(): List<MathExpression> = when (this) {
    is MathExpression.Number, is MathExpression.Variable, is MathExpression.Constant -> emptyList()
    is MathExpression.UnaryOperation -> listOf(operand)
    is MathExpression.BinaryOperation -> listOf(left, right)
    is MathExpression.Power -> listOf(base, exponent)
    is MathExpression.Root -> listOfNotNull(radicand, degree)
    is MathExpression.Fraction -> listOf(numerator, denominator)
    is MathExpression.Function -> arguments
    is MathExpression.Equation -> listOf(left, right)
    is MathExpression.Inequality -> listOf(left, right)
    is MathExpression.ExpressionList -> values
    is MathExpression.SystemOfEquations -> equations
}

private fun MathExpression.functionNames(): Set<String> = buildSet {
    if (this@functionNames is MathExpression.Function) add(this@functionNames.name)
    this@functionNames.childrenForClassifier().forEach { addAll(it.functionNames()) }
}
