package com.indianservers.aiexplorer.solver.domain.parser

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.solver.domain.model.BinaryOperator
import com.indianservers.aiexplorer.solver.domain.model.InequalityOperator
import com.indianservers.aiexplorer.solver.domain.model.MathExpression
import com.indianservers.aiexplorer.solver.domain.model.SolverParseError
import com.indianservers.aiexplorer.solver.domain.model.SolverParseResult
import com.indianservers.aiexplorer.solver.domain.model.SourceSpan
import com.indianservers.aiexplorer.solver.domain.model.UnaryOperator

object SolverInputNormalizer {
    private val superscripts = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
    )

    fun normalize(source: String): String {
        var text = source
            .replace(Regex("""√\s*([A-Za-zπ_][A-Za-z0-9π_]*|\d+(?:\.\d+)?)"""), "sqrt($1)")
            .replace('−', '-')
            .replace('–', '-')
            .replace('×', '*')
            .replace('·', '*')
            .replace('÷', '/')
            .replace('≤', '≤')
            .replace('≥', '≥')
            .replace('π', 'π')
            .replace("√", "sqrt")
            .replace(Regex("(?i)\\bpi\\b"), "π")
            .trim()
        text = text.replace(Regex("(?i)^\\s*(solve|simplify|expand|factorise|factorize|factor|check)\\s*:?[ ]*"), "")
        text = text.replace(Regex("(?i)\\s+of\\s+"), "*")
        text = normalizeSuperscripts(text)
        text = Regex("(\\d+)\\s+(\\d+)\\s*/\\s*(\\d+)").replace(text) { match ->
            "(${match.groupValues[1]}+${match.groupValues[2]}/${match.groupValues[3]})"
        }
        return text
    }

    private fun normalizeSuperscripts(source: String): String {
        val out = StringBuilder()
        var inRun = false
        source.forEach { character ->
            val mapped = superscripts[character]
            if (mapped != null) {
                if (!inRun) out.append('^')
                out.append(mapped)
                inRun = true
            } else {
                out.append(character)
                inRun = false
            }
        }
        return out.toString()
    }
}

private enum class TokenType {
    Number, Identifier, Plus, Minus, Star, Slash, Caret, Percent, LeftParen, RightParen,
    Comma, Semicolon, Colon, Equal, Less, LessEqual, Greater, GreaterEqual, Bar, End,
}

private data class Token(val type: TokenType, val text: String, val span: SourceSpan)
private class ParseFailure(val detail: SolverParseError) : RuntimeException(detail.message)

private class Tokenizer(private val source: String) {
    private var index = 0

    fun tokenize(): List<Token> {
        val output = mutableListOf<Token>()
        while (index < source.length) {
            if (source[index].isWhitespace()) {
                index++
                continue
            }
            val start = index
            val token = when (val character = source[index]) {
                '+' -> single(TokenType.Plus)
                '-' -> single(TokenType.Minus)
                '*' -> single(TokenType.Star)
                '/' -> single(TokenType.Slash)
                '^' -> single(TokenType.Caret)
                '%' -> single(TokenType.Percent)
                '(' -> single(TokenType.LeftParen)
                ')' -> single(TokenType.RightParen)
                ',' -> single(TokenType.Comma)
                ';' -> single(TokenType.Semicolon)
                ':' -> single(TokenType.Colon)
                '=' -> single(TokenType.Equal)
                '|' -> single(TokenType.Bar)
                '<' -> if (peekNext('=')) double(TokenType.LessEqual) else single(TokenType.Less)
                '>' -> if (peekNext('=')) double(TokenType.GreaterEqual) else single(TokenType.Greater)
                '≤' -> single(TokenType.LessEqual)
                '≥' -> single(TokenType.GreaterEqual)
                else -> when {
                    character.isDigit() || character == '.' -> number()
                    character.isLetter() || character == 'π' || character == '_' -> identifier()
                    else -> throw ParseFailure(SolverParseError("Unsupported symbol '$character'.", SourceSpan(start, start + 1)))
                }
            }
            output += token
        }
        output += Token(TokenType.End, "", SourceSpan(source.length, source.length))
        return output
    }

    private fun single(type: TokenType): Token {
        val start = index++
        return Token(type, source.substring(start, index), SourceSpan(start, index))
    }

    private fun double(type: TokenType): Token {
        val start = index
        index += 2
        return Token(type, source.substring(start, index), SourceSpan(start, index))
    }

    private fun peekNext(expected: Char): Boolean = index + 1 < source.length && source[index + 1] == expected

    private fun number(): Token {
        val start = index
        var dotSeen = false
        while (index < source.length) {
            val character = source[index]
            if (character == '.') {
                if (dotSeen) break
                dotSeen = true
                index++
            } else if (character.isDigit()) {
                index++
            } else break
        }
        val text = source.substring(start, index)
        if (text == ".") throw ParseFailure(SolverParseError("A decimal point needs digits.", SourceSpan(start, index), setOf("number")))
        return Token(TokenType.Number, text, SourceSpan(start, index))
    }

    private fun identifier(): Token {
        val start = index
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_' || source[index] == 'π')) index++
        return Token(TokenType.Identifier, source.substring(start, index), SourceSpan(start, index))
    }
}

class SolverParser {
    fun parse(raw: String): SolverParseResult {
        val normalized = SolverInputNormalizer.normalize(raw)
        if (normalized.isBlank()) {
            return SolverParseResult.Error(SolverParseError("Enter an expression.", SourceSpan(0, 0)), normalized)
        }
        return try {
            val parser = ParserState(Tokenizer(normalized).tokenize())
            SolverParseResult.Success(parser.parseDocument(), normalized)
        } catch (failure: ParseFailure) {
            SolverParseResult.Error(failure.detail, normalized)
        } catch (failure: ArithmeticException) {
            SolverParseResult.Error(SolverParseError(failure.message ?: "Invalid number.", SourceSpan(0, normalized.length)), normalized)
        } catch (failure: IllegalArgumentException) {
            SolverParseResult.Error(SolverParseError(failure.message ?: "Invalid expression.", SourceSpan(0, normalized.length)), normalized)
        }
    }
}

private class ParserState(private val tokens: List<Token>) {
    private var current = 0
    private var absoluteDepth = 0

    fun parseDocument(): MathExpression {
        val expressions = mutableListOf(parseRelation())
        while (match(TokenType.Semicolon)) expressions += parseRelation()
        expect(TokenType.End, "end of expression")
        if (expressions.size == 1) return expressions.single()
        val equations = expressions.mapNotNull { it as? MathExpression.Equation }
        if (equations.size == expressions.size) {
            return MathExpression.SystemOfEquations(equations, span(expressions.first(), expressions.last()))
        }
        return MathExpression.ExpressionList(expressions, span(expressions.first(), expressions.last()))
    }

    private fun parseRelation(): MathExpression {
        val left = parseExpressionList()
        val operator = advanceIf(
            TokenType.Equal, TokenType.Less, TokenType.LessEqual, TokenType.Greater, TokenType.GreaterEqual,
        ) ?: return left
        val right = parseExpressionList()
        return when (operator.type) {
            TokenType.Equal -> MathExpression.Equation(left, right, span(left, right))
            TokenType.Less -> MathExpression.Inequality(left, InequalityOperator.Less, right, span(left, right))
            TokenType.LessEqual -> MathExpression.Inequality(left, InequalityOperator.LessOrEqual, right, span(left, right))
            TokenType.Greater -> MathExpression.Inequality(left, InequalityOperator.Greater, right, span(left, right))
            TokenType.GreaterEqual -> MathExpression.Inequality(left, InequalityOperator.GreaterOrEqual, right, span(left, right))
            else -> error("unreachable")
        }
    }

    private fun parseExpressionList(): MathExpression {
        val values = mutableListOf(parseAdditive())
        while (match(TokenType.Comma)) values += parseAdditive()
        return if (values.size == 1) values.single() else MathExpression.ExpressionList(values, span(values.first(), values.last()))
    }

    private fun parseAdditive(): MathExpression {
        var expression = parseMultiplicative()
        while (true) {
            expression = when {
                match(TokenType.Plus) -> binary(BinaryOperator.Add, expression, parseMultiplicative())
                match(TokenType.Minus) -> binary(BinaryOperator.Subtract, expression, parseMultiplicative())
                else -> return expression
            }
        }
    }

    private fun parseMultiplicative(): MathExpression {
        var expression = parseUnary()
        while (true) {
            expression = when {
                match(TokenType.Star) -> binary(BinaryOperator.Multiply, expression, parseUnary())
                match(TokenType.Slash) -> {
                    val denominator = parseUnary()
                    MathExpression.Fraction(expression, denominator, span(expression, denominator))
                }
                match(TokenType.Colon) -> {
                    val right = parseUnary()
                    MathExpression.Function("ratio", listOf(expression, right), span(expression, right))
                }
                startsImplicitFactor(peek().type) -> binary(BinaryOperator.Multiply, expression, parseUnary())
                else -> return expression
            }
        }
    }

    private fun parseUnary(): MathExpression = when {
        match(TokenType.Plus) -> previous().let { operator ->
            val operand = parseUnary()
            MathExpression.UnaryOperation(UnaryOperator.Positive, operand, SourceSpan(operator.span.start, operand.span.endExclusive))
        }
        match(TokenType.Minus) -> previous().let { operator ->
            val operand = parseUnary()
            MathExpression.UnaryOperation(UnaryOperator.Negative, operand, SourceSpan(operator.span.start, operand.span.endExclusive))
        }
        else -> parsePower()
    }

    private fun parsePower(): MathExpression {
        var expression = parsePostfix()
        if (match(TokenType.Caret)) {
            val exponent = parseUnary()
            expression = MathExpression.Power(expression, exponent, span(expression, exponent))
        }
        return expression
    }

    private fun parsePostfix(): MathExpression {
        var expression = parsePrimary()
        while (match(TokenType.Percent)) {
            expression = MathExpression.UnaryOperation(
                UnaryOperator.Percent,
                expression,
                SourceSpan(expression.span.start, previous().span.endExclusive),
            )
        }
        return expression
    }

    private fun parsePrimary(): MathExpression {
        if (match(TokenType.Number)) {
            val token = previous()
            return MathExpression.Number(ExactRational.parse(token.text), token.span)
        }
        if (match(TokenType.Identifier)) {
            val token = previous()
            val normalizedName = token.text.lowercase()
            if (match(TokenType.LeftParen)) {
                val arguments = mutableListOf<MathExpression>()
                if (!check(TokenType.RightParen)) {
                    do {
                        arguments += parseAdditive()
                    } while (match(TokenType.Comma))
                }
                val close = expect(TokenType.RightParen, "')'")
                if (normalizedName == "sqrt") {
                    if (arguments.size != 1) fail("sqrt needs one argument.", token.span, setOf("sqrt(expression)"))
                    return MathExpression.Root(arguments.single(), null, SourceSpan(token.span.start, close.span.endExclusive))
                }
                if (normalizedName == "root") {
                    if (arguments.size != 2) fail("root needs a radicand and degree.", token.span, setOf("root(expression, degree)"))
                    return MathExpression.Root(arguments[0], arguments[1], SourceSpan(token.span.start, close.span.endExclusive))
                }
                return MathExpression.Function(normalizedName, arguments, SourceSpan(token.span.start, close.span.endExclusive))
            }
            return if (normalizedName in setOf("π", "e")) MathExpression.Constant(normalizedName, token.span)
            else MathExpression.Variable(token.text, token.span)
        }
        if (match(TokenType.LeftParen)) {
            val open = previous()
            val expression = parseRelation()
            val close = expect(TokenType.RightParen, "')'")
            return withSpan(expression, SourceSpan(open.span.start, close.span.endExclusive))
        }
        if (match(TokenType.Bar)) {
            val open = previous()
            absoluteDepth++
            val expression = try {
                parseAdditive()
            } finally {
                absoluteDepth--
            }
            val close = expect(TokenType.Bar, "closing '|'")
            return MathExpression.Function("abs", listOf(expression), SourceSpan(open.span.start, close.span.endExclusive))
        }
        val token = peek()
        fail(
            if (token.type == TokenType.End) "The expression ends before a value was provided." else "Unexpected token '${token.text}'.",
            token.span,
            setOf("number", "variable", "'('", "function"),
        )
    }

    private fun withSpan(expression: MathExpression, span: SourceSpan): MathExpression = when (expression) {
        is MathExpression.Number -> expression.copy(span = span)
        is MathExpression.Variable -> expression.copy(span = span)
        is MathExpression.Constant -> expression.copy(span = span)
        is MathExpression.UnaryOperation -> expression.copy(span = span)
        is MathExpression.BinaryOperation -> expression.copy(span = span)
        is MathExpression.Power -> expression.copy(span = span)
        is MathExpression.Root -> expression.copy(span = span)
        is MathExpression.Fraction -> expression.copy(span = span)
        is MathExpression.Function -> expression.copy(span = span)
        is MathExpression.Equation -> expression.copy(span = span)
        is MathExpression.Inequality -> expression.copy(span = span)
        is MathExpression.ExpressionList -> expression.copy(span = span)
        is MathExpression.SystemOfEquations -> expression.copy(span = span)
    }

    private fun binary(operator: BinaryOperator, left: MathExpression, right: MathExpression) =
        MathExpression.BinaryOperation(operator, left, right, span(left, right))

    private fun startsImplicitFactor(type: TokenType): Boolean =
        type in setOf(TokenType.Identifier, TokenType.LeftParen) || (type == TokenType.Bar && absoluteDepth == 0)

    private fun span(first: MathExpression, last: MathExpression) = SourceSpan(first.span.start, last.span.endExclusive)
    private fun peek() = tokens[current]
    private fun previous() = tokens[current - 1]
    private fun check(type: TokenType) = peek().type == type
    private fun match(type: TokenType): Boolean = if (check(type)) { current++; true } else false
    private fun advanceIf(vararg types: TokenType): Token? = types.firstOrNull(::check)?.let { tokens[current++] }
    private fun expect(type: TokenType, expected: String): Token {
        if (check(type)) return tokens[current++]
        val token = peek()
        fail("Expected $expected.", token.span, setOf(expected))
    }
    private fun fail(message: String, span: SourceSpan, expected: Set<String> = emptySet()): Nothing =
        throw ParseFailure(SolverParseError(message, span, expected))
}
