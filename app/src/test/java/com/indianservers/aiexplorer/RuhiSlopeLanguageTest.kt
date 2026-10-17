package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class RuhiSlopeLanguageTest {
    private val root:File=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
    @Test fun all25SlopeRowsMatchTheSourceSlotsAndIndependentRiseOverRun() {
        val rows=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="CALCULATE_SLOPE"};assertEquals(25,rows.size)
        val errors=mutableListOf<String>()
        rows.forEach{row->
            val text=String(Base64.getDecoder().decode(row[1]),Charsets.UTF_8);val parameters=RuhiSeedJson.objectValue(String(Base64.getDecoder().decode(row[6]),Charsets.UTF_8))
            val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context();val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager();val r=m.execute(s,text,h,c)
            val check=RuhiSeedBehaviorOracle.check(row[2],parameters,s,r,c);if(!check.verified)errors+="${row[0]}: ${check.diagnostics.joinToString()}"
            if(RuhiSlopeLanguage.marks(r.state).isNotEmpty()) {
                assertEquals(1,h.forModule(s.module).protocol.size)
                val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!;assertEquals(RuhiSlopeLanguage.marks(r.state),RuhiSlopeLanguage.marks(restored))
                val undo=m.execute(r.state,"Undo.",h,c);assertTrue(RuhiSlopeLanguage.marks(undo.state).isEmpty());val redo=m.execute(undo.state,"Redo.",h,c);assertEquals(RuhiSlopeLanguage.marks(r.state),RuhiSlopeLanguage.marks(redo.state))
            }else assertFalse("Read-only slope answer must not add history",h.forModule(s.module).canUndo)
        }
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun verticalAndCoincidentPointsAndNonlinearCurvesAreHandledHonestly() {
        val base=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(1.0,2.0),Vec2(1.0,7.0)),labSessionValues=mapOf("ruhi.studio.point.LeftNode" to "0","ruhi.studio.point.RightNode" to "1"))
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val r=m.execute(base,"Find the slope between LeftNode and RightNode.",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("undefined"));assertFalse(r.message.contains("Infinity"));assertFalse(h.forModule(base.module).canUndo)
        val same=base.copy(points=listOf(Vec2(1.0,2.0),Vec2(1.0,2.0)));assertEquals(GraphCommandStatus.INVALID,m.execute(same,"Find the slope between LeftNode and RightNode.",h).status)
        assertEquals(GraphCommandStatus.INVALID,m.execute(base,"Compute the slope of y = x^2.",h).status);assertEquals(GraphCommandStatus.INVALID,m.execute(base,"Compute the slope of y = 2^999999999.",h).status)
    }
    @Test fun annotationReferencesRemapOrDisappearWhenNativePointsAreRemoved() {
        val before=mapOf("ruhi.studio.point.End" to "2","ruhi.geometry2d.slope.1.2" to "1,2|triangle")
        val remapped=StudioReferenceMetadata.remapPoints(before,mapOf(1 to 0,2 to 1))
        assertEquals("0,1|triangle",remapped["ruhi.geometry2d.slope.0.1"]);assertEquals("1",remapped["ruhi.studio.point.End"])
        val removed=StudioReferenceMetadata.remapPoints(before,mapOf(2 to 0));assertFalse(removed.keys.any{it.startsWith(RuhiSlopeLanguage.PREFIX)})
        assertEquals(before,StudioReferenceMetadata.restorePointReferences(removed,before))
    }
    @Test fun negativeInstructionsCannotCreateMidpointsOrSlopeAnnotations() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D");val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        for(text in listOf("Do not mark the midpoint of AB.","Don't show the slope triangle of AB.","Never annotate the slope of AB.")) {
            val r=m.execute(s,text,h,RuhiSeed500Fixture.context());assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes);assertTrue(RuhiSlopeLanguage.marks(r.state).isEmpty());assertFalse(h.forModule(s.module).canUndo)
        }
    }
}
