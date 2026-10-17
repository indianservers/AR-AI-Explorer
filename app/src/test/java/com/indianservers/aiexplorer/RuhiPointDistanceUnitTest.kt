package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class RuhiPointDistanceUnitTest {
    private fun state(module:MathModule,delta:Vec3):WorkspaceState=WorkspaceState(module=module,
        points=listOf(Vec2(0.0,0.0),Vec2(delta.x,delta.y)),points3D=listOf(Point3D("opaque-left","Left_907",Vec3(0.0,0.0,0.0)),Point3D("opaque-right","Right_811",delta)),
        labSessionValues=mapOf("ruhi.studio.point.Left_907" to "0","ruhi.studio.point.Right_811" to "1"))
    private fun query(s:WorkspaceState)=RuhiDialogueManager().execute(s,"Find the distance between Left_907 and Right_811",WorkspaceHistoryByModule())
    @Test fun confirmedCentimeterCounterexampleReturnsConvertedDistanceAndTypedRoles() {
        val initial=state(MathModule.Geometry2D,Vec3(6.0,0.0,0.0));val s=GeometryMeasurementUnits.set(initial,Length.METER,Length.CENTIMETER)
        val r=query(s);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("= 600.0 cm."))
        assertTrue(r.message,r.message.contains("Native-coordinate formula: √((6.0)² + (0.0)²) m."));assertTrue(r.message.contains("1 m = 100.0 cm."))
        assertEquals(6.0,r.semanticFrame!!.parameters["distance_native"]);assertEquals(600.0,r.semanticFrame!!.parameters["distance_display"])
        assertEquals(initial.points,r.state.points);assertEquals(initial.points3D,r.state.points3D)
    }
    @Test fun sameUtteranceUsesCurrentCoordinatesInBothDimensionsAndNeverCubicScaling() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(delta in listOf(Vec3(3.0,4.0,0.0),Vec3(2.0,3.0,6.0),Vec3(-1.5,2.0,-2.5))) {
            val initial=state(module,delta);val s=GeometryMeasurementUnits.set(initial,Length.METER,Length.CENTIMETER)
            val native=sqrt(delta.x*delta.x+delta.y*delta.y+if(module==MathModule.Geometry3D)delta.z*delta.z else 0.0)
            val r=query(s);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            assertEquals(native,(r.semanticFrame!!.parameters["distance_native"] as Number).toDouble(),1e-9)
            assertEquals(native*100,(r.semanticFrame!!.parameters["distance_display"] as Number).toDouble(),1e-7)
            assertEquals(initial.points,r.state.points);assertEquals(initial.points3D,r.state.points3D)
        }
    }
    @Test fun unknownCoordinateUnitsRetainGenericUnitsWithoutAssumingMeters() {
        val r=query(state(MathModule.Geometry3D,Vec3(2.0,3.0,6.0)))
        assertTrue(r.message,r.message.contains("= 7.0 units."));assertFalse(r.message.contains(" cm"));assertEquals("unit",r.semanticFrame!!.parameters["coordinate_unit"])
    }
    @Test fun centimeterBaseToMeterDisplayDoesNotReinterpretInputCoordinates() {
        val initial=state(MathModule.Geometry2D,Vec3(300.0,400.0,0.0));val s=GeometryMeasurementUnits.set(initial,Length.CENTIMETER,Length.METER)
        val r=query(s);assertTrue(r.message,r.message.contains("= 5.0 m."));assertTrue(r.message.contains("1 cm = 0.01 m."))
        assertEquals(500.0,r.semanticFrame!!.parameters["distance_native"]);assertEquals(initial.points,r.state.points)
    }
    @Test fun pointQueryAndExistingSelectedSegmentLabelAgreeAfterSaveAndParentEdit() {
        val initial=state(MathModule.Geometry2D,Vec3(6.0,0.0,0.0)).copy(shapes=listOf(Shape2D("segment,/907",Shape2DType.Segment,listOf(0,1))))
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(GeometryMeasurementUnits.set(initial,Length.METER,Length.CENTIMETER))).state!!
        val edited=saved.copy(points=listOf(Vec2(0.0,0.0),Vec2(3.0,4.0)))
        val r=query(edited);assertTrue(r.message,r.message.contains("= 500.0 cm."))
        val label=GeometrySelectionMeasurements.label(GeometrySelectionMeasurements.measure(edited.points,edited.shapes,setOf(0)),GeometryMeasurementUnits.read(edited),::trim)
        assertTrue(label,label.contains("length 500 cm"));assertTrue(label.contains("slope 1.333333"));assertEquals(edited.points,r.state.points)
    }
    @Test fun unitUndoAndModuleScopeAreObservedByTheNextPointQuery() {
        val initial=state(MathModule.Geometry2D,Vec3(3.0,4.0,0.0));val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val meters=manager.execute(initial,"Use meters for coordinate units",h).state
        val centimeters=manager.execute(meters,"Use centimeters instead",h).state
        assertTrue(query(centimeters).message.contains("= 500.0 cm."))
        val undone=manager.execute(centimeters,"Undo",h).state;assertTrue(query(undone).message.contains("= 5.0 m."))
        assertTrue(query(centimeters.copy(module=MathModule.Geometry3D)).message.contains("= 5.0 units."))
    }
    @Test fun currentDistanceConvertsImmediatelyAfterRestorationAndParentEditWithoutRequery() {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val meters=manager.execute(state(MathModule.Geometry2D,Vec3(6.0,0.0,0.0)),"Use meters for coordinate units",h).state
        val count=h.forModule(meters.module).protocol.size
        val measured=manager.execute(meters,"Find the distance between Left_907 and Right_811",h).state;assertEquals(count,h.forModule(meters.module).protocol.size)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(measured)).state!!
        val edited=saved.copy(points=listOf(Vec2(0.0,0.0),Vec2(3.0,4.0)))
        val r=manager.execute(edited,"Use centimeters instead",h)
        assertTrue(r.message,r.message.contains("Distance Left_907 to Right_811 = 500.0 cm"));assertEquals(edited.points,r.state.points)
        assertEquals(RuhiDistanceContext("Geometry2D","Left_907","Right_811"),RuhiDialogueMemory.read(r.state).distanceContexts["Geometry2D"])
    }
    @Test fun distanceBindingsStaySeparateAcrossWorkspaceModules() {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val two=query(state(MathModule.Geometry2D,Vec3(6.0,0.0,0.0))).state
        val three=manager.execute(two.copy(module=MathModule.Geometry3D),"Find the distance between Left_907 and Right_811",h).state
        assertEquals(2,RuhiDialogueMemory.read(three).distanceContexts.size)
        val known=GeometryMeasurementUnits.set(three.copy(module=MathModule.Geometry2D),Length.METER,Length.CENTIMETER)
        val r=manager.execute(known,"Use centimeters instead",h)
        assertTrue(r.message,r.message.contains("= 600.0 cm"));assertEquals(two.points,r.state.points)
    }
    @Test fun legacySchemaFiveMemoryStillReadsWithoutDistanceBindings() {
        val s=state(MathModule.Geometry2D,Vec3(3.0,4.0,0.0));val bytes=java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(bytes).use{o->o.writeInt(5);listOf(s.id,s.module.name,"","","","","").forEach(o::writeUTF);o.writeUTF("prior failure");o.writeInt(0);o.writeInt(0);o.writeInt(0);o.writeUTF("");o.writeBoolean(false);o.writeInt(0)}
        val legacy=s.copy(labSessionValues=s.labSessionValues+(RuhiDialogueMemory.KEY to java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())))
        val memory=RuhiDialogueMemory.read(legacy);assertEquals("prior failure",memory.lastError);assertTrue(memory.distanceContexts.isEmpty());assertEquals(s.id,memory.workspace)
    }
    @Test fun lostEndpointDoesNotReuseCachedDistanceDuringUnitConversion() {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val meters=GeometryMeasurementUnits.set(state(MathModule.Geometry2D,Vec3(6.0,0.0,0.0)),Length.METER,Length.METER)
        val measured=query(meters).state;val missing=measured.copy(labSessionValues=measured.labSessionValues-"ruhi.studio.point.Right_811")
        val r=manager.execute(missing,"Use centimeters instead",h)
        assertFalse(r.message,r.message.contains("Distance Left_907"));assertEquals(missing.points,r.state.points)
        assertEquals(GraphCommandStatus.CLARIFICATION,query(r.state).status)
    }
    @Test fun coincidentNamedEndpointsStillProduceZeroCentimeters() {
        val s=GeometryMeasurementUnits.set(state(MathModule.Geometry3D,Vec3(0.0,0.0,0.0)),Length.METER,Length.CENTIMETER)
        val r=query(s);assertEquals(GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("= 0.0 cm."));assertEquals(0.0,r.semanticFrame!!.parameters["distance_display"])
    }
}
