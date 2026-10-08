package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class GeneratedMathRoundsTest {
    private val games = gamifyGamesForAudit().filterNot { it.id.startsWith("speed-") }

    @Test fun everyLiveWorldGeneratesValidFreshReproducibleChallengesAtEveryLevel() {
        assertEquals(27, games.size)
        games.forEach { game ->
            repeat(GeneratedLevelCount) { level ->
                repeat(8) { variant ->
                    val seed = (level * 100L + variant)
                    val round = generateMathRound(game.id,level,variant%4,seed,if(variant%2==0) 4 else 6)
                    assertEquals(round,generateMathRound(game.id,level,variant%4,seed,if(variant%2==0) 4 else 6))
                    assertTrue("${game.id}/$level",round.answer.isFinite())
                    assertTrue(round.explanation.length >= 35)
                    assertFalse(round.hint.contains(round.explanation))
                    if(round.interaction == RoundInteraction.Answer) {
                        assertEquals(if(variant%2==0) 4 else 6, round.choices.distinct().size)
                        assertEquals("${game.id}/$level correct option",1,round.choices.count(round::accepts))
                        assertTrue(round.accepts(roundNumber(round.answer)))
                        assertFalse(round.accepts("NaN"))
                        assertFalse(round.accepts("1/0"))
                    } else assertTrue("${game.id}/$level construction must be solvable",hasConstructionSolution(round))
                }
            }
            val variants=(0L..127L).map{generateMathRound(game.id,0,0,it).signature}.distinct()
            assertTrue("${game.id} must change on replay",variants.size>20)
        }
    }

    private fun hasConstructionSolution(round:GeneratedRound):Boolean = when(round.interaction) {
        RoundInteraction.BuildShapes -> (0..round.requiredPieces-round.requiredCurves).any { triangles ->
            val squares=round.requiredPieces-round.requiredCurves-triangles
            round.acceptsPieces(List(round.requiredCurves){0}+List(triangles){3}+List(squares){4})
        }
        RoundInteraction.BuildPlaceValue -> {
            var value=round.answer.toInt(); val pieces=mutableListOf<Int>();var place=1
            while(value>0) { repeat(value%10){pieces+=place};value/=10;place*=10 }
            round.acceptsPieces(pieces)
        }
        RoundInteraction.BuildSum -> (0 until (1 shl round.pieces.size)).any { mask ->
            round.acceptsPieces(round.pieces.filterIndexed { index,_ -> mask and (1 shl index)!=0 })
        }
        else -> false
    }

    @Test fun generatedAnswersFollowIndependentMathematicalChecks() {
        repeat(200) { seed ->
            fun round(id:String,level:Int) = generateMathRound(id,level,seed%4,seed.toLong())
            fun equals(expected:Double, round:GeneratedRound) = assertEquals(round.prompt,expected,round.answer,.000000001)
            val perimeter=round("measure",0); equals(2*perimeter.operands.sum(),perimeter)
            val area=round("measure",1); equals(area.operands[0]*area.operands[1]/2,area)
            val volume=round("measure",2); equals(volume.operands.reduce(Double::times),volume)
            val mean=round("data",0); equals(mean.operands.average(),mean)
            val median=round("data",1); equals(median.operands.sorted()[median.operands.size/2],median)
            val range=round("data",2); equals(range.operands.max()-range.operands.min(),range)
            val determinant=round("matrix-mission",0); val m=determinant.operands; equals(m[0]*m[3]-m[1]*m[2],determinant)
            val mixture=round("potions",0); val mix=mixture.operands; equals(mix[1]*mix[2],mixture)
            val derivative=round("calculus-climber",0); val d=derivative.operands; equals(2*d[0]*d[2]+d[1],derivative)
            val integral=round("calculus-climber",1); val i=integral.operands; equals(i[0]*i[1]*i[1]/2,integral)
            val probability=round("chance",0); val p=probability.operands; equals(p[0]/p.sum(),probability)
            val tree=round("chance",4); val t=tree.operands; equals(t[0]/t.sum()*(t[0]-1)/(t.sum()-1),tree)
            val sd=round("chance",13); val average=sd.operands.average(); equals(sqrt(sd.operands.map{(it-average)*(it-average)}.average()),sd)
            val sample=round("chance",10); val s=sample.operands; equals(s[0]/100*s[1],sample); assertTrue(s[0] in 0.0..100.0)
            val cargo=round("optimization-arena",0)
            val best=IntArray(cargo.capacity+1)
            cargo.pieces.forEach{value ->
                val weight=cargo.weights.getValue(value)
                for(space in cargo.capacity downTo weight) best[space]=maxOf(best[space],best[space-weight]+value)
            }
            equals(best.max().toDouble(),cargo)
            for(level in listOf(0,2,3,4,7)) {
                val chance=round("chance",level)
                assertTrue(chance.answer in 0.0..1.0)
                assertTrue(chance.choices.all{it.toDouble() in 0.0..1.0})
            }
        }
    }

    @Test fun constructionRejectsInventedTilesAndWrongPieceCounts() {
        val round=generateMathRound("number-bonds",0,0,18)
        assertFalse(round.acceptsPieces(listOf(round.answer.toInt())))
        val shape=generateMathRound("shapes",0,0,18)
        assertFalse(shape.acceptsPieces(List(shape.requiredPieces){-1}))
        val forge=generateMathRound("forge",0,0,18)
        assertFalse(forge.acceptsPieces(listOf(forge.answer.toInt())))
        val fraction=generateMathRound("chance",0,0,18)
        val p=fraction.operands
        assertTrue(fraction.accepts("${p[0].toInt()*2}/${p.sum().toInt()*2}"))
    }

    @Test fun exhaustedSprintPoolAddsRealArithmeticAndUpdatesTheAnswer() {
        val random=object:Random(){ override fun nextBits(bitCount:Int)=0 }
        val seen=mutableSetOf<String>()
        val first=nextUniqueSpeedProblem(SpeedCalculationMode.Basic,1,setOf("Addition"),emptySet(),seen,random)
        val second=nextUniqueSpeedProblem(SpeedCalculationMode.Basic,1,setOf("Addition"),emptySet(),seen,random)
        assertNotEquals(first.prompt,second.prompt)
        assertEquals(first.answer.toDouble()+1,second.answer.toDouble(),0.0)
        assertFalse(second.prompt.contains("0 \\times"))
    }
}
