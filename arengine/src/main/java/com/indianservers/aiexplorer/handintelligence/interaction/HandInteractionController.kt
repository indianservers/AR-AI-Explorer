package com.indianservers.aiexplorer.handintelligence.interaction
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.handintelligence.features.*
import com.indianservers.aiexplorer.handintelligence.intelligence.*
import com.indianservers.aiexplorer.handintelligence.spatial.*
import com.indianservers.aiexplorer.handintelligence.semantics.*
import com.indianservers.aiexplorer.arengine.interaction.ArHandFrame

interface HandInteractionListener { fun onEvent(event:HandInteractionEvent,frame:MathInteractionFrame) }
enum class HandInteractionEvent { INTENT_START,INTENT_UPDATE,INTENT_END,TARGET_PREDICTED,TARGET_LOCKED,TARGET_RELEASED,GRAB_START,GRAB_UPDATE,GRAB_END,TWO_HAND_START,TWO_HAND_UPDATE,TWO_HAND_END,INSPECT_START,INSPECT_UPDATE,INSPECT_END }
/** Both CameraX and ARCore submit their landmarks to this same provider-independent facade. */
class HandInteractionController(private val features:HandFeatureEngine=HandFeatureEngine(),private val temporal:TemporalFeatureEngine=TemporalFeatureEngine(),private val intelligence:HandIntelligenceEngine=HandIntelligenceEngine(),private val spatial:SpatialProcessingLayer=SpatialProcessingLayer(),private val semantics:MathSemanticEngine=MathSemanticEngine()) {
    var listener:HandInteractionListener?=null
    private var previous:MathInteractionFrame?=null; private var mode:String?=null
    @Synchronized fun reset() { temporal.reset(); intelligence.reset(); spatial.reset(); previous=null; mode=null }
    @Synchronized fun processFrame(frame:ArHandFrame,scene:MathSceneSnapshot):MathInteractionFrame {
        if(mode!=null && mode!=scene.mode) reset()
        mode=scene.mode
        val hands=temporal.process(features.process(frame),frame.timestampMillis,previous?.intelligence?.precisionMode==true || scene.profile==IntelligenceProfile.PRECISION)
        val state=intelligence.process(hands,scene,frame.timestampMillis)
        val result=semantics.resolve(state,spatial.process(state,scene),scene)
        fun emit(event:HandInteractionEvent) { listener?.onEvent(event,result) }
        when(state.phase) {
            InteractionPhase.BEGIN -> { emit(HandInteractionEvent.INTENT_START); emit(HandInteractionEvent.TARGET_LOCKED); emit(HandInteractionEvent.GRAB_START) }
            InteractionPhase.UPDATE -> { emit(HandInteractionEvent.INTENT_UPDATE); emit(HandInteractionEvent.GRAB_UPDATE) }
            InteractionPhase.END,InteractionPhase.CANCEL -> { emit(HandInteractionEvent.INTENT_END); emit(HandInteractionEvent.GRAB_END); emit(HandInteractionEvent.TARGET_RELEASED) }
            InteractionPhase.INSPECT -> emit(if(previous?.intelligence?.phase==InteractionPhase.INSPECT) HandInteractionEvent.INSPECT_UPDATE else HandInteractionEvent.INSPECT_START)
            else -> if(state.targetObjectId!=null) emit(HandInteractionEvent.TARGET_PREDICTED)
        }
        val two=state.twoHandMode==TwoHandMode.COMBINED; val before=previous?.intelligence?.twoHandMode==TwoHandMode.COMBINED
        if(two) emit(if(before) HandInteractionEvent.TWO_HAND_UPDATE else HandInteractionEvent.TWO_HAND_START) else if(before) emit(HandInteractionEvent.TWO_HAND_END)
        if(previous?.intelligence?.phase==InteractionPhase.INSPECT && state.phase!=InteractionPhase.INSPECT) emit(HandInteractionEvent.INSPECT_END)
        previous=result; return result
    }
}
