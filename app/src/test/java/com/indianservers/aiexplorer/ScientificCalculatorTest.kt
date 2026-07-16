package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.AngleMode
import com.indianservers.aiexplorer.core.ScientificCalculator
import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScientificCalculatorTest {
    private val calculator = ScientificCalculator()

    @Test
    fun evaluatesTrigonometryInDegreeMode() {
        val result = calculator.evaluate("sin(30)+cos(60)", AngleMode.Degrees)

        assertEquals(1.0, result.value, 1e-9)
        assertTrue(result.normalizedExpression.contains("*pi/180"))
        assertTrue(result.warnings.any { it.contains("degrees", ignoreCase = true) })
    }

    @Test
    fun evaluatesTrigonometryInRadianMode() {
        val result = calculator.evaluate("sin(pi/2)", AngleMode.Radians)

        assertEquals(1.0, result.value, 1e-9)
        assertEquals("1", result.decimal)
    }

    @Test
    fun supportsPercentFactorialAndScientificNotationViews() {
        val result = calculator.evaluate("7!/(5!*2!) + 15%*200")

        assertEquals(51.0, result.value, 1e-9)
        assertEquals("51", result.exactHint)
        assertTrue(result.scientific.contains("e"))
        assertTrue(result.engineering.contains("e"))
    }

    @Test
    fun exposesConstantsReferenceCardsAndConversions() {
        val gravity = calculator.evaluate("g*10")
        val speed = calculator.constants.first { it.key == "c" }
        val speedConversion = calculator.conversions.first { it.fromUnit == "km/h" && it.toUnit == "m/s" }

        assertEquals(98.0665, gravity.value, 1e-9)
        assertEquals(299_792_458.0, speed.value, 0.0)
        assertEquals(20.0, calculator.convert(72.0, speedConversion), 1e-9)
        assertTrue(calculator.referenceCards.size >= 10)
    }

    @Test
    fun supportsDegreeSymbolAsRadiansLiteral() {
        val result = calculator.evaluate("180°", AngleMode.Radians)

        assertEquals(PI, result.value, 1e-9)
    }
}
