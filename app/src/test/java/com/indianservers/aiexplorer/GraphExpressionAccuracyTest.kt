package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.tan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphExpressionAccuracyTest {
    private val tolerance = 1e-5

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
    fun graphConstantsRetainExpectedExactValues() {
        val engine = ExpressionEngine()
        assertEquals(E, engine.compile("e").eval(), tolerance)
        assertEquals(PI, engine.compile("pi").eval(), tolerance)
        assertEquals(1.0, engine.compile("sin(30)+cos(60)").eval(), tolerance)
        assertEquals(1.0, engine.compile("sin(pi/2)").eval(), tolerance)
    }
}
