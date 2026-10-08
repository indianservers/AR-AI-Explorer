from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/test/java/com/indianservers/aiexplorer/handintelligence/HandPipelineReplayTest.kt';p.write_text('''package com.indianservers.aiexplorer.handintelligence
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
''',encoding='utf-8')
p=r/'app/src/androidTest/java/com/indianservers/aiexplorer/ArHandInferenceDeviceTest.kt';s=p.read_text(encoding='utf-8')
s=s.replace('val ready = CountDownLatch(1); val done = CountDownLatch(1)','val ready = CountDownLatch(1); val done = CountDownLatch(1)\n        val frames=java.util.concurrent.LinkedBlockingQueue<ArHandFrame>()')
s=s.replace('{ result.set(it); done.countDown() }','{ result.set(it); frames.offer(it); done.countDown() }')
s=s.replace('            detector.close()\n            assertFalse', '''            val stableIds=result.get().hands.map { it.id }.toSet()
            // Repeated real model inference verifies bitmap reuse and identity continuity.
            frames.clear()
            val durations=mutableListOf<Long>()
            repeat(5) {
                val deadline=SystemClock.uptimeMillis()+5000
                while(!detector.canAcceptFrame && SystemClock.uptimeMillis()<deadline) Thread.sleep(5)
                val start=SystemClock.uptimeMillis()
                detector.submit(ArCameraImage(start,width,height,listOf(ArCameraImagePlane(y,width,1),ArCameraImagePlane(u,cw,1),ArCameraImagePlane(v,cw,1)),listOf(ArVector2(0f,0f),ArVector2(1f,0f),ArVector2(0f,1f))))
                val next=frames.poll(15,TimeUnit.SECONDS);assertNotNull("Repeated inference failed",next);assertEquals(stableIds,next!!.hands.map { it.id }.toSet());durations+=SystemClock.uptimeMillis()-start
                val features=com.indianservers.aiexplorer.handintelligence.features.HandFeatureEngine().process(next);assertEquals(expectedHands,features.size)
            }
            android.util.Log.i("HandInferenceMetrics","hands=$expectedHands repeated model inference milliseconds=$durations")
            detector.close()
            assertFalse''')
p.write_text(s,encoding='utf-8')
p=r/'app/src/androidTest/java/com/indianservers/aiexplorer/HandIntelligenceDeviceTest.kt';p.write_text('''package com.indianservers.aiexplorer
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.handintelligence.interaction.HandInteractionController
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.interaction.*
import com.indianservers.aiexplorer.core.SolidType
import org.junit.Assert.*
import org.junit.Test

class HandIntelligenceDeviceTest {
    @Test fun bothCameraProvidersProduceTheSameRealMathEditAndOneUndo() {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val endings=mutableListOf<com.indianservers.aiexplorer.core.Vec3>();val durations=mutableListOf<Long>()
        for(mode in listOf("camera","ar")) {
            lateinit var vm:ExplorerViewModel;var index=0
            instrumentation.runOnMainSync { vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);index=vm.state.solids.lastIndex }
            val before=vm.state.solids[index]
            val mapper=object:CoordinateMapper {
                override fun project(point:Vec3)=Vec3(point.x,point.y,0.0)
                override fun atDepth(normalized:Vec3,reference:Vec3)=Vec3(normalized.x,normalized.y,reference.z)
                override fun screenToRay(normalized:Vec3)=Ray3(Vec3(normalized.x,normalized.y,10.0),Vec3(0.0,0.0,-1.0))
            }
            val obj=MathObjectSnapshot("solid-$index",MathSemanticType.CUBOID,listOf(Vec3(.3,.3,0.0),Vec3(.7,.3,0.0),Vec3(.7,.7,0.0),Vec3(.3,.7,0.0)),intArrayOf(0,1,2,0,2,3))
            val scene=MathSceneSnapshot(listOf(obj),mapper,mode=mode);val controller=HandInteractionController();var started=false;var updates=0
            for(t in 0L..1100L step 33) {
                val x=if(t<500) .5f else .6f
                val landmarks=MutableList(21) { ArVector2(x,.5f) }.apply { this[0]=ArVector2(x,.65f);this[9]=ArVector2(x,.55f);this[5]=ArVector2(x-.05f,.55f);this[17]=ArVector2(x+.05f,.55f);this[4]=ArVector2(x-.005f,.5f);this[8]=ArVector2(x+.005f,.5f) }
                val start=System.nanoTime();val output=controller.processFrame(ArHandFrame(t,listOf(ArTrackedHand("primary",landmarks,handedness="Right"))),scene);durations+=(System.nanoTime()-start)/1000
                instrumentation.runOnMainSync { when(output.intelligence.phase) { InteractionPhase.BEGIN -> { vm.beginSolidDrag(index);started=true }; InteractionPhase.UPDATE -> { output.action?.let { vm.previewIntelligentHand(it,null);updates++ } }; else -> {} } }
            }
            assertTrue(started);assertTrue(updates>0);assertTrue(vm.state.solids[index].position.x>before.position.x+.03)
            endings+=vm.state.solids[index].position
            instrumentation.runOnMainSync { vm.endSolidDrag();vm.undo() };assertEquals(before,vm.state.solids[index])
        }
        assertEquals(endings[0],endings[1]);android.util.Log.i("HandIntelligenceMetrics","shared pipeline microseconds median=${durations.sorted()[durations.size/2]} max=${durations.maxOrNull()}")
    }
}
''',encoding='utf-8')
