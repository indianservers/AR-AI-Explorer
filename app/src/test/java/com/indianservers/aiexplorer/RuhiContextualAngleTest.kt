package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiContextualAngleTest {
    private fun fixture()=WorkspaceState(module=MathModule.Geometry2D,
        points=listOf(Vec2(4.0,1.0),Vec2(1.0,1.0),Vec2(1.0,6.0),Vec2(20.0,30.0),Vec2(1.0,3.5)),
        shapes=listOf(Shape2D("roof,/907",Shape2DType.Triangle,listOf(0,1,2),"Roof_811")),
        pointDependencies=listOf(PointDependency(4,listOf(1,2),PointDependencyType.Midpoint)),
        solids=listOf(Solid(SolidType.Sphere,2.0)),
        labSessionValues=mapOf(RuhiAngleLanguage.PREFIX+"arc907" to "0,1,2|interior|","ruhi.studio.point.Outer_907" to "0","ruhi.studio.point.Vertex_811" to "1","ruhi.studio.point.Arm_662" to "2"))
    private fun independentDegrees(s:WorkspaceState):Double {
        val u=s.points[0]-s.points[1];val v=s.points[2]-s.points[1]
        return Math.toDegrees(acos(((u.x*v.x+u.y*v.y)/(hypot(u.x,u.y)*hypot(v.x,v.y))).coerceIn(-1.0,1.0)))
    }
    @Test fun originalSixtyDegreeRequestMovesNativeArmAndUpdatesDependentPointWithUndo() {
        val before=fixture();val histories=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        val r=manager.execute(before,"Can we make the angle 60 degrees?",histories,RuhiSceneContext(selectedShapeId="roof,/907"))
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(60.0,independentDegrees(r.state),1e-7)
        assertEquals(before.points.take(2),r.state.points.take(2))
        assertEquals(5.0,r.state.points[1].distanceTo(r.state.points[2]),1e-8)
        assertEquals(before.points[3],r.state.points[3])
        assertEquals((r.state.points[1]+r.state.points[2])*.5,r.state.points[4])
        assertEquals(before.shapes,r.state.shapes);assertEquals(before.solids,r.state.solids)
        assertEquals(before.labSessionValues[RuhiAngleLanguage.PREFIX+"arc907"],r.state.labSessionValues[RuhiAngleLanguage.PREFIX+"arc907"])
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!
        assertEquals(60.0,independentDegrees(saved),1e-7)
        val undone=manager.execute(r.state,"Undo",histories,r.context)
        assertEquals(before.points,undone.state.points)
        assertEquals(60.0,independentDegrees(manager.execute(undone.state,"Redo",histories,r.context).state),1e-7)
    }
    @Test fun differentRequestedAnglesPreserveArmLengthsAndClockwiseSide() {
        for(degrees in listOf(17.5,82.0,134.25))for(side in listOf(-1.0,1.0)) {
            val before=fixture().let{it.copy(points=it.points.mapIndexed{i,p->if(i==2)Vec2(1.0,1+5*side)else p})}.recomputed()
            val r=RuhiDialogueManager().execute(before,"Set the angle $degrees degrees",WorkspaceHistoryByModule())
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            assertEquals(degrees,independentDegrees(r.state),1e-7)
            assertTrue((r.state.points[2].y-1)*side>0)
            assertEquals(5.0,r.state.points[1].distanceTo(r.state.points[2]),1e-8)
        }
    }
    @Test fun dependentAndLockedArmsAreNotSilentlyDetachedOrMoved() {
        val base=fixture()
        val cases=listOf(base.copy(shapes=base.shapes.map{it.copy(locked=true)}),base.copy(pointDependencies=base.pointDependencies+PointDependency(2,listOf(0,1),PointDependencyType.Midpoint)))
        for(s in cases) {
            val r=RuhiDialogueManager().execute(s,"Can we make the angle 60 degrees?",WorkspaceHistoryByModule())
            assertTrue(r.message,r.status in setOf(GraphCommandStatus.INVALID,GraphCommandStatus.CLARIFICATION))
            assertEquals(s.points,r.state.points);assertEquals(s.pointDependencies,r.state.pointDependencies)
        }
    }
    @Test fun actualAngleChoicesSurviveRestorationAndRejectChangedGeometry() {
        val before=fixture().let{it.copy(labSessionValues=it.labSessionValues+(RuhiAngleLanguage.PREFIX+"arc811" to "0,1,3|interior|"))}
        val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
        val q=manager.execute(before,"Can we make the angle 60 degrees?",histories)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        assertEquals(2,q.dialogue!!.suggestions.size)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!
        assertEquals(q.dialogue!!.suggestions,manager.restoredView(saved)!!.suggestions)
        val answer=manager.execute(saved,"Just the first one",histories)
        assertEquals(answer.message,GraphCommandStatus.APPLIED,answer.status)
        assertEquals(60.0,independentDegrees(answer.state),1e-7)
        assertEquals(before.points[3],answer.state.points[3])
        val changed=saved.copy(points=saved.points.mapIndexed{i,p->if(i==0)p+Vec2(1.0,0.0)else p})
        val rejected=manager.execute(changed,q.dialogue!!.suggestions.first().answer,WorkspaceHistoryByModule())
        assertEquals(GraphCommandStatus.CLARIFICATION,rejected.status)
        assertEquals(changed.points,rejected.state.points)
    }
    @Test fun orientedAndExteriorRequestsSetTheMeasuredAngleRatherThanJustItsLabel() {
        for((mode,target) in listOf("oriented" to 270.0,"oriented" to 37.5,"exterior" to 60.0)) {
            val s=fixture().let{it.copy(labSessionValues=it.labSessionValues+(RuhiAngleLanguage.PREFIX+"arc907" to "0,1,2|$mode|"))}
            val r=RuhiDialogueManager().execute(s,"Set the angle $target degrees",WorkspaceHistoryByModule())
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val actual=independentDegrees(r.state)
            assertEquals(if(mode=="exterior")180-target else if(target>180)360-target else target,actual,1e-7)
            assertEquals(target,RuhiAngleLanguage.markDegrees(r.state,RuhiAngleLanguage.marks(r.state).single()),1e-7)
            assertEquals(s.points.take(2),r.state.points.take(2))
        }
    }
    @Test fun impossibleInteriorAnglesAndMissingConstructionDoNotEditOrInventPoints() {
        for(target in listOf(0,180,360,999999)) {
            val s=fixture();val r=RuhiDialogueManager().execute(s,"Set the angle $target degrees",WorkspaceHistoryByModule())
            assertEquals(GraphCommandStatus.INVALID,r.status)
            assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes)
        }
        val s=fixture().copy(labSessionValues=emptyMap())
        val r=RuhiDialogueManager().execute(s,"Can we make the angle 60 degrees?",WorkspaceHistoryByModule())
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status)
        assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes)
    }
    @Test fun incidentalImportedPointSelectionDoesNotHideTheOnlyMarkedAngle() {
        val s=fixture()
        val r=RuhiDialogueManager().execute(s,"Can we make the angle 60 degrees?",WorkspaceHistoryByModule(),RuhiSceneContext(selectedPointIndex=0))
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(60.0,independentDegrees(r.state),1e-7)
        val two=s.copy(labSessionValues=s.labSessionValues+(RuhiAngleLanguage.PREFIX+"other" to "2,3,0|interior|"))
        val chosen=RuhiDialogueManager().execute(two,"Set the angle 60 degrees",WorkspaceHistoryByModule(),RuhiSceneContext(selectedPointIndex=3))
        assertEquals(chosen.message,GraphCommandStatus.APPLIED,chosen.status)
        val selected=RuhiAngleLanguage.marks(chosen.state).single{it.indices[1]==3}
        assertEquals(60.0,RuhiAngleLanguage.markDegrees(chosen.state,selected),1e-7)
        assertEquals(two.points[2],chosen.state.points[2]);assertEquals(two.points[3],chosen.state.points[3])
    }
}
