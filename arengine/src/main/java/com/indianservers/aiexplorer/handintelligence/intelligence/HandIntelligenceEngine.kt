package com.indianservers.aiexplorer.handintelligence.intelligence
import com.indianservers.aiexplorer.handintelligence.*

class IntentStateMachine {
    var state=InteractionState.IDLE; private set
    fun set(next:InteractionState) { state=next }
    fun reset() { state=InteractionState.IDLE }
}
class HandIntelligenceEngine(private val predictor:IntentPredictor=HeuristicIntentPredictor()) {
    private val targets=TargetPredictor(); private val attention=AttentionEstimator(); private val intentLock=IntentLockManager()
    private val contacts=com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver()
    private val machine=IntentStateMachine(); private val adaptation=UserAdaptationEngine(); private val dominance=DominantHandDetector()
    private var locked:SpatialTarget?=null; private var primary:String?=null; private var lastSeen=0L
    private var candidate:String?=null; private var armedAt=0L; private var releasingAt:Long?=null
    private var support:String?=null; private var supportCandidate:String?=null; private var supportAt=0L
    private var lastTime=-1L; private var lastState:HandIntelligenceState?=null
    fun reset() { locked=null; primary=null; support=null; supportCandidate=null; candidate=null; armedAt=0; supportAt=0; releasingAt=null; lastTime=-1; lastState=null; targets.reset(); intentLock.reset(); machine.reset() }
    fun process(hands:List<IntelligentHand>,scene:MathSceneSnapshot,time:Long):HandIntelligenceState {
        if(time<=lastTime) return (lastState ?: HandIntelligenceState(time*1_000_000,hands)).copy(phase=InteractionPhase.IDLE)
        lastTime=time
        fun result(intent:HandIntent,phase:InteractionPhase,hand:IntelligentHand?,confidence:Float,target:SpatialTarget?=locked):HandIntelligenceState {
            val precision=target?.region?.precision?.let { it>.7 }==true && (hand?.motion?.velocity?.magnitude() ?: 0.0)<.35
            val s=HandIntelligenceState(time*1_000_000,hands,intent,confidence,target?.confidence ?: 0f,confidence*(hand?.pose?.quality ?: 0f),target?.objectId,target?.region?.id,if(locked!=null) if(precision) OneHandMode.PRECISION else OneHandMode.DIRECT else null,if(support!=null) TwoHandMode.COMBINED else if(supportCandidate!=null) TwoHandMode.READY else null,predictedNextIntent=if(intent==HandIntent.POINT) HandIntent.INSPECT else null,uncertainty=1-confidence,stable=confidence>.7,precisionMode=precision,targetLocked=locked!=null,trackingQuality=hand?.pose?.quality ?: 0f,state=machine.state,target=target,primaryHandId=primary ?: hand?.id,phase=phase,supportHandId=support)
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
        if(attention.estimate(hand,scene,null)==HandIntent.UI_INTERACTION) return result(HandIntent.UI_INTERACTION,InteractionPhase.IDLE,hand,1f,locked)
        val candidateTarget=if(locked==null) targets.predict(hand,scene,time,lastState?.targetObjectId) else null
        val heldTarget=locked?.let { l -> candidateTarget?.takeIf { it.objectId==l.objectId } ?: TargetCandidate(l.objectId,l.region,l.position,1f,1f,1f,1f,1f,1f,1f,l.confidence) }
        val scores=predictor.predict(IntentFeatureSequence(hand,heldTarget ?: candidateTarget,locked!=null,lastState?.primaryIntent ?: HandIntent.IDLE))
        if(locked==null) {
            val confidence=scores.probabilities[HandIntent.GRAB] ?: 0f
            val canGrab=candidateTarget!=null && candidateTarget.finalScore>=InteractionThresholds.TARGET_LOCK && confidence>=(if(scene.profile==IntelligenceProfile.PRECISION) .86f else InteractionThresholds.GRAB_START) && (hand.pose.pinchDistance<adaptation.profile.normalizedPinchThreshold || hand.pose.grabStrength>.78) && hand.motion.sampleCount>=3 && hand.pose.quality>=.75
            if(canGrab) {
                if(candidate!=candidateTarget!!.objectId) { candidate=candidateTarget.objectId; armedAt=time }
                machine.set(InteractionState.GRAB_READY)
                if(time-armedAt>=150) {
                    val c=candidateTarget!!; locked=SpatialTarget(c.objectId,c.region,c.position,c.finalScore); primary=hand.id; releasingAt=null; intentLock.reset(); machine.set(InteractionState.GRABBED)
                    adaptation.profile.dominantHand=dominance.observe(hand,c.region.precision>.7)
                    return result(if(c.region.precision>.7) HandIntent.PRECISION_GRAB else HandIntent.GRAB,InteractionPhase.BEGIN,hand,confidence)
                }
                val c=candidateTarget!!
                return result(if(c.region.precision>.7) HandIntent.PRECISION_GRAB else HandIntent.GRAB,InteractionPhase.IDLE,hand,confidence,SpatialTarget(c.objectId,c.region,c.position,c.finalScore))
            } else candidate=null
            val inspect=scores.probabilities[HandIntent.INSPECT] ?: 0f
            val target=candidateTarget?.let { SpatialTarget(it.objectId,it.region,it.position,it.finalScore) }
            if(inspect>.6 && hand.motion.sampleCount>=3 && hand.motion.velocity.magnitude()<.6) { machine.set(InteractionState.INSPECTING); return result(HandIntent.INSPECT,InteractionPhase.INSPECT,hand,inspect,target) }
            machine.set(if(target==null) InteractionState.IDLE else if(hand.motion.velocity.magnitude()>.15) InteractionState.APPROACH else if((candidateTarget?.proximityScore ?: 0f)>.8f) InteractionState.CONTACT else InteractionState.HOVER)
            return result(if(hand.pose.pointScore>.6) HandIntent.POINT else HandIntent.EXPLORE,InteractionPhase.IDLE,hand,candidateTarget?.finalScore ?: 0f,target)
        }
        val release=(scores.probabilities[HandIntent.RELEASE] ?: 0f)>=InteractionThresholds.RELEASE && hand.pose.pinchDistance>adaptation.profile.releaseThreshold && hand.pose.grabStrength<.5
        if(release) {
            if(releasingAt==null) releasingAt=time
            machine.set(InteractionState.RELEASING)
            if(time-releasingAt!!>=110) { val target=locked; locked=null; primary=null; support=null; supportCandidate=null; candidate=null; machine.reset(); return result(HandIntent.RELEASE,InteractionPhase.END,hand,1f,target) }
            return result(HandIntent.GRAB,InteractionPhase.IDLE,hand,.7f)
        } else releasingAt=null
        if(maxOf(hand.pose.pinchStrength,hand.pose.grabStrength)<InteractionThresholds.GRAB_CONTINUE) return result(HandIntent.GRAB,InteractionPhase.IDLE,hand,.45f)
        val other=hands.firstOrNull { it.id!=primary && it.pose.quality>=.75 && (it.pose.pinchStrength>.7 || it.pose.grabStrength>.78) }
        val otherTarget=other?.let { if(it.id==support) heldTarget else contacts.candidates(it,scene,locked!!.objectId,180).firstOrNull { c -> c.objectId==locked!!.objectId && c.finalScore>.8 } }
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
