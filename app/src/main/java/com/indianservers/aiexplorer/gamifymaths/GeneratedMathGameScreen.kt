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
    var level by rememberSaveable(game.id){mutableIntStateOf(completed.coerceIn(0,GeneratedLevelCount-1))}
    var seed by rememberSaveable(game.id){mutableLongStateOf(initialSeed?:Random.nextLong())}
    var history by rememberSaveable(game.id){mutableStateOf(arrayListOf<String>())}
    var showHint by rememberSaveable(seed,level){mutableStateOf(false)}
    var solved by rememberSaveable(seed,level,difficulty){mutableStateOf(false)}
    val round=remember(game.id,level,difficulty,seed){generateWorkshopChallenge(game.id,level,difficulty,seed)}
    LaunchedEffect(difficulty,hints){preferences.edit().putInt("difficulty_${game.id}",difficulty).putBoolean("hints_${game.id}",hints).apply()}
    fun start(index:Int) {level=index;seed=Random.nextLong();playing=true;solved=false}
    fun next() {
        history=ArrayList((history+round.signature).takeLast(GeneratedLevelCount))
        level=(level+1)%GeneratedLevelCount
        var newSeed=Random.nextLong()
        var candidate=generateWorkshopChallenge(game.id,level,difficulty,newSeed)
        var attempts=0
        while((candidate.signature in history || (round.kind==WorkshopKind.Pieces && candidate.prompt==round.prompt)) && attempts++<1000){newSeed=Random.nextLong();candidate=generateWorkshopChallenge(game.id,level,difficulty,newSeed)}
        seed=newSeed
    }
    GameScreen(game.title,level+1,game.accent,3,{if(playing)playing=false else onBack()},{if(hints && playing)showHint=!showHint}) {
        if(!playing) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                GameEmblem(game,Modifier.size(58.dp))
                Column(Modifier.weight(1f)) {
                    Text("122 LEVELS • INTERACTIVE OFFLINE PLAY",color=game.accent,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    Text("Build, move, mix and test. Every mission is generated from a fresh seed.",color=GameInk,fontSize=14.sp)
                }
            }
            Text("Difficulty",color=GameMuted)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("Easy","Medium","Hard","Expert").forEachIndexed{index,label->
                    Button(onClick={difficulty=index;solved=false},modifier=Modifier.weight(1f),contentPadding=PaddingValues(3.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=if(difficulty==index)game.accent else GamePanel)){Text(label,fontSize=11.sp,color=GameInk)}
                }
            }
            Button(onClick={hints=!hints}){Text(if(hints)"Hints: on" else "Hints: off")}
            Text("120 progressive missions + 2 advanced mastery boards. Worked solutions appear only after your construction passes.",color=GameMuted,fontSize=12.sp)
            Button(onClick={playing=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=game.accent)){
                Text(if(completed==0)"Start playing" else "Continue at level ${level+1}",color=GameSpace,fontWeight=FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Button(onClick={start(120)},modifier=Modifier.weight(1f)){Text("Mastery I",fontSize=12.sp)}
                Button(onClick={start(121)},modifier=Modifier.weight(1f)){Text("Mastery II",fontSize=12.sp)}
            }
            if(completed>0) TextButton(onClick={start(0)}){Text("Replay from level 1 • fresh missions",color=game.accent)}
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(GamePanel).padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(if(round.mastery>0)"ADVANCED MASTERY ${round.mastery} • ${round.title.uppercase()}" else "${round.title.uppercase()} • STAGE ${level/30+1}/4",
                        color=game.accent,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    TextButton(onClick={playing=false},contentPadding=PaddingValues(0.dp),modifier=Modifier.height(24.dp).semantics{contentDescription="Game session settings"}){Text("⚙",color=game.accent,fontSize=20.sp)}
                }
                Text(round.prompt,color=GameInk,fontSize=16.sp,fontWeight=FontWeight.Bold)
            }
            if(showHint)Text(round.hint,color=GameGold,fontSize=12.sp)
            key(game.id,seed,level,difficulty) {
                MathWorkshopBoard(game,round,solved){if(!solved){solved=true;onComplete(level+1)}}
            }
            if(solved) {
                Text("Mission complete! ${round.explanation}",color=GameGreen,fontSize=12.sp)
                Button(onClick={next()},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=game.accent)){
                    Text(if(level==GeneratedLevelCount-1)"Start a fresh expedition" else "Next level",color=GameSpace)
                }
            }
        }
    }
}
