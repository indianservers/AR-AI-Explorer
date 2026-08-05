package com.indianservers.aiexplorer.gamifymaths

import com.indianservers.aiexplorer.gamifymaths.probability.ProbabilityArcadeGames
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
    fun everyProbabilityArcadeLevelHasAValidLearningContract() {
        assertEquals(15, ProbabilityArcadeGames.size)
        ProbabilityArcadeGames.forEach { game ->
            assertEquals("${game.title} should have three progressive levels", 3, game.levels.size)
            game.levels.forEach { level ->
                assertTrue(level.answer in level.choices)
                assertFalse(level.hint.isBlank())
                assertFalse(level.explanation.isBlank())
                assertEquals(4, level.choices.distinct().size)
            }
        }
    }

    @Test
    fun everyGeneralMissionHasUniqueChoicesAnswerAndTeachingExplanation() {
        val missions = gamifyMissionAudit()
        assertTrue(missions.size >= 40)
        assertEquals(missions.size, missions.map { "${it.gameId}:${it.title}" }.distinct().size)
        missions.forEach { mission ->
            assertTrue("${mission.gameId}/${mission.title} answer must be selectable", mission.answer in mission.choices)
            assertEquals("${mission.gameId}/${mission.title} needs four distinct choices", 4, mission.choices.distinct().size)
            assertTrue("${mission.gameId}/${mission.title} explanation is too shallow", mission.explanation.length >= 35)
        }
    }

    @Test
    fun formerlyShortWorldsNowCoverProgressiveMultiLevelLearning() {
        val byGame = gamifyMissionAudit().groupBy { it.gameId }
        assertTrue(byGame.getValue("vectors").size >= 8)
        assertTrue(byGame.getValue("patterns").size >= 8)
        assertTrue(byGame.getValue("logic").size >= 8)
    }
}
