package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.workspace.MathModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AR3DGraphNavigationTest {
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
    fun existingGraphRouteRemainsDistinct() {
        assertNotEquals(MathModule.Graph3D, MathModule.ARGraph3D)
        assertEquals("3D Graph", MathModule.Graph3D.label)
    }
}
