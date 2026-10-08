from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/arengine/interaction/ArHandIdentityTracker.kt'
p.write_text('''package com.indianservers.aiexplorer.arengine.interaction
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import kotlin.math.hypot

/** Global assignment against predicted trajectories survives detector ordering and short occlusion. */
class ArHandIdentityTracker {
    private data class Track(val hand:ArTrackedHand,val velocity:ArVector2,val time:Long)
    private val tracks=linkedMapOf<String,Track>(); private var nextId=0
    fun assign(hands:List<ArTrackedHand>,time:Long):List<ArTrackedHand> {
        tracks.entries.removeAll { time-it.value.time>350 }
        val input=hands.take(2); val old=tracks.values.toList()
        fun cost(hand:ArTrackedHand,track:Track):Double {
            val p=hand.landmarks.firstOrNull() ?: return 100.0; val q=track.hand.landmarks.firstOrNull() ?: return 100.0
            val dt=((time-track.time)/1000f).coerceIn(0f,.2f)
            val distance=hypot((p.x-q.x-track.velocity.x*dt).toDouble(),(p.y-q.y-track.velocity.y*dt).toDouble())
            return if(distance>.35) 100.0 else distance+if(hand.handedness!=track.hand.handedness) .025 else 0.0
        }
        var best=Double.MAX_VALUE; var assignment=List(input.size) { -1 }
        fun search(index:Int,used:Set<Int>,chosen:List<Int>,total:Double) {
            if(index==input.size) { if(total<best) { best=total; assignment=chosen }; return }
            search(index+1,used,chosen+(-1),total+.4)
            old.indices.filter { it !in used }.forEach { j -> search(index+1,used+j,chosen+j,total+cost(input[index],old[j])) }
        }
        search(0,emptySet(),emptyList(),0.0)
        return input.mapIndexed { i,h ->
            val previous=assignment[i].takeIf { it>=0 }?.let(old::get)
            val id=previous?.hand?.id ?: "tracked-${nextId++}"; val assigned=h.copy(id=id)
            val p=h.landmarks.firstOrNull();val q=previous?.hand?.landmarks?.firstOrNull();val dt=previous?.let { (time-it.time)/1000f } ?: 0f
            val velocity=if(p!=null && q!=null && dt>0) ArVector2((p.x-q.x)/dt,(p.y-q.y)/dt) else ArVector2(0f,0f)
            tracks[id]=Track(assigned,velocity,time); assigned
        }
    }
}
''',encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/Models.kt';s=p.read_text(encoding='utf-8')
s=s.replace('val allowed:Set<MathInteraction> = MathInteraction.entries.toSet()', 'val allowed:Set<MathInteraction> = MathInteraction.entries.toSet(),val inspect:((Vec3)->Map<String,Double>)?=null')
s=s.replace('val mode:String="camera")','val mode:String="camera",val profile:IntelligenceProfile=IntelligenceProfile.BALANCED)')
s=s.replace('val phase:InteractionPhase=InteractionPhase.IDLE)','val phase:InteractionPhase=InteractionPhase.IDLE,val supportHandId:String?=null)')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/intelligence/HandIntelligenceEngine.kt';s=p.read_text(encoding='utf-8')
s=s.replace('private val machine=','private val contacts=com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver()\n    private val machine=')
s=s.replace('primaryHandId=primary ?: hand?.id,phase=phase)','primaryHandId=primary ?: hand?.id,phase=phase,supportHandId=support)')
s=s.replace('com.indianservers.aiexplorer.handintelligence.spatial.ContactSolver().candidates','contacts.candidates')
s=s.replace('confidence>=InteractionThresholds.GRAB_START','confidence>=(if(scene.profile==IntelligenceProfile.PRECISION) .86f else InteractionThresholds.GRAB_START)')
s=s.replace('if(attention.estimate(hand,scene,null)==HandIntent.UI_INTERACTION) { if(locked==null) return result(HandIntent.UI_INTERACTION,InteractionPhase.IDLE,hand,1f,null) }','if(attention.estimate(hand,scene,null)==HandIntent.UI_INTERACTION) return result(HandIntent.UI_INTERACTION,InteractionPhase.IDLE,hand,1f,locked)')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/interaction/HandInteractionController.kt';s=p.read_text(encoding='utf-8').replace('previous?.intelligence?.precisionMode==true','previous?.intelligence?.precisionMode==true || scene.profile==IntelligenceProfile.PRECISION');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/semantics/MathSemanticEngine.kt';s=p.read_text(encoding='utf-8').replace('} else emptyMap()','            obj.inspect?.let { evaluate -> putAll(runCatching { evaluate(p) }.getOrDefault(emptyMap())) }\n        } else emptyMap()');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/ContactSolver.kt';s=p.read_text(encoding='utf-8').replace('if(obj.vertices.size<=256)', 'if(obj.vertices.size<=256 && obj.semanticType !in setOf(MathSemanticType.SPHERE,MathSemanticType.CIRCLE,MathSemanticType.FUNCTION_SURFACE))').replace('if(gap<tolerance*.7 && (hit==null || obj.triangles.isEmpty()))','if(gap<tolerance*.7)').replace('if(obj.triangles.isEmpty()) RegionKind.CURVE else RegionKind.EDGE','if(obj.semanticType==MathSemanticType.CIRCLE) RegionKind.RADIUS else if(obj.triangles.isEmpty()) RegionKind.CURVE else RegionKind.EDGE');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/SpatialProcessingLayer.kt';s=p.read_text(encoding='utf-8')
s=s.replace('private var baselineMapper:CoordinateMapper?=null;', 'private var gain=1.0; private var baseNormals=emptyList<Vec3>(); private val jitter=com.indianservers.aiexplorer.handintelligence.filtering.JitterClassifier()\n    private var baselineMapper:CoordinateMapper?=null;')
s=s.replace('baselineMapper=scene.mapper; rebase(state,scene)','baselineMapper=scene.mapper; gain=if(state.precisionMode || scene.profile==IntelligenceProfile.PRECISION) .65 else if(scene.profile==IntelligenceProfile.PLAY) 1.1 else 1.0; rebase(state,scene)')
s=s.replace('state.hands.filter { it.id!=first.id }.take(1)', 'state.hands.filter { it.id==state.supportHandId || (state.supportHandId==null && it.id!=first.id) }.take(1)')
s=s.replace('baseTransform=current\n', 'baseTransform=current; baseNormals=hands.map { it.pose.palmNormal }\n')
s=s.replace('        val gain=if(state.precisionMode && hands.size==1) .65 else 1.0','        if(jitter.classify(hands[0].motion,hands[0].pose.quality)==com.indianservers.aiexplorer.handintelligence.filtering.JitterKind.TRACKING_JITTER) return current')
s=s.replace('        val turned=baseTransform.rotation+if(rotationActive) Vec3(0.0,0.0,rotation) else Vec3.Zero','''        val n0=baseNormals.firstOrNull() ?: Vec3.Zero; val n1=hands[0].pose.palmNormal
        val normalAxis=n0.cross(n1); val normalAngle=Math.toDegrees(acos(n0.dot(n1).coerceIn(-1.0,1.0)))
        val tilt=if(normalAngle>5 && normalAngle<120 && normalAxis.magnitude()>1e-6) normalAxis.unit()*normalAngle else Vec3.Zero
        val turned=baseTransform.rotation+Vec3(tilt.x,tilt.y,if(rotationActive) rotation else 0.0)''')
s=s.replace('for(obj in scene.objects) for(v', 'for(obj in scene.objects.filter { !it.selected }) for(v')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/arengine/interaction/ArHandLandmarker.kt';s=p.read_text(encoding='utf-8').replace('Bounded on-device inference on the ARCore image stream. No CameraX session or network upload.','Bounded, pooled on-device inference shared by copied CameraX and ARCore images.');p.write_text(s,encoding='utf-8')
