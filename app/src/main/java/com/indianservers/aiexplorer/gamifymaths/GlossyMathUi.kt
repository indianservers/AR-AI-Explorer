package com.indianservers.aiexplorer.gamifymaths

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.R
import androidx.core.view.WindowCompat
import kotlin.math.cos
import kotlin.math.sin

internal val glossyMathGameIds = setOf("speed-basic", "speed-advanced", "calculus-climber", "chance", "combinatorics")
internal val LocalGlossyMath = staticCompositionLocalOf { false }
internal val LocalGlossyCompact = staticCompositionLocalOf { true }
private val Blue = Color(0xFF0879FF)
private val Ink = Color(0xFF07133F)
private val Muted = Color(0xFF647DA9)

internal fun Modifier.glossySurface(radius: Int = 20, tint: Color = Color(0xFFF1F7FF), raised: Boolean = true): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this.then(if (raised) Modifier.shadow(8.dp, shape, ambientColor = Color(0xFF7C9BC5), spotColor = Color(0xFF7C9BC5)) else Modifier)
        .clip(shape).background(Brush.linearGradient(listOf(Color.White, tint)))
        .border(1.dp, Color.White, shape)
}

/** Vector icons stay crisp at every phone density and follow the live control state. */
@Composable
internal fun GlossyIcon(kind: String, modifier: Modifier = Modifier, color: Color = Blue) {
    Canvas(modifier.size(24.dp)) {
        val s=size.minDimension; val c=Offset(size.width/2,size.height/2); val w=s*.085f
        fun line(a:Offset,b:Offset)=drawLine(color,a,b,w,StrokeCap.Round)
        fun p(x:Float,y:Float)=Offset(c.x+(x-.5f)*s,c.y+(y-.5f)*s)
        when(kind) {
            "back" -> {line(p(.62f,.18f),p(.32f,.5f));line(p(.32f,.5f),p(.62f,.82f))}
            "settings" -> {
                repeat(8){val a=it*Math.PI/4;line(c+Offset(cos(a).toFloat(),sin(a).toFloat())*s*.32f,c+Offset(cos(a).toFloat(),sin(a).toFloat())*s*.43f)}
                drawCircle(color,s*.29f,c,style=Stroke(w*1.4f));drawCircle(color,s*.1f,c)
            }
            "time" -> {drawCircle(color,s*.37f,c,style=Stroke(w));line(p(.5f,.27f),p(.5f,.51f));line(p(.5f,.51f),p(.66f,.58f))}
            "best" -> repeat(3){i->drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF69BDFF),color)),p(.15f+i*.25f,.63f-i*.2f),androidx.compose.ui.geometry.Size(s*.17f,s*(.27f+i*.2f)),androidx.compose.ui.geometry.CornerRadius(s*.04f))}
            "score" -> {
                val cup=Path().apply{moveTo(s*.27f,s*.14f);lineTo(s*.73f,s*.14f);lineTo(s*.68f,s*.51f);quadraticTo(s*.5f,s*.78f,s*.32f,s*.51f);close()}
                drawPath(cup,Brush.linearGradient(listOf(Color(0xFFFFED79),Color(0xFFFFAA0A))))
                drawArc(Color(0xFFFFB514),60f,240f,false,p(.1f,.2f),androidx.compose.ui.geometry.Size(s*.3f,s*.4f),style=Stroke(w))
                drawArc(Color(0xFFFFB514),-120f,240f,false,p(.6f,.2f),androidx.compose.ui.geometry.Size(s*.3f,s*.4f),style=Stroke(w))
                drawLine(Color(0xFFD38C13),p(.5f,.61f),p(.5f,.8f),w);drawRoundRect(Color(0xFFF4B323),p(.3f,.8f),androidx.compose.ui.geometry.Size(s*.4f,s*.1f))
            }
            "streak" -> {
                val flame=Path().apply{moveTo(s*.55f,s*.03f);cubicTo(s*.9f,s*.34f,s*.89f,s*.7f,s*.68f,s*.87f);cubicTo(s*.3f,s*1.07f,s*.03f,s*.67f,s*.28f,s*.35f);lineTo(s*.34f,s*.55f);quadraticTo(s*.57f,s*.32f,s*.55f,s*.03f);close()}
                drawPath(flame,Brush.verticalGradient(listOf(Color(0xFFFF6736),Color(0xFFFFB618))))
                drawOval(Color(0xFFFFEA68),p(.39f,.53f),androidx.compose.ui.geometry.Size(s*.25f,s*.35f))
            }
            "run" -> drawPath(Path().apply{moveTo(s*.28f,s*.1f);lineTo(s*.85f,s*.5f);lineTo(s*.28f,s*.9f);close()},color)
            "undo" -> {drawArc(color,-95f,280f,false,p(.25f,.3f),androidx.compose.ui.geometry.Size(s*.58f,s*.56f),style=Stroke(w,cap=StrokeCap.Round));line(p(.22f,.32f),p(.43f,.13f));line(p(.22f,.32f),p(.44f,.46f))}
            "check" -> {line(p(.15f,.5f),p(.4f,.75f));line(p(.4f,.75f),p(.87f,.23f))}
            "keyboard" -> {drawRoundRect(color,p(.05f,.18f),androidx.compose.ui.geometry.Size(s*.9f,s*.64f),androidx.compose.ui.geometry.CornerRadius(s*.06f),style=Stroke(w));repeat(3){y->repeat(5){x->drawCircle(color,s*.028f,p(.22f+x*.14f,.34f+y*.14f))}}}
            "info" -> {drawCircle(color,s*.39f,c,style=Stroke(w));line(p(.5f,.46f),p(.5f,.7f));drawCircle(color,s*.04f,p(.5f,.3f))}
            "cap" -> {drawPath(Path().apply{moveTo(s*.03f,s*.33f);lineTo(s*.5f,s*.08f);lineTo(s*.97f,s*.33f);lineTo(s*.5f,s*.6f);close()},Brush.verticalGradient(listOf(Color(0xFF65C9FF),Blue)));drawRoundRect(Blue,p(.25f,.53f),androidx.compose.ui.geometry.Size(s*.5f,s*.24f),androidx.compose.ui.geometry.CornerRadius(s*.08f));line(p(.9f,.37f),p(.9f,.75f))}
            "bolt" -> drawPath(Path().apply{moveTo(s*.62f,0f);lineTo(s*.17f,s*.56f);lineTo(s*.48f,s*.56f);lineTo(s*.34f,s);lineTo(s*.85f,s*.38f);lineTo(s*.56f,s*.38f);close()},Brush.verticalGradient(listOf(Color(0xFF55DAFF),Blue)))
            else -> {drawCircle(color,s*.36f,c,style=Stroke(w));line(p(.5f,.3f),p(.5f,.7f));line(p(.3f,.5f),p(.7f,.5f))}
        }
    }
}

@Composable
private fun GlossyCircle(kind: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(if(LocalGlossyCompact.current)38.dp else 43.dp).glossySurface(50).clickable(onClick=onClick).semantics{contentDescription=description},contentAlignment=Alignment.Center){
        GlossyIcon(kind,Modifier.size(22.dp),Ink)
    }
}

@Composable
internal fun GlossyMathFrame(game: MathsGame, level: Int?, time: String, score: Int, streak: Int,
    progress: Float, topic: String, prompt: String, onBack: () -> Unit, onSettings: () -> Unit,
    onHint: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val view=LocalView.current
    DisposableEffect(view) {
        var context=view.context
        while(context is ContextWrapper && context !is Activity) context=context.baseContext
        val controller=(context as? Activity)?.window?.let{WindowCompat.getInsetsController(it,view)}
        val previousStatus=controller?.isAppearanceLightStatusBars
        val previousNavigation=controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars=true
        controller?.isAppearanceLightNavigationBars=true
        onDispose {
            if(previousStatus!=null)controller.isAppearanceLightStatusBars=previousStatus
            if(previousNavigation!=null)controller.isAppearanceLightNavigationBars=previousNavigation
        }
    }
    MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Color(0xFFF8FBFF),surface=Color.White,onSurface=Ink)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFEDF5FF),Color.White,Color(0xFFF7FBFF))))) {
            val compact=maxHeight<740.dp
            CompositionLocalProvider(LocalGlossyMath provides true,LocalGlossyCompact provides compact,LocalCompactWorldControls provides compact) {
                Canvas(Modifier.fillMaxSize()) {
                    val wave=Path().apply{moveTo(0f,size.height*.94f);cubicTo(size.width*.35f,size.height*.87f,size.width*.5f,size.height*1.04f,size.width,size.height*.91f);lineTo(size.width,size.height);lineTo(0f,size.height);close()}
                    drawPath(wave,Color(0xFFDEEDFF).copy(.5f));drawCircle(Color(0xFFE8F2FF),size.width*.28f,Offset(size.width*.95f,0f))
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal=14.dp,vertical=if(compact)5.dp else 8.dp),
                    verticalArrangement=Arrangement.spacedBy(if(compact)3.dp else 16.dp)) {
                    Box {
                        if(game.id=="calculus-climber") Image(painterResource(R.drawable.math_glossy_mountain),null,Modifier.align(Alignment.CenterEnd).size(140.dp,75.dp),alpha=.65f)
                        Row(Modifier.fillMaxWidth().heightIn(min=46.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                            GlossyCircle("back",if(level==null)"Exit challenge" else "Back",onBack)
                            if(level!=null) Box(Modifier.size(39.dp).shadow(5.dp,CircleShape,spotColor=Blue).background(Brush.linearGradient(listOf(Color(0xFF28BCFF),Blue,Color(0xFF0055FA))),CircleShape).border(1.dp,Color(0xFFADE7FF),CircleShape),contentAlignment=Alignment.Center){Text("$level",color=Color.White,fontSize=17.sp,fontWeight=FontWeight.Bold)}
                            else if(game.id=="speed-basic") GlossyIcon("bolt",Modifier.size(26.dp))
                            Column(Modifier.weight(1f)) {
                                Text(game.title,color=Ink,fontSize=if(compact)if(game.title.length>20)14.sp else 17.sp else 21.sp,lineHeight=if(compact)20.sp else 25.sp,fontWeight=FontWeight.Bold)
                                if(level!=null) Text("Level $level · Best ${LocalPreviousGameBest.current}",color=Muted,fontSize=12.sp)
                            }
                            GlossyCircle("settings",if(level==null)"Open challenge settings" else "Game session settings",onSettings)
                        }
                    }
                    Row(Modifier.fillMaxWidth().glossySurface(20).padding(horizontal=7.dp,vertical=if(compact)7.dp else 18.dp),verticalAlignment=Alignment.CenterVertically) {
                        listOf(Triple("time","Time",time),Triple("score","Score","$score"),Triple("streak","Streak","$streak"),Triple("best","Best","${LocalPreviousGameBest.current}")).forEachIndexed{i,(icon,label,value)->
                            Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
                                GlossyIcon(icon,Modifier.size(if(compact)21.dp else 28.dp));Spacer(Modifier.width(5.dp))
                                Column{Text(label,color=Muted,fontSize=10.sp);Text(value,color=Ink,fontSize=if(compact)16.sp else 19.sp,fontWeight=FontWeight.Bold)}
                            }
                            if(i<3) Box(Modifier.width(1.dp).height(32.dp).background(Color(0xFFD8E5F6)))
                        }
                    }
                    if(level!=null) Text(if(level>=121)"ADVANCED MASTERY ${level-120}" else "Stage ${(level-1)/30+1}/4",color=Muted,fontSize=11.sp,letterSpacing=if(level>=121)1.sp else 0.sp)
                    Box(Modifier.fillMaxWidth().height(8.dp).shadow(3.dp,CircleShape).clip(CircleShape).background(Color(0xFFDAE5F3)).border(1.dp,Color.White,CircleShape)){
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(if(level==null)listOf(Blue,Color(0xFF853BFF),Color(0xFFFF49B2),Color(0xFFFFD13B))else listOf(Color(0xFF09BFFF),Blue,Color(0xFF61ACFF)))) )
                    }
                    Box(Modifier.fillMaxWidth().glossySurface(22).padding(if(compact)8.dp else 17.dp)) {
                        val art=when(game.id){"calculus-climber"->if(level==122)R.drawable.math_glossy_integral else R.drawable.math_glossy_slope;"chance"->if(level==122)R.drawable.math_glossy_dice else R.drawable.math_glossy_bag;"combinatorics"->R.drawable.math_glossy_robot_0;"speed-basic","speed-advanced"->gameCardArtwork(game.id);else->null}
                        if(art!=null && !compact) Image(painterResource(art),null,Modifier.align(Alignment.BottomEnd).size(if(compact)85.dp else 120.dp),contentScale=ContentScale.Fit,alpha=.9f)
                        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFDFEFFF),Color(0xFFEFF8FF)))).padding(7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
                                    GlossyIcon(if(level==null || level>=121)"cap" else if(game.id=="chance")"info" else "bolt",Modifier.size(22.dp))
                                    Text(topic,color=Blue,fontSize=12.sp,fontWeight=FontWeight.Bold)
                                }
                                if(art!=null && compact) Image(painterResource(art),null,Modifier.size(44.dp,30.dp),contentScale=ContentScale.Fit)
                                if(onHint!=null) Box(Modifier.size(30.dp).clickable(onClick=onHint).semantics{contentDescription="Show hint"},contentAlignment=Alignment.Center){GlossyIcon("info")}
                            }
                            MathQuestionText(prompt,gameId=game.id,color=Ink,fontSize=if(level==null)27.sp else if(compact)14.sp else 18.sp,lineHeight=if(level==null)34.sp else if(compact)19.sp else 24.sp,fontWeight=FontWeight.SemiBold,
                                modifier=Modifier.padding(end=if(art!=null)if(compact)0.dp else 95.dp else 0.dp))
                        }
                    }
                    content()
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    }
}

internal val glossyRobots=listOf(R.drawable.math_glossy_robot_0,R.drawable.math_glossy_robot_1,R.drawable.math_glossy_robot_2,R.drawable.math_glossy_robot_3,R.drawable.math_glossy_robot_4)
