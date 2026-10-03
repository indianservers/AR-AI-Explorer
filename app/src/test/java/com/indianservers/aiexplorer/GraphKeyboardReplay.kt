package com.indianservers.aiexplorer

import androidx.compose.ui.text.input.TextFieldValue
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.input.*
import com.indianservers.aiexplorer.spatial.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Replays actual key definitions through the exact dispatcher used by the Compose keyboard.
 * This checks editing and graph engines, not Android touch dispatch or popup layout.
 */
internal object GraphKeyboardReplay {
    private val keys = basicNumberPadRows.flatten() + letterKeys + symbolKeys + functionKeys +
        trigonometryKeys(false) + inverseFunctionKeys + advancedNotationKeys.flatMap { it.variants }

    fun enter(source: String): TextFieldValue {
        var value = TextFieldValue("")
        val frames = ArrayDeque<Boolean>()
        var index = 0
        while (index < source.length) {
            val ch = source[index]
            if (ch == '^' && source.getOrNull(index + 1)?.isDigit() == true) {
                val digit = keys.first { it.insertion == source[index + 1].toString() }
                value = applyMathKeyboardKey(value, numberPowerLongPressKey(digit)!!)
                index += 2
            } else {
                val function = Regex("[a-z]+\\(").find(source, index)?.takeIf { it.range.first == index }
                val template = function?.value?.dropLast(1)?.let { name ->
                    keys.firstOrNull { it.insertion.replace("%s", "") == "$name()" }
                }
                if (template != null) {
                    value = applyMathKeyboardKey(value, template)
                    frames.addLast(true)
                    index += function.value.length
                } else if (ch == ')' && frames.lastOrNull() == true) {
                    value = StructuredMathEditing.move(value, 1)
                    frames.removeLast()
                    index++
                } else {
                    val token = if (source.startsWith("<=", index)) "<=" else ch.toString()
                    val key = keys.firstOrNull { it.insertion == token }
                        ?: error("No MathKeyboard key for $token in $source")
                    value = applyMathKeyboardKey(value, key)
                    if (ch == '(') frames.addLast(false)
                    if (ch == ')' && frames.isNotEmpty()) frames.removeLast()
                    index += token.length
                }
            }
            assertTrue("Cursor out of range: $source", value.selection.min >= 0 && value.selection.max <= value.text.length)
        }
        return StructuredMathCodec.toParser(value)
    }

    fun verify(case: KeyboardGraphCase) {
        val entered = enter(case.source).text
        if (case.kind == "invalid") {
            val rejected = runCatching {
                require(MathInputIntelligence.analyze(entered).validBrackets)
                if (case.dimension == 2) {
                    val parsed = TypedGraphExpressionParser.parse(entered)
                    val sample = TypedGraphEngine().sample(parsed, samples = 48)
                    require(sample.curves.isNotEmpty() || sample.implicitSegments.isNotEmpty())
                } else {
                    val parsed = SurfaceInputInterpreter.interpret(entered).getOrThrow()
                    when (parsed.kind) {
                        SpatialSurfaceKind.Explicit -> ExpressionEngine().compile(parsed.expression)
                        SpatialSurfaceKind.Implicit -> {
                            val sides = parsed.expression.split('=', limit = 2)
                            ExpressionEngine().compile("(${sides[0]})-(${sides[1]})")
                        }
                        else -> error("Invalid parametric input should be rejected")
                    }
                }
            }.isFailure
            assertTrue("${case.id} should reject $entered", rejected)
            return
        }
        assertTrue("${case.id} unbalanced: $entered", MathInputIntelligence.analyze(entered).validBrackets)
        fun canonical(source: String) = MathExpressionNormalizer.normalize(source)
            .replace(Regex("\\^\\(([0-9])\\)"), "^$1").replace(" ", "")
        assertEquals("${case.id} keyboard must preserve the intended expression", canonical(case.source), canonical(entered))
        if (case.dimension == 2) verify2d(case, entered) else verify3d(case, entered)
    }

    private fun verify2d(c: KeyboardGraphCase, entered: String) {
        val definition = TypedGraphExpressionParser.parse(entered)
        val graph = TypedGraphEngine()
        val sample = graph.sample(definition, GraphDomain(-4.0, 4.0), GraphDomain(-4.0, 4.0, "y"), samples = 96)
        val points = sample.curves.flatMap { it.points } + sample.points
        assertTrue(points.all { it.x.isFinite() && it.y.isFinite() })
        when (c.kind) {
            "explicit" -> {
                assertTrue(definition is TypedGraphExpression.Explicit)
                assertEquals(c.id, c.expected, graph.evaluate(definition, c.x)!!.y, 1e-8)
                assertTrue(c.id, points.isNotEmpty())
            }
            "polar", "parametric" -> {
                val point = graph.evaluate(definition, 0.0)!!
                assertEquals(c.id, c.expected, point.x, 1e-8)
                assertEquals(c.id, 0.0, point.y, 1e-8)
                assertTrue(c.id, points.isNotEmpty())
            }
            "implicit" -> {
                assertTrue(definition is TypedGraphExpression.Implicit)
                val residual = (definition as TypedGraphExpression.Implicit).residual
                assertEquals(c.id, 0.0, ExpressionEngine().compile(residual).eval(mapOf("x" to c.x, "y" to c.y)), 1e-8)
                assertTrue(c.id, sample.implicitSegments.isNotEmpty())
            }
            "inequality" -> {
                assertTrue(definition is TypedGraphExpression.Inequality)
                assertTrue(c.id, sample.inequalityCells.isNotEmpty())
            }
        }
    }

    private fun verify3d(c: KeyboardGraphCase, entered: String) {
        val parsed = SurfaceInputInterpreter.interpret(entered).getOrThrow()
        val engine = ExpressionEngine()
        when (c.kind) {
            "explicit" -> {
                assertEquals(SpatialSurfaceKind.Explicit, parsed.kind)
                assertEquals(c.id, c.expected, engine.compile(parsed.expression).eval(mapOf("x" to c.x, "y" to c.y)), 1e-8)
                val mesh = Graph3D().mesh(parsed.expression, min = -1.0, max = 1.0, density = 8)
                assertEquals(81, mesh.vertices.size)
                assertTrue(mesh.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
                assertEquals("${c.id} mesh must preserve mathematical height", c.expected, mesh.vertices[6 * 9 + 2].z, 1e-8)
            }
            "implicit", "parametric" -> {
                val definition = if (c.kind == "implicit") {
                    assertEquals(SpatialSurfaceKind.Implicit, parsed.kind)
                    val sides = parsed.expression.split('=', limit = 2)
                    assertEquals(c.id, 0.0, engine.compile("(${sides[0]})-(${sides[1]})").eval(mapOf("x" to c.x, "y" to c.y, "z" to c.z)), 1e-8)
                    SurfaceDefinition3D.Implicit(c.id, parsed.expression)
                } else {
                    assertEquals(SpatialSurfaceKind.Parametric, parsed.kind)
                    assertEquals(c.id, c.expected, engine.compile(parsed.expressionZ).eval(mapOf("u" to c.x, "v" to c.y)), 1e-8)
                    SurfaceDefinition3D.Parametric(c.id, parsed.expression, parsed.expressionY, parsed.expressionZ)
                }
                val geometry = TypedSurfaceMesher().mesh(definition, density = 12).geometry
                assertTrue(c.id, geometry.vertices.isNotEmpty())
                assertTrue(c.id, geometry.vertices.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
            }
        }
    }
}
