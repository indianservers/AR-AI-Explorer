package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test

class PlayerInstructionsTest {
    @Test fun robotInstructionsExplainOrderReuseAndCompletion() {
        for (focus in 0..1) {
            val round = generateWorkshopChallenge("combinatorics", 0, 0, 12L, focus)
            val text = round.playerPrompt()
            assertTrue(text.contains("Use each robot only once"))
            assertTrue(text.contains("all ${round.parameters[3]}"))
            assertTrue(text.contains("tap Collect"))
            assertTrue(text.contains("tap Check"))
            assertTrue(text.contains(if (focus == 1) "AB and BA count as different" else "AB and BA count as the same"))
        }
    }

    @Test fun masteryRobotInstructionsKeepEnergyRule() {
        for (level in 120..121) {
            val round = generateWorkshopChallenge("combinatorics", level, 0, 12L)
            assertTrue(round.playerPrompt().contains("remainder of ${round.parameters[5]} when divided by ${round.parameters[4]}"))
        }
    }
}
