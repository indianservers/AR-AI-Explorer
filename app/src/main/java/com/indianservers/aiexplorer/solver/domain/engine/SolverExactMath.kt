package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.model.BinaryOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.UnaryOperator
import java.math.BigInteger

internal object SolverExactMath {
    fun evaluate(expression: MathExpression, variables: Map<String, ExactRational> = emptyMap()): Result<ExactRational> =
        runCatching { evaluateOrThrow(expression, variables) }

    private fun evaluateOrThrow(expression: MathExpression, variables: Map<String, ExactRational>): ExactRational = when (expression) {
        is MathExpression.Number -> expression.value
        is MathExpression.Variable -> variables[expression.name] ?: error("Missing value for ${expression.name}")
        is MathExpression.Constant -> error("Exact evaluation of ${expression.name} is not available")
        is MathExpression.UnaryOperation -> when (expression.operator) {
            UnaryOperator.Positive -> evaluateOrThrow(expression.operand, variables)
            UnaryOperator.Negative -> -evaluateOrThrow(expression.operand, variables)
            UnaryOperator.Percent -> evaluateOrThrow(expression.operand, variables) / ExactRational.of(100)
        }
        is MathExpression.BinaryOperation -> {
            val left = evaluateOrThrow(expression.left, variables)
            val right = evaluateOrThrow(expression.right, variables)
            when (expression.operator) {
                BinaryOperator.Add -> left + right
                BinaryOperator.Subtract -> left - right
                BinaryOperator.Multiply -> left * right
                BinaryOperator.Divide -> left / right
            }
        }
        is MathExpression.Fraction -> evaluateOrThrow(expression.numerator, variables) / evaluateOrThrow(expression.denominator, variables)
        is MathExpression.Power -> {
            val base = evaluateOrThrow(expression.base, variables)
            val exponent = evaluateOrThrow(expression.exponent, variables)
            require(exponent.denominator == BigInteger.ONE && exponent.numerator.bitLength() < 31) { "Only integer powers are exact in Phase 1" }
            base.pow(exponent.numerator.toInt())
        }
        is MathExpression.Root -> {
            val value = evaluateOrThrow(expression.radicand, variables)
            val degree = expression.degree?.let { evaluateOrThrow(it, variables) } ?: ExactRational.of(2)
            require(degree == ExactRational.of(2)) { "Only exact square roots are supported in Phase 1" }
            require(value.numerator.signum() >= 0) { "Square root requires a non-negative value" }
            val numeratorRoot = perfectSquareRoot(value.numerator) ?: error("The square root is irrational")
            val denominatorRoot = perfectSquareRoot(value.denominator) ?: error("The square root is irrational")
            ExactRational.of(numeratorRoot, denominatorRoot)
        }
        is MathExpression.Function -> when (expression.name) {
            "abs" -> {
                val value = evaluateOrThrow(expression.arguments.single(), variables)
                if (value < ExactRational.ZERO) -value else value
            }
            else -> error("Exact function ${expression.name} is not supported")
        }
        is MathExpression.Equation, is MathExpression.Inequality,
        is MathExpression.ExpressionList, is MathExpression.SystemOfEquations -> error("A relation is not a scalar expression")
    }

    private fun perfectSquareRoot(value: BigInteger): BigInteger? {
        if (value.signum() < 0) return null
        var low = BigInteger.ZERO
        var high = value + BigInteger.ONE
        while (low + BigInteger.ONE < high) {
            val middle = (low + high).shiftRight(1)
            if (middle * middle <= value) low = middle else high = middle
        }
        return low.takeIf { it * it == value }
    }
}

internal data class LinearForm(
    val coefficients: Map<String, ExactRational>,
    val constant: ExactRational,
) {
    operator fun plus(other: LinearForm) = LinearForm(
        (coefficients.keys + other.coefficients.keys).associateWith { coefficients[it].orZero() + other.coefficients[it].orZero() }.filterValues { !it.isZero },
        constant + other.constant,
    )
    operator fun minus(other: LinearForm) = this + other.scale(-ExactRational.ONE)
    fun scale(value: ExactRational) = LinearForm(coefficients.mapValues { it.value * value }.filterValues { !it.isZero }, constant * value)
}

internal fun linearForm(expression: MathExpression): LinearForm? = when (expression) {
    is MathExpression.Number -> LinearForm(emptyMap(), expression.value)
    is MathExpression.Variable -> LinearForm(mapOf(expression.name to ExactRational.ONE), ExactRational.ZERO)
    is MathExpression.Constant -> null
    is MathExpression.UnaryOperation -> when (expression.operator) {
        UnaryOperator.Positive -> linearForm(expression.operand)
        UnaryOperator.Negative -> linearForm(expression.operand)?.scale(-ExactRational.ONE)
        UnaryOperator.Percent -> linearForm(expression.operand)?.scale(ExactRational.of(1) / ExactRational.of(100))
    }
    is MathExpression.BinaryOperation -> {
        val left = linearForm(expression.left) ?: return null
        val right = linearForm(expression.right) ?: return null
        when (expression.operator) {
            BinaryOperator.Add -> left + right
            BinaryOperator.Subtract -> left - right
            BinaryOperator.Multiply -> when {
                left.coefficients.isEmpty() -> right.scale(left.constant)
                right.coefficients.isEmpty() -> left.scale(right.constant)
                else -> null
            }
            BinaryOperator.Divide -> if (right.coefficients.isEmpty() && !right.constant.isZero) left.scale(ExactRational.ONE / right.constant) else null
        }
    }
    is MathExpression.Fraction -> {
        val numerator = linearForm(expression.numerator) ?: return null
        val denominator = SolverExactMath.evaluate(expression.denominator).getOrNull() ?: return null
        if (denominator.isZero) null else numerator.scale(ExactRational.ONE / denominator)
    }
    is MathExpression.Power -> {
        val exponent = SolverExactMath.evaluate(expression.exponent).getOrNull()
        when (exponent) {
            ExactRational.ZERO -> LinearForm(emptyMap(), ExactRational.ONE)
            ExactRational.ONE -> linearForm(expression.base)
            else -> null
        }
    }
    is MathExpression.Root, is MathExpression.Function,
    is MathExpression.Equation, is MathExpression.Inequality,
    is MathExpression.ExpressionList, is MathExpression.SystemOfEquations -> null
}

private fun ExactRational?.orZero() = this ?: ExactRational.ZERO

