package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keep every question rule visible while making detailed controls available on demand. */
@Composable internal fun MathQuestionText(prompt:String,color:Color,fontSize:TextUnit,lineHeight:TextUnit,
    fontWeight:FontWeight,modifier:Modifier=Modifier,fontFamily:FontFamily?=null,gameId:String?=null) {
    val split=prompt.lastIndexOf('\n')
    val objective=if(split<0)prompt else prompt.substring(0,split)
    val instructions=if(split<0)"" else prompt.substring(split+1)
    var help by remember(prompt) { mutableStateOf(false) }
    Column(modifier) {
        Crossfade(targetState=objective,animationSpec=tween(180),label="Next mathematical mission"){question->
            Text(coloredMathText(question),color=color,fontSize=fontSize,lineHeight=lineHeight,fontWeight=fontWeight,fontFamily=fontFamily)
        }
        if(instructions.isNotBlank() || gameId!=null)Text(if(gameId==null)"How to play ⓘ"else "How to play · Strategy ⓘ",color=MaterialTheme.colorScheme.primary,fontSize=11.sp,
            modifier=Modifier.clickable{help=true})
    }
    if(help && gameId!=null)MathStrategyDialog(gameId,objective,instructions){help=false}
    else if(help)AlertDialog(onDismissRequest={help=false},title={Text("How to play")},text={Text(instructions)},
        confirmButton={TextButton(onClick={help=false}){Text("Close")}})
}
