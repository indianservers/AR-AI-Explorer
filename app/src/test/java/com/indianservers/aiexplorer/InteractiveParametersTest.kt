package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.InteractiveParameterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveParametersTest {
    @Test
    fun discoversOnlyFreeParametersInTwoDimensionalFunctions() {
        val parameters = InteractiveParameterEngine.discover(
            listOf("y = a*x^2 + b*x + c + sin(x)"),
            independentVariables = setOf("x", "y"),
        )

        assertEquals(listOf("a", "b", "c"), parameters.map { it.name })
        assertEquals(listOf(1.0, 0.0, 0.0), parameters.map { it.value })
    }

    @Test
    fun resolvesThreeDimensionalParametersWithoutReplacingCoordinatesOrFunctions() {
        val source = "z = amplitude*sin(frequency*x) + c*y"
        val parameters = InteractiveParameterEngine.discover(
            listOf(source),
            values = mapOf("amplitude" to 2.0, "frequency" to 3.0, "c" to -1.0),
            independentVariables = setOf("x", "y", "z"),
        )
        val resolved = InteractiveParameterEngine.resolve(
            source,
            InteractiveParameterEngine.values(parameters),
            independentVariables = setOf("x", "y", "z"),
        )

        assertEquals(listOf("amplitude", "c", "frequency"), parameters.map { it.name })
        assertTrue(resolved.contains("(2)*sin((3)*x)"))
        assertTrue(resolved.contains("(-1)*y"))
        assertFalse(resolved.contains("amplitude"))
        assertTrue(resolved.contains("sin"))
    }

    @Test
    fun appliesUsefulRangesAndSnapsValues() {
        val parameters = InteractiveParameterEngine.discover(listOf("p*x + n"))
        val probability = parameters.first { it.name == "p" }
        val count = parameters.first { it.name == "n" }

        assertEquals(0.0, probability.minimum, 0.0)
        assertEquals(1.0, probability.maximum, 0.0)
        assertEquals(.34, probability.snap(.337), 1e-9)
        assertEquals(1.0, count.snap(.2), 0.0)
        assertEquals(8.0, count.snap(7.6), 0.0)
    }

    @Test
    fun keepsExistingValuesWhenExpressionsAreRediscovered() {
        val parameters = InteractiveParameterEngine.discover(
            listOf("a*x + b"),
            values = mapOf("a" to 4.5, "b" to -2.0),
        )

        assertEquals(4.5, parameters.first { it.name == "a" }.value, 0.0)
        assertEquals(-2.0, parameters.first { it.name == "b" }.value, 0.0)
    }
}
