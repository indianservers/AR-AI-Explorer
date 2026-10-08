package com.indianservers.aiexplorer.handintelligence

import com.indianservers.aiexplorer.handintelligence.intelligence.HandIntelligenceEngine
import com.indianservers.aiexplorer.handintelligence.spatial.*
import com.indianservers.aiexplorer.handintelligence.semantics.MathSemanticEngine
import org.junit.Assert.*
import org.junit.Test

class HandIntelligenceTest {
    private val mapper=object:CoordinateMapper {
        override fun project(point:Vec3)=Vec3(point.x,point.y,0.0)
        override fun atDepth(normalized:Vec3,reference:Vec3)=Vec3(normalized.x,normalized.y,reference.z)
        override fun screenToRay(normalized:Vec3)=Ray3(Vec3(normalized.x,normalized.y,10.0),Vec3(0.0,0.0,-1.0))
    }
    private fun objectAt(id:String="box",x:Double=.5)=MathObjectSnapshot(id,MathSemanticType.CUBOID,listOf(Vec3(x-.2,.3,0.0),Vec3(x+.2,.3,0.0),Vec3(x+.2,.7,0.0),Vec3(x-.2,.7,0.0)),intArrayOf(0,1,2,0,2,3),selected=true)
    private fun scene(objects:List<MathObjectSnapshot> = listOf(objectAt()))=MathSceneSnapshot(objects,mapper)
    private fun hand(id:String="primary",x:Double=.5,open:Boolean=false,quality:Float=1f):IntelligentHand {
        val p=Vec3(x,.5,0.0); val curls=if(open) 0f else 1f
        return IntelligentHand(HandPoseFeatures(id,Handedness.RIGHT,p,p,p,p,p,p,Vec3(0.0,0.0,1.0),0.0,.1,if(open) .9 else .1,if(open) 0f else 1f,FingerCurlState(curls,curls,curls,curls,curls),if(open) 1f else 0f,if(open) 0f else 1f,0f,quality),TemporalFeatures(stabilityScore=1f,motionConsistency=1f,sampleCount=12),p)
    }
    private fun grabbed(engine:HandIntelligenceEngine,scene:MathSceneSnapshot=scene()):HandIntelligenceState { var state=engine.process(listOf(hand()),scene,0); for(t in 50L..500L step 50) { state=engine.process(listOf(hand()),scene,t); if(state.phase==InteractionPhase.BEGIN) return state }; fail("Stable contact must begin a grab"); return state }
    @Test fun stableContactLocksAndCannotJumpToAnotherObject() { val e=HandIntelligenceEngine(); grabbed(e); val s=e.process(listOf(hand(x=.8)),scene(listOf(objectAt(),objectAt("other",.8))),550); assertEquals("box",s.targetObjectId); assertTrue(s.targetLocked) }
    @Test fun briefOcclusionHoldsThenReleasesOnce() { val e=HandIntelligenceEngine(); val begin=grabbed(e); val time=begin.timestampNanos/1_000_000; assertEquals(InteractionPhase.IDLE,e.process(emptyList(),scene(),time+100).phase); assertEquals(InteractionPhase.END,e.process(emptyList(),scene(),time+220).phase); assertEquals(InteractionPhase.IDLE,e.process(emptyList(),scene(),time+300).phase) }
    @Test fun openReleaseNeedsDwell() { val e=HandIntelligenceEngine(); val start=grabbed(e).timestampNanos/1_000_000; assertEquals(InteractionPhase.IDLE,e.process(listOf(hand(open=true)),scene(),start+50).phase); assertEquals(InteractionPhase.END,e.process(listOf(hand(open=true)),scene(),start+180).phase) }
    @Test fun lowQualityCannotGrab() { val e=HandIntelligenceEngine(); for(t in 0L..1000L step 33) assertFalse(e.process(listOf(hand(quality=.4f)),scene(),t).targetLocked) }
    @Test fun uiOwnsContact() { val e=HandIntelligenceEngine(); val s=scene().copy(uiRegions=listOf(UiRegion(.4,.4,.6,.6))); for(t in 0L..1000L step 33) assertEquals(HandIntent.UI_INTERACTION,e.process(listOf(hand()),s,t).primaryIntent) }
    @Test fun trackingOrDeletedTargetCancels() { val e=HandIntelligenceEngine(); grabbed(e); assertEquals(InteractionPhase.CANCEL,e.process(listOf(hand()),scene(emptyList()),600).phase); val e2=HandIntelligenceEngine(); grabbed(e2); assertEquals(InteractionPhase.CANCEL,e2.process(listOf(hand()),scene().copy(ready=false),600).phase) }
    @Test fun secondHandMustReachSameObjectAndDwell() { val e=HandIntelligenceEngine(); grabbed(e); assertNull(e.process(listOf(hand(),hand("support",.95)),scene(),600).twoHandMode); assertEquals(TwoHandMode.READY,e.process(listOf(hand(),hand("support",.55)),scene(),650).twoHandMode); assertEquals(TwoHandMode.COMBINED,e.process(listOf(hand(),hand("support",.55)),scene(),850).twoHandMode); val s=e.process(listOf(hand()),scene(),900); assertNull(s.twoHandMode); assertEquals("box",s.targetObjectId) }
    @Test fun supportJoiningAndLeavingDoesNotJumpTransform() {
        val target=SpatialTarget("box",SemanticHitRegion("body",RegionKind.BODY,position=Vec3(.5,.5,0.0)),Vec3(.5,.5,0.0),1f)
        fun state(hands:List<IntelligentHand>,two:Boolean=false)=HandIntelligenceState(0,hands,target=target,primaryHandId="primary",twoHandMode=if(two) TwoHandMode.COMBINED else null,phase=InteractionPhase.UPDATE)
        val solver=TransformSolver(); solver.begin(state(listOf(hand())),scene()); val moved=solver.update(state(listOf(hand(x=.6))),scene()); val joined=solver.update(state(listOf(hand(x=.6),hand("support",.7)),true),scene()); assertEquals(moved,joined); val leave=solver.update(state(listOf(hand(x=.6))),scene()); assertEquals(moved,leave)
    }
    @Test fun semanticRadiusAndVectorEndpointsAreTyped() {
        fun resolve(type:MathSemanticType,kind:RegionKind):MathAction? {
            val obj=objectAt().copy(semanticType=type); val region=SemanticHitRegion("region",kind,index=1,position=Vec3(.5,.5,0.0)); val target=SpatialTarget("box",region,region.position,1f)
            val state=HandIntelligenceState(0,listOf(hand()),target=target,phase=InteractionPhase.BEGIN)
            return MathSemanticEngine().resolve(state,SpatialFrame(0,emptyList(),target,null,emptyList(),emptyList(),null,null),scene(listOf(obj))).action
        }
        assertTrue(MathInteraction.CHANGE_RADIUS in resolve(MathSemanticType.SPHERE,RegionKind.SURFACE)!!.interactions)
        assertTrue(MathInteraction.EDIT_VECTOR_HEAD in resolve(MathSemanticType.VECTOR,RegionKind.VECTOR_HEAD)!!.interactions)
    }
    @Test fun facePullInfersDimensionInsteadOfWholeBody() {
        val region=SemanticHitRegion("face",RegionKind.FACE,axis=0,position=Vec3(.7,.5,0.0));val target=SpatialTarget("box",region,region.position,1f)
        val state=HandIntelligenceState(0,listOf(hand()),target=target,phase=InteractionPhase.UPDATE)
        val spatial=SpatialFrame(0,emptyList(),target,null,emptyList(),emptyList(),SpatialTransformIntent(translation=Vec3(.1,0.0,0.0)),null)
        assertEquals(setOf(MathInteraction.STRETCH_X),MathSemanticEngine().resolve(state,spatial,scene()).action!!.interactions)
    }
    @Test fun duplicateTimestampCannotApplyAnUpdateTwice() { val e=HandIntelligenceEngine(); val state=grabbed(e); assertEquals(InteractionPhase.IDLE,e.process(listOf(hand()),scene(),state.timestampNanos/1_000_000).phase) }
    @Test fun projectionAffectsMappingNotSemantics() { val a=HandIntelligenceEngine(); val b=HandIntelligenceEngine(); val camera=grabbed(a,scene().copy(mode="camera")); val ar=grabbed(b,scene().copy(mode="ar")); assertEquals(camera.targetRegionId,ar.targetRegionId); assertEquals(camera.primaryIntent,ar.primaryIntent) }    @Test fun motionCanOutvoteTheStaticGrabPriorWithoutManualTools() {
        val e=HandIntelligenceEngine();grabbed(e)
        val moving=hand().copy(motion=TemporalFeatures(velocity=Vec3(.5,0.0,0.0),stabilityScore=.5f,motionConsistency=1f,sampleCount=20))
        e.process(listOf(moving),scene(),600);assertEquals(HandIntent.MOVE,e.process(listOf(moving),scene(),750).primaryIntent)
        val turning=hand().copy(motion=TemporalFeatures(angularVelocity=120f,stabilityScore=.5f,motionConsistency=1f,sampleCount=30))
        e.process(listOf(turning),scene(),800);assertEquals(HandIntent.ROTATE,e.process(listOf(turning),scene(),950).primaryIntent)
    }
    @Test fun occludedSelectedObjectCannotWinOverFrontSurface() {
        val front=objectAt("front").copy(vertices=objectAt().vertices.map { it+Vec3(0.0,0.0,1.0) },selected=false)
        val back=objectAt("back").copy(selected=true)
        val candidates=ContactSolver().candidates(hand(),scene(listOf(back,front)),"back",300)
        assertEquals("front",candidates.first().objectId);assertTrue(candidates.none { it.objectId=="back" })
    }
    @Test fun armingReportsGrabReadyAndDoesNotCommitAnEarlyContact() { val e=HandIntelligenceEngine();val state=e.process(listOf(hand()),scene(),0);assertEquals(InteractionState.GRAB_READY,state.state);assertFalse(state.targetLocked);assertEquals(InteractionPhase.IDLE,state.phase) }

}
