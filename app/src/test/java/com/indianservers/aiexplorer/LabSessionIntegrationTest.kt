package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.assistant.workspace.WorkspaceAssistantContextFactory
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceSnapshotCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabSessionIntegrationTest {
    @Test
    fun labInputsSurviveWorkspaceSwitchAndViewModelRecreation() {
        val handle = SavedStateHandle()
        val first = ExplorerViewModel(handle)
        first.setLabValue(MathModule.CalculusLab, "expression", "sin(x)/x")
        first.setLabValue(MathModule.MathematicalArt, "fractal.depth", "6")

        first.open(MathModule.Graph2D)
        first.open(MathModule.CalculusLab)
        assertEquals("sin(x)/x", first.labValue(MathModule.CalculusLab, "expression", "x"))

        val restored = ExplorerViewModel(handle)
        assertEquals("sin(x)/x", restored.labValue(MathModule.CalculusLab, "expression", "x"))
        assertEquals("6", restored.labValue(MathModule.MathematicalArt, "fractal.depth", "4"))
    }

    @Test
    fun clearOnlyResetsTheActiveLabSession() {
        val model = ExplorerViewModel(SavedStateHandle())
        model.setLabValue(MathModule.CalculusLab, "point", "7")
        model.setLabValue(MathModule.PhysicsMath, "motion.duration", "12")
        model.open(MathModule.CalculusLab)
        val revision = model.labSessionRevision

        model.clearCurrentWorkspace()

        assertEquals("1", model.labValue(MathModule.CalculusLab, "point", "1"))
        assertEquals("12", model.labValue(MathModule.PhysicsMath, "motion.duration", "5"))
        assertEquals(revision + 1, model.labSessionRevision)
    }

    @Test
    fun assistantContextDescribesNewLabCapabilities() {
        val modules = listOf(MathModule.CalculusLab, MathModule.MatricesLinearTransformations, MathModule.PhysicsMath, MathModule.MathematicalArt)

        modules.forEach { module ->
            val context = WorkspaceAssistantContextFactory.from(WorkspaceState(module = module))
            assertEquals(module, context.module)
            assertTrue(context.summaryFacts.size >= 4)
            assertTrue(context.summaryFacts.all { it.startsWith("supports ") })
        }
    }

    @Test
    fun labSessionsRoundTripThroughSnapshotAndProjectFiles() {
        val values = mapOf(
            "CalculusLab::expression" to "sin(x)/x",
            "MatricesLinearTransformations::matrixA" to "1,1/2;0,1",
            "PhysicsMath::motion.duration" to "12.5",
            "MathematicalArt::fractal.depth" to "6",
        )
        val state = WorkspaceState(module = MathModule.MathematicalArt, labSessionValues = values, modifiedAt = 42L)

        val snapshot = WorkspaceSnapshotCodec.decode(WorkspaceSnapshotCodec.encode(state), recover = false).state!!
        val project = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state), recover = false).state!!

        assertEquals(values, snapshot.labSessionValues)
        assertEquals(values, project.labSessionValues)
        val json = WorkspaceJson.export(state)
        assertTrue(json.contains("\"CalculusLab::expression\":\"sin(x)/x\""))
    }

    @Test
    fun legacySnapshotWithoutLabRecordsMigratesToEmptySession() {
        val current = WorkspaceSnapshotCodec.encode(WorkspaceState(labSessionValues = emptyMap()))
        val legacy = current.replaceFirst("AIEXPLORER_WORKSPACE|${WorkspaceSnapshotCodec.currentSchema}|", "AIEXPLORER_WORKSPACE|10|")

        val recovery = WorkspaceSnapshotCodec.decode(legacy, recover = false)

        assertTrue(recovery.recovered)
        assertTrue(recovery.state!!.labSessionValues.isEmpty())
    }

    @Test
    fun importedProjectHydratesLiveLabValues() {
        val model = ExplorerViewModel(SavedStateHandle())
        val imported = WorkspaceState(labSessionValues = mapOf("CalculusLab::point" to "9"))

        model.importWorkspace(imported, recovered = false, diagnostics = emptyList())

        assertEquals("9", model.labValue(MathModule.CalculusLab, "point", "1"))
    }
}
