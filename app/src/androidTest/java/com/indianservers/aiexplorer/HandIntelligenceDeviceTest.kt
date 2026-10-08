package com.indianservers.aiexplorer
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
    }    @Test fun arProjectionPreservesMathCoordinatesWithAnchorRotationAndScale() {
        val projection=FloatArray(16);android.opengl.Matrix.perspectiveM(projection,0,60f,.6f,.05f,100f)
        val pose=com.indianservers.aiexplorer.arengine.contract.ArPose(Vec3(0.0,0.0,-3.0),com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(10.0,20.0,30.0))
        val placement=com.indianservers.aiexplorer.arengine.contract.ArScenePlacement(anchorPose=pose,localTransform=com.indianservers.aiexplorer.arengine.contract.ArLocalTransform(uniformScale=1.4),metersPerMathUnit=.2)
        val camera=com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot(com.indianservers.aiexplorer.arengine.contract.ArPose(),com.indianservers.aiexplorer.arengine.contract.ArTrackingState.Tracking,projectionMatrix=com.indianservers.aiexplorer.arengine.contract.ArMatrix4(projection.toList()))
        val mapper=HandArProjection(com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot(1,camera),placement)
        val point=Vec3(.4,-.3,.2);val screen=mapper.project(point)!!;val result=mapper.atDepth(screen,point)!!
        assertEquals(point.x,result.x,1e-5);assertEquals(point.y,result.y,1e-5);assertEquals(point.z,result.z,1e-5)
    }
    @Test fun movingArCameraDoesNotMoveAStationaryMathGrab() {
        val projection=FloatArray(16);android.opengl.Matrix.perspectiveM(projection,0,60f,.6f,.05f,100f)
        val placement=com.indianservers.aiexplorer.arengine.contract.ArScenePlacement(anchorPose=com.indianservers.aiexplorer.arengine.contract.ArPose(Vec3(0.0,0.0,-3.0)),metersPerMathUnit=1.0)
        fun mapper(x:Float):HandArProjection { val view=FloatArray(16);android.opengl.Matrix.setIdentityM(view,0);android.opengl.Matrix.translateM(view,0,-x,0f,0f);val camera=com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot(com.indianservers.aiexplorer.arengine.contract.ArPose(Vec3(x.toDouble(),0.0,0.0)),com.indianservers.aiexplorer.arengine.contract.ArTrackingState.Tracking,viewMatrix=com.indianservers.aiexplorer.arengine.contract.ArMatrix4(view.toList()),projectionMatrix=com.indianservers.aiexplorer.arengine.contract.ArMatrix4(projection.toList()));return HandArProjection(com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot(1,camera),placement) }
        val original=mapper(0f);val shifted=mapper(.2f);val point=Vec3(.4,.1,0.0)
        val pose=HandPoseFeatures("hand",Handedness.RIGHT,Vec3.Zero,Vec3.Zero,Vec3.Zero,Vec3.Zero,Vec3.Zero,Vec3.Zero,Vec3(0.0,0.0,1.0),0.0,.1,.1,1f,FingerCurlState(1f,1f,1f,1f,1f),0f,1f,0f,1f)
        val target=SpatialTarget("object",SemanticHitRegion("body",RegionKind.BODY,position=point),point,1f)
        fun state(screen:Vec3)=HandIntelligenceState(1,listOf(IntelligentHand(pose,TemporalFeatures(),screen)),target=target,primaryHandId="hand",phase=InteractionPhase.UPDATE)
        val solver=com.indianservers.aiexplorer.handintelligence.spatial.TransformSolver();solver.begin(state(original.project(point)!!),MathSceneSnapshot(emptyList(),original,mode="ar"))
        val t=solver.update(state(shifted.project(point)!!),MathSceneSnapshot(emptyList(),shifted,mode="ar"));assertTrue("Camera motion cannot become an authored translation",t.translation.magnitude()<1e-5)
    }

}
