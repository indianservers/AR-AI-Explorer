package com.indianservers.aiexplorer.handintelligence
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.interaction.*
import com.indianservers.aiexplorer.handintelligence.interaction.HandInteractionController
import com.indianservers.aiexplorer.handintelligence.debug.*
import org.junit.Assert.*
import org.junit.Test

class HandPipelineReplayTest {
    private val mapper=object:CoordinateMapper {
        override fun project(point:Vec3)=Vec3(point.x,point.y,0.0)
        override fun atDepth(normalized:Vec3,reference:Vec3)=Vec3(normalized.x,normalized.y,reference.z)
        override fun screenToRay(normalized:Vec3)=Ray3(Vec3(normalized.x,normalized.y,10.0),Vec3(0.0,0.0,-1.0))
    }
    private val obj=MathObjectSnapshot("box",MathSemanticType.CUBOID,listOf(Vec3(.3,.3,0.0),Vec3(.7,.3,0.0),Vec3(.7,.7,0.0),Vec3(.3,.7,0.0)),intArrayOf(0,1,2,0,2,3))
    private fun hand(x:Float=.5f)=ArTrackedHand("primary",MutableList(21) { ArVector2(x,.5f) }.apply { this[0]=ArVector2(x,.65f);this[9]=ArVector2(x,.55f);this[5]=ArVector2(x-.05f,.55f);this[17]=ArVector2(x+.05f,.55f);this[4]=ArVector2(x-.005f,.5f);this[8]=ArVector2(x+.005f,.5f) },handedness="Right")
    @Test fun rawLandmarksRunAllLayersAndReplayDeterministicallyInBothModes() {
        val recorder=GestureReplayRecorder();recorder.enabled=true;val controller=HandInteractionController();val scene=MathSceneSnapshot(listOf(obj),mapper)
        for(t in 0L..1100L step 33) { val frame=ArHandFrame(t,listOf(hand(if(t<500) .5f else .6f)));val result=controller.processFrame(frame,scene);recorder.record(frame,scene,result,HandIntent.MOVE) }
        val recorded=recorder.snapshot(); assertTrue(recorded.any { it.result.intelligence.phase==InteractionPhase.BEGIN });assertTrue(recorded.any { (it.result.action?.transform?.translation?.magnitude() ?: 0.0)>.03 })
        val replay=GestureReplayPlayer().replay(recorded);assertEquals(recorded.map { it.result },replay)
        val ar=GestureReplayPlayer().replay(recorded.map { it.copy(scene=it.scene.copy(mode="ar")) });assertEquals(replay,ar)
        val out=java.io.StringWriter();recorder.exportJson(out);assertTrue(out.toString().contains("landmarks"));assertTrue(out.toString().contains("MOVE"))
    }
    @Test fun recorderIsBounded() { val recorder=GestureReplayRecorder(3);recorder.enabled=true;val scene=MathSceneSnapshot(listOf(obj),mapper);val controller=HandInteractionController();for(t in 0L..300L step 33) { val f=ArHandFrame(t,listOf(hand()));recorder.record(f,scene,controller.processFrame(f,scene)) };assertEquals(3,recorder.snapshot().size) }
    @Test fun malformedAndAccidentalHandFramesNeverGrab() { val c=HandInteractionController();val scene=MathSceneSnapshot(listOf(obj),mapper);for(t in 0L..1000L step 33) { val h=hand(if(t%2==0L) .05f else .95f);assertFalse(c.processFrame(ArHandFrame(t,listOf(h)),scene).intelligence.targetLocked) }; assertFalse(c.processFrame(ArHandFrame(1100,listOf(ArTrackedHand("bad",emptyList()))),scene).intelligence.targetLocked) }
    @Test fun crossingsUseTrajectoryAndDetectorOrderRatherThanHandedness() {
        val tracker=ArHandIdentityTracker()
        fun h(x:Float)=ArTrackedHand("source",List(21) { ArVector2(x,.5f) },handedness="Right")
        val first=tracker.assign(listOf(h(.3f),h(.7f)),0);tracker.assign(listOf(h(.4f),h(.6f)),100)
        val crossed=tracker.assign(listOf(h(.45f),h(.55f)),250)
        assertEquals(first[0].id,crossed[1].id);assertEquals(first[1].id,crossed[0].id)
        tracker.assign(emptyList(),300);val again=tracker.assign(listOf(h(.35f),h(.65f)),350);assertEquals(first[0].id,again[1].id)
    }
}
