package com.indianservers.aiexplorer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphExpressionDeviceAccuracyTest {
    private fun number(value: Double) = String.format(Locale.US, "%.6f", value)

    @Test
    fun deviceEvaluatesOneHundredPlus2dAnd3dGraphExpressions() {
        data class Case2D(val source: String, val x: Double, val expected: Double)
        data class Case3D(val source: String, val x: Double, val y: Double, val expected: Double)

        val twoD = mutableListOf<Case2D>()
        val threeD = mutableListOf<Case3D>()

        for (i in 1..20) {
            val x = (i - 10) / 5.0
            twoD += Case2D("f(x)=${i}x+${i + 2}", x, i * x + i + 2.0)
        }
        for (i in 1..20) {
            val a = i / 12.0
            val x = 0.2 + i / 25.0
            twoD += Case2D("sin^2(x+${number(a)})+cos^2(x+${number(a)})+tan^2(0)", x, 1.0)
        }
        twoD += Case2D("f(x)=tan\u00B2(x)", kotlin.math.PI / 4.0, 1.0)
        twoD += Case2D("f(x)=tan^(2)(x)", kotlin.math.PI / 4.0, 1.0)
        for (i in 1..20) {
            val a = i / 9.0
            val x = (i - 8) / 7.0
            twoD += Case2D("sqrt((x+${number(a)})^2)+abs(x-${number(a)})", x, abs(x + a) + abs(x - a))
        }
        for (i in 1..20) {
            val a = i / 8.0
            val x = i / 10.0
            twoD += Case2D("min(x,${number(a)})+max(x,${number(a + 1.0)})", x, min(x, a) + max(x, a + 1.0))
        }
        for (i in 1..20) {
            val a = i / 10.0
            val x = if (i % 2 == 0) a + 0.2 else a - 0.2
            twoD += Case2D("if(x>${number(a)},x^2,${number(a)}x)", x, if (x > a) x.pow(2.0) else a * x)
        }

        for (i in 1..20) {
            val x = (i - 7) / 6.0
            val y = (12 - i) / 7.0
            threeD += Case3D("z=${i}x+${i + 1}y+${i + 2}", x, y, i * x + (i + 1) * y + i + 2.0)
        }
        for (i in 1..20) {
            val a = i / 10.0
            val x = 0.15 + i / 20.0
            val y = 0.25 + i / 18.0
            threeD += Case3D("z=sin(x+${number(a)})+cos(y-${number(a / 2.0)})", x, y, sin(x + a) + cos(y - a / 2.0))
        }
        for (i in 1..20) {
            val a = i / 11.0
            val x = 0.2 + i / 21.0
            val y = 0.3 + i / 22.0
            threeD += Case3D("z=sin^2(x+${number(a)})+cos^2(y-${number(a)})+tan^2(0)", x, y, sin(x + a).pow(2.0) + cos(y - a).pow(2.0))
        }
        threeD += Case3D("z=sin\u00B2(x)+cos\u00B2(x)+tan\u00B2(y)", kotlin.math.PI / 3.0, kotlin.math.PI / 4.0, 2.0)
        threeD += Case3D("z=sin^(2)(x)+cos^(2)(x)+tan^(2)(y)", kotlin.math.PI / 3.0, kotlin.math.PI / 4.0, 2.0)
        for (i in 1..20) {
            val a = i / 9.0
            val x = (i - 9) / 5.0
            val y = (i - 6) / 6.0
            threeD += Case3D("z=sqrt((x-${number(a)})^2+(y+${number(a / 2.0)})^2)", x, y, sqrt((x - a).pow(2.0) + (y + a / 2.0).pow(2.0)))
        }
        for (i in 1..20) {
            val a = i / 14.0
            val x = if (i % 2 == 0) a + 0.3 else a - 0.3
            val y = i / 16.0
            threeD += Case3D("z=if(x>${number(a)},x*y,x-y)+floor(y)+ceil(x)", x, y, (if (x > a) x * y else x - y) + floor(y) + ceil(x))
        }

        assertTrue("Expected at least 100 2D device cases", twoD.size >= 100)
        assertTrue("Expected at least 100 3D device cases", threeD.size >= 100)

        val graph = TypedGraphEngine()
        twoD.forEach { case ->
            val point = graph.evaluate(TypedGraphExpressionParser.parse(case.source), case.x)
            assertEquals("${case.source} at x=${case.x}", case.expected, point?.y ?: Double.NaN, 1e-4)
        }

        val engine = ExpressionEngine()
        threeD.forEach { case ->
            val interpretation = SurfaceInputInterpreter.explicit(case.source).getOrThrow()
            val result = engine.compile(interpretation.expression).eval(mapOf("x" to case.x, "y" to case.y))
            assertEquals("${case.source} at (${case.x}, ${case.y})", case.expected, result, 1e-4)
            assertTrue("${case.source} must produce a finite surface value", result.isFinite())
        }
    }
}
