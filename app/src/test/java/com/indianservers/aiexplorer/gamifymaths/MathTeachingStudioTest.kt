package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test

class MathTeachingStudioTest {
    @Test fun everyGameHasAMethodVerificationMisconceptionAndReflection(){
        val games=gamifyGamesForAudit()
        assertEquals(29,games.size)
        games.forEach{game->
            val prompts=if(game.id.startsWith("speed-"))listOf("4 + 8")else (0L..15L).map{generateWorkshopChallenge(game.id,0,0,it).prompt}
            prompts.forEach{prompt->
                val plan=mathTeachingPlan(game.id,prompt)
                assertTrue(game.id,plan.concept.isNotBlank() && plan.model.isNotBlank())
                assertTrue(game.id,plan.plan.length>40 && plan.check.length>35 && plan.misconception.length>35)
                assertTrue(game.id,plan.reflection.endsWith("?"))
            }
        }
    }
    @Test fun guidanceDistinguishesDifferentMathematicsInTheSameGame(){
        assertEquals("Expected value",mathTeachingPlan("chance","Construct the expected net payoff").concept)
        assertEquals("Probability models",mathTeachingPlan("chance","Find P(red)").concept)
        assertEquals("Measuring variability",mathTeachingPlan("data","Find the population variance").concept)
        assertEquals("Median",mathTeachingPlan("data-story","Mark both middle readings").concept)
        assertEquals("Systematic counting",mathTeachingPlan("logic").concept)
        assertEquals("Functions and rules",mathTeachingPlan("patterns").concept)
        assertFalse(mathTeachingPlan("chance","expected payoff").check.contains("between zero and one"))
    }
}
