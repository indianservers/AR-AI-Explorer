package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiSurfaceLanguageTest {
    private fun empty()=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList(),surfaceExpression="0")
    private fun query(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule=WorkspaceHistoryByModule())=RuhiDialogueManager().execute(s,t,h)
    @Test fun original25SurfaceRowsKeepUnchangedSlotsAndIndependentNativeMeshIdentities() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()});fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="PLOT_SURFACE_3D"};assertEquals(25,rows.size);val errors=mutableListOf<String>()
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(s,decode(row[1]),h,c);val check=RuhiSurfaceOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="${row[0]} ${check.diagnostics.take(6)}";if(r.status==GraphCommandStatus.APPLIED)assertEquals(1,h.forModule(s.module).protocol.size)};assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun unseenCoefficientsAndIndependentRectangularBoundsProduceNewCoordinates() {
        for(a in listOf(.375,1.75,4.2)){val r=query(empty(),"Ruhi, could you plot z = $a*x squared + 3xy - 2y squared on the square [-2,1] by [-1,3]");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val l=r.state.surfaceLayers.single();val m=GraphSurfaceGeometry.mesh(l,13);assertEquals(-2.0,m.vertices.minOf{it.x},1e-9);assertEquals(1.0,m.vertices.maxOf{it.x},1e-9);assertEquals(-1.0,m.vertices.minOf{it.y},1e-9);assertEquals(3.0,m.vertices.maxOf{it.y},1e-9);m.vertices.forEach{v->assertEquals(a*v.x*v.x+3*v.x*v.y-2*v.y*v.y,v.z,1e-7)}}
    }
    @Test fun polesBetweenFiniteSamplesNeverGetTrianglesAndRemovableHolesStayContinuous() {
        for(density in listOf(8,13,17,24)){val m=Graph3D().mesh("x/(y-0.37)",-3.0,3.0,density,-2.17,2.83);assertTrue(m.vertices.isNotEmpty());RuhiSurfaceOracle.triangles(m).forEach{t->assertFalse(t.minOf{it.y}<.37&&t.maxOf{it.y}>.37)};m.vertices.forEach{assertEquals(it.x/(it.y-.37),it.z,1e-7)}}
        val sinc=Graph3D().mesh("sin(sqrt(x^2+y^2))/sqrt(x^2+y^2)",density=24);assertEquals(625,sinc.vertices.size);assertTrue(sinc.vertices.all{it.z.isFinite()})
    }
    @Test fun pairedSurfacesAreAtomicPersistAndUndoRedoTogether() {
        val s=empty();val h=WorkspaceHistoryByModule();val r=query(s,"Show two surfaces z = x squared+y squared and z = 4 together",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(2,r.state.surfaceLayers.size);assertEquals(1,h.forModule(s.module).protocol.size)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.state.surfaceLayers,saved.surfaceLayers)
        val undo=query(r.state,"Undo",h);assertTrue(undo.state.surfaceLayers.isEmpty());assertEquals(2,query(undo.state,"Redo",h).state.surfaceLayers.size)
        val full=s.copy(surfaceLayers=(1..23).map{SpatialSurfaceLayer("old-$it","x+y")});val rejected=query(full,"Show two surfaces z = x squared+y squared and z = 4 together");assertEquals(GraphCommandStatus.INVALID,rejected.status);assertEquals(full.surfaceLayers,rejected.state.surfaceLayers)
        val invalid=query(s,"Plot z = sqrt(-1)");assertEquals(GraphCommandStatus.INVALID,invalid.status);assertTrue(invalid.state.surfaceLayers.isEmpty())
    }
    @Test fun missingParametersAskForValuesAndWireframeRemainsNative() {
        val missing=query(empty(),"Plot z = a*x+y");assertEquals(missing.message,GraphCommandStatus.CLARIFICATION,missing.status);assertTrue(missing.message.contains("a"));assertTrue(missing.state.surfaceLayers.isEmpty())
        val wire=query(empty(),"Plot z = sin(x)sin(y) as wireframe");assertEquals(wire.message,GraphCommandStatus.APPLIED,wire.status);assertEquals(SpatialSurfaceRenderMode.Wireframe,wire.state.surfaceLayers.single().renderMode)
        val precedence=query(empty(),"Plot z = x cubed - 3xy squared");assertEquals(precedence.message,GraphCommandStatus.APPLIED,precedence.status);assertEquals("x^3-3*x*y^2",precedence.state.surfaceLayers.single().expression)
    }
}
