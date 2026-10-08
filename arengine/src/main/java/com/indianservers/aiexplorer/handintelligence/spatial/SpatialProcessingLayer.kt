package com.indianservers.aiexplorer.handintelligence.spatial
import com.indianservers.aiexplorer.handintelligence.*
import kotlin.math.*

class SmartSnapEngine {
    fun point(p:Vec3,scene:MathSceneSnapshot,precision:Boolean,speed:Double):Pair<Vec3,Boolean> {
        if(!precision || speed>.35) return p to false
        val tolerance=.08
        var best=Vec3(round(p.x),round(p.y),round(p.z)); var distance=(best-p).magnitude()
        for(obj in scene.objects.filter { !it.selected }) for(v in obj.vertices.take(256)) { val d=(v-p).magnitude(); if(d<distance) { best=v; distance=d } }
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
    private var gain=1.0; private var baseNormals=emptyList<Vec3>(); private val jitter=com.indianservers.aiexplorer.handintelligence.filtering.JitterClassifier()
    private var baselineMapper:CoordinateMapper?=null; private var translationActive=false; private var rotationActive=false; private var scaleActive=false
    private fun turn(from:Vec3,to:Vec3):com.indianservers.aiexplorer.arengine.contract.ArQuaternion {
        val a=from.unit();val b=to.unit();val dot=a.dot(b).coerceIn(-1.0,1.0)
        if(a.magnitude()<.5 || b.magnitude()<.5) return com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity
        val axis=if(dot<-.9999) a.cross(if(abs(a.x)<.9) Vec3(1.0,0.0,0.0) else Vec3(0.0,1.0,0.0)).unit() else a.cross(b)
        return com.indianservers.aiexplorer.arengine.contract.ArQuaternion(axis.x,axis.y,axis.z,if(dot<-.9999) 0.0 else 1+dot).normalized()
    }
    private fun euler(q:com.indianservers.aiexplorer.arengine.contract.ArQuaternion)=Vec3(
        Math.toDegrees(atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y))),
        Math.toDegrees(asin((2*(q.w*q.y-q.z*q.x)).coerceIn(-1.0,1.0))),
        Math.toDegrees(atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z))))
    fun reset() { ids=emptyList(); baselineMapper=null; current=SpatialTransformIntent(); baseTransform=current; translationActive=false; rotationActive=false; scaleActive=false }
    fun begin(state:HandIntelligenceState,scene:MathSceneSnapshot) { reset(); anchor=state.target?.position ?: Vec3.Zero; baselineMapper=scene.mapper; gain=if(state.precisionMode || scene.profile==IntelligenceProfile.PRECISION) .65 else if(scene.profile==IntelligenceProfile.PLAY) 1.1 else 1.0; rebase(state,scene) }
    private fun involved(state:HandIntelligenceState):List<IntelligentHand> {
        val first=state.hands.firstOrNull { it.id==state.primaryHandId } ?: return emptyList()
        return if(state.twoHandMode==TwoHandMode.COMBINED) listOf(first)+state.hands.filter { it.id==state.supportHandId || (state.supportHandId==null && it.id!=first.id) }.take(1) else listOf(first)
    }
    private fun rebase(state:HandIntelligenceState,scene:MathSceneSnapshot) {
        val hands=involved(state); ids=hands.map { it.id }; basePositions=hands.map { scene.mapper.atDepth(it.filteredPinch,anchor) ?: anchor }; baseAngles=hands.map { it.pose.angle }; baseScales=hands.map { it.pose.handScale }; baseTransform=current; baseNormals=hands.map { it.pose.palmNormal }
    }
    fun update(state:HandIntelligenceState,scene:MathSceneSnapshot):SpatialTransformIntent {
        val hands=involved(state); if(hands.isEmpty()) return current
        if(hands.map { it.id }!=ids) { rebase(state,scene); return current }
        // Retain math-space baselines; project current camera frames through the current adapter.
        val mapper=scene.mapper
        val points=hands.map { mapper.atDepth(it.filteredPinch,anchor) ?: anchor }
        val start=basePositions.reduce(Vec3::plus)*(1.0/basePositions.size); val now=points.reduce(Vec3::plus)*(1.0/points.size)
        val delta=now-start
        var rotation=-angleDelta(hands[0].pose.angle,baseAngles[0]); var scale=1.0
        var interHand=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity
        if(hands.size==2) {
            val before=basePositions[1]-basePositions[0]; val after=points[1]-points[0]
            scale=(after.magnitude()/before.magnitude().coerceAtLeast(.02)).coerceIn(.25,4.0)
            interHand=turn(before,after)
            // Full inter-hand vector rotation, expressed in scene-local Euler axes.
            rotation=angleDelta(Math.toDegrees(atan2(after.y,after.x)),Math.toDegrees(atan2(before.y,before.x)))
            scaleActive=scaleActive || abs(ln(scale))>.015
        }
        translationActive=translationActive || delta.magnitude()>.01
        rotationActive=rotationActive || abs(rotation)>3
        val speed=hands[0].motion.velocity.magnitude()
        // Small deliberate edits retain micro movement; no per-frame gain change may jump a held object.
        if(jitter.classify(hands[0].motion,hands[0].pose.quality)==com.indianservers.aiexplorer.handintelligence.filtering.JitterKind.TRACKING_JITTER) return current
        val translated=baseTransform.translation+if(translationActive) delta*gain else Vec3.Zero
        val n0=baseNormals.firstOrNull() ?: Vec3.Zero; val n1=hands[0].pose.palmNormal
        val normalAngle=Math.toDegrees(acos(n0.dot(n1).coerceIn(-1.0,1.0)))
        val tilt=if(normalAngle>5 && normalAngle<120) turn(n0,n1) else com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity
        val axis=mapper.screenToRay(Vec3(.5,.5,0.0)).direction*(-1.0)
        val half=Math.toRadians(if(rotationActive) rotation else 0.0)*.5
        val spin=if(hands.size==2) interHand else com.indianservers.aiexplorer.arengine.contract.ArQuaternion(axis.x*sin(half),axis.y*sin(half),axis.z*sin(half),cos(half))
        val combined=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(baseTransform.rotation.x,baseTransform.rotation.y,baseTransform.rotation.z)*spin*tilt
        val turned=euler(combined)
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
