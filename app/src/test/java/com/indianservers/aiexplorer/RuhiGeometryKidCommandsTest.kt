package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.File
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class RuhiGeometryKidCommandsTest {
    @Test fun all300ReviewedRequestsHaveExpectedSceneOutcomes() {
        val root:File=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-geometry3d-300/replay.tsv").exists()})
        val cases=File(root,"docs/qa/ruhi-geometry3d-300/replay.tsv").readLines().filter{it.isNotBlank()}.map{val p=it.split('\t');RuhiGeometryKidAudit.Case(p[0].toInt(),String(Base64.getDecoder().decode(p[1]),Charsets.UTF_8),p[2])}
        assertEquals((1..300).toList(),cases.map{it.id})
        val records=cases.map(RuhiGeometryKidAudit::run)
        fun quote(s:String)="\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","")+"\""
        val json="{\"total\":300,\"passed\":${records.count{it.errors.isEmpty()}},\"rendering\":\"NOT_RUN\",\"results\":["+records.joinToString{r->"{\"id\":${r.id},\"status\":${quote(r.status)},\"message\":${quote(r.message)},\"errors\":[${r.errors.joinToString{quote(it)}}]}"}+"]}"
        File(root,"app/build/reports/ruhi-geometry3d-300.json").apply{parentFile.mkdirs();writeText(json)}
        assertTrue(records.filter{it.errors.isNotEmpty()}.joinToString("\n"){"${it.id}: ${it.errors.joinToString()}"},records.all{it.errors.isEmpty()})
    }
    @Test fun kidConversationKeepsTargetsAndOtherObjectsUnchanged() {
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule();var s=WorkspaceState(module=MathModule.Geometry3D);var c=RuhiSceneContext()
        fun run(text:String):RuhiExecutionResult {val r=manager.execute(s,text,h,c);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);s=r.state;c=r.context;return r}
        run("Can you draw a cube for me?");run("Draw another one like that.")
        val original=s.solids[0];run("Make the new one smaller.");assertEquals(original,s.solids[0]);assertEquals(1.6,s.solids[1].width,1e-9)
        run("Move it 3 units up the z-axis.");assertEquals(original,s.solids[0]);assertEquals(3.0,s.solids[1].position.z,1e-9)
        run("Turn it another 15 degrees.");assertEquals(15.0,s.solids[1].rotation.y,1e-9)
        run("Color the cube red.");assertEquals("red",s.labSessionValues["ruhi.studio.appearance.1"]);assertFalse(s.labSessionValues.containsKey("ruhi.studio.appearance.0"))
        val decoded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!;assertEquals(s.solids,decoded.solids);assertEquals(s.labSessionValues,decoded.labSessionValues)
    }
    @Test fun invalidDimensionsNeverBecomeDefaultShapesAndQuestionsHaveNoUndoEntry() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val s=RuhiGeometryKidAudit.base();val c=RuhiSceneContext(selectedSolidIndex=0)
        for(text in listOf("Draw a sphere with radius minus 3.","Make a cube with side length zero.","Give this box a height of 1000000000.","Put a 3D point at (2, 3).")) {
            val r=m.execute(s,text,h,c);assertEquals(text,GraphCommandStatus.INVALID,r.status);assertEquals(s.solids,r.state.solids);assertEquals(s.points3D,r.state.points3D);assertFalse(h.forModule(s.module).canUndo)
        }
        val r=m.execute(s,"What is the volume of this cube?",h,c);assertEquals(GraphCommandStatus.APPLIED,r.status);assertFalse(h.forModule(s.module).canUndo)
    }
    @Test fun requestedScaleAndAxisQuestionsWaitForAnswersAndRejectStaleScenes() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val c=RuhiSceneContext(selectedSolidIndex=0);val s=RuhiGeometryKidAudit.base()
        val question=m.execute(s,"Make it bigger, but ask me how much first.",h,c)
        assertEquals(GraphCommandStatus.CLARIFICATION,question.status);assertEquals(s.solids,question.state.solids);assertTrue(question.dialogue!!.pending)
        val invalid=m.execute(question.state,"minus 2",h,c);assertEquals(GraphCommandStatus.CLARIFICATION,invalid.status);assertEquals(s.solids,invalid.state.solids)
        val answer=m.execute(invalid.state,"2",h,c);assertEquals(GraphCommandStatus.APPLIED,answer.status);assertEquals(4.0,answer.state.solids[0].width,1e-9);assertEquals(s.solids.drop(1),answer.state.solids.drop(1))
        val axis=m.execute(s,"Rotate it, but ask me which axis first.",h,c);assertEquals(GraphCommandStatus.CLARIFICATION,axis.status)
        val rotation=m.execute(axis.state,"Z",h,c);assertEquals(GraphCommandStatus.APPLIED,rotation.status);assertEquals(45.0,rotation.state.solids[0].rotation.z,1e-9)
        val changed=axis.state.copy(points3D=axis.state.points3D.dropLast(1));val stale=m.execute(changed,"X",h,c);assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertEquals(s.solids,stale.state.solids)
    }
    @Test fun nativeDeletionCompactsNamesAndSelectionWithTheirObjects() {
        val values=mapOf("ruhi.geometry3d.name.0" to "Removed","ruhi.geometry3d.name.1" to "Ball","ruhi.geometry3d.selection" to "0,1","ruhi.studio.appearance.1" to "blue","ruhi.geometry3d.saved" to "snapshot")
        val remapped=StudioReferenceMetadata.remapSolidAppearance(values,listOf(1))
        assertEquals("Ball",remapped["ruhi.geometry3d.name.0"]);assertFalse(remapped.containsKey("ruhi.geometry3d.name.1"));assertEquals("0",remapped["ruhi.geometry3d.selection"]);assertEquals("blue",remapped["ruhi.studio.appearance.0"]);assertEquals("snapshot",remapped["ruhi.geometry3d.saved"])
    }
}
