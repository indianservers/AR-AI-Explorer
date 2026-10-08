package com.indianservers.aiexplorer.handintelligence

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
data class MathObjectSnapshot(override val objectId:String,override val semanticType:MathSemanticType,val vertices:List<Vec3>,val triangles:IntArray=intArrayOf(),val edges:List<Pair<Int,Int>> = emptyList(),val regions:List<SemanticHitRegion> = emptyList(),val visible:Boolean=true,val locked:Boolean=false,val selected:Boolean=false,val dimensions:Vec3=Vec3(1.0,1.0,1.0),val radius:Double=.5,val allowed:Set<MathInteraction> = MathInteraction.entries.toSet(),val inspect:((Vec3)->Map<String,Double>)?=null,val orientation:com.indianservers.aiexplorer.arengine.contract.ArQuaternion=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity):MathObjectAffordance {
    val center=if(vertices.isEmpty()) Vec3.Zero else vertices.reduce(Vec3::plus)*(1.0/vertices.size)
    val boundsMin=Vec3(vertices.minOfOrNull { it.x } ?: 0.0,vertices.minOfOrNull { it.y } ?: 0.0,vertices.minOfOrNull { it.z } ?: 0.0)
    val boundsMax=Vec3(vertices.maxOfOrNull { it.x } ?: 0.0,vertices.maxOfOrNull { it.y } ?: 0.0,vertices.maxOfOrNull { it.z } ?: 0.0)
    val boundsCorners=(0..7).map { i -> Vec3(if(i and 1==0) boundsMin.x else boundsMax.x,if(i and 2==0) boundsMin.y else boundsMax.y,if(i and 4==0) boundsMin.z else boundsMax.z) }
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
data class MathSceneSnapshot(val objects:List<MathObjectSnapshot>,val mapper:CoordinateMapper,val ready:Boolean=true,val uiRegions:List<UiRegion> = emptyList(),val revision:Long=0,val mode:String="camera",val profile:IntelligenceProfile=IntelligenceProfile.BALANCED)
data class TargetCandidate(val objectId:String,val region:SemanticHitRegion,val position:Vec3,val rayAlignmentScore:Float,val proximityScore:Float,val hoverScore:Float,val depthScore:Float,val focusScore:Float,val affordanceScore:Float,val trajectoryScore:Float,val finalScore:Float,val rayDistance:Double=Double.POSITIVE_INFINITY)
data class SpatialTarget(val objectId:String,val region:SemanticHitRegion,val position:Vec3,val confidence:Float)
data class HandIntelligenceState(val timestampNanos:Long,val hands:List<IntelligentHand>,val primaryIntent:HandIntent=HandIntent.IDLE,val intentConfidence:Float=0f,val targetConfidence:Float=0f,val interactionConfidence:Float=0f,val targetObjectId:String?=null,val targetRegionId:String?=null,val oneHandMode:OneHandMode?=null,val twoHandMode:TwoHandMode?=null,val transformWeights:TransformIntentWeights=TransformIntentWeights(),val predictedNextIntent:HandIntent?=null,val uncertainty:Float=1f,val stable:Boolean=false,val precisionMode:Boolean=false,val targetLocked:Boolean=false,val trackingQuality:Float=0f,val state:InteractionState=InteractionState.IDLE,val target:SpatialTarget?=null,val primaryHandId:String?=null,val phase:InteractionPhase=InteractionPhase.IDLE,val supportHandId:String?=null)
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
