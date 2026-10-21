package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
internal fun GeneratedMathGameScreen(game:MathsGame,completed:Int,onBack:()->Unit,onComplete:(Int)->Unit,
                                    initiallyPlaying:Boolean=false,initialSeed:Long?=null) {
    val context=LocalContext.current
    val preferences=remember{context.getSharedPreferences("maths_games",0)}
    var playing by rememberSaveable(game.id){mutableStateOf(initiallyPlaying)}
    var difficulty by rememberSaveable(game.id){mutableIntStateOf(preferences.getInt("difficulty_${game.id}",0).coerceIn(0,3))}
    var hints by rememberSaveable(game.id){mutableStateOf(preferences.getBoolean("hints_${game.id}",true))}
    var focus by rememberSaveable(game.id){mutableIntStateOf(preferences.getInt("focus_${game.id}",-1).takeIf { it in -1 until gameSetupProfile(game.id).topics.size } ?: -1)}
    var board by rememberSaveable(game.id){mutableIntStateOf(0)}
    var level by rememberSaveable(game.id){mutableIntStateOf(completed.coerceIn(0,GeneratedLevelCount-1))}
    var seed by rememberSaveable(game.id){mutableLongStateOf(initialSeed?:Random.nextLong())}
    var history by rememberSaveable(game.id){mutableStateOf(ArrayList(preferences.getString("recent_questions_${game.id}","").orEmpty().split('\n').filter{it.isNotBlank()}.takeLast(RecentMathQuestionCount)))}
    var showHint by rememberSaveable(seed,level){mutableStateOf(false)}
    var solved by rememberSaveable(seed,level,difficulty){mutableStateOf(false)}
    val round=remember(game.id,level,difficulty,seed,focus){generateWorkshopChallenge(game.id,level,difficulty,seed,focus)}
    LaunchedEffect(playing,round.signature) {
        if(playing) {
            history=rememberMathQuestion(history,workshopQuestionHistoryKey(round))
            preferences.edit().putString("recent_questions_${game.id}",history.joinToString("\n")).apply()
        }
    }
    LaunchedEffect(difficulty,hints,focus){preferences.edit().putInt("focus_${game.id}",focus).putInt("difficulty_${game.id}",difficulty).putBoolean("hints_${game.id}",hints).apply()}
    fun start(index:Int) {
        val fresh=freshWorkshopQuestion(game.id,index,difficulty,focus,history)
        level=index;seed=fresh.seed;playing=true;solved=false
    }
    fun next() {
        history=rememberMathQuestion(history,workshopQuestionHistoryKey(round))
        val nextLevel=if(level>=120) level else (level+1)%120
        val fresh=freshWorkshopQuestion(game.id,nextLevel,difficulty,focus,history)
        level=nextLevel;seed=fresh.seed
    }
    if(!playing) {
        LearningGameSettings(game, completed, difficulty, hints, focus, board,
            {difficulty=it;solved=false}, {hints=it}, {focus=it}, {board=it}, onBack,
            {start(if(board>0) 119+board else level.coerceAtMost(119))}, {start(0)})
        return
    }
    if (game.id in themedMathGameIds) {
        ThemedLearningGame(game,round,level,solved,showHint,hints,{showHint=!showHint},onBack,
            {board=if(level>=120)level-119 else 0;playing=false},
            {if(!solved){solved=true;onComplete(level+1)}},::next)
        return
    }
    if (game.id in lightMathGameIds) {
        LightLearningGame(game, round, level, solved, showHint, hints, {showHint=!showHint}, onBack,
            {board=if(level>=120) level-119 else 0;playing=false},
            {if(!solved){solved=true;onComplete(level+1)}}, ::next)
        return
    }
    GameScreen(game.title,level+1,game.accent,3,onBack,{if(hints)showHint=!showHint}) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GamePanel).padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(if(round.mastery>0)"ADVANCED MASTERY ${round.mastery} • ${round.title.uppercase()}" else "${round.title.uppercase()} • STAGE ${level/30+1}/4",
                        color=game.accent,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    TextButton(onClick={board=if(level>=120) level-119 else 0;playing=false},contentPadding=PaddingValues(0.dp),modifier=Modifier.height(24.dp).semantics{contentDescription="Game session settings"}){Text("⚙",color=game.accent,fontSize=20.sp)}
                }
                Text(round.playerPrompt(),color=GameInk,fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
            if(showHint)Text(round.hint,color=GameGold,fontSize=12.sp)
            key(game.id,seed,level,difficulty) {
                MathWorkshopBoard(game,round,solved){if(!solved){solved=true;onComplete(level+1)}}
            }
            if(solved) {
                Text("Mission complete! ${round.explanation}",color=GameGreen,fontSize=12.sp)
                Button(onClick={next()},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=game.accent)){
                    Text(if(level>=120)"New mastery mission" else "Next level",color=GameSpace)
                }
            }
    }
}
