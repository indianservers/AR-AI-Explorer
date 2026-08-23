package com.indianservers.aiexplorer.ar3dgraph.rendering

import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ARGraphRendererTest {
    @Test
    fun phaseOnePlaceholderHasNoGeometryAnchorsOrPlaneFindingAndCleansUp() {
        val renderer = ARGraphRenderer()
        assertEquals(0, renderer.renderedGeometryCount())
        assertFalse(renderer.scene.graphGeometryAttached)
        assertFalse(renderer.scene.planeFindingEnabled)
        assertEquals(0, renderer.scene.anchorsCreated)
        renderer.close()
        assertTrue(renderer.closed)
    }

    @Test
    fun userTransformSnapshotIsAppliedWithoutCreatingAnAnchor() {
        val renderer = ARGraphRenderer()
        renderer.submitTransform(ARGraphTransformState(25f, -15f, 1.8f))
        renderer.onDrawFrame(null)
        assertEquals(25f, renderer.scene.userYawDegrees, 0f)
        assertEquals(-15f, renderer.scene.userPitchDegrees, 0f)
        assertEquals(1.8f, renderer.scene.userScale, 0f)
        assertEquals(0, renderer.scene.anchorsCreated)
    }

    @Test
    fun callsAfterCloseCannotReviveRendererState() {
        val renderer = ARGraphRenderer()
        renderer.close()
        renderer.submitTransform(ARGraphTransformState(45f, 20f, 2f))
        renderer.resetPlacement()
        renderer.clearGraph()
        renderer.requestPlacement(10f, 10f)
        renderer.onDrawFrame(null)
        assertTrue(renderer.closed)
        assertEquals(ARGraphScene(), renderer.scene)
    }
}
