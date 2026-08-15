package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.VectorCalculusEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class VectorCalculusPhase4Test {
    private val engine = VectorCalculusEngine()

    @Test fun jacobianAndHessianCarryIndependentSymmetryEvidence() {
        val jacobian = engine.jacobian(listOf("x^2+y", "x+y^2"), listOf("x", "y"), mapOf("x" to 1.0, "y" to 2.0))
        val hessian = engine.hessian("x^2+3*x*y+2*y^2", listOf("x", "y"), mapOf("x" to 1.0, "y" to 2.0))

        assertEquals(2.0, jacobian.values[0][0], 1e-6)
        assertEquals(4.0, jacobian.values[1][1], 1e-6)
        assertEquals(3.0, hessian.values[0][1], 1e-4)
        assertEquals(4.0, hessian.values[1][1], 1e-4)
        assertTrue(hessian.symmetryError < 1e-6)
    }

    @Test fun lagrangeOptimizationFindsBothCircleExtrema() {
        val points = engine.lagrange2D("x+y", "x^2+y^2", 1.0, listOf(Vec2(1.0, 1.0), Vec2(-1.0, -1.0), Vec2(1.0, -1.0)))

        assertTrue(points.size >= 2)
        assertEquals(-sqrt(2.0), points.first().value, 1e-6)
        assertEquals(sqrt(2.0), points.last().value, 1e-6)
        assertTrue(points.all { it.constraintResidual < 1e-7 && it.stationarityResidual < 1e-7 })
    }

    @Test fun lineAndSurfaceIntegralsRespectOrientation() {
        val scalar = engine.scalarLineIntegral("1", "cos(t)", "sin(t)", from = 0.0, to = 2 * PI)
        val work = engine.workLineIntegral(listOf("-y", "x"), "cos(t)", "sin(t)", from = 0.0, to = 2 * PI)
        val flux = engine.surfaceFlux(listOf("0", "0", "1"), listOf("u", "v", "0"), 0.0..2.0, 0.0..3.0)

        assertEquals(2 * PI, scalar.value, 1e-5)
        assertEquals(2 * PI, work.value, 1e-5)
        assertEquals(6.0, flux.value, 1e-6)
        assertEquals(-work.value, engine.workLineIntegral(listOf("-y", "x"), "cos(t)", "sin(t)", from = 2 * PI, to = 0.0).value, 1e-5)
    }

    @Test fun greenGaussAndStokesProduceVerificationCertificates() {
        val green = engine.greenRectangle("-y", "x", -1.0..1.0, -1.0..1.0)
        val gauss = engine.gaussBox(listOf("x", "y", "z"), 0.0..1.0, 0.0..1.0, 0.0..1.0)
        val stokes = engine.stokesPlanarRectangle(listOf("-y", "x", "0"), -1.0..1.0, -1.0..1.0)

        assertTrue(green.passed && gauss.passed && stokes.passed)
        assertEquals(8.0, green.boundaryValue, 1e-5)
        assertEquals(3.0, gauss.interiorValue, 1e-5)
        assertEquals(8.0, stokes.interiorValue, 1e-5)
    }

    @Test fun calculatorRoutesJacobianAndHessianCommands() {
        val jacobian = AdvancedScientificCalculator().evaluate("jacobian x^2+y;x+y^2 vars x,y at x=1,y=2")!!
        val hessian = AdvancedScientificCalculator().evaluate("hessian x^2+3*x*y+2*y^2 vars x,y at x=1,y=2")!!

        assertEquals("[[2, 1], [1, 4]]", jacobian.primary)
        assertTrue(hessian.alternatives.any { it.first == "Symmetry residual" })
    }
}
