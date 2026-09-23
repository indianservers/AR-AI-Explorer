package com.indianservers.aiexplorer.mathworkspace.calculus

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CalculusMathTest {
    private fun f(s:String):(Double)->Double=FunctionParser.parse(s)::value
    @Test fun parsesCommonExpressionsAndImplicitProducts() {
        assertEquals(-2.0,f("x^3 - 3x")(1.0),1e-12)
        assertEquals(4+sin(2.0),f("sin(x) + x^2")(2.0),1e-12)
        assertEquals(3.0,f("(x^2 - 1)/(x - 1)")(2.0),1e-12)
        assertFalse(f("ln(x)")(-1.0).isFinite())
        assertThrows(IllegalArgumentException::class.java){FunctionParser.parse("sin(")}
    }
    @Test fun derivativeAndIntegralsAreNumericallyComputed() {
        assertEquals(0.0,CalculusMath.derivative(f("x^3 - 3x"),1.0),1e-7)
        assertEquals(9.0,CalculusMath.derivative(f("x^3 - 3x"),2.0),1e-6)
        assertEquals(2.0,CalculusMath.integrate(f("x"),0.0,2.0),1e-9)
        assertEquals(2.0,CalculusMath.integrate(f("sin(x)"),0.0,PI),2e-8)
        assertEquals(-2.0,CalculusMath.integrate(f("x"),2.0,0.0),1e-9)
    }
    @Test fun limitsMustAgreeFromBothSides() {
        val removable=CalculusMath.limit(f("sin(x)/x"),0.0)
        assertTrue(removable.converges);assertEquals(1.0,removable.value!!,1e-6)
        val jump=CalculusMath.limit(f("1/x"),0.0)
        assertFalse(jump.converges);assertNull(jump.value)
        val cancelled=CalculusMath.limit(f("(x^2 - 1)/(x - 1)"),1.0)
        assertTrue(cancelled.converges);assertEquals(2.0,cancelled.value!!,1e-6)
    }
    @Test fun discontinuitiesRemainUndefinedAtSingularPoints() {
        assertFalse(f("1/x")(0.0).isFinite())
        assertTrue(abs(f("1/x")(-.001)-f("1/x")(.001))>1000)
        assertTrue(abs(f("tan(x)")(PI/2))>1e12)
    }
}
