from pathlib import Path
base=Path('arengine/src/main/java/com/indianservers/aiexplorer/handintelligence')
files={}
files['spatial/ContactSolver.kt']=r'''package com.indianservers.aiexplorer.handintelligence.spatial
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class ContactSolver {
    fun candidates(hand:IntelligentHand,scene:MathSceneSnapshot,focus:String?,dwell:Long):List<TargetCandidate> {
        val finger=if(hand.pose.pointScore>.55) hand.pose.indexTip else hand.filteredPinch
        val ray=scene.mapper.screenToRay(finger)
        val predicted=finger+hand.motion.velocity*.08
        val tolerance=(hand.pose.handScale*.22).coerceIn(.008,.045)
        return scene.objects.asSequence().filter { it.visible && !it.locked && it.vertices.isNotEmpty() }.mapNotNull { obj ->
            val projectedCenter=scene.mapper.project(obj.center) ?: return@mapNotNull null
            val edgeProjection=scene.mapper.project(obj.center+Vec3(obj.size,0.0,0.0)) ?: projectedCenter
            val projectedSize=max(.025,(edgeProjection-projectedCenter).magnitude())
            if((finger-projectedCenter).magnitude()>projectedSize*2.5+tolerance && !obj.selected) return@mapNotNull null
            var hit:Vec3?=null; var hitIndex:Int?=null; var nearest=Double.MAX_VALUE
            val triangles=obj.triangles
            var i=0
            while(i+2<triangles.size) {
                val a=obj.vertices[triangles[i]]; val b=obj.vertices[triangles[i+1]]; val c=obj.vertices[triangles[i+2]]
                val e1=b-a; val e2=c-a; val h=ray.direction.cross(e2); val det=e1.dot(h)
                if(abs(det)>1e-9) {
                    val inv=1/det; val s=ray.origin-a; val u=s.dot(h)*inv; val q=s.cross(e1); val v=ray.direction.dot(q)*inv; val t=e2.dot(q)*inv
                    if(u>=0 && v>=0 && u+v<=1 && t>0 && t<nearest) { nearest=t; hit=ray.origin+ray.direction*t; hitIndex=i/3 }
                }; i+=3
            }
            var region=SemanticHitRegion("body",RegionKind.BODY,position=hit ?: obj.center)
            var distance=if(hit!=null) 0.0 else Double.MAX_VALUE
            var position=hit ?: obj.center
            // Vertices/edges are actual topology, with a hand-size-relative selection aperture.
            if(obj.vertices.size<=256) obj.vertices.forEachIndexed { index,v ->
                val screen=scene.mapper.project(v) ?: return@forEachIndexed
                val d=(screen-finger).magnitude()
                if(d<tolerance && d<distance.coerceAtLeast(tolerance)) {
                    val kind=if(obj.semanticType==MathSemanticType.VECTOR) if(index==0) RegionKind.VECTOR_ORIGIN else if(index==1) RegionKind.VECTOR_HEAD else RegionKind.VERTEX else RegionKind.VERTEX
                    region=SemanticHitRegion("vertex-$index",kind,index,position=v,precision=.95f); position=v; distance=d
                }
            }
            if(region.kind==RegionKind.BODY) obj.edges.forEachIndexed { index,(ia,ib) ->
                val a=obj.vertices[ia]; val b=obj.vertices[ib]; val sa=scene.mapper.project(a) ?: return@forEachIndexed; val sb=scene.mapper.project(b) ?: return@forEachIndexed
                val d=sb-sa; val t=((finger-sa).dot(d)/d.dot(d).coerceAtLeast(1e-12)).coerceIn(0.0,1.0); val q=sa+d*t; val gap=(q-finger).magnitude()
                if(gap<tolerance*.7 && (hit==null || obj.triangles.isEmpty())) { position=a+(b-a)*t; distance=gap; region=SemanticHitRegion("edge-$index",if(obj.triangles.isEmpty()) RegionKind.CURVE else RegionKind.EDGE,index,position=position,precision=.8f) }
            }
            if(region.kind==RegionKind.BODY && hit!=null) {
                val radial=(position-obj.center).unit()
                region=when(obj.semanticType) {
                    MathSemanticType.SPHERE,MathSemanticType.CIRCLE -> SemanticHitRegion("surface",RegionKind.SURFACE,position=position,normal=radial)
                    MathSemanticType.GRAPH_3D,MathSemanticType.FUNCTION_SURFACE -> SemanticHitRegion("surface",RegionKind.SURFACE,hitIndex,position=position,normal=radial)
                    MathSemanticType.CUBE,MathSemanticType.CUBOID,MathSemanticType.CYLINDER,MathSemanticType.CONE -> {
                        if((finger-projectedCenter).magnitude()<projectedSize*.25) region else {
                            val normal=if(hitIndex!=null) { val f=hitIndex!!*3; (obj.vertices[triangles[f+1]]-obj.vertices[triangles[f]]).cross(obj.vertices[triangles[f+2]]-obj.vertices[triangles[f]]).unit() } else radial
                            val axis=if(abs(normal.x)>abs(normal.y) && abs(normal.x)>abs(normal.z)) 0 else if(abs(normal.y)>abs(normal.z)) 1 else 2
                            SemanticHitRegion("face-${hitIndex ?: axis}",RegionKind.FACE,hitIndex,axis,if(listOf(normal.x,normal.y,normal.z)[axis]>=0) 1.0 else -1.0,position,normal,.4f)
                        }
                    }
                    else -> region
                }
            }
            if(hit==null && distance>tolerance) return@mapNotNull null
            val alignment=if(hit!=null) 1f else clamp(1-distance/tolerance)
            val proximity=clamp(1-distance.coerceAtMost(tolerance)/tolerance)
            val hover=clamp(dwell/180.0); val focusScore=if(obj.objectId==focus) 1f else if(obj.selected) .7f else .25f
            val trajectory=clamp(1-(predicted-(scene.mapper.project(position) ?: finger)).magnitude()/(projectedSize+tolerance))
            val depth=clamp(1/(1+abs(projectedCenter.z)*.01))
            val score=alignment*.35f+proximity*.20f+hover*.15f+trajectory*.10f+focusScore*.10f+depth*.05f+.05f
            TargetCandidate(obj.objectId,region,position,alignment,proximity,hover,depth,focusScore,1f,trajectory,score)
        }.sortedByDescending { it.finalScore }.take(8).toList()
    }
}
class SpatialConflictResolver { fun uiOwns(p:Vec3,scene:MathSceneSnapshot)=scene.uiRegions.any { it.contains(p) } }
class MotionPredictor { fun predict(p:Vec3,v:Vec3,horizon:Double=.08):Vec3 { val offset=v*horizon.coerceIn(.05,.15); return p+offset*(min(1.0,.04/offset.magnitude().coerceAtLeast(1e-9))) } }
'''
files['intelligence/IntentScorer.kt']=r'''package com.indianservers.aiexplorer.handintelligence.intelligence
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class IntentScorer {
    fun score(input:IntentFeatureSequence):IntentPrediction {
        val h=input.hand; val p=h.pose; val m=h.motion; val t=input.target
        val target=t?.finalScore ?: 0f; val contact=t?.proximityScore ?: 0f
        val closure=max(p.pinchStrength,p.grabStrength)
        val grab=clamp(closure*.30+target*.25+contact*.20+m.motionConsistency*.15+p.quality*.10)
        val movement=clamp(m.velocity.magnitude()*2.5); val rotation=clamp(abs(m.angularVelocity)/80.0)
        val release=clamp((1-closure)*.65+p.openness*.25+max(0f,m.pinchVelocity)*.10)
        val point=p.pointScore*target
        val probabilities=HandIntent.entries.associateWith { intent -> when(intent) {
            HandIntent.IDLE -> 1-max(point,grab*target)
            HandIntent.EXPLORE -> point*.5f
            HandIntent.POINT -> point
            HandIntent.TOUCH -> contact*(1-closure)
            HandIntent.PRECISION_GRAB -> grab*(t?.region?.precision ?: 0f)
            HandIntent.GRAB -> grab
            HandIntent.MOVE -> if(input.held) movement*closure else 0f
            HandIntent.ROTATE -> if(input.held) rotation*closure else 0f
            HandIntent.RESIZE -> if(input.held && input.support!=null) closure else 0f
            HandIntent.STRETCH -> if(input.held && t?.region?.kind==RegionKind.FACE) movement*closure else 0f
            HandIntent.PUSH,HandIntent.PULL -> if(input.held) clamp(abs(m.pinchVelocity)*.1)*closure else 0f
            HandIntent.INSPECT -> point*(.7f+.3f*m.stabilityScore)
            HandIntent.RELEASE -> if(input.held) release else 0f
            HandIntent.UI_INTERACTION -> 0f
            HandIntent.UNKNOWN -> 1-p.quality
        }.coerceIn(0f,1f) }
        return IntentPrediction(probabilities,TransformIntentWeights(movement,rotation,if(input.support!=null) .8f else 0f),t?.objectId,1-max(grab*target,point))
    }
}
class HeuristicIntentPredictor(private val scorer:IntentScorer=IntentScorer()):IntentPredictor { override fun predict(input:IntentFeatureSequence)=scorer.score(input) }
class IntentLockManager {
    private var locked=HandIntent.GRAB; private var contender=locked; private var since=0L
    fun update(scores:Map<HandIntent,Float>,time:Long):HandIntent {
        val choices=setOf(HandIntent.MOVE,HandIntent.ROTATE,HandIntent.RESIZE,HandIntent.STRETCH)
        val best=choices.maxByOrNull { scores[it] ?: 0f } ?: locked
        if(best!=contender) { contender=best; since=time }
        if(best!=locked && (scores[best] ?: 0f)>(scores[locked] ?: 0f)+.20f && time-since>=100) locked=best
        return locked
    }
    fun reset() { locked=HandIntent.GRAB; contender=locked; since=0 }
}
class DominantHandDetector {
    private val scores=mutableMapOf<Handedness,Double>()
    fun observe(hand:IntelligentHand,precision:Boolean):Handedness? { val side=hand.pose.handedness; if(side!=Handedness.UNKNOWN) scores[side]=(scores[side] ?: 0.0)+if(precision) 3.0 else .1; return scores.maxByOrNull { it.value }?.key }
}
class UserAdaptationEngine {
    val profile=UserHandProfile()
    fun observe(h:IntelligentHand,held:Boolean) {
        if(h.pose.quality<.85 || !h.motion.stabilityScore.isFinite()) return
        if(held && h.pose.pinchDistance<.32) profile.normalizedPinchThreshold=(profile.normalizedPinchThreshold*.995f+(h.pose.pinchDistance+.10).toFloat()*.005f).coerceIn(.25f,.38f)
        profile.jitterTolerance=(profile.jitterTolerance*.995f+h.motion.velocity.magnitude().coerceAtMost(.01).toFloat()*.005f).coerceIn(.001f,.006f)
        profile.movementGain=profile.movementGain.coerceIn(.65f,1.15f); profile.releaseThreshold=profile.releaseThreshold.coerceIn(.45f,.60f)
    }
}
class AttentionEstimator { fun estimate(hand:IntelligentHand,scene:MathSceneSnapshot,target:TargetCandidate?)=if(scene.uiRegions.any { it.contains(hand.pose.indexTip) }) HandIntent.UI_INTERACTION else if(target!=null) HandIntent.EXPLORE else HandIntent.IDLE }
class TargetPredictor(private val contacts:com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver=com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver()) {
    private var candidate:String?=null; private var since=0L
    fun predict(hand:IntelligentHand,scene:MathSceneSnapshot,time:Long,focus:String?):TargetCandidate? {
        val result=contacts.candidates(hand,scene,focus,if(candidate!=null) time-since else 0).firstOrNull()
        if(result?.objectId!=candidate) { candidate=result?.objectId; since=time }
        return result
    }
    fun reset() { candidate=null; since=0 }
}
'''
files['intelligence/HandIntelligenceEngine.kt']=r'''package com.indianservers.aiexplorer.handintelligence.intelligence
import com.indianservers.aiexplorer.handintelligence.*

class IntentStateMachine {
    var state=InteractionState.IDLE; private set
    fun set(next:InteractionState) { state=next }
    fun reset() { state=InteractionState.IDLE }
}
class HandIntelligenceEngine(private val predictor:IntentPredictor=HeuristicIntentPredictor()) {
    private val targets=TargetPredictor(); private val attention=AttentionEstimator(); private val intentLock=IntentLockManager()
    private val machine=IntentStateMachine(); private val adaptation=UserAdaptationEngine(); private val dominance=DominantHandDetector()
    private var locked:SpatialTarget?=null; private var primary:String?=null; private var lastSeen=0L
    private var candidate:String?=null; private var armedAt=0L; private var releasingAt:Long?=null
    private var support:String?=null; private var supportCandidate:String?=null; private var supportAt=0L
    private var lastTime=-1L; private var lastState:HandIntelligenceState?=null
    fun reset() { locked=null; primary=null; support=null; candidate=null; releasingAt=null; lastTime=-1; lastState=null; targets.reset(); intentLock.reset(); machine.reset() }
    fun process(hands:List<IntelligentHand>,scene:MathSceneSnapshot,time:Long):HandIntelligenceState {
        if(time<=lastTime) return (lastState ?: HandIntelligenceState(time*1_000_000,hands)).copy(phase=InteractionPhase.IDLE)
        lastTime=time
        fun result(intent:HandIntent,phase:InteractionPhase,hand:IntelligentHand?,confidence:Float,target:SpatialTarget?=locked):HandIntelligenceState {
            val precision=target?.region?.precision?.let { it>.7 }==true && (hand?.motion?.velocity?.magnitude() ?: 0.0)<.35
            val s=HandIntelligenceState(time*1_000_000,hands,intent,confidence,target?.confidence ?: 0f,confidence*(hand?.pose?.quality ?: 0f),target?.objectId,target?.region?.id,if(locked!=null) if(precision) OneHandMode.PRECISION else OneHandMode.DIRECT else null,if(support!=null) TwoHandMode.COMBINED else if(supportCandidate!=null) TwoHandMode.READY else null,predictedNextIntent=if(intent==HandIntent.POINT) HandIntent.INSPECT else null,uncertainty=1-confidence,stable=confidence>.7,precisionMode=precision,targetLocked=locked!=null,trackingQuality=hand?.pose?.quality ?: 0f,state=machine.state,target=target,primaryHandId=primary ?: hand?.id,phase=phase)
            lastState=s; return s
        }
        if(!scene.ready) { val had=locked!=null; val target=locked; reset(); return result(HandIntent.IDLE,if(had) InteractionPhase.CANCEL else InteractionPhase.IDLE,null,0f,target) }
        if(locked!=null && scene.objects.none { it.objectId==locked!!.objectId && it.visible && !it.locked }) { val target=locked; reset(); return result(HandIntent.IDLE,InteractionPhase.CANCEL,null,0f,target) }
        val hand=hands.firstOrNull { it.id==primary } ?: if(locked==null) hands.maxByOrNull { it.pose.quality+if(it.pose.handedness==adaptation.profile.dominantHand) .03f else 0f } else null
        if(hand==null || hand.pose.quality<.6) {
            if(locked!=null && time-lastSeen<=180) return result(lastState?.primaryIntent ?: HandIntent.GRAB,InteractionPhase.IDLE,hand,.45f)
            val had=locked!=null; val target=locked; locked=null; primary=null; support=null; machine.reset()
            return result(if(had) HandIntent.RELEASE else HandIntent.IDLE,if(had) InteractionPhase.END else InteractionPhase.IDLE,hand,0f,target)
        }
        lastSeen=time; adaptation.observe(hand,locked!=null)
        if(attention.estimate(hand,scene,null)==HandIntent.UI_INTERACTION) { if(locked==null) return result(HandIntent.UI_INTERACTION,InteractionPhase.IDLE,hand,1f,null) }
        val candidateTarget=targets.predict(hand,scene,time,locked?.objectId ?: lastState?.targetObjectId)
        val heldTarget=locked?.let { l -> candidateTarget?.takeIf { it.objectId==l.objectId } ?: TargetCandidate(l.objectId,l.region,l.position,1f,1f,1f,1f,1f,1f,1f,l.confidence) }
        val scores=predictor.predict(IntentFeatureSequence(hand,heldTarget ?: candidateTarget,locked!=null,lastState?.primaryIntent ?: HandIntent.IDLE))
        if(locked==null) {
            val confidence=scores.probabilities[HandIntent.GRAB] ?: 0f
            val canGrab=candidateTarget!=null && candidateTarget.finalScore>=InteractionThresholds.TARGET_LOCK && confidence>=InteractionThresholds.GRAB_START && (hand.pose.pinchDistance<adaptation.profile.normalizedPinchThreshold || hand.pose.grabStrength>.78) && hand.motion.sampleCount>=3 && hand.pose.quality>=.75
            if(canGrab) {
                if(candidate!=candidateTarget!!.objectId) { candidate=candidateTarget.objectId; armedAt=time }
                machine.set(InteractionState.GRAB_READY)
                if(time-armedAt>=150) {
                    val c=candidateTarget!!; locked=SpatialTarget(c.objectId,c.region,c.position,c.finalScore); primary=hand.id; releasingAt=null; intentLock.reset(); machine.set(InteractionState.GRABBED)
                    adaptation.profile.dominantHand=dominance.observe(hand,c.region.precision>.7)
                    return result(if(c.region.precision>.7) HandIntent.PRECISION_GRAB else HandIntent.GRAB,InteractionPhase.BEGIN,hand,confidence)
                }
            } else candidate=null
            val inspect=scores.probabilities[HandIntent.INSPECT] ?: 0f
            val target=candidateTarget?.let { SpatialTarget(it.objectId,it.region,it.position,it.finalScore) }
            if(inspect>.6 && hand.motion.sampleCount>=3 && hand.motion.velocity.magnitude()<.6) { machine.set(InteractionState.INSPECTING); return result(HandIntent.INSPECT,InteractionPhase.INSPECT,hand,inspect,target) }
            machine.set(if(target!=null) InteractionState.HOVER else InteractionState.IDLE)
            return result(if(hand.pose.pointScore>.6) HandIntent.POINT else HandIntent.EXPLORE,InteractionPhase.IDLE,hand,candidateTarget?.finalScore ?: 0f,target)
        }
        val release=(scores.probabilities[HandIntent.RELEASE] ?: 0f)>=InteractionThresholds.RELEASE && hand.pose.pinchDistance>adaptation.profile.releaseThreshold && hand.pose.grabStrength<.5
        if(release) {
            if(releasingAt==null) releasingAt=time
            machine.set(InteractionState.RELEASING)
            if(time-releasingAt!!>=110) { val target=locked; locked=null; primary=null; support=null; supportCandidate=null; candidate=null; machine.reset(); return result(HandIntent.RELEASE,InteractionPhase.END,hand,1f,target) }
            return result(HandIntent.GRAB,InteractionPhase.IDLE,hand,.7f)
        } else releasingAt=null
        val other=hands.firstOrNull { it.id!=primary && it.pose.quality>=.75 && (it.pose.pinchStrength>.7 || it.pose.grabStrength>.78) }
        val otherTarget=other?.let { com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver().candidates(it,scene,locked!!.objectId,180).firstOrNull { c -> c.objectId==locked!!.objectId && c.finalScore>.8 } }
        if(other!=null && otherTarget!=null) {
            if(supportCandidate!=other.id) { supportCandidate=other.id; supportAt=time }
            if(time-supportAt>=150) support=other.id
        } else { supportCandidate=null; support=null }
        val prediction=predictor.predict(IntentFeatureSequence(hand,heldTarget,true,lastState?.primaryIntent ?: HandIntent.GRAB,hands.firstOrNull { it.id==support }))
        val intent=intentLock.update(prediction.probabilities,time)
        machine.set(if(support!=null) InteractionState.TWO_HAND_GRABBED else when(intent) { HandIntent.ROTATE -> InteractionState.ROTATING; HandIntent.RESIZE -> InteractionState.SCALING; HandIntent.STRETCH -> InteractionState.STRETCHING; HandIntent.MOVE -> InteractionState.MOVING; else -> InteractionState.GRABBED })
        val base=result(intent,InteractionPhase.UPDATE,hand,(prediction.probabilities[intent] ?: .7f).coerceAtLeast(.5f))
        return base.copy(transformWeights=prediction.transformWeights).also { lastState=it }
    }
}
'''
for name,content in files.items():
 p=base/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
