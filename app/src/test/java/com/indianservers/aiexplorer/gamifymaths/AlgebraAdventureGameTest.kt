package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test

class AlgebraAdventureGameTest {
    @Test fun everyBalanceMissionCanBeSolvedByEquivalentOperations() {
        repeat(GeneratedLevelCount) { level -> repeat(10) { seed ->
            val challenge=generateWorkshopChallenge("balance",level,seed%4,seed.toLong())
            assertEquals(WorkshopKind.Balance,challenge.kind)
            assertTrue(challenge.accepts(emptyList(),challenge.witness))
            assertFalse(challenge.accepts(emptyList(),emptyList()))
            val x=challenge.numbers[4]
            for(size in 0..challenge.witness.size step 2) {
                val state=challenge.balanceState(challenge.witness.take(size))!!
                assertEquals(state[0]*x+state[1],state[2]*x+state[3],.0000001)
            }
        } }
    }
    @Test fun invalidOperationsDoNotUnlockTheBalance() {
        val challenge=generateWorkshopChallenge("balance",121,3,42L)
        assertNull(challenge.balanceState(listOf("÷","0")))
        assertFalse(challenge.accepts(emptyList(),listOf("−","100")))
    }
}
