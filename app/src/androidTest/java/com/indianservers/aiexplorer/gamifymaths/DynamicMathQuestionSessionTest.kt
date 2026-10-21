package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DynamicMathQuestionSessionTest {
    @get:Rule val compose=createComposeRule()

    @Test fun reopeningEachOfThe29GamesShowsFreshContentAndSavesItsHistory() {
        val preferences=InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("maths_games",0)
        val games=gamifyGamesForAudit()
        assertEquals(29,games.size)
        val historyKeys=games.map{if(it.id.startsWith("speed-"))"recent_speed_questions_${if(it.id=="speed-basic")SpeedCalculationMode.Basic else SpeedCalculationMode.Advanced}" else "recent_questions_${it.id}"}
        val previous=historyKeys.associateWith{preferences.all[it]}
        val cases=games.flatMap{listOf(it,it)}
        var index by mutableIntStateOf(0)
        try {
            compose.setContent { Box(Modifier.fillMaxSize().safeDrawingPadding()) { if(index<cases.size) key(index) {
                val game=cases[index]
                if(game.id.startsWith("speed-")) SpeedCalculationGame(if(game.id=="speed-basic")SpeedCalculationMode.Basic else SpeedCalculationMode.Advanced,{index++},{})
                else GeneratedMathGameScreen(game,0,{index++},{})
            } } }
            cases.forEachIndexed { caseIndex,game->
                val historyKey=historyKeys[caseIndex/2]
                val speed=game.id.startsWith("speed-")
                val before=if(speed)preferences.getStringSet(historyKey,emptySet()).orEmpty().toSet()else preferences.getString(historyKey,"").orEmpty().split('\n').filter(String::isNotEmpty).toSet()
                if(speed) compose.onNode(hasText("Start ",substring=true) and hasText(" Sprint",substring=true)).performScrollTo().performClick()
                else compose.onNodeWithText("Start playing").performScrollTo().performClick()
                compose.waitForIdle()
                compose.waitUntil(10_000){
                    if(speed)preferences.getStringSet(historyKey,emptySet()).orEmpty().any{it !in before}
                    else preferences.getString(historyKey,"").orEmpty().split('\n').lastOrNull().let{it!=null && it !in before}
                }
                if(!speed) {
                    val stored=preferences.getString(historyKey,"").orEmpty().split('\n')
                    assertTrue(stored.size<=RecentMathQuestionCount)
                    assertFalse("${game.id} repeated a recent question",stored.last() in before)
                }
                compose.onNodeWithContentDescription(if(speed)"Exit challenge" else "Back").performClick()
                compose.waitForIdle()
                assertEquals(caseIndex+1,index)
            }
        } finally {
            val editor=preferences.edit()
            previous.forEach{(key,value)->when(value){
                is String->editor.putString(key,value)
                is Set<*>->editor.putStringSet(key,value.filterIsInstance<String>().toSet())
                else->editor.remove(key)
            }}
            editor.commit()
        }
    }
}
