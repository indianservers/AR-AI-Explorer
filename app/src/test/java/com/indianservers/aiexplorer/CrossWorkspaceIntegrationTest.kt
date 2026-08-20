package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SpatialMaterial
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode
import com.indianservers.aiexplorer.workspace.MathModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossWorkspaceIntegrationTest {
    @Test
    fun threeDimensionalContentSurvivesSwitchingAwayAndBack() {
        val viewModel = ExplorerViewModel(SavedStateHandle())
        viewModel.open(MathModule.Geometry3D)
        viewModel.addSolid(SolidType.Cube)
        val scene = viewModel.state.solids

        viewModel.open(MathModule.Geometry2D)
        viewModel.open(MathModule.Graph2D)
        viewModel.open(MathModule.Geometry3D)

        assertEquals(scene, viewModel.state.solids)
    }

    @Test
    fun graph3dLayersSurviveWorkspaceSwitchAndUndoRedoAsOneDocumentEdit() {
        val viewModel = ExplorerViewModel(SavedStateHandle())
        viewModel.open(MathModule.Graph3D)
        val layers = listOf(
            SpatialSurfaceLayer("surface-main", "z = x^2 + y^2"),
            SpatialSurfaceLayer(
                id = "surface-wave",
                expression = "z = sin(x) + cos(y)",
                material = SpatialMaterial.Glass,
                opacity = .45,
                renderMode = SpatialSurfaceRenderMode.Wireframe,
            ),
        )

        viewModel.replaceSurfaceLayers(layers, "Add comparison surface")
        assertEquals(layers, viewModel.state.surfaceLayers)
        assertTrue(viewModel.canUndo)

        viewModel.open(MathModule.Geometry2D)
        viewModel.open(MathModule.Graph3D)
        assertEquals(layers, viewModel.state.surfaceLayers)

        viewModel.undo()
        assertEquals(1, viewModel.state.surfaceLayers.size)
        assertEquals("surface-main", viewModel.state.surfaceLayers.single().id)
        viewModel.redo()
        assertEquals(layers, viewModel.state.surfaceLayers)
        assertTrue(viewModel.state.universalMathDocument!!.objects.keys.containsAll(listOf("surface-main", "surface-wave")))
    }
}
