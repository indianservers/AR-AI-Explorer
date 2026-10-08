package com.indianservers.aiexplorer.gamifymaths.probability

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
