package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.TrigViewport
import com.indianservers.aiexplorer.core.TrigViewportEngine
import com.indianservers.aiexplorer.core.Vec2
import org.junit.Assert.assertEquals
import org.junit.Test

class TrigViewportTest {
    private val epsilon = 1e-7

    @Test fun screenAndContentCoordinatesRoundTrip() {
        val viewport = TrigViewport(2.4f, Vec2(83.0, -41.0))
        val content = Vec2(217.0, 544.0)
        val screen = TrigViewportEngine.contentToScreen(viewport, content, 1080.0, 1920.0)
        val restored = TrigViewportEngine.screenToContent(viewport, screen, 1080.0, 1920.0)
        assertEquals(content.x, restored.x, epsilon)
        assertEquals(content.y, restored.y, epsilon)
    }

    @Test fun pinchKeepsContentUnderCentroidStable() {
        val original = TrigViewport()
        val centroid = Vec2(320.0, 620.0)
        val anchoredContent = TrigViewportEngine.screenToContent(original, centroid, 1080.0, 1920.0)
        val zoomed = TrigViewportEngine.transform(original, 1080.0, 1920.0, centroid, Vec2(0.0, 0.0), 2.5f)
        val projected = TrigViewportEngine.contentToScreen(zoomed, anchoredContent, 1080.0, 1920.0)
        assertEquals(centroid.x, projected.x, epsilon)
        assertEquals(centroid.y, projected.y, epsilon)
    }

    @Test fun twoFingerPanAndZoomUseCurrentCentroidAndRespectBounds() {
        val current = TrigViewport(1.5f, Vec2(20.0, -10.0))
        val centroid = Vec2(700.0, 800.0)
        val delta = Vec2(35.0, -22.0)
        val contentBefore = TrigViewportEngine.screenToContent(current, centroid - delta, 1080.0, 1920.0)
        val changed = TrigViewportEngine.transform(current, 1080.0, 1920.0, centroid, delta, 1.4f)
        val screenAfter = TrigViewportEngine.contentToScreen(changed, contentBefore, 1080.0, 1920.0)
        assertEquals(centroid.x, screenAfter.x, epsilon)
        assertEquals(centroid.y, screenAfter.y, epsilon)
        assertEquals(TrigViewportEngine.maximumZoom, TrigViewportEngine.transform(current, 1080.0, 1920.0, centroid, Vec2(0.0, 0.0), 100f).zoom)
        assertEquals(TrigViewportEngine.minimumZoom, TrigViewportEngine.transform(current, 1080.0, 1920.0, centroid, Vec2(0.0, 0.0), .0001f).zoom)
    }
}
