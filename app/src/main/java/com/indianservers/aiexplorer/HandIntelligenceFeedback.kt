package com.indianservers.aiexplorer
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.indianservers.aiexplorer.handintelligence.*

/** The animation clock invalidates only this drawing, never the parent screen or raw landmark state. */
@Composable
fun HandIntelligenceFeedback(modifier:Modifier,session:HandIntelligenceSession,developer:Boolean,skeleton:Boolean,ray:Boolean) {
    val tick=remember { mutableLongStateOf(0L) }
    LaunchedEffect(session) { while(true) androidx.compose.animation.core.withInfiniteAnimationFrameNanos { tick.longValue=it } }
    Canvas(modifier) {
        tick.longValue
        val frame=session.latest ?: return@Canvas; val state=frame.intelligence
        val h=state.hands.firstOrNull { it.id==state.primaryHandId } ?: return@Canvas
        val p=if(state.primaryIntent==HandIntent.INSPECT || state.primaryIntent==HandIntent.POINT) h.pose.indexTip else h.filteredPinch
        val cursor=Offset((p.x*size.width).toFloat(),(p.y*size.height).toFloat())
        val color=if(state.targetLocked) Color(0xFF68EB9B) else Color(0xFF49DDEE)
        drawCircle(color,if(state.targetLocked) 14f else 10f,cursor,style=Stroke(2f))
        val target=state.target
        val marker=target?.let { session.projection?.project(it.position) }
        if(marker!=null) { val point=Offset((marker.x*size.width).toFloat(),(marker.y*size.height).toFloat()); drawCircle(color.copy(alpha=.18f),24f,point); if(state.precisionMode || state.phase==InteractionPhase.INSPECT) { drawLine(color,point-Offset(16f,0f),point+Offset(16f,0f),1f); drawLine(color,point-Offset(0f,16f),point+Offset(0f,16f),1f) } }
        val action=frame.action
        if(action?.interactions?.contains(MathInteraction.ROTATE)==true) drawArc(color.copy(alpha=.5f),-90f,110f,false,cursor-Offset(25f,25f),androidx.compose.ui.geometry.Size(50f,50f),style=Stroke(1f))
        if(action?.interactions?.any { it==MathInteraction.SCALE || it==MathInteraction.CHANGE_RADIUS || it.name.startsWith("STRETCH_") }==true) drawLine(color.copy(alpha=.5f),cursor-Offset(30f,0f),cursor+Offset(30f,0f),1f)
        if(action?.transform?.snapped==true) drawCircle(Color(0xFFFFBD46),4f,cursor+Offset(16f,0f))
        if(developer && (skeleton || ray)) session.latestTracking?.hands?.forEach { hand ->
            fun at(i:Int)=hand.landmarks[i].let { Offset(it.x*size.width,it.y*size.height) }
            if(hand.landmarks.size==21) {
                if(ray) drawLine(color.copy(alpha=.4f),at(5),at(8),2f)
                if(skeleton) listOf(listOf(0,1,2,3,4),listOf(0,5,6,7,8),listOf(5,9,10,11,12),listOf(9,13,14,15,16),listOf(13,17,18,19,20),listOf(17,0)).forEach { chain -> chain.zipWithNext().forEach { (a,b) -> drawLine(color.copy(alpha=.5f),at(a),at(b),1f) } }
            }
        }
    }
}
