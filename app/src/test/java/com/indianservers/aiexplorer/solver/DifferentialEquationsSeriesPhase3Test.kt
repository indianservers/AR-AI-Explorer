package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.DifferentialEquationsSeriesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.E
import kotlin.math.PI

class DifferentialEquationsSeriesPhase3Test {
    private val engine = DifferentialEquationsSeriesEngine()

    @Test fun firstOrderLinearAndLogisticIvpsCarryResidualEvidence() {
        val linear = engine.linearConstant(2.0, 0.0, 0.0, 3.0, 1.0)
        val logistic = engine.logistic(1.0, 10.0, 0.0, 2.0, 1.0)

        assertEquals(3 * E * E, linear.points.last().values.single(), 1e-8)
        assertTrue(linear.residual < 1e-7)
        assertTrue(logistic.points.last().values.single() in 4.0..4.1)
        assertTrue(logistic.verification.contains("residual"))
    }

    @Test fun secondOrderSolverHandlesRealRepeatedAndComplexRoots() {
        val real = engine.secondOrderHomogeneous(-3.0, 2.0, 0.0, 1.0, 0.0, 1.0)
        val repeated = engine.secondOrderHomogeneous(-2.0, 1.0, 0.0, 1.0, 0.0, 1.0)
        val oscillator = engine.secondOrderHomogeneous(0.0, 1.0, 0.0, 0.0, 1.0, PI / 2)

        assertTrue(real.exact!!.contains("exp"))
        assertTrue(repeated.exact!!.contains("*t"))
        assertEquals(1.0, oscillator.points.last().values.single(), 1e-8)
    }

    @Test fun rk4AndCoupledSystemLandExactlyOnTarget() {
        val scalar = engine.rk4("x+y", 0.0, 1.0, 1.0, .05)
        val system = engine.systemRk4(listOf("y", "-x"), listOf(1.0, 0.0), 0.0, PI / 2, .02)

        assertEquals(2 * E - 2, scalar.points.last().values.single(), 2e-6)
        assertEquals(1.0, scalar.points.last().x, 0.0)
        assertEquals(PI / 2, system.points.last().x, 1e-12)
        assertEquals(0.0, system.points.last().values[0], 2e-6)
        assertEquals(-1.0, system.points.last().values[1], 2e-6)
    }

    @Test fun seriesRecurrenceAndLaplaceWorkflowAreExplicit() {
        val series = engine.exponentialSeries(2.0, 0.0, 1.0, 4)
        val transform = engine.laplaceWorkflow("sin(2*x)")

        assertEquals(listOf(1.0, 2.0, 2.0, 4.0 / 3.0, 2.0 / 3.0), series.coefficients)
        assertTrue(series.recurrence.contains("a_n"))
        assertTrue(transform.supported)
        assertTrue(transform.output!!.contains("s"))
        assertTrue(transform.conditions.isNotEmpty())
    }

    @Test fun calculatorRoutesAnalyticNumericAndSeriesOdeCommands() {
        val analytic = AdvancedScientificCalculator().evaluate("linear ivp a 2 b 0 x0 0 y0 3 at 1")!!
        val numeric = AdvancedScientificCalculator().evaluate("rk4 x+y x0 0 y0 1 to 1 step 0.05")!!
        val series = AdvancedScientificCalculator().evaluate("ode series lambda 2 x0 0 y0 1 order 4")!!

        assertEquals(3 * E * E, analytic.primary.toDouble(), 1e-6)
        assertEquals(2 * E - 2, numeric.primary.toDouble(), 1e-5)
        assertTrue(series.alternatives.any { it.first == "Recurrence" })
    }
}
