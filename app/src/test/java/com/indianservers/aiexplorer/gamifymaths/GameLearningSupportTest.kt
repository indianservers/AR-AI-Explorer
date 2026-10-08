package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLearningSupportTest {
    @Test
    fun everyThreeLevelsFormLearnPractiseMasterCycle() {
        assertEquals(GameLearningPhase.Explore, GameLearningCoach.phase(1))
        assertEquals(GameLearningPhase.Practise, GameLearningCoach.phase(2))
        assertEquals(GameLearningPhase.Master, GameLearningCoach.phase(3))
        assertEquals(GameLearningPhase.Explore, GameLearningCoach.phase(4))
    }

    @Test
    fun coordinateGuidanceTeachesSignReasoningWithoutOnlyGivingGenericRetry() {
        val guidance = GameLearningCoach.guidance(1, "Which point lies in Quadrant II?")
        assertTrue("x < 0" in guidance.hint)
        assertTrue("y > 0" in guidance.hint)
        assertEquals(3, guidance.retrySteps.size)
    }

    @Test
    fun guidanceCoversMajorGameConceptFamilies() {
        val prompts = listOf(
            "Find the mean of the data",
            "Find the probability on a die",
            "Which fraction is equivalent?",
            "Find the triangle area",
            "Solve the equation x + 2 = 5",
            "Complete the sequence",
        )
        prompts.forEach { prompt ->
            val guidance = GameLearningCoach.guidance(2, prompt)
            assertFalse(guidance.objective.isBlank())
            assertFalse(guidance.hint.isBlank())
            assertFalse(guidance.reflection.isBlank())
        }
    }

    @Test
    fun allProbabilityFamiliesUseGeneratedConstructionBoards() {
        (0 until 15).forEach { level ->
            val challenge = generateWorkshopChallenge("chance", level, 0, 42L)
            assertEquals(WorkshopKind.Expression, challenge.kind)
            assertTrue(challenge.source!!.answer.isFinite())
            assertFalse(challenge.hint.isBlank())
            assertFalse(challenge.explanation.isBlank())
        }
    }

    @Test fun catalogueAuditCoversAllLiveBoardsAndBothMasteries() {
        val boards=gamifyWorkshopAudit()
        assertEquals(27*122,boards.size)
        assertEquals(boards.size,boards.map{"${it.gameId}:${it.level}"}.distinct().size)
        boards.groupBy{it.gameId}.forEach{(_,levels)->
            assertEquals(122,levels.size)
            assertEquals(listOf(1,2),levels.takeLast(2).map{it.mastery})
            assertTrue(levels.all{it.prompt.isNotBlank()})
        }
        assertEquals(WorkshopKind.entries.toSet(),boards.map{it.kind}.toSet())
    }

    @Test
    fun onlyCalculationModesAreUntaggedAndEveryDifficultyIsRepresented() {
        val catalogue = gamifyCatalogueAudit()
        assertEquals(29, catalogue.size)
        assertEquals(
            setOf("speed-basic", "speed-advanced"),
            catalogue.filter { it.difficulty == null }.map { it.id }.toSet(),
        )
        GameDifficulty.entries.forEach { difficulty ->
            assertTrue(
                "$difficulty should be represented in the game catalogue",
                catalogue.any { it.difficulty == difficulty },
            )
        }
    }

    @Test
    fun expandedWorldsAreUniqueAndHaveCompleteLearningRuns() {
        val catalogue = gamifyCatalogueAudit()
        assertEquals(catalogue.size, catalogue.map { it.id }.distinct().size)
        assertEquals(catalogue.size, catalogue.map { it.title }.distinct().size)

        val additions = catalogue.filter { it.id in expandedMathsGameIds }
        assertEquals(15, additions.size)
        additions.forEach { game ->
            assertTrue("${game.title} needs a meaningful learning run", game.missionCount >= 5)
            assertTrue("${game.title} needs a difficulty tag", game.difficulty != null)
        }
    }
}
