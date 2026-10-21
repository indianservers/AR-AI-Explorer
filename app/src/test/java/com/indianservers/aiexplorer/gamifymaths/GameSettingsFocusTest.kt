package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test

class GameSettingsFocusTest {
    @Test fun focusedTaskFamiliesRemainSelectedAcrossProgression() {
        for (level in listOf(0, 17, 33, 67, 119)) {
            for (seed in 1L..12L) {
                assertEquals("Mean evidence", generateWorkshopChallenge("data", level, 0, seed, 0).source!!.topic)
                assertEquals(WorkshopKind.Sort, generateWorkshopChallenge("data", level, 0, seed, 1).kind)
                assertEquals("Range evidence", generateWorkshopChallenge("data", level, 0, seed, 2).source!!.topic)
                assertEquals("Discount studio", generateWorkshopChallenge("percent-studio", level, 0, seed, 1).source!!.topic)
                assertEquals("Bridge workshop", generateWorkshopChallenge("measure", level, 0, seed, 3).source!!.topic)
                assertEquals(0.0, generateWorkshopChallenge("balance", level, 0, seed, 0).numbers[2], 0.0)
                assertTrue(generateWorkshopChallenge("balance", level, 0, seed, 1).numbers[2] > 0)
                for (id in listOf("logic", "combinatorics")) {
                    assertEquals(0, generateWorkshopChallenge(id, level, 0, seed, 0).parameters[2])
                    assertEquals(1, generateWorkshopChallenge(id, level, 0, seed, 1).parameters[2])
                }
                assertEquals(1 to 0, generateWorkshopChallenge("vectors", level, 0, seed, 0).moves[0])
                assertTrue(generateWorkshopChallenge("vectors", level, 0, seed, 1).moves[0].second > 0)
                assertEquals(5 to 0, generateWorkshopChallenge("integer-expedition", level, 0, seed, 0).moves[0])
                assertEquals(10 to 0, generateWorkshopChallenge("integer-expedition", level, 0, seed, 1).moves[0])
                assertEquals(12, generateWorkshopChallenge("fractions", level, 0, seed, 0).parameters[0])
                assertEquals(60, generateWorkshopChallenge("fractions", level, 0, seed, 1).parameters[0])
                assertTrue(generateWorkshopChallenge("matrix-mission", level, 0, seed, 0).moves.all { kotlin.math.abs(it.first) <= 2 && kotlin.math.abs(it.second) <= 2 })
                assertTrue(generateWorkshopChallenge("times-table", level, 0, seed, 0).witness.all { it.toInt() <= 9 })
                assertTrue(generateWorkshopChallenge("number-theory", level, 0, seed, 0).witness.all { it.toInt() <= 7 })
            }
        }
    }
}
