package com.indianservers.aiexplorer.handintelligence.intelligence
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
        val motion=choices.maxByOrNull { scores[it] ?: 0f } ?: HandIntent.GRAB
        val best=if((scores[motion] ?: 0f)<.15f) HandIntent.GRAB else motion
        fun evidence(intent:HandIntent)=if(intent==HandIntent.GRAB) .55f else scores[intent] ?: 0f
        if(best!=contender) { contender=best; since=time }
        if(best!=locked && evidence(best)>evidence(locked)+.20f && time-since>=100) locked=best
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
