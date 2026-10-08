from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt';s=p.read_text(encoding='utf-8').replace('mutableLongStateOf(0L)','androidx.compose.runtime.mutableLongStateOf(0L)').replace('BuildConfig.DEBUG','(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)')
s=s.replace('import com.indianservers.aiexplorer.arengine.contract.ArVector2','import com.indianservers.aiexplorer.arengine.contract.ArVector3\nimport com.indianservers.aiexplorer.arengine.contract.ArVector2',1);p.write_text(s,encoding='utf-8')
p=r/'app/src/test/java/com/indianservers/aiexplorer/IntelligentHandMathEditTest.kt';p.write_text('''package com.indianservers.aiexplorer
import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.handintelligence.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec3 as MathVec3
import org.junit.Assert.*
import org.junit.Test

class IntelligentHandMathEditTest {
    private fun action(id:String,kind:RegionKind=RegionKind.BODY,ops:Set<MathInteraction> = setOf(MathInteraction.TRANSLATE),delta:Vec3=Vec3.Zero,rotation:Vec3=Vec3.Zero,scale:Double=1.0,radial:Double=0.0,axis:Int?=null,sign:Double=1.0)=MathAction(id,SemanticHitRegion("region",kind,index=if(kind==RegionKind.VECTOR_HEAD) 1 else 0,axis=axis,sign=sign,position=Vec3.Zero),ops,SpatialTransformIntent(delta,rotation,scale,radial))
    @Test fun combinedSolidChannelsUseOneBaselineAndUndo() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);val i=vm.state.solids.lastIndex;val base=vm.state.solids[i]
        vm.beginSolidDrag(i);vm.previewIntelligentHand(action("solid-$i",delta=Vec3(1.0,2.0,3.0),rotation=Vec3(10.0,20.0,30.0),scale=2.0),null)
        vm.previewIntelligentHand(action("solid-$i",delta=Vec3(2.0,3.0,4.0),rotation=Vec3(20.0,30.0,40.0),scale=3.0),null)
        val result=vm.state.solids[i];assertEquals(base.position+MathVec3(2.0,3.0,4.0),result.position);assertEquals(base.rotation+MathVec3(20.0,30.0,40.0),result.rotation);assertEquals(base.width*3,result.width,1e-9)
        vm.endSolidDrag();vm.undo();assertEquals(base,vm.state.solids[i]);vm.redo();assertEquals(result,vm.state.solids[i])
    }
    @Test fun cuboidFaceChangesDimensionAndKeepsOppositeFaceFixed() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);val i=vm.state.solids.lastIndex;val b=vm.state.solids[i];vm.beginSolidDrag(i)
        vm.previewIntelligentHand(action("solid-$i",RegionKind.FACE,setOf(MathInteraction.STRETCH_X),Vec3(.5,0.0,0.0),axis=0),null)
        val a=vm.state.solids[i];assertEquals(b.width+.5,a.width,1e-9);assertEquals(b.position.x+.25,a.position.x,1e-9);assertEquals(b.height,a.height,0.0);assertEquals(b.position.x-b.width*.5,a.position.x-a.width*.5,1e-9)
        vm.cancelSolidDrag();assertEquals(b,vm.state.solids[i])
    }
    @Test fun negativeCuboidFacePullExpandsCorrectSide() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Cuboid);val i=vm.state.solids.lastIndex;val b=vm.state.solids[i];vm.beginSolidDrag(i)
        vm.previewIntelligentHand(action("solid-$i",RegionKind.FACE,setOf(MathInteraction.STRETCH_Y),Vec3(0.0,-.5,0.0),axis=1,sign=-1.0),null)
        val a=vm.state.solids[i];assertEquals(b.height+.5,a.height,1e-9);assertEquals(b.position.y-.25,a.position.y,1e-9)
    }
    @Test fun sphereSurfacePullChangesRadiusWithoutMovingCenter() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addSolid(SolidType.Sphere);val i=vm.state.solids.lastIndex;val b=vm.state.solids[i];vm.beginSolidDrag(i)
        vm.previewIntelligentHand(action("solid-$i",RegionKind.SURFACE,setOf(MathInteraction.CHANGE_RADIUS),Vec3(.3,0.0,0.0),scale=1.5,radial=.2),null)
        assertEquals(b.radius*1.5+.2,vm.state.solids[i].radius,1e-9);assertEquals(b.position,vm.state.solids[i].position)
    }
    @Test fun vectorHeadEditKeepsOriginAndUndoRestoresComponents() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addVector3D();val i=vm.state.vectors3D.lastIndex;val b=vm.state.vectors3D[i];vm.beginVectorDrag(i)
        vm.previewIntelligentHand(action("vector-$i",RegionKind.VECTOR_HEAD,setOf(MathInteraction.EDIT_VECTOR_HEAD),Vec3(.2,.3,.4)),null)
        val a=vm.state.vectors3D[i];assertEquals(b.start,a.start);assertEquals(b.end+MathVec3(.2,.3,.4),a.end);assertEquals(a.end-a.start,a.components)
        vm.endVectorDrag();vm.undo();assertEquals(b,vm.state.vectors3D[i])
    }
    @Test fun vectorOriginEditKeepsArrowhead() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.addVector3D();val i=vm.state.vectors3D.lastIndex;val b=vm.state.vectors3D[i];vm.beginVectorDrag(i)
        vm.previewIntelligentHand(action("vector-$i",RegionKind.VECTOR_ORIGIN,setOf(MathInteraction.EDIT_VECTOR_ORIGIN),Vec3(.2,.3,.4)),null)
        assertEquals(b.end,vm.state.vectors3D[i].end);assertEquals(b.start+MathVec3(.2,.3,.4),vm.state.vectors3D[i].start)
    }
    @Test fun cadRadiusEditsAnalyticNodeAndUndo() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.upsertArCadNode(ArCadNode("sphere",ArCadType.Sphere,mapOf("radius" to "1")));val b=vm.state;vm.beginArGraphObjectGesture()
        vm.previewIntelligentHand(action("sphere",RegionKind.SURFACE,setOf(MathInteraction.CHANGE_RADIUS),radial=.4,scale=2.0),null)
        assertEquals(2.4,vm.state.arCadNodes().getValue("sphere").parameters.getValue("radius").toDouble(),1e-9);vm.endArGraphObjectGesture();vm.undo();assertEquals(b,vm.state)
    }
    @Test fun previewProjectionRoundTripsCameraPlane() {
        val mapper=HandPreviewProjection(1080.0,1920.0,-25.0,20.0,60.0,Vec3(1.0,2.0,3.0));val p=Vec3(3.0,-1.0,4.0);val projected=mapper.project(p);val actual=mapper.atDepth(projected,p)
        assertEquals(p.x,actual.x,1e-9);assertEquals(p.y,actual.y,1e-9);assertEquals(p.z,actual.z,1e-9)
    }
    @Test fun inspectorEvaluatesSurfaceAndGradient() {
        val vm=ExplorerViewModel(SavedStateHandle()); val geometry=SpatialGeometry(listOf(MathVec3(0.0,0.0,0.0),MathVec3(2.0,0.0,4.0),MathVec3(0.0,2.0,4.0)),listOf(0,1,2))
        val primitive=SpatialPrimitive("surface",SpatialPrimitiveKind.Surface,geometry,SpatialMaterial("test",listOf(1f,1f,1f,1f)))
        val objects=HandMathSceneAdapter.objects(SpatialRenderScene("scene",listOf(primitive)),com.indianservers.aiexplorer.arengine.interaction.ArSelectionState(),vm.state)
        val values=objects.single().inspect!!(Vec3(1.0,2.0,5.0));assertEquals(5.0,values.getValue("value"),1e-6);assertEquals(2.0,values.getValue("gradient x"),1e-6);assertEquals(4.0,values.getValue("gradient y"),1e-6)
    }
}
''',encoding='utf-8')
