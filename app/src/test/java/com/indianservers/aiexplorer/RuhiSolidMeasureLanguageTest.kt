package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiSolidMeasureLanguageTest {
    private fun empty()=WorkspaceState(module=MathModule.Geometry3D,solids=emptyList())
    private fun query(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule=WorkspaceHistoryByModule(),c:RuhiSceneContext=RuhiSceneContext())=RuhiDialogueManager().execute(s,t,h,c)
    @Test fun original25RowsPreserveSlotsAndHaveIndependentFormulaUnitAndTopologyChecks() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="MEASURE_SOLID"};assertEquals(25,rows.size);val errors=mutableListOf<String>()
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val r=query(s,decode(row[1]),h,c);val check=RuhiSolidMeasureOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="${row[0]} ${check.diagnostics}";if(r.status==GraphCommandStatus.APPLIED)assertEquals(if(RuhiSolidMeasureLanguage.decode(r.state.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK)).shape==null)0 else 1,h.forModule(s.module).protocol.size)}
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun unseenDimensionsUseActualNativeBoxAndCircularFormulas() {
        for(radius in listOf(.375,1.75,4.2)){val r=query(empty(),"Calculate volume of a sphere radius $radius");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(4*PI*radius.pow(3)/3,RuhiSolidMeasureLanguage.drawing(r.state)!!.entities.single().quantities.single().value,1e-8);assertTrue(r.state.solids.isEmpty())}
        val box=Solid(SolidType.Cube,2.0,3.0,7.0);assertEquals(42.0,RuhiSolidMeasureMath.quantity(box,"volume").value,1e-9);assertEquals(82.0,RuhiSolidMeasureMath.quantity(box,"surface_area").value,1e-9)
        val topology=RuhiSolidMeasureMath.topology(box);assertEquals(8,topology.vertices);assertEquals(12,topology.edges);assertEquals(6,topology.faces)
    }
    @Test fun ambiguousTargetsRestoreRealChipsAndComparisonRetainsBothChoices() {
        val s=empty().copy(solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0),Solid(SolidType.Sphere,4.0,radius=2.0),Solid(SolidType.Sphere,6.0,radius=3.0)))
        val r=query(s,"Compare volumes of the cube and sphere");assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(2,r.dialogue!!.suggestions.size)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.dialogue,RuhiDialogueManager().restoredView(loaded))
        val cube=query(loaded,r.dialogue!!.suggestions.last().answer);assertEquals(GraphCommandStatus.CLARIFICATION,cube.status);assertEquals(2,cube.dialogue!!.suggestions.size)
        val done=query(cube.state,cube.dialogue!!.suggestions.first().answer);assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);val d=RuhiSolidMeasureLanguage.drawing(done.state)!!;assertEquals(listOf(1,2),d.entities.map{it.index});assertEquals(32*PI/3-27, d.difference!!,1e-8)
        val stale=query(r.state.copy(solids=s.solids.drop(1)),r.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(stale.state).pending)
        assertNull(RuhiDialogueMemory.read(query(r.state,"Never mind").state).pending)
    }
    @Test fun missingDimensionsAccumulateAndInvalidDimensionsNeverFallBackToScene() {
        val first=query(empty(),"Calculate volume of a cylinder");assertEquals(first.message,GraphCommandStatus.CLARIFICATION,first.status);assertTrue(first.message.contains("radius"))
        val radius=query(first.state,"Use measurement value 3");assertEquals(radius.message,GraphCommandStatus.CLARIFICATION,radius.status);assertTrue(radius.message.contains("height"))
        val done=query(radius.state,"7");assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals(63*PI,RuhiSolidMeasureLanguage.drawing(done.state)!!.entities.single().quantities.single().value,1e-8)
        for(v in listOf("-3","0","1e99")){val bad=query(empty(),"Calculate volume of a sphere radius $v");assertEquals(bad.message,GraphCommandStatus.INVALID,bad.status);assertNull(RuhiSolidMeasureLanguage.drawing(bad.state))}
    }
    @Test fun liveDimensionsAndPosesPersistUndoRedoAndStructuralChangesInvalidateReferences() {
        val s=empty().copy(solids=listOf(Solid(SolidType.Cone,6.0,4.0,radius=3.0)));val h=WorkspaceHistoryByModule();val r=query(s,"What is lateral area of this cone?",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        val edited=r.state.copy(solids=listOf(s.solids.single().copy(radius=5.0,height=12.0,position=Vec3(2.0,3.0,4.0))));val d=RuhiSolidMeasureLanguage.drawing(edited)!!;assertEquals(65*PI,d.entities.single().quantities.single().value,1e-8);assertTrue(d.entities.single().segments.any{it.text=="s = 13"})
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(edited),recover=false).state!!;assertEquals(d,RuhiSolidMeasureLanguage.drawing(loaded))
        assertTrue(RuhiSolidMeasureLanguage.drawing(r.state.copy(solids=emptyList()))!!.diagnostic.isNotEmpty())
        assertFalse(h.forModule(s.module).canUndo)
        val literal=query(empty(),"Calculate volume of a sphere radius 3",h);val undo=query(literal.state,"Undo",h);assertNull(RuhiSolidMeasureLanguage.drawing(undo.state));assertNotNull(RuhiSolidMeasureLanguage.drawing(query(undo.state,"Redo",h).state))
    }
}
