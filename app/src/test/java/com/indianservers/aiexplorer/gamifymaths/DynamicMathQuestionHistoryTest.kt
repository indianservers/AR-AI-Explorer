package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DynamicMathQuestionHistoryTest {
    @Test fun everyWorldRefreshesActualQuestionContentAcrossSessionsAndMasteries() {
        val games=gamifyGamesForAudit().filterNot{it.id.startsWith("speed-")}
        assertEquals(27,games.size)
        games.forEach{game->for(level in listOf(0,119,120,121)) {
            var saved=""
            val answers=mutableSetOf<String>()
            repeat(18){session->
                // Reload exactly the stored representation, as when reopening a game.
                val history=saved.split('\n').filter(String::isNotEmpty)
                val fresh=freshWorkshopQuestion(game.id,level,session%4,-1,history,Random(session.toLong()+921L))
                val round=fresh.challenge
                val key=mathQuestionKey(round.signature)
                assertFalse("${game.id}/$level repeats recent content",key in history)
                assertEquals(round,generateWorkshopChallenge(game.id,level,session%4,fresh.seed))
                round.source?.let{source->
                    assertTrue(source.answer.isFinite())
                    if(source.interaction==RoundInteraction.Answer){
                        assertTrue(source.accepts(roundNumber(source.answer)))
                        assertFalse(source.accepts(roundNumber(source.answer+1)))
                    }
                }
                answers+=when(round.kind){
                    WorkshopKind.Expression,WorkshopKind.Pieces->round.source!!.answer.toString()
                    else->round.witness.toString()
                }
                saved=rememberMathQuestion(history,key).joinToString("\n")
            }
            assertTrue("${game.id}/$level needs varying solutions",answers.size>1)
        }}
    }

    @Test fun aReplayedRandomSequenceSkipsThePreviouslyShownQuestion() {
        val first=freshWorkshopQuestion("calculus-climber",121,3,-1,emptyList(),Random(42))
        val history=listOf(mathQuestionKey(first.challenge.signature))
        val reopened=freshWorkshopQuestion("calculus-climber",121,3,-1,history,Random(42))
        assertNotEquals(first.challenge.signature,reopened.challenge.signature)
        assertTrue(reopened.challenge.accepts(emptyList(),reopened.challenge.witness))
    }

    @Test fun anExhaustedPoolRemainsMathematicallyValidAndTerminates() {
        fun fixed()=object:Random(){override fun nextBits(bitCount:Int)=0}
        val first=freshWorkshopQuestion("times-table",0,0,-1,emptyList(),fixed())
        val repeat=freshWorkshopQuestion("times-table",0,0,-1,listOf(mathQuestionKey(first.challenge.signature)),fixed())
        assertTrue(repeat.challenge.accepts(repeat.challenge.witness.map(String::toInt)))
    }

    @Test fun bothSprintsKeepQuestionsAndComputedChoicesFreshAfterReopening() {
        SpeedCalculationMode.entries.forEach{mode->for(mastery in 0..2){
            var stored=emptyList<String>()
            repeat(40){session->
                val recent=stored.toMutableSet()
                val problem=nextUniqueSpeedProblem(mode,1,setOf("Addition","Division","Percent"),setOf("Algebra","Trigonometry","Advanced Calculation"),recent,Random(session),mastery)
                assertFalse(problem.prompt in stored)
                assertTrue(problem.answer.toDouble().isFinite())
                val choices=speedAnswerChoices(problem,Random(session))
                assertEquals(4,choices.distinct().size)
                assertTrue(problem.answer in choices)
                stored=recent.toList().takeLast(RecentMathQuestionCount)
            }
        }}
    }
}
