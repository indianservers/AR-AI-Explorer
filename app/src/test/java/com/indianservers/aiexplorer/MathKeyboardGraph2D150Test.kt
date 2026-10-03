package com.indianservers.aiexplorer

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MathKeyboardGraph2D150Test(private val case: KeyboardGraphCase) {
    @Test fun manualKeyboardSequencePlotsCorrectly() = GraphKeyboardReplay.verify(case)
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{index}: {0}")
        fun cases(): List<Array<Any>> = KeyboardGraphCases.all.filter { it.dimension == 2 }
            .also { require(it.size == 150) }.map { arrayOf<Any>(it) }
    }
}
