package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class RuhiDialogueSeedMatrixTest {
    private val root=requireNotNull(generateSequence(File(System.getProperty("user.dir"))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun rows()=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[0].toInt() in 476..500}
    @Test fun allOriginalDialogueRowsUseRealRestoredPrerequisitesAndSourceParameters() {
        val failures=mutableListOf<String>();val rows=rows();assertEquals(25,rows.size)
        rows.forEach{row->
            val id=row[0].toInt();val s=RuhiSeed500Fixture.state(row[3],id);val c=RuhiSeed500Fixture.context(id,s)
            val r=RuhiDialogueManager().execute(s,decode(row[1]),RuhiSeed500Fixture.history(id,s),c)
            val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c)
            if(!check.verified)failures+="$id: ${check.diagnostics}"
        }
        assertTrue(failures.joinToString("\n"),failures.isEmpty())
    }
    @Test fun eightCurrentSceneVariantsPerOriginalUtteranceRecomputeInheritedRoles() {
        val failures=mutableListOf<String>()
        rows().forEach{row->for(variant in 0..7){
            val s=RuhiDialogueSeedFixture.state(row[0].toInt(),variant);val c=RuhiDialogueSeedFixture.context(s)
            val r=RuhiDialogueManager().execute(s,decode(row[1]),WorkspaceHistoryByModule(),c)
            val check=RuhiDialogueSeedOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c)
            if(!check.verified)failures+="${row[0]}/$variant: ${check.diagnostics}"
        }}
        assertTrue(failures.take(40).joinToString("\n"),failures.isEmpty())
    }
    @Test fun eachContextualMutationHasOneUndoWhileExplanationsAndQuestionsHaveNone() {
        val edits=setOf(477,478,479,482,483,484,488,490,491,494,495,496,497,498)
        val readOnly=setOf(476,485,486,487,489,492,493,499,500)
        rows().filter{it[0].toInt() in edits+readOnly}.forEach{row->
            val id=row[0].toInt();val s=RuhiDialogueSeedFixture.state(id);val c=RuhiDialogueSeedFixture.context(s);val h=WorkspaceHistoryByModule()
            val r=RuhiDialogueManager().execute(s,decode(row[1]),h,c)
            assertEquals("Row $id: ${r.message}",if(id in edits)1 else 0,h.forModule(s.module).protocol.size)
            if(id in edits){val restored=h.forModule(s.module).undo(r.state);assertEquals("Undo points $id",s.points,restored.points);assertEquals("Undo shapes $id",s.shapes,restored.shapes);assertEquals("Undo solids $id",s.solids,restored.solids);assertEquals("Undo functions $id",s.functions,restored.functions);if(id==497)assertEquals(GeometryMeasurementUnits.read(s),GeometryMeasurementUnits.read(restored))}
        }
    }
    @Test fun contextlessResponsesCannotPassTheOriginalDialogueOracle() {
        rows().forEach{row->val s=WorkspaceState(module=MathModule.Geometry2D);val r=RuhiDialogueManager().execute(s,decode(row[1]),WorkspaceHistoryByModule());assertFalse(row[0],RuhiDialogueSeedOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,RuhiSceneContext()).verified)}
    }
    @Test fun correctSemanticTagsCannotHideMissingGeometryOrCannedReadonlyAnswers() {
        val edits=setOf(477,478,479,482,483,484,488,490,491,494,495,496,497,498)
        val queries=setOf(492,493,499,500)
        rows().filter{it[0].toInt() in edits+queries}.forEach{row->
            val id=row[0].toInt();val s=RuhiDialogueSeedFixture.state(id);val c=RuhiDialogueSeedFixture.context(s)
            val r=RuhiDialogueManager().execute(s,decode(row[1]),WorkspaceHistoryByModule(),c)
            val fake=if(id in edits)r.copy(state=s)else r.copy(message="The answer is 0.")
            assertFalse("Oracle accepted false applied evidence for $id",RuhiDialogueSeedOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,fake,c).verified)
        }
    }
}
