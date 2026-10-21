package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.R
import kotlin.math.*

internal val themedMathGameIds=setOf("times-table","vectors","number-theory","optimization-arena","patterns","percent-studio","potions","ratio-rangers","measure")
private val Ink=Color(0xFF101739)
private val Muted=Color(0xFF777A92)
private val LocalCompactTheme=staticCompositionLocalOf{false}
private data class WorldArt(val accent:Color,val scene:Int,val floor:Int,val serif:Boolean=false)
private fun worldArt(id:String)=when(id){
    "times-table"->WorldArt(Color(0xFF078CFF),R.drawable.math_theme_times,R.drawable.math_asset_times_table_floor)
    "vectors"->WorldArt(Color(0xFF079D9D),R.drawable.math_theme_vectors,R.drawable.math_asset_vectors_floor)
    "number-theory"->WorldArt(Color(0xFF8534DB),R.drawable.math_theme_number_theory,R.drawable.math_asset_number_theory_floor,true)
    "optimization-arena"->WorldArt(Color(0xFFFF7510),R.drawable.math_theme_optimization,R.drawable.math_asset_optimization_arena_floor)
    "patterns"->WorldArt(Color(0xFF8852D8),R.drawable.math_theme_patterns,R.drawable.math_asset_patterns_floor,true)
    "percent-studio"->WorldArt(Color(0xFFEE146C),R.drawable.math_theme_percent,R.drawable.math_asset_percent_studio_floor)
    "potions"->WorldArt(Color(0xFFC11D62),R.drawable.math_theme_potions,R.drawable.math_asset_potions_floor,true)
    "ratio-rangers"->WorldArt(Color(0xFFFF625B),R.drawable.math_theme_ratio_rangers,R.drawable.math_asset_ratio_rangers_floor)
    else->WorldArt(Color(0xFFF17C0A),R.drawable.math_theme_measure,R.drawable.math_asset_measure_floor)
}

@Composable
private fun WorldCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    val accent=LocalMathBoardAccent.current
    Column(modifier.fillMaxWidth().shadow(5.dp,RoundedCornerShape(20.dp),ambientColor=accent.copy(.15f),spotColor=accent.copy(.15f))
        .clip(RoundedCornerShape(20.dp)).background(Color.White.copy(.93f))
        .border(1.dp,accent.copy(.1f),RoundedCornerShape(20.dp)).padding(if(LocalCompactTheme.current)8.dp else 15.dp),
        verticalArrangement=Arrangement.spacedBy(7.dp),content=content)
}

@Composable
internal fun ThemedLearningGame(game:MathsGame,round:WorkshopChallenge,level:Int,solved:Boolean,showHint:Boolean,hints:Boolean,
    onHint:()->Unit,onBack:()->Unit,onSettings:()->Unit,onComplete:()->Unit,onNext:()->Unit){
    val art=worldArt(game.id)
    LightMathSystemBars()
    MaterialTheme(colorScheme=lightColorScheme(primary=art.accent,onSurface=Ink,surface=Color.White)){
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White,lerp(Color.White,art.accent,.1f))))){
            val compact=maxHeight<740.dp
            if(game.id=="potions")Image(painterResource(art.scene),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
            else Image(painterResource(art.floor),null,Modifier.fillMaxWidth().height(115.dp).align(Alignment.BottomCenter),contentScale=ContentScale.FillBounds)
            CompositionLocalProvider(LocalMathBoardAccent provides art.accent,LocalCompactTheme provides compact){
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(if(compact)8.dp else 14.dp),
                    verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 14.dp)){
                    Row(Modifier.fillMaxWidth().padding(vertical=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        WorldCircle("‹","Back",false,onBack)
                        Box(Modifier.size(39.dp).shadow(4.dp,CircleShape).background(Brush.linearGradient(listOf(lerp(art.accent,Color.White,.2f),art.accent)),CircleShape)
                            .border(1.dp,Color.White.copy(.8f),CircleShape),contentAlignment=Alignment.Center){Text("${level+1}",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp)}
                        Column(Modifier.weight(1f)){
                            Text(game.title,color=Ink,fontSize=if(compact)17.sp else 22.sp,fontWeight=FontWeight.Bold,fontFamily=if(art.serif)FontFamily.Serif else FontFamily.Default,lineHeight=23.sp)
                            Text("LEVEL ${level+1}  ·  BEST ${LocalPreviousGameBest.current}",color=art.accent,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
                        }
                        WorldCircle("?","Show hint",true,{if(hints)onHint()})
                    }
                    Box(Modifier.fillMaxWidth().shadow(5.dp,RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(.95f))){
                        if(game.id !in setOf("potions","optimization-arena","times-table"))Image(painterResource(art.scene),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,alpha=.8f)
                        if(game.id !in setOf("potions","optimization-arena","times-table"))Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.White,Color.White.copy(.92f),Color.White.copy(.15f)))))
                        if(game.id=="times-table")Image(painterResource(R.drawable.math_asset_times_table_emblem),null,Modifier.size(70.dp).align(Alignment.CenterEnd).padding(end=7.dp))
                        Column(Modifier.padding(if(compact)9.dp else 13.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text("${round.title.uppercase()} · ${if(level>=120)"MASTERY ${level-119}" else "STAGE ${level/30+1}/4"}",color=art.accent,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                Text("⚙",color=art.accent,fontSize=19.sp,modifier=Modifier.size(28.dp).clickable(onClick=onSettings).semantics{contentDescription="Game session settings"})
                            }
                            MathQuestionText(round.playerPrompt(),gameId=game.id,color=Ink,fontSize=if(compact)15.sp else 19.sp,lineHeight=if(compact)20.sp else 25.sp,fontWeight=FontWeight.SemiBold,
                                fontFamily=if(art.serif)FontFamily.Serif else FontFamily.Default,modifier=Modifier.padding(end=if(compact)0.dp else if(game.id=="patterns")112.dp else if(game.id in setOf("vectors","number-theory","times-table"))48.dp else 0.dp))
                        }
                    }
                    if(showHint && hints)WorldCard{Text(round.hint,color=Muted,fontSize=12.sp)}
                    key(game.id,round.signature){MathWorkshopBoard(game,round,solved){if(!solved)onComplete()}}
                    if(solved){
                        MathMissionDebrief(game.id,round)
                        WorldButton(if(level>=120)"New mastery mission" else "Next level",onClick=onNext)
                    }
                }
            }
        }
    }
}

@Composable private fun WorldCircle(label:String,description:String,gold:Boolean,onClick:()->Unit){
    val accent=if(gold)Color(0xFFFFB900) else LocalMathBoardAccent.current
    Box(Modifier.size(40.dp).shadow(4.dp,CircleShape).background(Brush.linearGradient(if(gold)listOf(Color(0xFFFFE183),accent) else listOf(Color.White,lerp(Color.White,accent,.1f))),CircleShape)
        .border(1.dp,Color.White,CircleShape).clickable(onClick=onClick).semantics{contentDescription=description},contentAlignment=Alignment.Center){
        Text(label,color=if(gold)Color.White else accent,fontSize=28.sp,fontWeight=FontWeight.Bold)
    }
}

@Composable private fun WorldButton(label:String,modifier:Modifier=Modifier,enabled:Boolean=true,onClick:()->Unit){
    val accent=LocalMathBoardAccent.current
    Box(modifier.fillMaxWidth().heightIn(min=48.dp).shadow(if(enabled)5.dp else 0.dp,RoundedCornerShape(25.dp))
        .clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(if(enabled)listOf(lerp(accent,Color.White,.2f),accent) else listOf(Color(0xFFE8E5EC),Color(0xFFE8E5EC))))
        .border(1.dp,Color.White.copy(.7f),RoundedCornerShape(25.dp)).clickable(enabled=enabled,onClick=onClick)
        .semantics{role=Role.Button;if(!enabled)disabled()}.padding(12.dp),contentAlignment=Alignment.Center){Text(label,color=if(enabled)Color.White else Muted,fontWeight=FontWeight.Bold,fontSize=17.sp)}
}

private class WorldDrop {var bounds by mutableStateOf(Rect.Zero);var hover by mutableStateOf(false)}
@Composable private fun WorldDropBoard(area:WorldDrop,modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit){
    val accent=LocalMathBoardAccent.current
    Box(modifier.fillMaxWidth().onGloballyPositioned{area.bounds=it.boundsInRoot()}.semantics{contentDescription="Construction drop zone"}
        .background(if(area.hover)accent.copy(.1f) else Color.Transparent,RoundedCornerShape(15.dp)),content=content)
}
@Composable private fun WorldTile(label:String,description:String,area:WorldDrop?,enabled:Boolean,modifier:Modifier=Modifier,icon:Int?=null,onClick:()->Unit){
    val accent=LocalMathBoardAccent.current
    val category=mathTokenCategory(label)
    var rect by remember{mutableStateOf(Rect.Zero)};var offset by remember{mutableStateOf(Offset.Zero)};var pointer by remember{mutableStateOf(Offset.Zero)}
    val latest by rememberUpdatedState(onClick);val available by rememberUpdatedState(enabled)
    Column(modifier.heightIn(min=43.dp).onGloballyPositioned{rect=it.boundsInRoot()}.graphicsLayer{translationX=offset.x;translationY=offset.y;alpha=if(enabled)1f else .35f}
        .shadow(2.dp,RoundedCornerShape(13.dp)).clip(RoundedCornerShape(13.dp)).background(Brush.verticalGradient(listOf(Color.White,category?.fill ?: lerp(Color.White,accent,.1f))))
        .border(1.dp,(category?.ink ?: accent).copy(.3f),RoundedCornerShape(13.dp)).semantics{contentDescription=description;category?.let{stateDescription="${it.label} tile"};role=Role.Button;if(!enabled)disabled()}
        .pointerInput(enabled,label){if(enabled && area!=null)detectDragGestures(onDragStart={pointer=rect.topLeft+it},onDrag={change,delta->change.consume();pointer+=delta;offset+=delta;area.hover=area.bounds.contains(pointer)},
            onDragEnd={val valid=area.bounds.contains(pointer);offset=Offset.Zero;area.hover=false;if(valid && available)latest()},onDragCancel={offset=Offset.Zero;area.hover=false})}
        .clickable(enabled=enabled,onClick=onClick).padding(horizontal=if(icon!=null && LocalCompactTheme.current)4.dp else 8.dp,vertical=if(icon!=null && LocalCompactTheme.current)5.dp else 8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
        if(icon!=null)Image(painterResource(icon),null,Modifier.size(if(LocalCompactTheme.current)22.dp else 49.dp))
        Text(label,color=category?.ink ?: Ink,fontSize=if(icon!=null && LocalCompactTheme.current)12.sp else 15.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,lineHeight=if(icon!=null && LocalCompactTheme.current)14.sp else 18.sp)
    }
}

@Composable internal fun ThemedWorkshopBoard(game:MathsGame,round:WorkshopChallenge,solved:Boolean,onAttempt:(Boolean)->Unit,onSolved:()->Unit){
    if(round.kind==WorkshopKind.Expression){
        if(game.id=="percent-studio" && round.mastery==0)PercentDiagram(round)
        if(game.id=="measure" && round.kind==WorkshopKind.Expression && round.mastery==0)RescueDiagram(round)
        LightWorkshopBoard(game,round,solved,onAttempt,onSolved)
        return
    }
    var selection by rememberSaveable{mutableStateOf(arrayListOf<Int>())}
    var assembly by rememberSaveable{mutableStateOf(arrayListOf<String>())}
    var status by rememberSaveable{mutableStateOf("")}
    val area=remember{WorldDrop()};val accent=LocalMathBoardAccent.current;val compact=LocalCompactTheme.current
    var checked by rememberSaveable{mutableStateOf(false)}
    fun check(){if(!solved){val valid=round.accepts(selection,assembly);onAttempt(valid);status=round.attemptFeedback(selection,assembly);checked=true;if(valid)onSolved()}}
    fun add(value:Int){if(!solved){selection=ArrayList(selection+value);status=""}}
    when(round.kind){
        WorkshopKind.Machine->{
            var operand by rememberSaveable{mutableStateOf(roundNumber(round.numbers.first()))}
            WorldCard{
                Text("EXAMPLES · INPUT → OUTPUT",color=accent,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    round.examples.forEach{(input,output)->Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly){
                        Text(roundNumber(input),color=Muted,fontSize=17.sp,lineHeight=22.sp,fontFamily=FontFamily.Serif)
                        Text("→",color=accent.copy(.6f),fontSize=17.sp,lineHeight=22.sp)
                        Text(roundNumber(output),color=accent,fontSize=17.sp,lineHeight=22.sp,fontWeight=FontWeight.Bold,fontFamily=FontFamily.Serif)
                    }}
                }
            }
            WorldCard{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("YOUR CIRCUIT",color=accent,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold);Text("Steps: ${assembly.size/2} / 6",color=Muted,fontSize=10.sp,lineHeight=14.sp)}
                WorldDropBoard(area){Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("x →",color=accent,fontFamily=FontFamily.Serif,fontSize=20.sp)
                    repeat(6){i->Box(Modifier.weight(1f).height(38.dp).border(1.dp,accent.copy(.3f),RoundedCornerShape(8.dp))
                        .background(accent.copy(.045f),RoundedCornerShape(8.dp)).clickable(enabled=!solved && i<assembly.size/2){assembly=ArrayList(assembly.filterIndexed{index,_->index/2!=i});status=""},contentAlignment=Alignment.Center){
                        Text(if(i<assembly.size/2){if(assembly[i*2]=="²")"x²" else assembly[i*2]+assembly[i*2+1]}else"+",color=accent,fontSize=11.sp,lineHeight=14.sp)
                    }}
                    Text("→ ?",color=accent,fontFamily=FontFamily.Serif,fontSize=20.sp)
                }}
            }
            WorldCard{
                Text("OPERATION BLOCKS",color=accent,fontSize=10.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){
                    round.numbers.distinct().forEach{value->WorldTile(roundNumber(value),"Select core ${roundNumber(value)}",null,!solved,Modifier.weight(1f)){operand=roundNumber(value)}}
                    LightAction("−1",enabled=!solved){operand=roundNumber(operand.toDouble()-1)}
                    LightAction("+1",enabled=!solved){operand=roundNumber(operand.toDouble()+1)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("+","×","²").forEach{op->WorldTile(when(op){"+"->"+ Add\nx + $operand";"×"->"× Multiply\nx × $operand";else->"x² Power"},"Add $op block",area,!solved && assembly.size<12,Modifier.weight(1f)){
                        assembly=ArrayList(assembly+listOf(op,if(op=="²")"2" else operand));status=""
                    }}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                LightAction("Undo block",enabled=!solved && assembly.isNotEmpty(),modifier=Modifier.weight(1f)){assembly=ArrayList(assembly.dropLast(2));status=""}
                WorldButton("Test circuit",enabled=!solved,modifier=Modifier.weight(1.2f),onClick=::check)
            }
        }
        WorkshopKind.Grid->{
            val current=if(selection.size==2)selection else listOf(1,1)
            if(selection.size!=2)SideEffect{selection=arrayListOf(1,1)}
            WorldCard{
                Box(Modifier.fillMaxWidth().height(if(compact)105.dp else 240.dp).clip(RoundedCornerShape(16.dp))){
                    Image(painterResource(R.drawable.math_theme_times),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
                    Column(Modifier.fillMaxSize().padding(horizontal=22.dp,vertical=25.dp),verticalArrangement=Arrangement.SpaceEvenly){
                        repeat(current[0]){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){repeat(current[1]){
                            Image(painterResource(R.drawable.math_asset_star),null,Modifier.size(minOf(if(compact)24.dp else 42.dp,((if(compact)82f else 180f)/current[0]).dp,(240f/current[1]).dp)))
                        }}}
                    }
                }
            }
            current.forEachIndexed{axis,value->WorldCard{
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(if(axis==0)"Rows" else "Columns",fontWeight=FontWeight.Bold,color=Ink,fontSize=14.sp,modifier=Modifier.weight(1f))
                    Text("$value",color=accent,fontWeight=FontWeight.Bold,fontSize=23.sp,modifier=Modifier.width(27.dp),textAlign=TextAlign.Center)
                    listOf(-1,1,3).forEach{delta->WorldTile(if(delta>0)"+$delta" else "$delta","${if(delta>0)"Increase" else "Decrease"} ${if(axis==0)"rows" else "columns"} by ${abs(delta)}",null,!solved,Modifier.width(45.dp)){
                        val next=current.toMutableList();next[axis]=(value+delta).coerceIn(1,40);selection=ArrayList(next);status=""
                    }}
                }
            }}
            WorldCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Total stars","${current[0]*current[1]}");Metric("Area","${current[0]} × ${current[1]}");Metric("Perimeter","${2*current.sum()}")}}
            WorldButton("Launch array",enabled=!solved,onClick=::check)
        }
        WorkshopKind.Mixture->{
            val current=if(selection.size==2)selection else listOf(0,0)
            if(selection.size!=2)SideEffect{selection=arrayListOf(0,0)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                repeat(2){axis->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(if(axis==0)"BLUE" else "RED",color=if(axis==0)Color(0xFF128EFF) else Color(0xFFEF3F70),fontSize=13.sp,fontWeight=FontWeight.Bold)
                    LiquidFlask(current[axis],round.parameters[2],axis==0,game.id=="potions",if(compact)120 else 285)
                    Text("${current[axis]} ml",color=Ink,fontSize=19.sp,fontWeight=FontWeight.Bold,fontFamily=if(game.id=="potions")FontFamily.Serif else FontFamily.Default)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        listOf(-1,1,5).forEach{delta->WorldTile(if(delta>0)"+$delta" else "$delta","${if(delta>0)"Increase" else "Decrease"} ${if(axis==0)"blue volume" else "red volume"} by ${abs(delta)}",null,!solved,Modifier.weight(1f)){
                            val next=current.toMutableList();next[axis]=(next[axis]+delta).coerceIn(0,round.parameters[2]);selection=ArrayList(next);status=""
                        }}
                    }
                }}
            }
            WorldCard{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Current ratio","${current[0]} : ${current[1]}");Metric("Total batch","${current.sum()} / ${round.parameters[2]} ml")}
                LinearProgressIndicator(progress={minOf(1f,current.sum().toFloat()/round.parameters[2])},modifier=Modifier.fillMaxWidth(),color=accent,trackColor=accent.copy(.1f))
            }
            WorldButton("Mix batch",enabled=!solved,onClick=::check)
        }
        WorkshopKind.Factors->{
            WorldCard{
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("YOUR FACTOR CHAIN",color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold);Metric("Product","${selection.fold(1L){a,b->a*b}}")}
                WorldDropBoard(area){FlowRow(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    if(selection.isEmpty())repeat(5){Box(Modifier.size(43.dp).border(1.dp,accent.copy(.3f),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){Text("+",color=accent,fontSize=25.sp)}}
                    selection.forEachIndexed{i,prime->WorldTile("$prime","Remove factor ${i+1}",null,!solved,Modifier.width(44.dp)){selection=ArrayList(selection.filterIndexed{index,_->index!=i});status=""}}
                }}
                Text("CHOOSE A PRIME CORE",color=accent,fontSize=11.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(2,3,5,7,11,13).forEach{prime->WorldTile("$prime","Add prime $prime",area,!solved && selection.size<12,Modifier.weight(1f)){add(prime)}}}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                LightAction("Remove core",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1f)){selection=ArrayList(selection.dropLast(1));status=""}
                WorldButton("Unlock crypt",enabled=!solved,modifier=Modifier.weight(1.5f),onClick=::check)
            }
            WorldCard{Text("How it works",color=accent,fontWeight=FontWeight.Bold);Text("The product updates as you add prime cores. Reach ${round.parameters[0]} to unlock the crypt.",color=Muted,fontSize=12.sp)}
        }
        WorkshopKind.Pieces->{
            val source=round.source!!
            if(source.capacity>0){
                WorldDropBoard(area){Box(Modifier.fillMaxWidth().height(if(compact)100.dp else 250.dp)){
                    Image(painterResource(R.drawable.math_theme_optimization),null,Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
                    FlowRow(Modifier.fillMaxWidth(.68f).align(Alignment.CenterStart).padding(start=25.dp,top=48.dp,bottom=27.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        selection.forEachIndexed{i,value->WorldTile("$value","Remove cargo ${i+1}",null,!solved,Modifier.width(35.dp)){selection=ArrayList(selection.filterIndexed{index,_->index!=i});status=""}}
                        if(selection.isEmpty())repeat(4){Text("＋",color=Color(0xFF9B877B),fontSize=22.sp,modifier=Modifier.padding(8.dp))}
                    }
                    Column(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.32f).padding(end=15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                        Metric("Value","${selection.sum()}");Text("${selection.sumOf{source.weights.getValue(it)}} / ${source.capacity} kg",color=Ink,fontSize=11.sp,fontWeight=FontWeight.Bold)
                    }
                }}
                WorldCard{
                    Text("Available Cargo · drag to add",color=Ink,fontWeight=FontWeight.Bold,fontSize=13.sp)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        source.pieces.forEachIndexed{index,value->val icon=listOf(R.drawable.math_asset_crate,R.drawable.math_asset_suitcase,R.drawable.math_asset_laptop,R.drawable.math_asset_camera,R.drawable.math_asset_gold_bars)[index%5]
                            WorldTile("$value\n${source.weights.getValue(value)}kg","Add $value / ${source.weights.getValue(value)}kg core",area,!solved && value !in selection,Modifier.width(if(compact)54.dp else 83.dp),icon){add(value)}}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    LightAction("Undo",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1f)){selection=ArrayList(selection.dropLast(1));status=""}
                    LightAction("Clear",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1f)){selection=arrayListOf();status=""}
                    WorldButton("Check build",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1.5f),onClick=::check)
                }
            }else LightWorkshopBoard(game,round,solved,onAttempt,onSolved)
        }
        WorkshopKind.Navigation->{
            val p=round.parameters
            val x=p[0]+selection.sumOf{round.moves[it].first};val y=p[1]+selection.sumOf{round.moves[it].second}
            WorldCard{
                Text("Coordinate Grid",color=accent,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
                WorldDropBoard(area){Canvas(Modifier.fillMaxWidth().height(if(compact)140.dp else 285.dp)){
                    val points=mutableListOf(p[0] to p[1]);selection.forEach{move->val last=points.last();points+=last.first+round.moves[move].first to last.second+round.moves[move].second}
                    val all=points+listOf(p[2] to p[3]);val ex=maxOf(4,all.maxOf{abs(it.first)}+1);val ey=maxOf(4,all.maxOf{abs(it.second)}+1)
                    val unit=min((size.width-28.dp.toPx())/(2*ex),(size.height-24.dp.toPx())/(2*ey))
                    fun point(a:Int,b:Int)=Offset(size.width/2+a*unit,size.height/2-b*unit)
                    val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(118,145,161);textSize=9.sp.toPx();textAlign=android.graphics.Paint.Align.CENTER}
                    val step=maxOf(1,ceil(maxOf(ex,ey)/6.0).toInt())
                    for(a in -ex..ex step step){val xx=point(a,0).x;drawLine(accent.copy(.08f),Offset(xx,0f),Offset(xx,size.height));if(a!=0)drawContext.canvas.nativeCanvas.drawText("$a",xx,size.height/2+12.dp.toPx(),paint)}
                    for(b in -ey..ey step step){val yy=point(0,b).y;drawLine(accent.copy(.08f),Offset(0f,yy),Offset(size.width,yy));if(b!=0)drawContext.canvas.nativeCanvas.drawText("$b",size.width/2-10.dp.toPx(),yy+3.dp.toPx(),paint)}
                    drawLine(Color(0xFF8CA3B1),Offset(0f,size.height/2),Offset(size.width,size.height/2),1.2.dp.toPx())
                    drawLine(Color(0xFF8CA3B1),Offset(size.width/2,0f),Offset(size.width/2,size.height),1.2.dp.toPx())
                    val target=point(p[2],p[3]);val ship=point(x,y)
                    drawLine(Color(0xFFC5D2DC),ship,target,2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx())))
                    points.zipWithNext().forEach{(a,b)->drawLine(accent,point(a.first,a.second),point(b.first,b.second),3.dp.toPx())}
                    drawCircle(Color(0xFFFFD56B).copy(.25f),12.dp.toPx(),target);drawCircle(Color(0xFFFFBD29),8.dp.toPx(),target)
                    paint.color=android.graphics.Color.WHITE;paint.textSize=13.sp.toPx();drawContext.canvas.nativeCanvas.drawText("★",target.x,target.y+4.dp.toPx(),paint)
                    drawCircle(accent.copy(.15f),12.dp.toPx(),ship);drawCircle(accent,8.dp.toPx(),ship)
                    drawContext.canvas.nativeCanvas.drawText("➤",ship.x,ship.y+4.dp.toPx(),paint)
                }}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Your ship ($x,$y)",color=accent,fontSize=11.sp);Text("Destination (${p[2]},${p[3]})",color=Color(0xFFBE8B0C),fontSize=11.sp)}
            }
            WorldCard{
                Text("Ship ($x,$y) • ${selection.size}/${p[4]} moves",color=Ink,fontSize=13.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    round.moves.forEachIndexed{i,move->val arrow=if(abs(move.first)>=abs(move.second)){if(move.first>=0)"→" else "←"}else if(move.second>=0)"↑" else "↓"
                        WorldTile("$arrow\n(${move.first},${move.second})","Move by (${move.first},${move.second})",area,!solved && selection.size<p[4],Modifier.weight(1f)){add(i)}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    LightAction("Undo move",enabled=!solved && selection.isNotEmpty(),modifier=Modifier.weight(1f)){selection=ArrayList(selection.dropLast(1));status=""}
                    WorldButton("Land ship",enabled=!solved,modifier=Modifier.weight(1f),onClick=::check)
                }
            }
        }
        else->error("Unsupported themed board ${round.kind}")
    }
    if(status.isNotEmpty())MathFeedbackCue(status,status=="Construction verified")
    if(checked && status.isNotEmpty())MathSolutionFeedback(round,game.id)
}

@Composable private fun Metric(label:String,value:String){Column{Text(label,color=Muted,fontSize=10.sp,lineHeight=13.sp);Text(value,color=Ink,fontSize=19.sp,lineHeight=23.sp,fontWeight=FontWeight.Bold)}}

@Composable private fun LiquidFlask(value:Int,total:Int,blue:Boolean,potion:Boolean,height:Int){
    val color=if(blue)Color(0xFF008EFF) else Color(0xFFF51B69)
    Box(Modifier.height(height.dp).aspectRatio(200f/380f)){
        Canvas(Modifier.matchParentSize()){
            scale(size.width/200f,size.height/380f,pivot=Offset.Zero){
                val vessel=Path().apply{if(potion){moveTo(77f,38f);lineTo(77f,171f);cubicTo(77f,187f,29f,233f,28f,283f);cubicTo(27f,336f,63f,355f,100f,355f);cubicTo(137f,355f,173f,336f,172f,283f);cubicTo(171f,233f,123f,187f,123f,171f);lineTo(123f,38f)}else{moveTo(62f,53f);lineTo(62f,76f);cubicTo(25f,82f,25f,100f,25f,116f);lineTo(25f,325f);cubicTo(25f,364f,175f,364f,175f,325f);lineTo(175f,116f);cubicTo(175f,90f,160f,80f,138f,76f);lineTo(138f,53f)};close()}
                clipPath(vessel){val fill=280f*value/total;drawRect(Brush.verticalGradient(listOf(color.copy(.35f),color)),Offset(0f,350f-fill),Size(200f,fill));drawOval(color.copy(.5f),Offset(15f,344f-fill),Size(170f,12f))}
            }
        }
        Image(painterResource(if(potion){if(blue)R.drawable.math_asset_flask else R.drawable.math_asset_flask_red}else{if(blue)R.drawable.math_asset_bottle else R.drawable.math_asset_bottle_red}),null,Modifier.matchParentSize())
    }
}

@Composable private fun PercentDiagram(round:WorkshopChallenge){
    val source=round.source?:return;val inputs=source.operands
    val legacy=round.templateId.isEmpty() || round.witness.isEmpty()
    val p=source.visuals["percent"] ?: if(legacy)inputs.getOrNull(0) ?: return else return
    val whole=source.visuals["whole"] ?: if(legacy && source.topic!="Reverse percentage")inputs.getOrNull(1)else null
    val part=source.visuals["knownPart"] ?: if(legacy && source.topic=="Reverse percentage")inputs.getOrNull(1)else null
    val accent=LocalMathBoardAccent.current
    WorldCard{
        Text("VISUALIZE THE PERCENTAGE",color=accent,fontSize=11.sp,lineHeight=14.sp,fontWeight=FontWeight.Bold)
        Text("${roundNumber(p)}% means ${roundNumber(p)} out of 100 parts.",color=Muted,fontSize=11.sp,lineHeight=14.sp)
        Box(Modifier.fillMaxWidth().height(18.dp).clip(CircleShape).background(Color(0xFFE0DFE7))){Box(Modifier.fillMaxWidth((p/100).toFloat().coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(accent.copy(.65f),accent))))}
        Text(if(part!=null)"Known part: ${roundNumber(part)}; find the whole." else "The whole: ${roundNumber(whole ?: return@WorldCard)}",color=Ink,fontSize=13.sp,lineHeight=16.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable private fun RescueDiagram(round:WorkshopChallenge){
    val accent=LocalMathBoardAccent.current
    val source=round.source?:return
    val operands=if(source.visuals.containsKey("length"))listOf(source.visuals.getValue("length"),source.visuals.getValue("width"))else source.operands
    if(source.topic!="Perimeter")return
    WorldCard{
        Box(Modifier.fillMaxWidth().height(if(LocalCompactTheme.current)82.dp else 150.dp).clip(RoundedCornerShape(14.dp))){
            Image(painterResource(R.drawable.math_theme_measure),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
            if(operands.size>=2)Canvas(Modifier.matchParentSize()){
                val left=size.width*.2f;val right=size.width*.8f;val top=size.height*.3f;val bottom=size.height*.85f
                drawRect(accent,Offset(left,top),Size(right-left,bottom-top),style=Stroke(2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,5f))))
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply{color=android.graphics.Color.rgb(200,90,0);textSize=12.sp.toPx();textAlign=android.graphics.Paint.Align.CENTER;isFakeBoldText=true}
                drawContext.canvas.nativeCanvas.drawText("${roundNumber(operands[0])} m",size.width/2,top-3.dp.toPx(),paint)
                drawContext.canvas.nativeCanvas.drawText("${roundNumber(operands[1])} m",right+20.dp.toPx(),size.height*.65f,paint)
            }
        }
    }
}
