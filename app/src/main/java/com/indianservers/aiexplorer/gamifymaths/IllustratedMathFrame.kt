package com.indianservers.aiexplorer.gamifymaths

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.indianservers.aiexplorer.R

internal val LocalCompactWorldControls=staticCompositionLocalOf{false}
internal data class LearningWorldStyle(val accent:Color,val artwork:Int)
internal fun learningWorldStyle(id:String):LearningWorldStyle=when(id){
    "forge"->LearningWorldStyle(Color(0xFF8553D5),R.drawable.math_world_forge)
    "kitchen"->LearningWorldStyle(Color(0xFFDB791D),R.drawable.math_world_kitchen)
    "fractions"->LearningWorldStyle(Color(0xFF0795AD),R.drawable.math_world_fractions)
    "balance"->LearningWorldStyle(Color(0xFF258BE3),R.drawable.math_world_balance)
    "shapes"->LearningWorldStyle(Color(0xFF9156CA),R.drawable.math_world_shapes)
    "data"->LearningWorldStyle(Color(0xFF317DDD),R.drawable.math_world_data)
    "logic"->LearningWorldStyle(Color(0xFF7653CC),R.drawable.math_world_logic)
    "number-bonds"->LearningWorldStyle(Color(0xFF16976D),R.drawable.math_world_number_bonds)
    "decimal-harbor"->LearningWorldStyle(Color(0xFF0E9D9D),R.drawable.math_world_decimal_harbor)
    "math-market"->LearningWorldStyle(Color(0xFFBF7717),R.drawable.math_world_math_market)
    "integer-expedition"->LearningWorldStyle(Color(0xFF3E8BD5),R.drawable.math_world_integer_expedition)
    "function-machine"->LearningWorldStyle(Color(0xFF5573D5),R.drawable.math_world_function_machine)
    "geometry-proof"->LearningWorldStyle(Color(0xFFAE6830),R.drawable.math_world_geometry_proof)
    "data-story"->LearningWorldStyle(Color(0xFF158CA8),R.drawable.math_world_data_story)
    "matrix-mission"->LearningWorldStyle(Color(0xFF5274CF),R.drawable.math_world_matrix_mission)
    else->error("Missing illustrated learning world: $id")
}

@Composable internal fun LightMathSystemBars(){
    val view=LocalView.current
    DisposableEffect(view){
        var context=view.context
        while(context is ContextWrapper && context !is Activity)context=context.baseContext
        val controller=(context as? Activity)?.window?.let{WindowCompat.getInsetsController(it,view)}
        val status=controller?.isAppearanceLightStatusBars;val navigation=controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars=true;controller?.isAppearanceLightNavigationBars=true
        onDispose{if(status!=null)controller.isAppearanceLightStatusBars=status;if(navigation!=null)controller.isAppearanceLightNavigationBars=navigation}
    }
}

@Composable private fun WorldNavigationIcon(kind:String,description:String,accent:Color,onClick:()->Unit){
    Box(Modifier.size(40.dp).glossySurface(50,lerp(Color.White,accent,.08f)).clickable(onClick=onClick)
        .semantics{contentDescription=description},contentAlignment=Alignment.Center){GlossyIcon(kind,Modifier.size(22.dp),accent)}
}

@Composable internal fun IllustratedMathFrame(game:MathsGame,level:Int?,time:String,score:Int,streak:Int,progress:Float,
    topic:String,prompt:String,onBack:()->Unit,onSettings:()->Unit,onHint:(()->Unit)?,content:@Composable ColumnScope.()->Unit){
    val style=learningWorldStyle(game.id);val accent=style.accent;val ink=Color(0xFF111D40);val muted=Color(0xFF647B97)
    LightMathSystemBars()
    MaterialTheme(colorScheme=lightColorScheme(primary=accent,onSurface=ink,surface=Color.White,background=Color(0xFFF8FBFF))){
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White,lerp(Color.White,accent,.1f))))){
            val compact=maxHeight<740.dp
            CompositionLocalProvider(LocalMathBoardAccent provides accent,LocalGlossyMath provides true,LocalGlossyCompact provides compact,LocalCompactWorldControls provides compact){
                Canvas(Modifier.fillMaxSize()){
                    drawCircle(accent.copy(.045f),size.width*.4f,Offset(size.width,0f))
                    val wave=Path().apply{moveTo(0f,size.height*.94f);cubicTo(size.width*.25f,size.height*.85f,size.width*.6f,size.height*1.03f,size.width,size.height*.9f);lineTo(size.width,size.height);lineTo(0f,size.height);close()}
                    drawPath(wave,accent.copy(.08f))
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(if(compact)8.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 13.dp)){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        WorldNavigationIcon("back","Back",accent,onBack)
                        Box(Modifier.size(32.dp).shadow(4.dp,CircleShape,spotColor=accent).background(Brush.linearGradient(listOf(lerp(accent,Color.White,.25f),accent)),CircleShape).border(1.dp,Color.White,CircleShape),contentAlignment=Alignment.Center){Text("${level?:1}",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Bold)}
                        Column(Modifier.weight(1f)){
                            Text(game.title,color=ink,fontSize=if(compact)16.sp else 21.sp,lineHeight=if(compact)19.sp else 25.sp,fontWeight=FontWeight.Bold)
                            Text("Level ${level?:1} · Best ${LocalPreviousGameBest.current}",color=accent,fontSize=11.sp)
                        }
                        WorldNavigationIcon("settings","Game session settings",accent,onSettings)
                    }
                    Row(Modifier.fillMaxWidth().glossySurface(18,lerp(Color.White,accent,.055f)).padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){
                        listOf(Triple("time","Time",time),Triple("score","Score","$score"),Triple("streak","Streak","$streak"),Triple("best","Best","${LocalPreviousGameBest.current}")).forEachIndexed{i,(icon,label,value)->
                            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){GlossyIcon(icon,Modifier.size(19.dp),accent);Spacer(Modifier.width(4.dp));Column{Text(label,color=muted,fontSize=9.sp);Text(value,color=ink,fontSize=13.sp,fontWeight=FontWeight.Bold)}}
                            if(i<3)Box(Modifier.width(1.dp).height(28.dp).background(accent.copy(.12f)))
                        }
                    }
                    Text(if((level?:1)>=121)"ADVANCED MASTERY ${(level?:121)-120}" else "Stage ${((level?:1)-1)/30+1}/4",color=muted,fontSize=10.sp)
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(accent.copy(.12f))){Box(Modifier.fillMaxWidth(progress.coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(lerp(accent,Color.White,.3f),accent))))}
                    Box(Modifier.fillMaxWidth().glossySurface(20,lerp(Color.White,accent,.045f)).heightIn(min=if(compact)0.dp else 112.dp)){
                        if(!compact)Image(painterResource(style.artwork),null,Modifier.align(Alignment.CenterEnd).size(135.dp,105.dp),contentScale=ContentScale.Fit)
                        Column(Modifier.padding(9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                Text(topic,color=accent,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(accent.copy(.08f)).padding(horizontal=8.dp,vertical=4.dp))
                                if(compact)Image(painterResource(style.artwork),null,Modifier.size(37.dp,26.dp))
                                if(onHint!=null)Box(Modifier.size(28.dp).clickable(onClick=onHint).semantics{contentDescription="Show hint"},contentAlignment=Alignment.Center){GlossyIcon("info",Modifier.size(21.dp),accent)}
                            }
                            MathQuestionText(prompt,gameId=game.id,color=ink,fontSize=if(compact)14.sp else 17.sp,lineHeight=if(compact)18.sp else 23.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(end=if(compact)0.dp else 108.dp))
                        }
                    }
                    Column(Modifier.fillMaxWidth().glossySurface(20,lerp(Color.White,accent,.025f)).padding(if(compact)4.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
                }
            }
        }
    }
}
