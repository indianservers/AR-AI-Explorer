package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class InteractiveMathChallengesTest {
    private val games=gamifyGamesForAudit().filterNot{it.id.startsWith("speed-")}
    @Test fun everyWorldAndBothMasteriesHaveGeneratedSolvableInteractiveBoards() {
        val mechanics=mutableSetOf<WorkshopKind>()
        for(game in games) {
            repeat(GeneratedLevelCount){level->repeat(4){seed->
                val round=generateWorkshopChallenge(game.id,level,seed,level*101L+seed)
                assertEquals(round,generateWorkshopChallenge(game.id,level,seed,level*101L+seed))
                assertFalse(round.prompt.isBlank());assertTrue(round.explanation.length>=35)
                assertEquals(if(level>=120)level-119 else 0,round.mastery)
                mechanics+=round.kind
                val (selected,assembly,marks)=solve(round)
                assertTrue("${game.id}/$level/${seed}: ${round.prompt}",round.accepts(selected,assembly,marks))
            }}
            for(level in listOf(0,120,121)) {
                val variants=(0L..63L).map{generateWorkshopChallenge(game.id,level,0,it).signature}.distinct()
                assertTrue("${game.id}/$level must generate different tasks",variants.size>10)
            }
        }
        assertEquals(WorkshopKind.entries.toSet(),mechanics)
    }
    private fun solve(round:WorkshopChallenge):Triple<List<Int>,List<String>,List<Int>> = when(round.kind) {
        WorkshopKind.Expression->Triple(emptyList(),round.witness.ifEmpty{listOf(roundNumber(round.source!!.answer),"+","2","−","2")},emptyList())
        WorkshopKind.Navigation,WorkshopKind.Mixture,WorkshopKind.Grid,WorkshopKind.Factors->Triple(round.witness.map(String::toInt),emptyList(),emptyList())
        WorkshopKind.Balance,WorkshopKind.Machine,WorkshopKind.Configurations->Triple(emptyList(),round.witness,emptyList())
        WorkshopKind.Sort->{val split=round.witness.indexOf("|");Triple(round.witness.take(split).map(String::toInt),emptyList(),round.witness.drop(split+1).map(String::toInt))}
        WorkshopKind.Pieces->{
            val source=round.source!!
            val selected=when(source.interaction) {
                RoundInteraction.BuildPlaceValue->{var target=source.answer.toInt();var value=1;buildList{while(target>0){repeat(target%10){add(value)};target/=10;value*=10}}}
                RoundInteraction.BuildShapes->{
                    fun search(left:Int,sides:Int):List<Int>? {
                        if(left==0)return if(sides==0)emptyList() else null
                        for(value in source.pieces.filter{it>0}){if(value<=sides){val rest=search(left-1,sides-value);if(rest!=null)return listOf(value)+rest}}
                        return null
                    }
                    List(source.requiredCurves){0}+search(source.requiredPieces-source.requiredCurves,source.answer.toInt())!!
                }
                else->(0 until (1 shl source.pieces.size)).asSequence().map{mask->source.pieces.filterIndexed{i,_->mask and (1 shl i)!=0}}.first(source::acceptsPieces)
            }
            Triple(selected,emptyList(),emptyList())
        }
    }
    @Test fun boardValidatorsRejectIncorrectMovesAndSingleLiteralAnswers() {
        games.forEach{game->for(level in listOf(0,120,121)){
            val round=generateWorkshopChallenge(game.id,level,3,42L)
            assertFalse("${game.id}/$level empty board",round.accepts(emptyList()))
            if(round.kind==WorkshopKind.Expression)assertFalse(round.accepts(emptyList(),listOf(roundNumber(round.source!!.answer))))
            if(round.kind==WorkshopKind.Navigation)assertFalse(round.accepts(List(round.parameters[4]+1){0}))
            if(round.kind==WorkshopKind.Mixture)assertFalse(round.accepts(listOf(round.parameters[2],0)))
        }}
    }
    @Test fun arithmeticParserPreservesGroupingAndRejectsUndefinedExpressions() {
        assertEquals(35.0,evaluateMathAssembly(listOf("(","3","+","4",")","×","5"))!!.value,0.0)
        assertEquals(11.0,evaluateMathAssembly(listOf("3","+","2","^","3"))!!.value,0.0)
        assertEquals(10.0,evaluateMathAssembly(listOf("5","C","2"))!!.value,0.0)
        assertEquals(20.0,evaluateMathAssembly(listOf("5","P","2"))!!.value,0.0)
        assertEquals(.5,evaluateMathAssembly(listOf("sin","30"))!!.value,1e-10)
        assertNull(evaluateMathAssembly(listOf("2","÷","0")))
        assertNull(evaluateMathAssembly(listOf("√","−","1")))
        assertNull(evaluateMathAssembly(listOf("tan","90")))
        assertNull(evaluateMathAssembly(listOf("(","2","+","3")))
        assertNull(evaluateMathAssembly(listOf("NaN")))
    }
    @Test fun everyGameHasItsOwnIllustrationAndEachSprintHasTwoGeneratedMasteries() {
        val catalogue=gamifyGamesForAudit()
        assertEquals(29,catalogue.map{gameCardArtwork(it.id)}.distinct().size)
        SpeedCalculationMode.entries.forEach{mode->for(mastery in 1..2){
            val questions=(0L..99L).map{speedMasteryProblem(mode,mastery,Random(it))}
            assertTrue(questions.map{it.prompt}.distinct().size>90)
            assertTrue(questions.all{it.answer.toDouble().isFinite()})
        }}
    }
}
