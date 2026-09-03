package com.indianservers.aiexplorer.core

import com.indianservers.aiexplorer.physics.core.CollisionType
import com.indianservers.aiexplorer.physics.core.PhysicsMathWorkspaceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceStrengtheningPhase1Test {
    @Test
    fun coordinateTriangleReportsExactGeometryAndOrientation() {
        val result = CoordinatePlaneEngine.analyseTriangle(Vec2(0.0, 0.0), Vec2(4.0, 0.0), Vec2(0.0, 3.0))
        assertEquals(6.0, result.area, 1e-12)
        assertEquals(12.0, result.perimeter, 1e-12)
        assertEquals(Vec2(4.0 / 3.0, 1.0), result.centroid)
        assertEquals(TriangleOrientation.Counterclockwise, result.orientation)
        assertFalse(result.isCollinear)
    }

    @Test
    fun vectorDiagnosticsRecognizeRelationshipsAndArea() {
        val orthogonal = VectorLabEngine.analyse(Vec3(3.0, 0.0, 0.0), Vec3(0.0, 4.0, 0.0))
        assertTrue(orthogonal.areOrthogonal)
        assertFalse(orthogonal.areParallel)
        assertEquals(12.0, orthogonal.parallelogramArea, 1e-12)
        assertEquals(0.0, requireNotNull(orthogonal.cosineSimilarity), 1e-12)
    }

    @Test
    fun matrixInvariantsAndPowerRemainExact() {
        val matrix = ExactMatrix.parse(listOf(listOf("2", "1"), listOf("0", "3")))
        assertEquals(ExactRational.of(5), MatrixLabEngine.trace(matrix))
        assertEquals("λ² − 5λ + 6", MatrixLabEngine.characteristicPolynomial2x2(matrix))
        assertEquals("[[4, 5]\n [0, 9]]", MatrixLabEngine.power(matrix, 2).render())
    }

    @Test
    fun calculusLinearizationShowsApproximationError() {
        val result = CalculusLabEngine().linearization("x^2", 2.0, 2.1)
        assertEquals(4.4, result.predictedY, 1e-5)
        assertEquals(4.41, result.actualY, 1e-12)
        assertEquals(.01, result.absoluteError, 1e-5)
    }

    @Test
    fun collisionModelVerifiesMomentumAndEnergyBehavior() {
        val elastic = PhysicsMathWorkspaceEngine.collision1D(2.0, 5.0, 3.0, 0.0, CollisionType.Elastic)
        assertEquals(0.0, elastic.momentumResidual, 1e-12)
        assertEquals(0.0, elastic.kineticEnergyChange, 1e-12)
        assertEquals(1.0, elastic.coefficientOfRestitution, 1e-12)
        val stuck = PhysicsMathWorkspaceEngine.collision1D(2.0, 5.0, 3.0, 0.0, CollisionType.PerfectlyInelastic)
        assertEquals(2.0, stuck.firstFinalVelocity, 1e-12)
        assertEquals(stuck.firstFinalVelocity, stuck.secondFinalVelocity, 1e-12)
        assertTrue(stuck.kineticEnergyChange < 0.0)
    }

    @Test
    fun artCurveMeasurementsExposeLengthBoundsAndClosure() {
        val square = ArtCurve(
            listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 1.0), Vec2(0.0, 0.0)),
            null,
            "square",
        )
        val result = MathematicalArtEngine.measure(square)
        assertEquals(4.0, result.pathLength, 1e-12)
        assertEquals(Vec2(0.0, 0.0), result.boundsMin)
        assertEquals(Vec2(1.0, 1.0), result.boundsMax)
        assertTrue(result.closed)
    }
}
