package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.workspace.MathModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AR3DGraphNavigationTest {
    @Test
    fun dedicatedModulesSelectMatchingSharedWorkspace() {
        val model = ExplorerViewModel(SavedStateHandle())
        for ((module, mode) in listOf(
            MathModule.ARGraph2D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Graph2D,
            MathModule.ARGraph3D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Graph3D,
            MathModule.ARGeometry2D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Geometry2D,
            MathModule.ARGeometry3D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Geometry3D,
        )) {
            model.open(module)
            assertEquals(module, model.state.module)
            assertEquals(mode, model.arWorkspaceLaunchMode)
        }
    }
    @Test
    fun navigationOpensAR3DGraphAndBackRestoresPriorModule() {
        val model = ExplorerViewModel(SavedStateHandle())
        val prior = model.state.module
        model.open(MathModule.ARGraph3D)
        assertEquals(MathModule.ARGraph3D, model.state.module)
        assertEquals("AR 3D Graph", model.state.module.label)
        model.navigateBackIntent()
        assertEquals(prior, model.state.module)
    }

    @Test
    fun sharedArLaunchesTheSourceWorkspace() {
        val model = ExplorerViewModel(SavedStateHandle())
        for ((source, expected) in listOf(
            MathModule.Graph2D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Graph2D,
            MathModule.Graph3D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Graph3D,
            MathModule.Geometry2D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Geometry2D,
            MathModule.Geometry3D to com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Geometry3D,
        )) {
            model.open(source)
            model.open(MathModule.SpatialAR)
            assertEquals(expected, model.arWorkspaceLaunchMode)
        }
        model.open(MathModule.ARGraph3D)
        assertEquals(com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode.Graph3D, model.arWorkspaceLaunchMode)
    }

    @Test
    fun existingGraphRouteRemainsDistinct() {
        assertNotEquals(MathModule.Graph3D, MathModule.ARGraph3D)
        assertEquals("3D graph", MathModule.Graph3D.label)
    }
}
