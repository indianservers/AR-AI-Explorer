package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.GeometryGestureAction
import com.indianservers.aiexplorer.core.GeometryGesturePolicy
import com.indianservers.aiexplorer.core.GeometryGestureTarget
import org.junit.Assert.*
import org.junit.Test

class GeometryGesturePolicyTest {
    @Test fun junctionsAreTheOnlyResizeTarget() {
        assertEquals(GeometryGestureAction.ResizeFromHandle, GeometryGesturePolicy.action(GeometryGestureTarget.JunctionHandle, 1))
        assertEquals(GeometryGestureAction.MoveShape, GeometryGesturePolicy.action(GeometryGestureTarget.ShapeBody, 1))
        assertEquals(GeometryGestureAction.PanViewport, GeometryGesturePolicy.action(GeometryGestureTarget.EmptyCanvas, 1))
    }

    @Test fun twoFingerViewportZoomRequiresEmptyCanvasTarget() {
        assertEquals(GeometryGestureAction.ZoomViewport, GeometryGesturePolicy.action(GeometryGestureTarget.EmptyCanvas, 2))
        assertFalse(GeometryGesturePolicy.allowsViewportTransform(GeometryGestureTarget.JunctionHandle))
        assertFalse(GeometryGesturePolicy.allowsViewportTransform(GeometryGestureTarget.ShapeBody))
        assertTrue(GeometryGesturePolicy.allowsViewportTransform(GeometryGestureTarget.EmptyCanvas))
        assertEquals(GeometryGestureAction.ResizeFromHandle, GeometryGesturePolicy.action(GeometryGestureTarget.JunctionHandle, 2))
        assertEquals(GeometryGestureAction.MoveShape, GeometryGesturePolicy.action(GeometryGestureTarget.ShapeBody, 2))
    }
}
