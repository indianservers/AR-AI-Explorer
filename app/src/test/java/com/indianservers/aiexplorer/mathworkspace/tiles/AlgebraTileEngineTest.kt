package com.indianservers.aiexplorer.mathworkspace.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgebraTileEngineTest {
    @Test fun expressionIsInferredAndZeroPairsCancel() {
        val tiles = listOf(
            AlgebraTile("s", TileKind.XSquared, 0f, 0f),
            AlgebraTile("x1", TileKind.PositiveX, 0f, 0f),
            AlgebraTile("x2", TileKind.PositiveX, 0f, 0f),
            AlgebraTile("x3", TileKind.PositiveX, 0f, 0f),
            AlgebraTile("p", TileKind.PositiveOne, 0f, 0f),
            AlgebraTile("n1", TileKind.NegativeOne, 0f, 0f),
            AlgebraTile("n2", TileKind.NegativeOne, 0f, 0f),
        )
        assertEquals("x² + 3x − 1", AlgebraTileEngine.expression(tiles))
        assertEquals(1, AlgebraTileEngine.zeroPairs(tiles).size)
    }

    @Test fun parserBuildsRequestedPolynomialsAndReportsUnsupportedTerms() {
        val parsed = AlgebraTileEngine.parse("x^2 + 3x + 2").getOrThrow()
        assertEquals("x² + 3x + 2", parsed.text)
        assertEquals(6, parsed.tiles.size)
        assertTrue(AlgebraTileEngine.parse("sin(x)").isFailure)
    }
}
