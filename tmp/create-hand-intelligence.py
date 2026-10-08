from pathlib import Path
base=Path('arengine/src/main/java/com/indianservers/aiexplorer/handintelligence')
files={}
files['Models.kt']=r'''package com.indianservers.aiexplorer.handintelligence

import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.interaction.ArHandFrame
import kotlin.math.*

typealias Vec3 = ArVector3
fun Vec3.unit() = if (magnitude()>1e-9) this*(1.0/magnitude()) else Vec3.Zero
fun Vec3.cross(v:Vec3)=Vec3(y*v.z-z*v.y,z*v.x-x*v.z,x*v.y-y*v.x)
fun clamp(v:Double)=v.coerceIn(0.0,1.0).toFloat()
fun angleDelta(a:Double,b:Double)=((a-b+540.0)%360.0)-180.0

enum class HandIntent { IDLE, EXPLORE, POINT, TOUCH, PRECISION_GRAB, GRAB, MOVE, ROTATE, RESIZE, STRETCH, PUSH, PULL, INSPECT, RELEASE, UI_INTERACTION, UNKNOWN }
enum class InteractionState { IDLE, APPROACH, HOVER, CONTACT, GRAB_READY, GRABBED, MOVING, ROTATING, SCALING, STRETCHING, INSPECTING, TWO_HAND_READY, TWO_HAND_GRABBED, RELEASING }
enum class IntelligenceProfile { PRECISION, BALANCED, PLAY }
enum class OneHandMode { PRECISION, DIRECT }
enum class TwoHandMode { READY, COMBINED }
enum class Handedness { LEFT, RIGHT, UNKNOWN }
enum class InteractionPhase { IDLE, BEGIN, UPDATE, END, CANCEL, INSPECT }
enum class RegionKind { BODY, VERTEX, EDGE, FACE, SURFACE, CENTER, RADIUS, VECTOR_ORIGIN, VECTOR_HEAD, CURVE, GRAPH_POINT, AXIS }
enum class MathSemanticType { POINT, LINE, RAY, VECTOR, PLANE, TRIANGLE, POLYGON, CIRCLE, SPHERE, CUBE, CUBOID, CONE, CYLINDER, GRAPH_2D, GRAPH_3D, FUNCTION_SURFACE, PARAMETRIC_CURVE, AXIS, CONTROL_POINT, ANGLE, TRANSFORMATION_GIZMO, GENERIC_GEOMETRY }
enum class MathInteraction { TRANSLATE, ROTATE, SCALE, STRETCH_X, STRETCH_Y, STRETCH_Z, MOVE_VERTEX, MOVE_EDGE, MOVE_FACE, CHANGE_RADIUS, CHANGE_ANGLE, EDIT_VECTOR_ORIGIN, EDIT_VECTOR_HEAD, EDIT_GRAPH_POINT, SAMPLE_SURFACE, INSPECT }
data class TransformIntentWeights(val translation:Float=0f,val rotation:Float=0f,val scale:Float=0f,val stretchX:Float=0f,val stretchY:Float=0f,val stretchZ:Float=0f)
data class FingerCurlState(val thumb:Float,val index:Float,val middle:Float,val ring:Float,val pinky:Float)
data class HandPoseFeatures(val id:String,val handedness:Handedness,val wrist:Vec3,val palm:Vec3,val indexTip:Vec3,val thumbTip:Vec3,val middleTip:Vec3,val pinch:Vec3,val palmNormal:Vec3,val angle:Double,val handScale:Double,val pinchDistance:Double,val pinchStrength:Float,val curls:FingerCurlState,val openness:Float,val grabStrength:Float,val pointScore:Float,val quality:Float)
data class TemporalFeatures(val velocity:Vec3=Vec3.Zero,val acceleration:Vec3=Vec3.Zero,val jerk:Vec3=Vec3.Zero,val angularVelocity:Float=0f,val trajectoryCurvature:Float=0f,val pinchVelocity:Float=0f,val stabilityScore:Float=0f,val motionConsistency:Float=0f,val dwellMillis:Long=0,val sampleCount:Int=0)
data class IntelligentHand(val pose:HandPoseFeatures,val motion:TemporalFeatures,val filteredPinch:Vec3) { val id get()=pose.id }
data class SemanticHitRegion(val id:String,val kind:RegionKind,val index:Int?=null,val axis:Int?=null,val sign:Double=1.0,val position:Vec3,val normal:Vec3=Vec3.Zero,val precision:Float=0f)
interface MathObjectAffordance { val objectId:String; val semanticType:MathSemanticType; fun supportedInteractions():Set<MathInteraction>; fun hitRegions():List<SemanticHitRegion>; fun precisionRequirement():Float }
data class MathObjectSnapshot(override val objectId:String,override val semanticType:MathSemanticType,val vertices:List<Vec3>,val triangles:IntArray=intArrayOf(),val edges:List<Pair<Int,Int>> = emptyList(),val regions:List<SemanticHitRegion> = emptyList(),val visible:Boolean=true,val locked:Boolean=false,val selected:Boolean=false,val dimensions:Vec3=Vec3(1.0,1.0,1.0),val radius:Double=.5,val allowed:Set<MathInteraction> = MathInteraction.entries.toSet()):MathObjectAffordance {
    val center=if(vertices.isEmpty()) Vec3.Zero else vertices.reduce(Vec3::plus)*(1.0/vertices.size)
    val size=vertices.maxOfOrNull { (it-center).magnitude() }?.coerceAtLeast(.01) ?: .01
    override fun supportedInteractions()=allowed
    override fun hitRegions()=regions
    override fun precisionRequirement()=if(semanticType in setOf(MathSemanticType.POINT,MathSemanticType.CONTROL_POINT,MathSemanticType.VECTOR)) .9f else .2f
}
data class UiRegion(val left:Double,val top:Double,val right:Double,val bottom:Double) { fun contains(p:Vec3)=p.x in left..right && p.y in top..bottom }
interface CoordinateMapper {
    fun project(point:Vec3):Vec3?
    /** Intersect a screen ray with the camera-facing plane through the retained grab point. */
    fun atDepth(normalized:Vec3,reference:Vec3):Vec3?
    fun screenToRay(normalized:Vec3):Ray3
}
data class Ray3(val origin:Vec3,val direction:Vec3)
data class MathSceneSnapshot(val objects:List<MathObjectSnapshot>,val mapper:CoordinateMapper,val ready:Boolean=true,val uiRegions:List<UiRegion> = emptyList(),val revision:Long=0,val mode:String="camera")
data class TargetCandidate(val objectId:String,val region:SemanticHitRegion,val position:Vec3,val rayAlignmentScore:Float,val proximityScore:Float,val hoverScore:Float,val depthScore:Float,val focusScore:Float,val affordanceScore:Float,val trajectoryScore:Float,val finalScore:Float)
data class SpatialTarget(val objectId:String,val region:SemanticHitRegion,val position:Vec3,val confidence:Float)
data class HandIntelligenceState(val timestampNanos:Long,val hands:List<IntelligentHand>,val primaryIntent:HandIntent=HandIntent.IDLE,val intentConfidence:Float=0f,val targetConfidence:Float=0f,val interactionConfidence:Float=0f,val targetObjectId:String?=null,val targetRegionId:String?=null,val oneHandMode:OneHandMode?=null,val twoHandMode:TwoHandMode?=null,val transformWeights:TransformIntentWeights=TransformIntentWeights(),val predictedNextIntent:HandIntent?=null,val uncertainty:Float=1f,val stable:Boolean=false,val precisionMode:Boolean=false,val targetLocked:Boolean=false,val trackingQuality:Float=0f,val state:InteractionState=InteractionState.IDLE,val target:SpatialTarget?=null,val primaryHandId:String?=null,val phase:InteractionPhase=InteractionPhase.IDLE)
data class SpatialAnchor(val position:Vec3,val confidence:Float)
data class SpatialHand(val id:String,val wrist:SpatialAnchor,val palm:SpatialAnchor,val indexTip:SpatialAnchor,val thumbTip:SpatialAnchor,val middleTip:SpatialAnchor,val pinchAnchor:SpatialAnchor,val handRay:Ray3,val trackingQuality:Float)
data class ContactPoint(val handId:String,val target:SpatialTarget,val confidence:Float)
data class GrabAnchor(val handId:String,val targetId:String,val position:Vec3)
data class SpatialTransformIntent(val translation:Vec3=Vec3.Zero,val rotation:Vec3=Vec3.Zero,val scale:Double=1.0,val radialDelta:Double=0.0,val snapped:Boolean=false)
data class SpatialFrame(val timestampNanos:Long,val hands:List<SpatialHand>,val primaryTarget:SpatialTarget?,val secondaryTarget:SpatialTarget?,val contacts:List<ContactPoint>,val grabAnchors:List<GrabAnchor>,val oneHandTransform:SpatialTransformIntent?,val twoHandTransform:SpatialTransformIntent?)
data class MathAction(val objectId:String,val region:SemanticHitRegion,val interactions:Set<MathInteraction>,val transform:SpatialTransformIntent,val inspection:Map<String,Double> = emptyMap())
data class MathInteractionFrame(val intelligence:HandIntelligenceState,val spatial:SpatialFrame,val action:MathAction?)
data class IntentPrediction(val probabilities:Map<HandIntent,Float>,val transformWeights:TransformIntentWeights,val predictedTargetId:String?,val uncertainty:Float)
data class IntentFeatureSequence(val hand:IntelligentHand,val target:TargetCandidate?,val held:Boolean,val previous:HandIntent,val support:IntelligentHand?=null)
interface IntentPredictor { fun predict(input:IntentFeatureSequence):IntentPrediction }
data class UserHandProfile(var normalizedPinchThreshold:Float=.32f,var releaseThreshold:Float=.50f,var jitterTolerance:Float=.004f,var movementGain:Float=1f,var depthGain:Float=.15f,var dominantHand:Handedness?=null)
object InteractionThresholds { const val GRAB_START=.80f; const val GRAB_CONTINUE=.45f; const val RELEASE=.70f; const val TARGET_LOCK=.75f; const val TARGET_SWITCH=.90f }
'''
files['filtering/AdaptiveHandFilter.kt']=r'''package com.indianservers.aiexplorer.handintelligence.filtering
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class OneEuroFilter(private val minimumCutoff:Double=1.8,private val beta:Double=.12) {
    private var time:Long=-1; private var value=0.0; private var derivative=0.0; private var raw=0.0
    fun filter(input:Double,timestamp:Long,precision:Boolean=false,quality:Float=1f):Double {
        if(time<0 || timestamp-time>350) { time=timestamp; value=input; raw=input; derivative=0.0; return input }
        if(timestamp<=time) return value
        val dt=(timestamp-time)/1000.0
        fun alpha(cutoff:Double)=1.0/(1.0+1.0/(2*PI*cutoff*dt))
        derivative+=alpha(1.0)*((input-raw)/dt-derivative)
        val cutoff=(if(precision) minimumCutoff*.65 else minimumCutoff)*(.6+.4*quality)+beta*abs(derivative)
        value+=alpha(cutoff)*(input-value); raw=input; time=timestamp; return value
    }
    fun reset() { time=-1 }
}
class AdaptiveHandFilter {
    private val axes=Array(3) { OneEuroFilter() }
    fun filter(p:Vec3,t:Long,precision:Boolean,quality:Float)=Vec3(axes[0].filter(p.x,t,precision,quality),axes[1].filter(p.y,t,precision,quality),axes[2].filter(p.z,t,precision,quality))
    fun reset()=axes.forEach { it.reset() }
}
enum class JitterKind { TRACKING_JITTER, PRECISION_MOVEMENT, NORMAL, FAST }
class JitterClassifier {
    fun classify(m:TemporalFeatures,quality:Float):JitterKind=when {
        m.velocity.magnitude()>.8 -> JitterKind.FAST
        m.velocity.magnitude()<.15 && m.trajectoryCurvature>.6 && m.motionConsistency<.3 && quality<.8 -> JitterKind.TRACKING_JITTER
        m.velocity.magnitude()<.15 && m.motionConsistency>.6 -> JitterKind.PRECISION_MOVEMENT
        else -> JitterKind.NORMAL
    }
}
'''
files['features/HandFeatureEngine.kt']=r'''package com.indianservers.aiexplorer.handintelligence.features
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
'''
files['features/TemporalFeatureEngine.kt']=r'''package com.indianservers.aiexplorer.handintelligence.features
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.handintelligence.filtering.*
import kotlin.math.*

/** Ninety retained samples per identity, never an unbounded landmark history. */
class TemporalFeatureEngine(private val capacity:Int=60) {
    private class History(capacity:Int) { val positions=arrayOfNulls<Vec3>(capacity); val angles=DoubleArray(capacity); val pinches=DoubleArray(capacity); val times=LongArray(capacity); var cursor=0; var count=0; var last:TemporalFeatures=TemporalFeatures(); val filter=AdaptiveHandFilter() }
    private val histories=LinkedHashMap<String,History>()
    fun process(features:List<HandPoseFeatures>,time:Long,precision:Boolean):List<IntelligentHand> {
        histories.entries.removeAll { (_,h) -> h.count>0 && time-h.times[(h.cursor-1+h.times.size)%h.times.size]>1000 }
        return features.map { p ->
            val h=histories.getOrPut(p.id) { History(capacity) }; val prev=(h.cursor-1+capacity)%capacity
            val dt=if(h.count==0) 0.0 else (time-h.times[prev])/1000.0
            if(dt<=0 || dt>.35) { h.count=0; h.last=TemporalFeatures(); h.filter.reset() }
            val v=if(h.count==0) Vec3.Zero else (p.pinch-h.positions[prev]!!)*(1.0/dt)
            val a=if(h.count<2) Vec3.Zero else (v-h.last.velocity)*(1.0/dt)
            val jerk=if(h.count<3) Vec3.Zero else (a-h.last.acceleration)*(1.0/dt)
            val consistency=if(v.magnitude()<.015 || h.last.velocity.magnitude()<.015) .8f else clamp((v.unit().dot(h.last.velocity.unit())+1)*.5)
            val stability=clamp(1-v.magnitude()*1.5-a.magnitude()*.02)
            val motion=TemporalFeatures(v,a,jerk,if(h.count==0) 0f else (angleDelta(p.angle,h.angles[prev])/dt).toFloat(),1-consistency,if(h.count==0) 0f else ((p.pinchDistance-h.pinches[prev])/dt).toFloat(),stability,consistency,if(h.count==0) 0 else time-h.times[(h.cursor-h.count+capacity)%capacity],h.count+1)
            h.positions[h.cursor]=p.pinch; h.times[h.cursor]=time; h.angles[h.cursor]=p.angle; h.pinches[h.cursor]=p.pinchDistance; h.cursor=(h.cursor+1)%capacity; h.count=(h.count+1).coerceAtMost(capacity); h.last=motion
            IntelligentHand(p,motion,h.filter.filter(p.pinch,time,precision,p.quality))
        }
    }
    fun reset()=histories.clear()
}
'''
for name,content in files.items():
 p=base/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
