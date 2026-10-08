package com.indianservers.aiexplorer.handintelligence.debug
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
    fun exportCsv(writer:Writer) { writer.write("timestamp,intent,confirmedLabel,target,region,confidence,trackingQuality,tx,ty,tz,rx,ry,rz,scale\n"); snapshot().forEach { s -> val i=s.result.intelligence; val t=s.result.action?.transform ?: SpatialTransformIntent()
 fun quote(v:Any?)="\"${v.toString().replace("\"","\"\"")}\""
 writer.write(listOf(s.tracking.timestampMillis,i.primaryIntent,s.label,i.targetObjectId,i.targetRegionId,i.intentConfidence,i.trackingQuality,t.translation.x,t.translation.y,t.translation.z,t.rotation.x,t.rotation.y,t.rotation.z,t.scale).joinToString(",",transform=::quote)+"\n") }; writer.flush() }
    fun exportJson(writer:Writer) {
        fun q(value:Any?)="\"${(value?.toString() ?: "").replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")}\""
        fun vec(p:Vec3)="[${p.x},${p.y},${p.z}]"
        fun features(h:IntelligentHand):String {
            val p=h.pose;val m=h.motion
            return "{\"id\":${q(h.id)},\"handedness\":${q(p.handedness)},\"palm\":${vec(p.palm)},\"pinch\":${vec(p.pinch)},\"filteredPinch\":${vec(h.filteredPinch)},\"pinchDistance\":${p.pinchDistance},\"pinchStrength\":${p.pinchStrength},\"grabStrength\":${p.grabStrength},\"openness\":${p.openness},\"pointScore\":${p.pointScore},\"palmNormal\":${vec(p.palmNormal)},\"handScale\":${p.handScale},\"quality\":${p.quality},\"velocity\":${vec(m.velocity)},\"acceleration\":${vec(m.acceleration)},\"jerk\":${vec(m.jerk)},\"angularVelocity\":${m.angularVelocity},\"pinchVelocity\":${m.pinchVelocity},\"stability\":${m.stabilityScore},\"motionConsistency\":${m.motionConsistency},\"dwellMillis\":${m.dwellMillis}}"
        }
        writer.write("[")
        snapshot().forEachIndexed { index,sample ->
            if(index>0) writer.write(",")
            val state=sample.result.intelligence;val action=sample.result.action;val t=action?.transform ?: SpatialTransformIntent()
            writer.write("{\"timestamp\":${sample.tracking.timestampMillis},\"mode\":${q(sample.scene.mode)},\"profile\":${q(sample.scene.profile)},\"intent\":${q(state.primaryIntent)},\"label\":${q(sample.label)},\"target\":${q(state.targetObjectId)},\"region\":${q(state.targetRegionId)},\"state\":${q(state.state)},\"phase\":${q(state.phase)},\"confidence\":${state.intentConfidence},\"targetConfidence\":${state.targetConfidence},\"trackingQuality\":${state.trackingQuality},\"precision\":${state.precisionMode},\"targetLocked\":${state.targetLocked},\"translation\":${vec(t.translation)},\"rotation\":${vec(t.rotation)},\"scale\":${t.scale},\"radialDelta\":${t.radialDelta},\"snapped\":${t.snapped},\"interactions\":[${action?.interactions?.joinToString(",") { q(it) } ?: ""}],\"features\":[${state.hands.joinToString(",",transform=::features)}],\"hands\":[")
            sample.tracking.hands.forEachIndexed { n,hand ->
                if(n>0) writer.write(",")
                writer.write("{\"id\":${q(hand.id)},\"handedness\":${q(hand.handedness)},\"confidence\":${hand.confidence},\"landmarks\":[${hand.landmarks.joinToString(",") { "[${it.x},${it.y}]" }}],\"worldLandmarks\":[${hand.worldLandmarks.joinToString(",",transform=::vec)}]}")
            }
            writer.write("]}")
        }
        writer.write("]");writer.flush()
    }

}
class GestureReplayPlayer { fun replay(samples:List<ReplaySample>,controller:HandInteractionController=HandInteractionController()):List<MathInteractionFrame> { controller.reset(); return samples.map { controller.processFrame(it.tracking,it.scene) } } }
typealias GestureTrainingRecorder=GestureReplayRecorder
data class HandIntelligenceDebugState(val state:HandIntelligenceState,val processingMicros:Long,val droppedFrames:Long)
