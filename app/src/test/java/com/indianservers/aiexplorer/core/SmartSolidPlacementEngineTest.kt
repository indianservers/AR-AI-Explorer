package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class SmartSolidPlacementEngineTest {
    @Test
    fun firstShapeIsPlacedAtTheSceneOrigin() {
        assertEquals(Vec3(0.0, 0.0, 0.0), SmartSolidPlacementEngine.next(emptyList(), Solid(SolidType.Cube, width = 2.0)))
    }

    @Test
    fun repeatedInsertionsUseDistinctNonOverlappingFloorPositions() {
        val solids = mutableListOf<Solid>()
        repeat(18) {
            val incoming = Solid(SolidType.Cube, width = 2.0, depth = 2.0)
            solids += incoming.copy(position = SmartSolidPlacementEngine.next(solids, incoming))
        }

        assertEquals(solids.size, solids.map { it.position }.distinct().size)
        solids.indices.forEach { first ->
            for (second in first + 1 until solids.size) {
                assertTrue(
                    hypot(
                        solids[first].position.x - solids[second].position.x,
                        solids[first].position.z - solids[second].position.z,
                    ) >= 2.35,
                )
            }
        }
    }
}
