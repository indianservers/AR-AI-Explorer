package com.indianservers.aiexplorer.handintelligence.features
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.arengine.interaction.ArHandFrame
import kotlin.math.*

class HandFeatureEngine {
    fun process(frame:ArHandFrame):List<HandPoseFeatures> = frame.hands.take(2).mapNotNull { hand ->
        if(hand.landmarks.size!=21 || hand.landmarks.any { !it.x.isFinite() || !it.y.isFinite() }) return@mapNotNull null
        val p=hand.landmarks.map { Vec3(it.x.toDouble(),it.y.toDouble(),0.0) }
        val palmSize=(p[9]-p[0]).magnitude(); if(palmSize<.008) return@mapNotNull null
        val palm=(p[0]+p[5]+p[9]+p[13]+p[17])*.2
        fun curl(mcp:Int,pip:Int,tip:Int):Float { val a=(p[pip]-p[mcp]).unit(); val b=(p[tip]-p[pip]).unit(); val reach=(p[tip]-p[0]).magnitude()/(p[pip]-p[0]).magnitude().coerceAtLeast(.001); return clamp((1-a.dot(b))*.45+(1.15-reach).coerceAtLeast(0.0)) }
        val curls=FingerCurlState(curl(1,2,4),curl(5,6,8),curl(9,10,12),curl(13,14,16),curl(17,18,20))
        val ratio=(p[4]-p[8]).magnitude()/palmSize
        val openness=1-(curls.index+curls.middle+curls.ring+curls.pinky)*.25f
        val closed=(curls.index+curls.middle+curls.ring+curls.pinky)*.25f
        val point=(1-curls.index)*(curls.middle+curls.ring+curls.pinky)/3
        val world=hand.worldLandmarks
        val normal=if(world.size==21) (world[5]-world[0]).cross(world[17]-world[0]).unit() else (p[5]-p[0]).cross(p[17]-p[0]).unit()
        val framing=p.count { it.x in .015.. .985 && it.y in .015.. .985 }/21.0
        // Handedness score alone is not landmark reliability; geometry and framing also contribute.
        val quality=clamp(hand.confidence*.5+framing*.3+(palmSize/.07).coerceIn(0.0,1.0)*.2)
        HandPoseFeatures(hand.id,when(hand.handedness.lowercase()) { "left" -> Handedness.LEFT; "right" -> Handedness.RIGHT; else -> Handedness.UNKNOWN },p[0],palm,p[8],p[4],p[12],(p[4]+p[8])*.5,normal,Math.toDegrees(atan2(p[17].y-p[5].y,p[17].x-p[5].x)),palmSize,ratio,clamp((.55-ratio)/.35),curls,openness,closed,point,quality)
    }
}
