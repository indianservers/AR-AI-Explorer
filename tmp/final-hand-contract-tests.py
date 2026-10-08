from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/intelligence/IntentScorer.kt';s=p.read_text(encoding='utf-8');a=s.index('    fun update(scores:Map');b=s.index('    fun reset()',a)
s=s[:a]+'''    fun update(scores:Map<HandIntent,Float>,time:Long):HandIntent {
        val choices=setOf(HandIntent.MOVE,HandIntent.ROTATE,HandIntent.RESIZE,HandIntent.STRETCH)
        val motion=choices.maxByOrNull { scores[it] ?: 0f } ?: HandIntent.GRAB
        val best=if((scores[motion] ?: 0f)<.15f) HandIntent.GRAB else motion
        fun evidence(intent:HandIntent)=if(intent==HandIntent.GRAB) .55f else scores[intent] ?: 0f
        if(best!=contender) { contender=best; since=time }
        if(best!=locked && evidence(best)>evidence(locked)+.20f && time-since>=100) locked=best
        return locked
    }
'''+s[b:];p.write_text(s,encoding='utf-8')
p=r/'arengine/src/test/java/com/indianservers/aiexplorer/handintelligence/HandIntelligenceTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}')
s=s[:i]+'''    @Test fun motionCanOutvoteTheStaticGrabPriorWithoutManualTools() {
        val e=HandIntelligenceEngine();grabbed(e)
        val moving=hand().copy(motion=TemporalFeatures(velocity=Vec3(.5,0.0,0.0),stabilityScore=.5f,motionConsistency=1f,sampleCount=20))
        e.process(listOf(moving),scene(),600);assertEquals(HandIntent.MOVE,e.process(listOf(moving),scene(),750).primaryIntent)
        val turning=hand().copy(motion=TemporalFeatures(angularVelocity=120f,stabilityScore=.5f,motionConsistency=1f,sampleCount=30))
        e.process(listOf(turning),scene(),800);assertEquals(HandIntent.ROTATE,e.process(listOf(turning),scene(),950).primaryIntent)
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
p=r/'app/src/test/java/com/indianservers/aiexplorer/IntelligentHandMathEditTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}')
s=s[:i]+'''    @Test fun cuboidVertexEditsThreeDimensionsInsteadOfMovingWholeBody() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);val i=vm.state.solids.lastIndex;val b=vm.state.solids[i];vm.beginSolidDrag(i)
        val target=Vec3(b.position.x+b.width*.5,b.position.y+b.height*.5,b.position.z+b.depth*.5)
        val edit=action("solid-$i",RegionKind.VERTEX,setOf(MathInteraction.MOVE_VERTEX),Vec3(.2,.3,.4)).copy(region=SemanticHitRegion("vertex",RegionKind.VERTEX,index=6,position=target))
        vm.previewIntelligentHand(edit,null);val result=vm.state.solids[i];assertEquals(b.width+.2,result.width,1e-9);assertEquals(b.height+.3,result.height,1e-9);assertEquals(b.depth+.4,result.depth,1e-9);assertEquals(b.position.x+.1,result.position.x,1e-9)
    }
    @Test fun rotatedFacePullUsesLocalDimension() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);val i=vm.state.solids.lastIndex;vm.beginSolidDrag(i);vm.previewSolidRotation(i,MathVec3(0.0,0.0,90.0));vm.endSolidDrag();val b=vm.state.solids[i];vm.beginSolidDrag(i)
        vm.previewIntelligentHand(action("solid-$i",RegionKind.FACE,setOf(MathInteraction.STRETCH_X),Vec3(0.0,.5,0.0),axis=0),null)
        assertEquals(b.width+.5,vm.state.solids[i].width,1e-9);assertEquals(b.height,vm.state.solids[i].height,1e-9);assertEquals(b.position.y+.25,vm.state.solids[i].position.y,1e-9)
    }
    @Test fun vectorBodyCombinesRotationAndScaleAroundItsCenter() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addVector3D(start=MathVec3(0.0,0.0,0.0),end=MathVec3(2.0,0.0,0.0));val i=vm.state.vectors3D.lastIndex;vm.beginVectorDrag(i)
        vm.previewIntelligentHand(action("vector-$i",rotation=Vec3(0.0,0.0,90.0),scale=2.0),null);val v=vm.state.vectors3D[i];assertEquals(4.0,v.magnitude,1e-9);assertEquals(1.0,v.start.x,1e-9);assertEquals(-2.0,v.start.y,1e-9);assertEquals(2.0,v.end.y,1e-9)
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
p=r/'app/src/androidTest/java/com/indianservers/aiexplorer/HandIntelligenceDeviceTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}')
s=s[:i]+'''    @Test fun arProjectionPreservesMathCoordinatesWithAnchorRotationAndScale() {
        val projection=FloatArray(16);android.opengl.Matrix.perspectiveM(projection,0,60f,.6f,.05f,100f)
        val pose=com.indianservers.aiexplorer.arengine.contract.ArPose(Vec3(0.0,0.0,-3.0),com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(10.0,20.0,30.0))
        val placement=com.indianservers.aiexplorer.arengine.contract.ArScenePlacement(anchorPose=pose,localTransform=com.indianservers.aiexplorer.arengine.contract.ArLocalTransform(uniformScale=1.4),metersPerMathUnit=.2)
        val camera=com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot(com.indianservers.aiexplorer.arengine.contract.ArPose(),com.indianservers.aiexplorer.arengine.contract.ArTrackingState.Tracking,projectionMatrix=com.indianservers.aiexplorer.arengine.contract.ArMatrix4(projection.toList()))
        val mapper=HandArProjection(com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot(1,camera),placement)
        val point=Vec3(.4,-.3,.2);val screen=mapper.project(point)!!;val result=mapper.atDepth(screen,point)!!
        assertEquals(point.x,result.x,1e-5);assertEquals(point.y,result.y,1e-5);assertEquals(point.z,result.z,1e-5)
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
