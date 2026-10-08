from pathlib import Path
p=Path('app/src/test/java/com/indianservers/aiexplorer/gamifymaths/GameLearningSupportTest.kt');s=p.read_text(encoding='utf-8');s=s.replace('import com.indianservers.aiexplorer.gamifymaths.probability.ProbabilityArcadeGames\n','')
a=s.index('    @Test\n    fun everyProbabilityArcade');b=s.index('\n    @Test',a+10)
s=s[:a]+'''    @Test
    fun allProbabilityFamiliesUseGeneratedConstructionBoards() {
        (0 until 15).forEach { level ->
            val challenge = generateWorkshopChallenge("chance", level, 0, 42L)
            assertEquals(WorkshopKind.Expression, challenge.kind)
            assertTrue(challenge.source!!.answer.isFinite())
            assertFalse(challenge.hint.isBlank())
            assertFalse(challenge.explanation.isBlank())
        }
    }
'''+s[b:];p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/indianservers/aiexplorer/gamifymaths/AlgebraAdventureGameTest.kt')
p.write_text('''package com.indianservers.aiexplorer.gamifymaths

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
    @Test funInvalidOperationsDoNotUnlockTheBalance() {
        val challenge=generateWorkshopChallenge("balance",121,3,42L)
        assertNull(challenge.balanceState(listOf("÷","0")))
        assertFalse(challenge.accepts(emptyList(),listOf("−","100")))
    }
}
'''.replace('@Test funInvalid','@Test fun invalid'),encoding='utf-8')
p=Path('app/src/test/java/com/indianservers/aiexplorer/gamifymaths/probability/ProbabilityArcadeGameTest.kt')
p.write_text('''package com.indianservers.aiexplorer.gamifymaths.probability

import com.indianservers.aiexplorer.gamifymaths.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class ProbabilityArcadeGameTest {
    @Test fun coinAndBinomialProbabilitiesFollowIndependentCounting() {
        fun choose(n:Int,k:Int):Double = (1..k).fold(1.0){v,i->v*(n-i+1)/i}
        repeat(200) { seed ->
            val coin=generateMathRound("chance",2,seed%4,seed.toLong());val n=coin.operands[0].toInt();val k=coin.operands[1].toInt()
            assertEquals(choose(n,k)/2.0.pow(n),coin.answer,1e-10)
            val binomial=generateMathRound("chance",7,seed%4,seed.toLong());val b=binomial.operands
            assertEquals(choose(b[0].toInt(),b[1].toInt())*b[2].pow(b[1])*(1-b[2]).pow(b[0]-b[1]),binomial.answer,1e-10)
        }
    }
    @Test fun probabilityMasteriesRequireConstructedMethods() {
        repeat(100) { seed -> for(level in 120..121) {
            val challenge=generateWorkshopChallenge("chance",level,3,seed.toLong())
            assertTrue(challenge.accepts(emptyList(),challenge.witness))
            assertFalse(challenge.accepts(emptyList(),listOf(roundNumber(challenge.source!!.answer))))
        } }
    }
}
''',encoding='utf-8')
