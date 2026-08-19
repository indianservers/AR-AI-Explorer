package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.MathExpressionNormalizer
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
import java.util.Locale
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphExpressionAccuracyTest {
    private val tolerance = 1e-5
    private fun number(value: Double) = String.format(Locale.US, "%.6f", value)

    @Test
    fun graph2dEvaluatesMoreThanTwentyRepresentativeExpressions() {
        data class Case(val source: String, val x: Double, val expected: Double)

        val cases = listOf(
            Case("tan(12)", 0.0, tan(Math.toRadians(12.0))),
            Case("sin(30)", 0.0, 0.5),
            Case("cos(60)", 0.0, 0.5),
            Case("sin(pi/2)", 0.0, 1.0),
            Case("cos(pi)", 0.0, -1.0),
            Case("y=x^2", 3.0, 9.0),
            Case("f(x)=2x+1", 4.0, 9.0),
            Case("sqrt(x)", 9.0, 3.0),
            Case("abs(x)", -4.0, 4.0),
            Case("exp(x)", 0.0, 1.0),
            Case("ln(e)", 0.0, 1.0),
            Case("log(100)", 0.0, 2.0),
            Case("min(3,5)", 0.0, 3.0),
            Case("max(3,5)", 0.0, 5.0),
            Case("floor(2.9)", 0.0, 2.0),
            Case("ceil(2.1)", 0.0, 3.0),
            Case("if(x>0,x,-x)", -3.0, 3.0),
            Case("derivative(x^2,x)", 3.0, 6.0),
            Case("integral(x,x,0,2)", 0.0, 2.0),
            Case("sum(n,n,1,4)", 0.0, 10.0),
            Case("product(n,n,1,4)", 0.0, 24.0),
            Case("2(x+1)", 2.0, 6.0),
            Case("1/(x+1)", 1.0, 0.5),
            Case("sin(x)", PI / 2.0, 1.0),
            Case("tan(x)", 0.0, 0.0),
            Case("x^3-2x+1", 2.0, 5.0),
            Case("(x-1)(x+1)", 3.0, 8.0),
        )

        val graph = TypedGraphEngine()
        cases.forEach { case ->
            val definition = TypedGraphExpressionParser.parse(case.source)
            val point = graph.evaluate(definition, case.x)
            assertEquals("${case.source} at x=${case.x}", case.expected, point?.y ?: Double.NaN, tolerance)
        }
    }

    @Test
    fun graph2dNormalizesGeoGebraStyleTrigPowers() {
        val engine = ExpressionEngine()
        val graph = TypedGraphEngine()
        val x = PI / 2.0

        assertEquals("(sin(x))^2", MathExpressionNormalizer.normalize("sin\u00B2(x)"))
        assertEquals(1.0, engine.compile("sin\u00B2(x)").eval(mapOf("x" to x)), tolerance)
        assertEquals(1.0, engine.compile("sin^(2)(x)").eval(mapOf("x" to x)), tolerance)
        assertEquals(0.0, engine.compile("cos\u00B2(x)").eval(mapOf("x" to x)), tolerance)
        assertEquals(1.0, engine.compile("tan\u00B2(x/2)").eval(mapOf("x" to x)), tolerance)
        assertEquals(1.0, engine.compile("tan^2(x/2)").eval(mapOf("x" to x)), tolerance)
        assertEquals(1.0, engine.compile("tan^(2)(x/2)").eval(mapOf("x" to x)), tolerance)

        val definition = TypedGraphExpressionParser.parse("f(x)=sin\u00B2(x)")
        assertEquals(1.0, graph.evaluate(definition, x)?.y ?: Double.NaN, tolerance)
        val tangentDefinition = TypedGraphExpressionParser.parse("f(x)=tan\u00B2(x)")
        assertEquals(1.0, graph.evaluate(tangentDefinition, PI / 4.0)?.y ?: Double.NaN, tolerance)
    }

    @Test
    fun graph2dTypedModesRemainAccurate() {
        val graph = TypedGraphEngine()

        val piecewise = TypedGraphExpressionParser.parse("piecewise{x<0:-x;x>=0:x}")
        assertEquals(2.0, graph.evaluate(piecewise, -2.0)?.y ?: Double.NaN, tolerance)
        assertEquals(3.0, graph.evaluate(piecewise, 3.0)?.y ?: Double.NaN, tolerance)

        val parametric = TypedGraphExpressionParser.parse("x(t)=cos(t); y(t)=sin(t)")
        val parametricPoint = graph.evaluate(parametric, PI / 2.0)
        assertEquals(0.0, parametricPoint?.x ?: Double.NaN, tolerance)
        assertEquals(1.0, parametricPoint?.y ?: Double.NaN, tolerance)

        val polar = TypedGraphExpressionParser.parse("r=2")
        val polarPoint = graph.evaluate(polar, PI)
        assertEquals(-2.0, polarPoint?.x ?: Double.NaN, tolerance)
        assertEquals(0.0, polarPoint?.y ?: Double.NaN, tolerance)

        val inequality = TypedGraphExpressionParser.parse("x<=2")
        val inequalitySample = graph.sample(inequality, samples = 40)
        assertTrue("x<=2 should generate a shaded inequality region", inequalitySample.inequalityCells.isNotEmpty())
        val predicate = ExpressionEngine().compile("x<=2")
        assertEquals(1.0, predicate.eval(mapOf("x" to 2.0)), tolerance)
        assertEquals(0.0, predicate.eval(mapOf("x" to 3.0)), tolerance)
    }

    @Test
    fun graph2dEvaluatesOneHundredPlusComplexExpressions() {
        data class Case(val source: String, val x: Double, val expected: Double)

        val cases = mutableListOf<Case>()
        for (i in 1..25) {
            val x = (i - 13) / 5.0
            cases += Case("f(x)=${i}x+${i + 1}", x, i * x + i + 1.0)
        }
        for (i in 1..20) {
            val a = i / 10.0
            val x = 0.2 + i / 25.0
            cases += Case("sin(x+${number(a)})+cos(x-${number(a / 2.0)})", x, sin(x + a) + cos(x - a / 2.0))
        }
        for (i in 1..20) {
            val a = i / 12.0
            val x = 0.1 + i / 30.0
            cases += Case("sin^2(x+${number(a)})+cos^2(x+${number(a)})", x, 1.0)
        }
        for (i in 1..20) {
            val a = i / 8.0
            val x = (i - 10) / 6.0
            cases += Case("sqrt((x+${number(a)})^2)+abs(x-${number(a)})", x, abs(x + a) + abs(x - a))
        }
        for (i in 1..20) {
            val a = i / 7.0
            val x = i / 9.0
            cases += Case("min(x,${number(a)})+max(x,${number(a + 1.0)})", x, min(x, a) + max(x, a + 1.0))
        }
        for (i in 1..20) {
            val a = i / 10.0
            val x = if (i % 2 == 0) a + 0.25 else a - 0.25
            cases += Case("if(x>${number(a)},x^2,${number(a)}x)", x, if (x > a) x.pow(2.0) else a * x)
        }
        for (i in 1..20) {
            val a = i / 15.0
            val x = 1.0 + i / 20.0
            cases += Case("ln(exp(x-${number(a)}))+log(100)+floor(x)+ceil(x-${number(a)})", x, (x - a) + 2.0 + floor(x) + ceil(x - a))
        }

        assertTrue("Expected at least 100 2D expression cases", cases.size >= 100)
        val graph = TypedGraphEngine()
        cases.forEach { case ->
            val definition = TypedGraphExpressionParser.parse(case.source)
            val point = graph.evaluate(definition, case.x)
            assertEquals("${case.source} at x=${case.x}", case.expected, point?.y ?: Double.NaN, 1e-4)
        }
    }

    @Test
    fun graph3dEvaluatesMoreThanTwentyRepresentativeSurfaces() {
        data class Case(val source: String, val x: Double, val y: Double, val expected: Double)

        val cases = listOf(
            Case("z=x^2+y^2", 3.0, 4.0, 25.0),
            Case("z=sin(30)+cos(60)", 0.0, 0.0, 1.0),
            Case("z=tan(12)", 0.0, 0.0, tan(Math.toRadians(12.0))),
            Case("z=sin(x)+cos(y)", 0.0, 0.0, 1.0),
            Case("z=sqrt(x^2+y^2)", 3.0, 4.0, 5.0),
            Case("z=abs(x-y)", 2.0, 5.0, 3.0),
            Case("x+y=z", 2.0, 3.0, 5.0),
            Case("f(x,y)=x*y", 3.0, 4.0, 12.0),
            Case("x+y", 2.0, 3.0, 5.0),
            Case("z=exp(0)", 0.0, 0.0, 1.0),
            Case("z=ln(e)", 0.0, 0.0, 1.0),
            Case("z=log(1000)", 0.0, 0.0, 3.0),
            Case("z=min(x,y)", 2.0, 5.0, 2.0),
            Case("z=max(x,y)", 2.0, 5.0, 5.0),
            Case("z=if(x>y,x,y)", 7.0, 4.0, 7.0),
            Case("z=derivative(x^2+y,x)", 3.0, 2.0, 6.0),
            Case("z=partial(x*y,x)", 3.0, 4.0, 4.0),
            Case("z=integral(t,t,0,x)", 2.0, 0.0, 2.0),
            Case("z=sum(n,n,1,4)", 0.0, 0.0, 10.0),
            Case("z=2x+3y", 2.0, 4.0, 16.0),
            Case("z=(x+1)(y-1)", 2.0, 5.0, 12.0),
            Case("z=floor(x)+ceil(y)", 2.8, 3.1, 6.0),
            Case("z=1/(1+x^2+y^2)", 1.0, 1.0, 1.0 / 3.0),
            Case("z=sin(pi/2)*cos(0)", 0.0, 0.0, 1.0),
            Case("z=sin^2(x)+cos^2(x)+tan^2(y)", PI / 3.0, PI / 4.0, 2.0),
            Case("z=sin\u00B2(x)+cos\u00B2(x)+tan\u00B2(y)", PI / 3.0, PI / 4.0, 2.0),
            Case("z=e^0+pi-pi", 0.0, 0.0, 1.0),
        )

        val engine = ExpressionEngine()
        cases.forEach { case ->
            val interpretation = SurfaceInputInterpreter.explicit(case.source).getOrThrow()
            val result = engine.compile(interpretation.expression).eval(mapOf("x" to case.x, "y" to case.y))
            assertEquals("${case.source} at (${case.x}, ${case.y})", case.expected, result, tolerance)
            assertTrue("${case.source} must produce a finite surface value", result.isFinite())
        }
    }

    @Test
    fun graph3dEvaluatesOneHundredPlusComplexSurfaces() {
        data class Case(val source: String, val x: Double, val y: Double, val expected: Double)

        val cases = mutableListOf<Case>()
        for (i in 1..25) {
            val x = (i - 8) / 6.0
            val y = (13 - i) / 7.0
            cases += Case("z=${i}x+${i + 1}y+${i + 2}", x, y, i * x + (i + 1) * y + i + 2.0)
        }
        for (i in 1..20) {
            val a = i / 9.0
            val x = 0.15 + i / 22.0
            val y = 0.25 + i / 19.0
            cases += Case("z=sin(x+${number(a)})+cos(y-${number(a / 2.0)})", x, y, sin(x + a) + cos(y - a / 2.0))
        }
        for (i in 1..20) {
            val a = i / 11.0
            val x = 0.2 + i / 18.0
            val y = 0.3 + i / 21.0
            cases += Case("z=sin^2(x+${number(a)})+cos^2(y-${number(a)})", x, y, sin(x + a).pow(2.0) + cos(y - a).pow(2.0))
        }
        for (i in 1..20) {
            val a = i / 10.0
            val x = (i - 9) / 5.0
            val y = (i - 5) / 6.0
            cases += Case("z=sqrt((x-${number(a)})^2+(y+${number(a / 2.0)})^2)", x, y, sqrt((x - a).pow(2.0) + (y + a / 2.0).pow(2.0)))
        }
        for (i in 1..20) {
            val a = i / 8.0
            val x = i / 12.0
            val y = (21 - i) / 13.0
            cases += Case("z=min(x,y)+max(x,${number(a)})+abs(x-y)", x, y, min(x, y) + max(x, a) + abs(x - y))
        }
        for (i in 1..20) {
            val a = i / 14.0
            val x = if (i % 2 == 0) a + 0.4 else a - 0.3
            val y = i / 16.0
            cases += Case("z=if(x>${number(a)},x*y,x-y)", x, y, if (x > a) x * y else x - y)
        }
        for (i in 1..20) {
            val a = i / 20.0
            val x = 1.0 + i / 18.0
            val y = 1.0 + i / 17.0
            cases += Case("z=ln(exp(x))+log(1000)+floor(y)+ceil(x-${number(a)})", x, y, x + 3.0 + floor(y) + ceil(x - a))
        }

        assertTrue("Expected at least 100 3D surface cases", cases.size >= 100)
        val engine = ExpressionEngine()
        cases.forEach { case ->
            val interpretation = SurfaceInputInterpreter.explicit(case.source).getOrThrow()
            val result = engine.compile(interpretation.expression).eval(mapOf("x" to case.x, "y" to case.y))
            assertEquals("${case.source} at (${case.x}, ${case.y})", case.expected, result, 1e-4)
            assertTrue("${case.source} must produce a finite surface value", result.isFinite())
        }
    }

    @Test
    fun graphConstantsRetainExpectedExactValues() {
        val engine = ExpressionEngine()
        assertEquals(E, engine.compile("e").eval(), tolerance)
        assertEquals(PI, engine.compile("pi").eval(), tolerance)
        assertEquals(1.0, engine.compile("sin(30)+cos(60)").eval(), tolerance)
        assertEquals(1.0, engine.compile("sin(pi/2)").eval(), tolerance)
    }
}
