package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.model.BinaryOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.SolverParseResult
import com.indianservers.aiexplorer.solver.domain.model.children
import com.indianservers.aiexplorer.solver.domain.parser.SolverParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverParserPhase1Test {
    private val parser = SolverParser()

    @Test
    fun precedenceAndUnarySignsProduceTypedTrees() {
        val precedence = success("2 + 3 * 4")
        assertTrue(precedence is MathExpression.BinaryOperation)
        precedence as MathExpression.BinaryOperation
        assertEquals(BinaryOperator.Add, precedence.operator)
        assertTrue(precedence.right is MathExpression.BinaryOperation)
        assertEquals(BinaryOperator.Multiply, (precedence.right as MathExpression.BinaryOperation).operator)

        val unaryPower = success("-2^2")
        assertTrue(unaryPower is MathExpression.UnaryOperation)
        assertTrue((unaryPower as MathExpression.UnaryOperation).operand is MathExpression.Power)
    }

    @Test
    fun parserAcceptsAtLeastOneHundredDistinctValidPhase1Inputs() {
        val cases = buildList {
            for (i in 1..30) add("$i + ${i + 1} * ${i + 2}")
            for (i in 1..20) add("${i}x + ${i + 1}(x + 2)")
            for (i in 1..15) add("(($i + 2) * (3 + 4))")
            for (i in 1..15) add("$i/${i + 1} + 1/${i + 2}")
            for (i in 1..10) add("sqrt(${i * i}) + x^2")
            for (i in 1..10) add("${i}x + 3 = ${i + 3}")
            for (i in 1..10) add("${i}x - 2 <= ${i + 4}")
            for (i in 1..10) add("x + y = ${i + 2}; x - y = $i")
        }
        assertTrue("Expected at least 100 parser cases", cases.size >= 100)
        cases.forEachIndexed { index, source ->
            val result = parser.parse(source)
            assertTrue("Case $index failed to parse: $source -> $result", result is SolverParseResult.Success)
            assertSpansAreValid((result as SolverParseResult.Success).expression, result.normalized.length)
        }
    }

    @Test
    fun parserNormalizesKeyboardAndUnicodeNotation() {
        val cases = listOf(
            "2x" to MathExpression.BinaryOperation::class,
            "2(x+1)" to MathExpression.BinaryOperation::class,
            "3 1/2" to MathExpression.BinaryOperation::class,
            "5\u00D76" to MathExpression.BinaryOperation::class,
            "8\u00F72" to MathExpression.Fraction::class,
            "x\u00B2" to MathExpression.Power::class,
            "\u221A81" to MathExpression.Root::class,
            "|-7|" to MathExpression.Function::class,
            "12:18" to MathExpression.Function::class,
            "x\u22653" to MathExpression.Inequality::class,
        )
        cases.forEach { (source, expected) ->
            assertTrue("$source should normalize to ${expected.simpleName}", expected.isInstance(success(source)))
        }
    }

    @Test
    fun invalidSyntaxReturnsPositionedErrors() {
        val invalid = listOf(
            "",
            ".",
            "2+",
            "(2+3",
            "2+)",
            "sqrt()",
            "root(4)",
            "root(4,)",
            "1//2",
            "x=",
            "<3",
            "2**3",
            "|2+3",
            "2+@",
            "1.2.3",
        )
        invalid.forEach { source ->
            val result = parser.parse(source)
            assertTrue("$source should fail safely", result is SolverParseResult.Error)
            val error = (result as SolverParseResult.Error).error
            assertTrue(error.message.isNotBlank())
            assertTrue(error.span.start >= 0)
            assertTrue(error.span.endExclusive >= error.span.start)
        }
    }

    private fun success(source: String): MathExpression {
        val result = parser.parse(source)
        assertTrue("$source should parse: $result", result is SolverParseResult.Success)
        return (result as SolverParseResult.Success).expression
    }

    private fun assertSpansAreValid(expression: MathExpression, sourceLength: Int) {
        assertTrue(expression.span.start >= 0)
        assertTrue(expression.span.endExclusive >= expression.span.start)
        assertTrue(expression.span.endExclusive <= sourceLength)
        expression.children().forEach { assertSpansAreValid(it, sourceLength) }
    }
}
