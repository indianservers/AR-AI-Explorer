from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/SpatialProcessingLayer.kt';s=p.read_text(encoding='utf-8')
s=s.replace('(baselineMapper ?: scene.mapper).atDepth(it.filteredPinch,anchor)','scene.mapper.atDepth(it.filteredPinch,anchor)').replace('val mapper=baselineMapper ?: scene.mapper','// Retain math-space baselines; project current camera frames through the current adapter.\n        val mapper=scene.mapper')
s=s.replace('var rotation=angleDelta(hands[0].pose.angle,baseAngles[0])','var rotation=-angleDelta(hands[0].pose.angle,baseAngles[0])')
s=s.replace('val spin=if(hands.size==2) interHand else com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(0.0,0.0,if(rotationActive) rotation else 0.0)','''val axis=mapper.screenToRay(Vec3(.5,.5,0.0)).direction*(-1.0)
        val half=Math.toRadians(if(rotationActive) rotation else 0.0)*.5
        val spin=if(hands.size==2) interHand else com.indianservers.aiexplorer.arengine.contract.ArQuaternion(axis.x*sin(half),axis.y*sin(half),axis.z*sin(half),cos(half))''')
p.write_text(s,encoding='utf-8')
p=r/'app/src/androidTest/java/com/indianservers/aiexplorer/HandIntelligenceDeviceTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}')
s=s[:i]+'''    @Test fun movingArCameraDoesNotMoveAStationaryMathGrab() {
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
'''+s[i:];p.write_text(s,encoding='utf-8')
