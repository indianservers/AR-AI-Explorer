package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One brief visual beat. Compose animations honour the system animation-duration scale. */
@Composable internal fun MathFeedbackCue(message:String,verified:Boolean){
    var entered by remember(message){mutableStateOf(false)}
    LaunchedEffect(message){entered=true}
    val reveal by animateFloatAsState(if(entered)1f else 0f,tween(420),label="Mathematical feedback")
    val preview=message.startsWith("Your method produces") || message.startsWith("Result:")
    val ink=if(verified)Color(0xFF137451)else if(preview)Color(0xFF175A9E)else Color(0xFF895009)
    Row(Modifier.fillMaxWidth().background(if(verified)Color(0xFFE8F8EF)else if(preview)Color(0xFFEBF4FF)else Color(0xFFFFF5E3),RoundedCornerShape(12.dp))
        .padding(10.dp).semantics{liveRegion=LiveRegionMode.Polite},verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(9.dp)){
        Box(Modifier.size(32.dp),contentAlignment=Alignment.Center){
            Canvas(Modifier.fillMaxSize()){drawArc(ink.copy(.5f),-90f,360f*reveal,false,style=Stroke(2.dp.toPx()))}
            Text(if(verified)"✓"else if(preview)"="else "↻",color=ink,fontSize=22.sp,fontWeight=FontWeight.Bold,
                modifier=Modifier.graphicsLayer{scaleX=.8f+.2f*reveal;scaleY=scaleX})
        }
        Column(Modifier.weight(1f)){
            Text(if(verified)"Method verified"else if(preview)"Calculation preview"else "Review this step",color=ink,fontSize=12.sp,fontWeight=FontWeight.Bold)
            Text(message,color=ink,fontSize=11.sp)
        }
    }
}
