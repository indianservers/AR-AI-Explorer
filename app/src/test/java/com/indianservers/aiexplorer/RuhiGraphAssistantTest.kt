package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RuhiGraphAssistantTest {
    @Test fun supplied150CommandsHaveHonestSceneAndMeshOutcomes() {
        assertEquals(150,GraphAuditCases.all.size)
        val outcomes=GraphAuditCases.all.map(GraphAuditRunner::run)
        val report=File("build/reports/ruhi-graph-audit.json").apply { parentFile?.mkdirs() }
        java.io.FileOutputStream(report).use { stream -> stream.write(GraphAuditRunner.json(outcomes).toByteArray(Charsets.UTF_8)); stream.fd.sync() }
        assertTrue(outcomes.filter { it.errors.isNotEmpty() }.joinToString("\n") { "${it.case.id}: ${it.errors}; ${it.result.message}" },outcomes.none { it.errors.isNotEmpty() })
        assertTrue("JVM tests cannot establish rendered output",outcomes.none { it.status=="PASSED" })
    }

    @Test fun commandRoutingDoesNotCaptureLessonOrSolverQuestions() {
        assertFalse(GraphAssistant.isGraphInstruction("Teach me quadratic equations"))
        assertFalse(GraphAssistant.isGraphInstruction("What is a paraboloid?"))
        assertFalse(GraphAssistant.isGraphInstruction("Solve x² = 4"))
        assertTrue(GraphAssistant.isGraphInstruction("Move the cube to (abc, 2, 3)"))
        val result=GraphAssistant().execute(WorkspaceState(module=MathModule.Graph2D),"Could you   plot   y = x²?",CommandHistory())
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
    }

    @Test fun nativeLayerEditsPreserveAssistantSemanticReferences() {
        val assistant=GraphAssistant(); val history=CommandHistory()
        val created=assistant.execute(WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList()),"Create a cube.",history).state
        val controller=UnifiedSpatialMathController()
        val input=created.surfaceLayers.map { it.copy(opacity=.5) }
        val mutation=controller.replaceSurfaceLayers(controller.snapshot(created),input) as UnifiedSpatialMutation.Applied
        val rebound=GraphAssistant.rebindSurfaceContext(input,mutation.snapshot.state)
        assertEquals("cube",rebound.labSessionValues[GraphAssistant.TYPE_PREFIX+rebound.surfaceLayers.single().id])
        val deleted=assistant.execute(rebound,"Delete all cubes.",history)
        assertEquals(deleted.message,GraphCommandStatus.APPLIED,deleted.status)
        assertTrue(deleted.state.surfaceLayers.isEmpty())
    }

    @Test fun editsRetainUnrelatedAuthoritativeLinkedObjects() {
        val custom=UniversalMathObject("custom-note",UniversalMathKind.Text,"Keep this",UniversalMathPayload.Properties(mapOf("text" to "Unrelated studio content")))
        val before=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList(),universalMathDocument=UniversalMathDocument(objects=mapOf(custom.id to custom)))
        val history=CommandHistory()
        val result=GraphAssistant().execute(before,"Plot z = x² + y².",history)
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        assertEquals(custom.payload,result.state.universalMathDocument!!.objects[custom.id]!!.payload)
        assertTrue(result.state.universalMathDocument!!.objects.containsKey("surface-main"))
        val undone = history.undo(result.state).universalMathDocument!!
        assertEquals(custom, undone.objects[custom.id])
        assertFalse(undone.objects.containsKey("surface-main"))
    }

    @Test fun twoDCommandsProduceRealSampledCurvesAndRestoreAppearanceOnUndo() {
        val history=CommandHistory(); val assistant=GraphAssistant()
        var state=WorkspaceState(module=MathModule.Graph2D)
        fun run(text:String):GraphCommandResult=assistant.execute(state,text,history).also { state=it.state; assertEquals(it.message,GraphCommandStatus.APPLIED,it.status) }
        run("Plot y = x².")
        val original=state.functions.single()
        assertTrue(GraphAnalysis().sampleDefinition(original.expression,-3.0,3.0).points.all { kotlin.math.abs(it.y-it.x*it.x)<1e-9 })
        run("Change its color to purple.")
        assertEquals("#800080",state.functions.single().colorKey)
        run("Set the selected object opacity to 50%.")
        assertEquals(.5f,state.functions.single().appearance.opacity)
        val visible=state
        run("Hide the selected object.")
        assertFalse(state.functions.single().visible)
        run("Undo the last action.")
        assertEquals(visible.functions,state.functions)
        run("Redo the undone action.")
        assertFalse(state.functions.single().visible)
        run("Show all hidden objects.")
        run("Hide the coordinate grid.")
        assertFalse(state.graph2DView.gridVisible)
        run("Show the coordinate grid again.")
        assertTrue(state.graph2DView.gridVisible)
        run("Duplicate the selected object.")
        assertEquals(2,state.functions.size)
        assertNotEquals(state.functions[0].expression,state.functions[1].expression)
        run("Delete the first graph.")
        assertEquals(1,state.functions.size)
        run("Clear the graph but keep the coordinate axes.")
        assertTrue(state.functions.isEmpty())
    }

    @Test fun compoundCommandsRollbackAndNeverPolluteHistoryOnFailure() {
        val history=CommandHistory(); val assistant=GraphAssistant()
        val before=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        val failed=assistant.execute(before,"Create a cube, then move it to (abc, 2, 3).",history)
        assertEquals(GraphCommandStatus.INVALID,failed.status)
        assertEquals(before,failed.state)
        assertFalse(history.canUndo)
        val invalid=assistant.execute(before,"Plot z = unknownVariable + 1.",history)
        assertEquals(GraphCommandStatus.INVALID,invalid.status)
        assertEquals(before,invalid.state)
        assertFalse(history.canUndo)
        listOf("Create a cube with radius 3.","Create a sphere with height 3.","Create a cone with side length 3.").forEach { command ->
            val unsupported=assistant.execute(before,command,history)
            assertEquals(GraphCommandStatus.UNSUPPORTED,unsupported.status)
            assertEquals(before,unsupported.state)
        }
    }

    @Test fun ambiguousReferencesAskForOneTargetAndHistoryDoesNotPartiallyUndo() {
        val history=CommandHistory(); val assistant=GraphAssistant()
        var state=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        repeat(2) { state=assistant.execute(state,"Draw a sphere.",history).state }
        state=state.copy(labSessionValues=state.labSessionValues-GraphAssistant.SELECTION)
        val result=assistant.execute(state,"Remove the sphere.",history)
        assertEquals(GraphCommandStatus.CLARIFICATION,result.status)
        assertEquals(state,result.state)
        val missing=assistant.execute(state,"Delete an unknown layer.",history)
        assertEquals(GraphCommandStatus.INVALID,missing.status)
        assertEquals(state,missing.state)
        val depth=history.protocol.size
        val undo=assistant.execute(state,"Undo 10 actions.",history)
        assertEquals(GraphCommandStatus.INVALID,undo.status)
        assertEquals(state,undo.state)
        assertEquals(depth,history.protocol.size)
    }

    @Test fun singularitiesHaveNoFabricatedCellsAndRemovableHolesRemainContinuous() {
        val mesh=Graph3D().mesh("1/(x^2+y^2)",density=24)
        assertTrue(mesh.triangleIndices.isNotEmpty())
        assertTrue(mesh.vertices.all { it.z.isFinite() && it.x*it.x+it.y*it.y>0 })
        mesh.triangleIndices.chunked(3).forEach { indices ->
            val tri=indices.map(mesh.vertices::get)
            assertFalse(tri.minOf { it.x }<0 && tri.maxOf { it.x }>0 && tri.minOf { it.y }<0 && tri.maxOf { it.y }>0)
        }
        val domain=Graph3D().mesh("sqrt(1-x^2-y^2)",density=24)
        assertTrue(domain.triangleIndices.isNotEmpty())
        assertTrue(domain.vertices.all { it.x*it.x+it.y*it.y<=1.0+1e-9 })
        val directional=Graph3D().mesh("x*y/(x^2+y^2)",density=24)
        assertTrue("Axis-only samples must not invent a limit at a direction-dependent singularity",directional.triangleIndices.isNotEmpty())
        val sinc=Graph3D().mesh("sin(sqrt(x^2+y^2))/sqrt(x^2+y^2)",density=24)
        assertEquals(625,sinc.vertices.size)
        assertTrue(sinc.vertices.all { it.z.isFinite() })
    }

    @Test fun parametricSurfacesUseNativeGeometryAndColorSurvivesProjectRoundTrip() {
        val assistant=GraphAssistant(); val history=CommandHistory()
        var state=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=emptyList())
        val result=assistant.execute(state,"Plot x=cos(u)*cos(v); y=sin(u)*cos(v); z=sin(v).",history)
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        state=assistant.execute(result.state,"Make the selected object red.",history).state
        assertEquals("#FF0000",state.surfaceLayers.single().colorHex)
        state=assistant.execute(state,"Remove the highlight.",history).state
        assertEquals("Removing an inactive highlight must preserve the chosen color","#FF0000",state.surfaceLayers.single().colorHex)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!
        assertEquals(state.surfaceLayers,restored.surfaceLayers)
        val nativeAppearance=restored.surfaceLayers.single().workspaceAppearance()
        assertNotNull(nativeAppearance.tint)
        assertEquals("",nativeAppearance.switchPalette(WorkspaceVisualStyles.Aurora).colorHex)
        val mesh=GraphSurfaceGeometry.mesh(restored.surfaceLayers.single())
        assertTrue(mesh.vertices.all { kotlin.math.abs(it.x*it.x+it.y*it.y+it.z*it.z-1)<1e-8 })
    }
    @Test fun graphUndoPreservesOtherModuleEditsAndSelection() {
        val assistant = GraphAssistant(); val three = CommandHistory(); val two = CommandHistory()
        var state = WorkspaceState(module = MathModule.Graph3D, surfaceLayers = emptyList())
        state = assistant.execute(state, "Create a cube.", three).state
        val cube = state.surfaceLayers
        state = assistant.execute(state.copy(module = MathModule.Graph2D), "Plot y = x^2.", two).state
        val functions = state.functions
        val undone = assistant.execute(state.copy(module = MathModule.Graph3D), "Undo.", three)
        assertEquals(undone.message, GraphCommandStatus.APPLIED, undone.status)
        assertEquals(functions, undone.state.functions)
        assertTrue(undone.state.surfaceLayers.isEmpty())
        state = assistant.execute(undone.state, "Redo.", three).state
        assertEquals(cube, state.surfaceLayers)
        state = assistant.execute(state.copy(module = MathModule.Graph2D), "Undo.", two).state
        assertEquals(cube, state.surfaceLayers)
        assertTrue(state.functions.isEmpty())
    }

    @Test fun failedHistoryBatchDoesNotChangeStacks() {
        val history = CommandHistory(); val state = WorkspaceState()
        history.execute(state, GraphWorkspaceCommand(state, state, "blocked"))
        val other = state.copy(module = MathModule.Graph3D)
        history.recordApplied(GraphWorkspaceCommand(other, other, "later"))
        val protocol = history.protocol
        assertTrue(runCatching { history.undoMany(state.copy(module = MathModule.Graph3D), 2) }.isFailure)
        assertEquals(protocol, history.protocol)
        assertFalse(history.canRedo)
    }
}
