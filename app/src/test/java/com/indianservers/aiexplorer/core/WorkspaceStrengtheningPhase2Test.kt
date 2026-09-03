package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceStrengtheningPhase2Test {
    @Test
    fun triangleClassifiesShapeAndFindsCircumcenter() {
        val triangle = CoordinatePlaneEngine.analyseTriangle(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 3.0))
        assertEquals("scalene", triangle.sideClassification)
        assertEquals("right", triangle.angleClassification)
        val centre = requireNotNull(triangle.circumcenter)
        assertEquals(2.0, centre.x, 1e-12)
        assertEquals(1.5, centre.y, 1e-12)
        assertEquals(null, CoordinatePlaneEngine.analyseTriangle(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(2.0, 2.0)).circumcenter)
    }

    @Test
    fun gramSchmidtProducesAnOrthonormalBasis() {
        val basis = requireNotNull(VectorLabEngine.gramSchmidt(Vec3(1.0, 1.0, 0.0), Vec3(1.0, 0.0, 1.0)))
        assertEquals(1.0, basis.first.magnitude(), 1e-12)
        assertEquals(1.0, basis.second.magnitude(), 1e-12)
        assertEquals(0.0, basis.first.dot(basis.second), 1e-12)
        assertEquals(null, VectorLabEngine.gramSchmidt(Vec3(1.0, 0.0, 0.0), Vec3(2.0, 0.0, 0.0)))
    }

    @Test
    fun areaBetweenCurvesHandlesCrossingsGeometrically() {
        val result = CalculusLabEngine().areaBetween("x", "0", -1.0, 1.0)
        assertEquals(1.0, result.numerical.geometricArea, 1e-6)
        assertEquals(0.0, result.numerical.signedArea, 1e-6)
        assertTrue(result.firstCurve.isNotEmpty())
        assertEquals(result.firstCurve.size, result.secondCurve.size)
    }

    @Test
    fun customParametricArtUsesSharedExpressionEngine() {
        val curve = MathematicalArtEngine.customParametric("cos(t)", "sin(t)", samples = 480)
        assertTrue(curve.points.size > 400)
        assertNotNull(curve.graphSource)
        assertTrue(MathematicalArtEngine.measure(curve).closed)
    }

    @Test
    fun matrixPowerZeroReturnsIdentityAndScaleStaysExact() {
        val matrix = ExactMatrix.parse(listOf(listOf("2", "1"), listOf("0", "3")))
        assertEquals("[[1, 0]\n [0, 1]]", MatrixLabEngine.power(matrix, 0).render())
        assertEquals("[[1, 1/2]\n [0, 3/2]]", MatrixLabEngine.scale(matrix, ExactRational.parse("1/2")).render())
        assertFalse(MatrixLabEngine.determinant(matrix).isZero)
    }
}
