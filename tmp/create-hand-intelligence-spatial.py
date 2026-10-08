from pathlib import Path
base=Path('arengine/src/main/java/com/indianservers/aiexplorer/handintelligence')
files={}
files['spatial/SpatialProcessingLayer.kt']=r'''package com.indianservers.aiexplorer.handintelligence.spatial
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class SmartSnapEngine {
    fun point(p:Vec3,scene:MathSceneSnapshot,precision:Boolean,speed:Double):Pair<Vec3,Boolean> {
        if(!precision || speed>.35) return p to false
        val tolerance=.08
        var best=Vec3(round(p.x),round(p.y),round(p.z)); var distance=(best-p).magnitude()
        for(obj in scene.objects) for(v in obj.vertices.take(256)) { val d=(v-p).magnitude(); if(d<distance) { best=v; distance=d } }
        if(distance>tolerance) return p to false
        val weight=(1-distance/tolerance)*.7
        return (p+(best-p)*weight) to true
    }
    fun angle(degrees:Double,precision:Boolean):Double { if(!precision) return degrees; val targets=doubleArrayOf(-180.0,-90.0,-60.0,-45.0,-30.0,0.0,30.0,45.0,60.0,90.0,180.0); val nearest=targets.minByOrNull { abs(it-degrees) }!!; return if(abs(nearest-degrees)<3) degrees+(nearest-degrees)*.65 else degrees }
}
class GrabConstraintSolver {
    var target:SpatialTarget?=null; private set
    fun lock(t:SpatialTarget) { target=t }
    fun release() { target=null }
}
class TransformSolver {
    private var ids=emptyList<String>(); private var basePositions=emptyList<Vec3>(); private var baseAngles=emptyList<Double>(); private var baseScales=emptyList<Double>()
    private var baseTransform=SpatialTransformIntent(); private var current=baseTransform; private var anchor=Vec3.Zero
    private var baselineMapper:CoordinateMapper?=null; private var translationActive=false; private var rotationActive=false; private var scaleActive=false
    fun reset() { ids=emptyList(); baselineMapper=null; current=SpatialTransformIntent(); baseTransform=current; translationActive=false; rotationActive=false; scaleActive=false }
    fun begin(state:HandIntelligenceState,scene:MathSceneSnapshot) { reset(); anchor=state.target?.position ?: Vec3.Zero; baselineMapper=scene.mapper; rebase(state,scene) }
    private fun involved(state:HandIntelligenceState):List<IntelligentHand> {
        val first=state.hands.firstOrNull { it.id==state.primaryHandId } ?: return emptyList()
        return if(state.twoHandMode==TwoHandMode.COMBINED) listOf(first)+state.hands.filter { it.id!=first.id }.take(1) else listOf(first)
    }
    private fun rebase(state:HandIntelligenceState,scene:MathSceneSnapshot) {
        val hands=involved(state); ids=hands.map { it.id }; basePositions=hands.map { scene.mapper.atDepth(it.filteredPinch,anchor) ?: anchor }; baseAngles=hands.map { it.pose.angle }; baseScales=hands.map { it.pose.handScale }; baseTransform=current
    }
    fun update(state:HandIntelligenceState,scene:MathSceneSnapshot):SpatialTransformIntent {
        val hands=involved(state); if(hands.isEmpty()) return current
        if(hands.map { it.id }!=ids) { rebase(state,scene); return current }
        val mapper=baselineMapper ?: scene.mapper
        val points=hands.map { mapper.atDepth(it.filteredPinch,anchor) ?: anchor }
        val start=basePositions.reduce(Vec3::plus)*(1.0/basePositions.size); val now=points.reduce(Vec3::plus)*(1.0/points.size)
        val delta=now-start
        var rotation=angleDelta(hands[0].pose.angle,baseAngles[0]); var scale=1.0
        if(hands.size==2) {
            val before=basePositions[1]-basePositions[0]; val after=points[1]-points[0]
            scale=(after.magnitude()/before.magnitude().coerceAtLeast(.02)).coerceIn(.25,4.0)
            // Full inter-hand vector rotation, expressed in scene-local Euler axes.
            rotation=angleDelta(Math.toDegrees(atan2(after.y,after.x)),Math.toDegrees(atan2(before.y,before.x)))
            scaleActive=scaleActive || abs(ln(scale))>.015
        }
        translationActive=translationActive || delta.magnitude()>.01
        rotationActive=rotationActive || abs(rotation)>3
        val speed=hands[0].motion.velocity.magnitude()
        // Small deliberate edits retain micro movement; no per-frame gain change may jump a held object.
        val gain=if(state.precisionMode && hands.size==1) .65 else 1.0
        val translated=baseTransform.translation+if(translationActive) delta*gain else Vec3.Zero
        val turned=baseTransform.rotation+if(rotationActive) Vec3(0.0,0.0,rotation) else Vec3.Zero
        val scaled=(baseTransform.scale*if(scaleActive) scale else 1.0).coerceIn(.1,10.0)
        val depthRatio=ln((hands[0].pose.handScale/baseScales[0].coerceAtLeast(.01)).coerceIn(.5,2.0))
        val depth=if(hands.size==1 && abs(depthRatio)>.08 && speed<.8) depthRatio*.15 else 0.0
        current=SpatialTransformIntent(translated+Vec3(0.0,0.0,depth),turned,scaled,(state.target?.region?.normal ?: Vec3.Zero).dot(translated))
        return current
    }
}
class SpatialProcessingLayer {
    private val grab=GrabConstraintSolver(); private val transforms=TransformSolver(); private val snaps=SmartSnapEngine()
    fun reset() { grab.release(); transforms.reset() }
    fun process(state:HandIntelligenceState,scene:MathSceneSnapshot):SpatialFrame {
        if(state.phase==InteractionPhase.BEGIN && state.target!=null) { grab.lock(state.target); transforms.begin(state,scene) }
        val target=grab.target ?: state.target
        val hands=state.hands.map { h ->
            fun at(p:Vec3)=SpatialAnchor(scene.mapper.atDepth(p,target?.position ?: Vec3.Zero) ?: Vec3.Zero,h.pose.quality)
            SpatialHand(h.id,at(h.pose.wrist),at(h.pose.palm),at(h.pose.indexTip),at(h.pose.thumbTip),at(h.pose.middleTip),at(h.filteredPinch),scene.mapper.screenToRay(h.pose.indexTip),h.pose.quality)
        }
        val raw=if(state.phase==InteractionPhase.UPDATE) transforms.update(state,scene) else null
        val transform=raw?.let { t -> val origin=target?.position ?: Vec3.Zero; val speed=state.hands.firstOrNull()?.motion?.velocity?.magnitude() ?: 0.0; val snap=snaps.point(origin+t.translation,scene,state.precisionMode,speed); t.copy(translation=snap.first-origin,rotation=Vec3(t.rotation.x,t.rotation.y,snaps.angle(t.rotation.z,state.precisionMode)),snapped=snap.second) }
        val two=state.twoHandMode==TwoHandMode.COMBINED
        val frame=SpatialFrame(state.timestampNanos,hands,target,if(two) target else null,if(target==null) emptyList() else hands.map { ContactPoint(it.id,target,state.targetConfidence) },if(grab.target==null) emptyList() else hands.map { GrabAnchor(it.id,grab.target!!.objectId,grab.target!!.position) },if(two) null else transform,if(two) transform else null)
        if(state.phase in setOf(InteractionPhase.END,InteractionPhase.CANCEL)) reset()
        return frame
    }
}
'''
files['semantics/MathSemanticEngine.kt']=r'''package com.indianservers.aiexplorer.handintelligence.semantics
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class MathSemanticEngine {
    fun resolve(state:HandIntelligenceState,spatial:SpatialFrame,scene:MathSceneSnapshot):MathInteractionFrame {
        val target=state.target ?: spatial.primaryTarget ?: return MathInteractionFrame(state,spatial,null)
        val obj=scene.objects.firstOrNull { it.objectId==target.objectId } ?: return MathInteractionFrame(state,spatial,null)
        if(obj.locked || !obj.visible) return MathInteractionFrame(state,spatial,null)
        val t=spatial.twoHandTransform ?: spatial.oneHandTransform ?: SpatialTransformIntent()
        val operations=linkedSetOf<MathInteraction>()
        if(state.phase==InteractionPhase.INSPECT) operations+=MathInteraction.INSPECT
        else when(target.region.kind) {
            RegionKind.VECTOR_ORIGIN -> operations+=MathInteraction.EDIT_VECTOR_ORIGIN
            RegionKind.VECTOR_HEAD -> operations+=MathInteraction.EDIT_VECTOR_HEAD
            RegionKind.VERTEX -> operations+=if(obj.semanticType in setOf(MathSemanticType.GRAPH_2D,MathSemanticType.GRAPH_3D)) MathInteraction.EDIT_GRAPH_POINT else MathInteraction.MOVE_VERTEX
            RegionKind.EDGE -> operations+=MathInteraction.MOVE_EDGE
            RegionKind.FACE -> {
                val axis=target.region.axis
                val d=listOf(t.translation.x,t.translation.y,t.translation.z)
                val stretch=obj.semanticType in setOf(MathSemanticType.CUBE,MathSemanticType.CUBOID,MathSemanticType.CONE,MathSemanticType.CYLINDER) && axis!=null && (abs(d[axis])>=t.translation.magnitude()*.65 || state.phase==InteractionPhase.BEGIN)
                if(stretch) operations+=listOf(MathInteraction.STRETCH_X,MathInteraction.STRETCH_Y,MathInteraction.STRETCH_Z)[axis!!] else operations+=MathInteraction.MOVE_FACE
            }
            RegionKind.RADIUS,RegionKind.SURFACE -> {
                if(obj.semanticType in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE) && (abs(t.scale-1)>.001 || abs(t.radialDelta)>=t.translation.magnitude()*.45 || state.phase==InteractionPhase.BEGIN)) operations+=MathInteraction.CHANGE_RADIUS else operations+=MathInteraction.TRANSLATE
            }
            else -> operations+=MathInteraction.TRANSLATE
        }
        if(state.phase==InteractionPhase.UPDATE && target.region.kind in setOf(RegionKind.BODY,RegionKind.SURFACE,RegionKind.CURVE)) {
            if(t.rotation.magnitude()>.01) operations+=MathInteraction.ROTATE
            if(abs(t.scale-1)>.001 && MathInteraction.CHANGE_RADIUS !in operations) operations+=if(obj.semanticType in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE)) MathInteraction.CHANGE_RADIUS else MathInteraction.SCALE
        }
        operations.retainAll(obj.allowed)
        val p=target.position
        val inspection=if(state.phase==InteractionPhase.INSPECT) linkedMapOf("x" to p.x,"y" to p.y,"z" to p.z).apply {
            if(obj.semanticType==MathSemanticType.VECTOR && obj.vertices.size>=2) { val d=obj.vertices[1]-obj.vertices[0]; put("magnitude",d.magnitude()); put("dx",d.x); put("dy",d.y); put("dz",d.z) }
            if(obj.semanticType==MathSemanticType.SPHERE) { put("radius",obj.radius); put("volume",4*PI*obj.radius.pow(3)/3) }
        } else emptyMap()
        return MathInteractionFrame(state,spatial,MathAction(obj.objectId,target.region,operations,t,inspection))
    }
}
'''
files['interaction/HandInteractionController.kt']=r'''package com.indianservers.aiexplorer.handintelligence.interaction
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
        val hands=temporal.process(features.process(frame),frame.timestampMillis,previous?.intelligence?.precisionMode==true)
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
'''
files['debug/GestureReplay.kt']=r'''package com.indianservers.aiexplorer.handintelligence.debug
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.handintelligence.interaction.HandInteractionController
import com.indianservers.aiexplorer.arengine.interaction.ArHandFrame
import java.io.Writer

/** Bounded developer capture. Export occurs only on an explicit local action, never during inference. */
data class ReplaySample(val tracking:ArHandFrame,val scene:MathSceneSnapshot,val result:MathInteractionFrame,val label:HandIntent?)
class GestureReplayRecorder(private val limit:Int=900) {
    private val samples=java.util.ArrayDeque<ReplaySample>()
    var enabled=false
    @Synchronized fun record(frame:ArHandFrame,scene:MathSceneSnapshot,result:MathInteractionFrame,label:HandIntent?=null) { if(!enabled) return; if(samples.size>=limit) samples.removeFirst(); samples.addLast(ReplaySample(frame,scene,result,label)) }
    @Synchronized fun snapshot()=samples.toList()
    @Synchronized fun clear()=samples.clear()
    fun exportCsv(writer:Writer) { writer.write("timestamp,intent,confirmedLabel,target,region,confidence,trackingQuality,tx,ty,tz,rx,ry,rz,scale\n"); snapshot().forEach { s -> val i=s.result.intelligence; val t=s.result.action?.transform ?: SpatialTransformIntent(); fun quote(v:Any?)="\"${v.toString().replace("\"","\"\"")}\""; writer.write(listOf(s.tracking.timestampMillis,i.primaryIntent,s.label,i.targetObjectId,i.targetRegionId,i.intentConfidence,i.trackingQuality,t.translation.x,t.translation.y,t.translation.z,t.rotation.x,t.rotation.y,t.rotation.z,t.scale).joinToString(",",transform=::quote)+"\n") }; writer.flush() }
    fun exportJson(writer:Writer) { fun q(v:String?)="\"${(v ?: "").replace("\\","\\\\").replace("\"","\\\"")}\""; writer.write("["); snapshot().forEachIndexed { index,s -> if(index>0) writer.write(","); val i=s.result.intelligence; writer.write("{\"timestamp\":${s.tracking.timestampMillis},\"intent\":${q(i.primaryIntent.name)},\"label\":${q(s.label?.name)},\"target\":${q(i.targetObjectId)},\"region\":${q(i.targetRegionId)},\"confidence\":${i.intentConfidence},\"hands\":["); s.tracking.hands.forEachIndexed { n,h -> if(n>0) writer.write(","); writer.write("{\"id\":${q(h.id)},\"landmarks\":["); h.landmarks.forEachIndexed { p,v -> if(p>0) writer.write(","); writer.write("[${v.x},${v.y}]") }; writer.write("]}") }; writer.write("]}") }; writer.write("]"); writer.flush() }
}
class GestureReplayPlayer { fun replay(samples:List<ReplaySample>,controller:HandInteractionController=HandInteractionController()):List<MathInteractionFrame> { controller.reset(); return samples.map { controller.processFrame(it.tracking,it.scene) } } }
typealias GestureTrainingRecorder=GestureReplayRecorder
data class HandIntelligenceDebugState(val state:HandIntelligenceState,val processingMicros:Long,val droppedFrames:Long)
'''
for name,content in files.items():
 p=base/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
