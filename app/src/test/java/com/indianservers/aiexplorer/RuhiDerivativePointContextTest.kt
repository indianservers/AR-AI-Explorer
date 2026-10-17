package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiDerivativePointContextTest {
    private fun scene(p:Vec2)=WorkspaceState(module=MathModule.Graph2D,points=listOf(Vec2(-4.0,8.0),p),functions=listOf(FunctionDefinition("opaque,/curve907","Motion907","y=x^2","cyan")),labSessionValues=mapOf(GraphAssistant.selectionKey(MathModule.Graph2D) to "opaque,/curve907"))
    private fun say(s:WorkspaceState,text:String="Show the tangent slope at the selected point",h:WorkspaceHistoryByModule=WorkspaceHistoryByModule())=RuhiDialogueManager().execute(s,text,h,RuhiSceneContext(selectedPointIndex=1,selectedFunctionIds=listOf("opaque,/curve907")))
    @Test fun selectedPointAnnotationFollowsCurrentCoordinatesAfterRestore() {
        val initial=scene(Vec2(1.0,1.0));val r=say(initial)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!
        val moved=loaded.copy(points=loaded.points.mapIndexed{i,p->if(i==1)Vec2(3.0,9.0)else p})
        val d=RuhiDerivativeLanguage.drawing(moved)!!
        assertEquals(d.diagnostic,"",d.diagnostic);assertEquals(Vec2(3.0,9.0),d.points.single())
        assertEquals(initial.functions,moved.functions)
    }
    @Test fun offCurveSelectedPointAsksInsteadOfEvaluatingUnrelatedProjection() {
        val initial=scene(Vec2(2.0,99.0));val h=WorkspaceHistoryByModule();val r=say(initial,h=h)
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status)
        assertTrue(r.message.contains("curve"));assertEquals(initial.points,r.state.points);assertEquals(initial.functions,r.state.functions)
        assertNull(r.state.labSessionValues[RuhiDerivativeLanguage.MARK]);assertTrue(h.forModule(initial.module).protocol.isEmpty())
    }
    @Test fun bothOriginalSelectedPointRequestsUseCurrentCurveAndCurrentPoint() {
        for(text in listOf("Show the tangent slope at the selected point","What is the instantaneous gradient here?"))for(x in listOf(-2.5,.375,3.25)) {
            val s=scene(Vec2(x,x*x));val r=say(s,text)
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val slope=Regex("slope = ([-+0-9.eE]+)").find(r.message)!!.groupValues[1].toDouble();assertEquals(2*x,slope,1e-9)
            assertEquals(s.points,r.state.points);assertEquals(s.functions,r.state.functions)
        }
    }
    @Test fun functionEditOrOffCurveDragSuppressesStalePointUntilItReturnsToCurve() {
        val r=say(scene(Vec2(1.0,1.0)))
        val edited=r.state.copy(functions=r.state.functions.map{it.copy(expression="y=3*x^2")})
        val invalid=RuhiDerivativeLanguage.drawing(edited)!!;assertTrue(invalid.diagnostic.contains("not on"));assertTrue(invalid.points.isEmpty());assertTrue(invalid.helpers.isEmpty())
        val valid=edited.copy(points=edited.points.mapIndexed{i,p->if(i==1)Vec2(2.0,12.0)else p})
        val d=RuhiDerivativeLanguage.drawing(valid)!!;assertTrue(d.diagnostic.isEmpty());assertEquals(Vec2(2.0,12.0),d.points.single())
        val derivative=d.helpers.single().curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(12.0,RuhiIntersectionMath.y(derivative,2.0)!!,1e-8)
    }
    @Test fun nativeCompactionRemapsBindingAndDeletedAnchorNeverRetargetsNextPoint() {
        val r=say(scene(Vec2(2.0,4.0)))
        val compact=r.state.copy(points=r.state.points.drop(1),labSessionValues=StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(1 to 0)))
        assertEquals(Vec2(2.0,4.0),RuhiDerivativeLanguage.drawing(compact)!!.points.single())
        val deleted=compact.copy(points=listOf(Vec2(3.0,9.0)),labSessionValues=StudioReferenceMetadata.remapPoints(compact.labSessionValues,emptyMap()))
        val d=RuhiDerivativeLanguage.drawing(deleted)!!;assertTrue(d.diagnostic.contains("missing"));assertTrue(d.points.isEmpty())
        val undone=deleted.copy(points=r.state.points,labSessionValues=StudioReferenceMetadata.restorePointReferences(deleted.labSessionValues,r.state.labSessionValues))
        assertEquals(Vec2(2.0,4.0),RuhiDerivativeLanguage.drawing(undone)!!.points.single())
    }
    @Test fun explicitXRequestsRemainFixedWhileSelectedPointAnnotationsTrack() {
        val s=scene(Vec2(1.0,1.0));val r=say(s,"Draw a tangent to y = x squared at x = 1")
        assertEquals(GraphCommandStatus.APPLIED,r.status)
        val moved=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==1)Vec2(3.0,9.0)else p})
        assertEquals(Vec2(1.0,1.0),RuhiDerivativeLanguage.drawing(moved)!!.points.single())
        assertTrue(RuhiDerivativeLanguage.decode(moved.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).pointKey.isEmpty())
    }
    @Test fun offCurveClarificationRestoresAndAcceptsExplicitXWithoutBindingFalsePoint() {
        val r=say(scene(Vec2(2.0,99.0)));assertEquals(GraphCommandStatus.CLARIFICATION,r.status)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!
        val out=say(loaded,"Use derivative x 3")
        assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 6.0"))
        assertEquals(Vec2(3.0,9.0),RuhiDerivativeLanguage.drawing(out.state)!!.points.single());assertEquals(loaded.points,out.state.points)
        assertTrue(RuhiDerivativeLanguage.decode(out.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).pointKey.isEmpty())
    }
    @Test fun annotationUndoRedoRestoresBindingAndLeavesSourceGeometryUntouched() {
        val s=scene(Vec2(2.0,4.0));val h=WorkspaceHistoryByModule();val r=say(s,h=h)
        assertEquals(1,h.forModule(s.module).protocol.size)
        val undo=say(r.state,"Undo",h);assertNull(undo.state.labSessionValues[RuhiDerivativeLanguage.MARK]);assertFalse(undo.state.labSessionValues.keys.any{it.startsWith(RuhiDerivativeLanguage.POINT_PREFIX)})
        val redo=say(undo.state,"Redo",h);assertEquals(RuhiDerivativeLanguage.drawing(r.state),RuhiDerivativeLanguage.drawing(redo.state));assertEquals(s.points,redo.state.points)
    }
    @Test fun oldVersionOneAnnotationsRetainTheirExplicitCoordinateMeaning() {
        val bytes=java.io.ByteArrayOutputStream();java.io.DataOutputStream(bytes).use{it.writeInt(1);it.writeUTF("literal");it.writeUTF("x^2");it.writeUTF("tangent");it.writeInt(1);it.writeInt(1);it.writeDouble(2.0)}
        val record=java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());val q=RuhiDerivativeLanguage.decode(record)
        assertTrue(q.pointKey.isEmpty());assertNull(q.pointIndex)
        assertEquals(Vec2(2.0,4.0),RuhiDerivativeLanguage.drawing(scene(Vec2(7.0,49.0)).let{it.copy(labSessionValues=it.labSessionValues+(RuhiDerivativeLanguage.MARK to record))})!!.points.single())
    }
}
