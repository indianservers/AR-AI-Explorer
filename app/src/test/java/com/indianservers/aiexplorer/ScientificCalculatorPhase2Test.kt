package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class ScientificCalculatorPhase2Test {
    private val advanced = AdvancedScientificCalculator()

    @Test fun evaluatesHyperbolicFunctionsAndArbitraryBaseLogs() {
        val smart = SmartScientificCalculator()
        assertTrue(smart.evaluate("sinh(1)+cosh(1)").primary.toDouble() > 2.7)
        assertEquals("5", advanced.evaluate("log base 2 of 32")!!.primary)
    }

    @Test fun performsSymbolicDifferentiationIntegrationAndPolynomialOperations() {
        val derivative = advanced.evaluate("differentiate x^3+sin(x)")!!
        assertTrue(derivative.primary.contains("3*x^2"))
        val integral = advanced.evaluate("integrate 3*x^2+cos(x)")!!
        assertTrue(integral.primary.contains("x^3") && integral.primary.contains("sin(x)"))
        val expanded = advanced.evaluate("expand (x+2)^2")!!
        assertTrue(expanded.primary.contains("x^2"))
        val factored = advanced.evaluate("factor x^2-5*x+6")!!
        assertTrue(factored.primary.contains("x"))
    }

    @Test fun decomposesTwoDistinctLinearFactors() {
        val result = advanced.evaluate("partial fractions (2*x+3)/((x-1)*(x+2))")!!
        assertTrue(result.primary.contains("/(x-1)"))
        assertTrue(result.primary.contains("/(x+2)"))
    }

    @Test fun supportsLimitsTaylorAndLinearOdeFoundation() {
        assertTrue(advanced.evaluate("limit (x^2-4)/(x-2) as x -> 2")!!.primary.contains("4"))
        assertTrue(advanced.evaluate("taylor sin(x) order 7")!!.primary.contains("x^3"))
        assertTrue(advanced.evaluate("ode dy/dx = 2*y, y(0)=3")!!.primary.contains("exp"))
    }

    @Test fun numericalMethodsConvergeWithVerification() {
        val newton = advanced.evaluate("newton x^2-2 start 1")!!
        assertEquals(sqrt(2.0), newton.primary.toDouble(), 1e-8)
        val bisection = advanced.evaluate("bisection x^2-2 from 1 to 2")!!
        assertEquals(sqrt(2.0), bisection.primary.toDouble(), 1e-8)
        assertTrue(bisection.verification.contains("Residual"))
    }

    @Test fun complexRectangularPolarProductsAndRootsAreAvailable() {
        val value = advanced.evaluate("complex 3+4i")!!
        assertEquals("5", value.alternatives.first { it.first == "Magnitude" }.second)
        assertEquals("11 + 2i", advanced.evaluate("complex multiply 1+2i ; 3-4i")!!.primary)
        assertEquals(3, CalculatorComplex.parse("1+i").roots(3).size)
    }

    @Test fun modularArithmeticIsExact() {
        assertEquals("3", advanced.evaluate("modpow 7^128 mod 13")!!.primary)
        val inverse = advanced.evaluate("mod inverse 7 mod 26")!!
        assertEquals("15", inverse.primary)
        assertTrue(inverse.verification.endsWith("= 1."))
    }

    @Test fun phaseTwoTemplatesAndExamplesCoverEveryMode() {
        assertEquals(AdvancedCalculatorMode.entries.toSet(), advanced.examples.keys)
        val structural = CalculatorInputIntelligence.templates[CalculatorKeyboardLayer.Structural].orEmpty()
        assertTrue(structural.any { it.label == "Newton" })
        assertTrue(structural.any { it.label == "complex" })
        assertTrue(structural.any { it.label == "ODE" })
    }
}
