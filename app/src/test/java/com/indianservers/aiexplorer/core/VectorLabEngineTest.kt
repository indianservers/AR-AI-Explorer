package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VectorLabEngineTest {
    @Test
    fun computesPairOperations() {
        val result = VectorLabEngine.analyse(Vec3(2.0, 1.0, 0.0), Vec3(-1.0, 2.0, 0.0))

        assertEquals(Vec3(1.0, 3.0, 0.0), result.sum)
        assertEquals(Vec3(3.0, -1.0, 0.0), result.difference)
        assertEquals(0.0, result.dot, 1e-9)
        assertEquals(Vec3(0.0, -0.0, 5.0), result.cross)
        assertEquals(90.0, result.angleDegrees ?: Double.NaN, 1e-9)
    }

    @Test
    fun computesProjectionAndRejection() {
        val result = VectorLabEngine.analyse(Vec3(3.0, 4.0, 0.0), Vec3(1.0, 0.0, 0.0))

        assertEquals(Vec3(3.0, 0.0, 0.0), result.projectionAOnB)
        assertEquals(Vec3(0.0, 4.0, 0.0), result.rejectionAFromB)
        val unit = requireNotNull(VectorLabEngine.unit(Vec3(3.0, 4.0, 0.0)))
        assertEquals(0.6, unit.x, 1e-12)
        assertEquals(0.8, unit.y, 1e-12)
        assertEquals(0.0, unit.z, 1e-12)
    }

    @Test
    fun handlesZeroVectorWithoutInvalidNumbers() {
        val zero = Vec3(0.0, 0.0, 0.0)
        val result = VectorLabEngine.analyse(Vec3(1.0, 2.0, 3.0), zero)

        assertNull(result.angleDegrees)
        assertNull(result.projectionAOnB)
        assertNull(result.rejectionAFromB)
        assertNull(VectorLabEngine.unit(zero))
    }

    @Test
    fun computesLinearCombination() {
        assertEquals(
            Vec3(3.0, 3.0, -2.0),
            VectorLabEngine.linearCombination(Vec3(1.0, 2.0, 0.0), 2.0, Vec3(-1.0, 1.0, 2.0), -1.0),
        )
    }
}
