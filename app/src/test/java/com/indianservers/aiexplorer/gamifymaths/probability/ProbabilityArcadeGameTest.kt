package com.indianservers.aiexplorer.gamifymaths.probability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbabilityArcadeGameTest {
    @Test
    fun arcadeContainsFifteenGamesAndFortyFiveLevels() {
        val games = ProbabilityArcadeGames

        assertEquals(15, games.size)
        assertEquals(15, games.map { it.id }.toSet().size)
        assertEquals(45, games.sumOf { it.levels.size })
        games.forEach { assertEquals("${it.title} must have multiple levels", 3, it.levels.size) }
    }

    @Test
    fun everyLevelHasFeedbackAndASelectableAnswer() {
        ProbabilityArcadeGames.flatMap { it.levels }.forEach { level ->
            assertTrue(level.answer in level.choices)
            assertTrue(level.choices.size >= 3)
            assertTrue(level.hint.isNotBlank())
            assertTrue(level.explanation.isNotBlank())
        }
    }
}
