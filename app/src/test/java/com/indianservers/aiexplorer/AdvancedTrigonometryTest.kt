package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class AdvancedTrigonometryTest {
    @Test fun angleUnitsRoundTrip() {
        assertEquals(PI, InteractiveTrigEngine.toRadians(180.0, TrigAngleUnit.Degrees), 1e-12)
        assertEquals(PI, InteractiveTrigEngine.toRadians(200.0, TrigAngleUnit.Gradians), 1e-12)
        assertEquals(200.0, InteractiveTrigEngine.fromRadians(PI, TrigAngleUnit.Gradians).display, 1e-12)
    }

    @Test fun reciprocalAndInverseFunctionsAreDefinedSafely() {
        val transform = TrigTransform()
        assertEquals(1.0, transform.valueAt(0.0, TrigFunction.Secant), 1e-12)
        assertTrue(transform.valueAt(0.0, TrigFunction.Cosecant).isNaN())
        assertEquals(PI / 6, InteractiveTrigEngine.inverse(.5, InverseTrigFunction.ArcSine), 1e-12)
    }

    @Test fun trigEquationRootsIncludeExactSpecialAngles() {
        val roots = InteractiveTrigEngine.equationRoots(TrigFunction.Sine, .5, 0.0, 2 * PI)
        assertEquals(2, roots.size)
        assertTrue(roots.any { kotlin.math.abs(it.radians - PI / 6) < 1e-5 })
        assertTrue(roots.all { it.exactLabel != null })
    }

    @Test fun polarHarmonicAndApplicationLabsAreDeterministic() {
        val rose = InteractiveTrigEngine.polarSamples(PolarCurveType.Rose, 4.0, 101)
        assertEquals(101, rose.size)
        assertEquals(1.0, rose.first().x, 1e-12)
        assertEquals(.5, InteractiveTrigEngine.harmonicValue(PI / 2, listOf(HarmonicComponent(1.0, 1), HarmonicComponent(.5, 3))), 1e-12)
        assertEquals(11.6, InteractiveTrigEngine.heightFromObservation(10.0, 45.0, 1.6), 1e-10)
    }

    @Test fun asaAndAasTriangleSolutionsCloseToOneHundredEightyDegrees() {
        val asa = TriangleTrigSolver.asa(7.0, 50.0, 60.0)
        val aas = TriangleTrigSolver.aas(7.0, 50.0, 60.0)
        assertEquals(180.0, asa.angleA + asa.angleB + asa.angleC, 1e-12)
        assertEquals(180.0, aas.angleA + aas.angleB + aas.angleC, 1e-12)
        assertTrue(asa.area > 0 && aas.area > 0)
    }
}
