package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlgebraAdventureGameTest {
    @Test
    fun catalogHasSixChaptersAndSeventyTwoUniqueLevels() {
        val challenges = AlgebraAdventureCatalog.challenges

        assertEquals(6, AlgebraAdventureCatalog.chapterNames.size)
        assertEquals(72, challenges.size)
        assertEquals(72, challenges.map { it.id }.toSet().size)
        (0 until 6).forEach { chapter ->
            assertEquals(12, challenges.count { it.chapter == chapter })
            assertEquals((0 until 12).toSet(), challenges.filter { it.chapter == chapter }.map { it.stage }.toSet())
        }
    }

    @Test
    fun everyLevelHasASelectableCorrectAnswerAndExplanation() {
        AlgebraAdventureCatalog.challenges.forEach { challenge ->
            assertTrue("${challenge.id} must include its answer", challenge.answer in challenge.choices)
            assertTrue("${challenge.id} needs a hint", challenge.hint.isNotBlank())
            assertTrue("${challenge.id} needs worked steps", challenge.steps.size >= 3)
            assertTrue("${challenge.id} needs distinct choices", challenge.choices.size >= 3)
        }
    }
}
